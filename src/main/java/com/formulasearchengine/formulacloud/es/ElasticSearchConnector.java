package com.formulasearchengine.formulacloud.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.formulacloud.beans.DocumentSearchResult;
import com.formulasearchengine.formulacloud.beans.DocumentSearchResultMOI;
import com.formulasearchengine.formulacloud.data.*;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The connection to the elastic searcher server.
 *
 * @author Andre Greiner-Petter
 */
public class ElasticSearchConnector {
    private static final Logger LOG = LogManager.getLogger(ElasticSearchConnector.class.getName());

    private static final int MAX_MGET_CHUNK_SIZE = 50_000;
    private static final int REQUEST_PARALLELISM = 1;

    private static final TimeUnit FETCH_MOI_TIMEOUT_TIMEUNIT = TimeUnit.MINUTES;
    private static final int FETCH_MOI_TIMEOUT = 5;

    private static final int SOCKET_TIMEOUT = 360_000;
    private static final int CONNECTION_TIMEOUT = 10_000;

    private static final String[] INCLUDE_FIELDS = new String[]{"title", "internalID", "moi"};
    private static final String[] EXCLUDE_FIELDS = new String[]{"content"};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RestHighLevelClient client;
    private ElasticsearchConfig config;

    /**
     * Before the es connection can be used, you need to call
     * {@link #start()} to initiate the connection. Use {@link #stop()}
     * to stop the connection to es.
     *
     * @param config the connection configuration
     */
    public ElasticSearchConnector(ElasticsearchConfig config) {
        this.config = config;
    }

    public void setupNewConnection(ElasticsearchConfig config) {
        this.config = config;
        this.start();
    }

    /**
     * Initiates the connection to the server
     */
    public void start() {
        if (client != null) {
            LOG.info("Restart elasticsearch connection.");
            stop();
        }

        LOG.info("Setup elasticsearch connection.");
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(
                                config.getElasticsearchHost(),
                                config.getElasticsearchPort(),
                                "http"
                        )
                ).setRequestConfigCallback(
                        new RestClientBuilder.RequestConfigCallback() {
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(
                                    RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(CONNECTION_TIMEOUT)
                                        .setSocketTimeout(SOCKET_TIMEOUT);
                            }
                        })
        );
    }

    /**
     * Stops the connection to the server.
     */
    public void stop() {
        try {
            client.close();
        } catch (IOException e) {
            LOG.error("Cannot close elasticsearch connection.", e);
        }
    }

    public MathElement getMathElement(String index, String expression) {
        String md5 = MOIConverter.getMD5(expression);
        return getMathElementByMD5(index, md5);
    }

    public MathElement getMathElementByMD5(String index, String md5) {
        try {
            if (!index.endsWith("moi")) index = index + "-moi";
            GetRequest getRequest = new GetRequest(index, md5);
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

            MathElement mathElement = new MathElement(md5);
            updateMathElementFromResponse(mathElement, response);
            return mathElement;
        } catch (IOException e) {
            return null;
        }
    }

    public void fetchGlobalMOI(String index, Collection<MathElement> expressions)
            throws IOException {
        if (!index.endsWith("moi")) index = index + "-moi";
        Map<String, MathElement> md5Mapping = new HashMap<>();
        ForkJoinPool requestPool = new ForkJoinPool(REQUEST_PARALLELISM);

        try {
            MultiGetRequest[] mgetArray = new MultiGetRequest[(expressions.size()/(MAX_MGET_CHUNK_SIZE+1))+1];
            int counter = 0;
            int chunkId = 0;
            LOG.info("Fetching " + expressions.size() + " MOI info in " + mgetArray.length + " chunks");

            mgetArray[chunkId] = new MultiGetRequest();
            for (MathElement MathElement : expressions) {
                mgetArray[chunkId].add(index, MathElement.getMoiMD5());
                md5Mapping.put(MathElement.getMoiMD5(), MathElement);
                counter++;

                if ( counter % MAX_MGET_CHUNK_SIZE == 0 ) {
                    chunkId++;
                    mgetArray[chunkId] = new MultiGetRequest();
                }
            }

            for ( int i = 0; i < mgetArray.length; i++ ) {
                final int idx = i;
                requestPool.submit(() -> {
                    try {
                        LOG.info("Reqeuesting chunk " + idx);
                        fetchChunkOfGlobalMOI(mgetArray[idx], md5Mapping);
                        LOG.info("Successfully fetched MOI info of chunk " + idx);
                    } catch (IOException e) {
                        LOG.error("Unable to fetch MOI info for chunk " + idx + ". Reason: " + e.getMessage());
                    }
                });
            }

            requestPool.shutdown();
            requestPool.awaitTermination(FETCH_MOI_TIMEOUT, FETCH_MOI_TIMEOUT_TIMEUNIT);
        } catch (InterruptedException e) {
            LOG.error("Fetching MOI timed out.");
            throw new IOException("Fetching MOI timed out", e);
        }
    }

    private void fetchChunkOfGlobalMOI(MultiGetRequest mgetReq, Map<String, MathElement> md5Mapping) throws IOException {
        MultiGetResponse mgr = client.mget(mgetReq, RequestOptions.DEFAULT);
        MultiGetItemResponse[] responses = mgr.getResponses();

        for (MultiGetItemResponse response : responses) {
            String moiMD5 = response.getId();
            MathElement mathElement = md5Mapping.get(moiMD5);
            updateMathElementFromResponse(mathElement, response);
        }
    }

    private void updateMathElementFromResponse(MathElement mathElement, MultiGetItemResponse response) {
        if (response.getFailure() != null) {
            LOG.debug("Unable to retrieve MOI for " + mathElement.getMoiMD5() + "\nReason: "
                    + response.getFailure().getMessage());
        } else updateMathElementFromResponse(mathElement, response.getResponse());
    }

    private void updateMathElementFromResponse(MathElement mathElement, GetResponse response) {
        if (!response.isExists()) {
            LOG.debug("Unable to find MOI for: " + mathElement.getMoiMD5());
            return;
        }
        Map<String, Object> fields = response.getSourceAsMap();
        short complexity = Short.parseShort(fields.get("complexity").toString());
        int tf = Integer.parseInt(fields.get("tf").toString());
        int df = Integer.parseInt(fields.get("df").toString());
        String moi = fields.get("moi").toString();

        LOG.trace("Retrieved: " + moi);
        mathElement.setComplexity(complexity);
        mathElement.setGlobalTermFrequency(tf);
        mathElement.setGlobalDocumentFrequency(df);
        mathElement.setMOI(moi);
    }

    public SearchRequest createEnhancedARQMathRequest(SearchConfig config) {
        String query = config.getDb().preprocessSearchQuery(config.getSearchQuery());
        MatchQueryBuilder matchQB = QueryBuilders.matchQuery("content", query);
        matchQB.fuzziness(Fuzziness.AUTO);

        NestedQueryBuilder nestedQB = requireMOIQuery();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(matchQB);
        boolQuery.must(nestedQB);

        Pattern p = Pattern.compile("<math.*?>.*</math>");
        Matcher m = p.matcher(query);
        while ( m.find() ) {
            MatchQueryBuilder mathQuery = QueryBuilders.matchQuery("content", m.group(0));
            mathQuery.boost(10f);
//            mathQuery.boost(2f);
            boolQuery.should(mathQuery);
        }

        boolQuery.minimumShouldMatch("50%");

        return buildReqeuest(boolQuery, config);
    }

    public SearchRequest createEnhancedSearchRequest(SearchConfig config) {
        // must match the given searchQuery at least 50% (the half of words must match)
        MatchQueryBuilder matchQB = QueryBuilders.matchQuery("content", config.getSearchQuery());
        matchQB.minimumShouldMatch("50%");

        NestedQueryBuilder nestedQB = requireMOIQuery();

        // it should match the searchQuery with a given sloppiness
        MatchPhraseQueryBuilder matchPhraseQB = QueryBuilders.matchPhraseQuery("content", config.getSearchQuery());
        matchPhraseQB.slop(4);

        // connect it
        BoolQueryBuilder bqb = QueryBuilders.boolQuery();
        bqb.must(matchQB);
        bqb.must(nestedQB);
        bqb.should(matchPhraseQB);

        return buildReqeuest(bqb, config);
    }

    public SearchRequest buildReqeuest(QueryBuilder query, SearchConfig config) {
        SearchSourceBuilder sb = new SearchSourceBuilder();
        sb.query(query);
        sb.size(config.getNumberOfDocsToRetrieve());
        sb.fetchSource(INCLUDE_FIELDS, EXCLUDE_FIELDS);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(config.getDb().toESString());
        searchRequest.source(sb);

        return searchRequest;
    }

    public NestedQueryBuilder requireMOIQuery() {
        return QueryBuilders.nestedQuery(
                "moi",
                QueryBuilders.existsQuery("moi.moiMD5"),
                ScoreMode.Max
        );
    }

    public List<DocumentSearchResult> searchOnlyDocuments(@NotNull SearchConfig searchConfig) throws IOException {
        LOG.info("Requesting documents from " + searchConfig.getDb());
        SearchRequest request = searchConfig.getDb().equals(Databases.ARQMATH) ?
                createEnhancedARQMathRequest(searchConfig) :
                createEnhancedSearchRequest(searchConfig);
        SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        LinkedList<DocumentSearchResult> results = new LinkedList<>();
        for ( SearchHit hit : hits ) {
            DocumentSearchResult docHit = OBJECT_MAPPER.readValue(hit.getSourceAsString(), DocumentSearchResult.class);
            docHit.setSearchScore(hit.getScore());
            results.addLast(docHit);
        }
        return results;
    }

    public RetrievedMOIDocuments searchDocuments(@NotNull SearchConfig searchConfig) throws IOException {
        List<MathDocument> mathDocuments = new LinkedList<>();
        Map<String, MathElement> mathElementMap = new HashMap<>();

        LOG.info("Requesting documents from " + searchConfig.getDb());
        SearchRequest request = searchConfig.getDb().equals(Databases.ARQMATH) ?
                createEnhancedARQMathRequest(searchConfig) :
                createEnhancedSearchRequest(searchConfig);
        SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            DocumentSearchResult docHit = OBJECT_MAPPER.readValue(hit.getSourceAsString(), DocumentSearchResult.class);
            List<MathElement> mathElementsOfDoc = new LinkedList<>();
            for (DocumentSearchResultMOI m : docHit.getMois()) {
                MathElement currMathElement = mathElementMap.computeIfAbsent(
                        m.getMoiMD5(),
                        (key) -> new MathElement(m.getMoiMD5())
                );
                currMathElement.addLocalFrequency(docHit.getTitle(), m.getLocalTF());
                currMathElement.setFormulaID(m.getLocalFormulaIDs());
                mathElementsOfDoc.add(currMathElement);
            }
            MathDocument mdoc = new MathDocument(
                    docHit.getTitle(),
                    hit.getScore(),
                    mathElementsOfDoc
            );
            mathDocuments.add(mdoc);
        }

        LOG.info("Retrieved and analyzed documents. Requesting global MOI information.");
        // ok we have all, now we need to load the global information
        fetchGlobalMOI(searchConfig.getDb().toESString(), mathElementMap.values());
        mathDocuments.forEach(MathDocument::init);

        return new RetrievedMOIDocuments(mathDocuments, mathElementMap.values(), searchConfig);
    }

    /**
     * Returns the total number of non-empty documents in the given index.
     * Non-empty means, there must be at least one MOI in the document.
     *
     * @param index the index, see {@link com.formulasearchengine.formulacloud.data.Databases}
     * @return the number of documents with at least one MOI
     */
    public long getNumberOfDocuments(String index) {
        NestedQueryBuilder nestedQB = QueryBuilders.nestedQuery(
                "moi",
                QueryBuilders.existsQuery("moi.moiMD5"),
                ScoreMode.Max
        );

        BoolQueryBuilder bqb = QueryBuilders.boolQuery();
        bqb.must(nestedQB);

        CountRequest cr = new CountRequest(index);
        cr.query(bqb);

        try {
            CountResponse response = client.count(cr, RequestOptions.DEFAULT);
            return response.getCount();
        } catch (IOException ioe) {
            LOG.error("Unable to retrieve number of documents from ES. " + ioe);
            return -1;
        }
    }
}
