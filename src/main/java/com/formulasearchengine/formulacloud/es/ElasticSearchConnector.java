package com.formulasearchengine.formulacloud.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.formulacloud.beans.DocumentSearchResult;
import com.formulasearchengine.formulacloud.beans.DocumentSearchResultMOI;
import com.formulasearchengine.formulacloud.data.MathDocument;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.data.RetrievedMOIDocuments;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * The connection to the elastic searcher server.
 * @author Andre Greiner-Petter
 */
public class ElasticSearchConnector {
    private static final Logger LOG = LogManager.getLogger(ElasticSearchConnector.class.getName());

    private static final String[] INCLUDE_FIELDS = new String[] {"title", "moi"};
    private static final String[] EXCLUDE_FIELDS = new String[] {"content"};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RestHighLevelClient client;
    private ElasticsearchConfig config;

    /**
     * Before the es connection can be used, you need to call
     * {@link #start()} to initiate the connection. Use {@link #stop()}
     * to stop the connection to es.
     * @param config the connection configuration
     */
    public ElasticSearchConnector(ElasticsearchConfig config){
        this.config = config;
    }

    public void setupNewConnection(ElasticsearchConfig config) {
        this.config = config;
        this.start();
    }

    /**
     * Initiates the connection to the server
     */
    public void start(){
        if (client != null){
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
                )
        );
    }

    /**
     * Stops the connection to the server.
     */
    public void stop(){
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
            if ( !index.endsWith("moi") ) index = index + "-moi";
            GetRequest getRequest = new GetRequest(index, md5);
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

            MathElement mathElement = new MathElement(md5);
            updateMathElementFromResponse(mathElement, response);
            return mathElement;
        } catch (IOException e) {
            return null;
        }
    }

    public Map<String, MathElement> getGlobalMathElements(String index, Collection<MathElement> expressions) {
        if ( !index.endsWith("moi") ) index = index + "-moi";
        Map<String, MathElement> md5Mapping = new HashMap<>();
        MultiGetRequest multiGetRequest = new MultiGetRequest();

        for ( MathElement MathElement : expressions ) {
            multiGetRequest.add(index, MathElement.getMoiMD5());
            md5Mapping.put(MathElement.getMoiMD5(), MathElement);
        }

        try {
            MultiGetResponse mgr = client.mget(multiGetRequest, RequestOptions.DEFAULT);
            MultiGetItemResponse[] responses = mgr.getResponses();

            for (MultiGetItemResponse response : responses) {
                String moiMD5 = response.getId();
                MathElement mathElement = md5Mapping.get(moiMD5);
                updateMathElementFromResponse(mathElement, response);
            }
        } catch (IOException ioe) {
            LOG.error("Unable to retrieve multi MOIs from ES.", ioe);
        }
        return md5Mapping;
    }

    private void updateMathElementFromResponse(MathElement mathElement, MultiGetItemResponse response) {
        if ( response.getFailure() != null ) {
            LOG.warn("Unable to retrieve MOI for " + mathElement.getMoiMD5() + "\nReason: "
                    + response.getFailure().getMessage());
        } else updateMathElementFromResponse(mathElement, response.getResponse());
    }

    private void updateMathElementFromResponse(MathElement mathElement, GetResponse response) {
        if ( !response.isExists() ) {
            LOG.warn("Unable to find MOI for: " + mathElement.getMoiMD5());
            return;
        }
        Map<String, Object> fields = response.getSourceAsMap();
        short complexity = Short.parseShort(fields.get("complexity").toString());
        int tf = Integer.parseInt(fields.get("tf").toString());
        int df = Integer.parseInt(fields.get("df").toString());
        String moi = fields.get("moi").toString();

        mathElement.setComplexity(complexity);
        mathElement.setGlobalTermFrequency(tf);
        mathElement.setGlobalDocumentFrequency(df);
        mathElement.setMOI(moi);
    }

    public SearchRequest createEnhancedSearchRequest(SearchConfig config){
        // must match the given searchQuery at least 50% (the half of words must match)
        MatchQueryBuilder matchQB = QueryBuilders.matchQuery("content", config.getSearchQuery());
        matchQB.minimumShouldMatch("50%");

        NestedQueryBuilder nestedQB = QueryBuilders.nestedQuery(
                "moi",
                QueryBuilders.existsQuery("moi.moiMD5"),
                ScoreMode.Max
        );

        // it should match the searchQuery with a given sloppiness
        MatchPhraseQueryBuilder matchPhraseQB = QueryBuilders.matchPhraseQuery("content", config.getSearchQuery());
        matchPhraseQB.slop(10);

        // connect it
        BoolQueryBuilder bqb = QueryBuilders.boolQuery();
        bqb.must(matchQB);
        bqb.must(nestedQB);
        bqb.should(matchPhraseQB);

        SearchSourceBuilder sb = new SearchSourceBuilder();
        sb.query(bqb);
        sb.size(config.getNumberOfDocsToRetrieve());
        sb.fetchSource(INCLUDE_FIELDS, EXCLUDE_FIELDS);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(config.getDb().toESString());
        searchRequest.source(sb);

        return searchRequest;
    }

    public RetrievedMOIDocuments searchDocuments(@NotNull SearchConfig searchConfig) {
        List<MathDocument> mathDocuments = new LinkedList<>();
        Map<String, MathElement> mathElementMap = new HashMap<>();

        LOG.info("Requesting documents from " + searchConfig.getDb());
        SearchRequest request = createEnhancedSearchRequest(searchConfig);
        try {
            SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = searchResponse.getHits().getHits();
            for ( SearchHit hit : hits ) {
                DocumentSearchResult docHit = OBJECT_MAPPER.readValue(hit.getSourceAsString(), DocumentSearchResult.class);
                List<MathElement> mathElementsOfDoc = new LinkedList<>();
                for ( DocumentSearchResultMOI m : docHit.getMois() ) {
                    MathElement currMathElement = mathElementMap.computeIfAbsent(
                            m.getMoiMD5(),
                            (key) -> new MathElement(m.getMoiMD5())
                    );
                    currMathElement.addLocalFrequency(docHit.getTitle(), m.getLocalTF());
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
            getGlobalMathElements(searchConfig.getDb().toESString(), mathElementMap.values());
            mathDocuments.forEach( MathDocument::init );
        } catch (IOException ioe) {
            LOG.fatal("Unexpected error occurred. Returning empty results.", ioe);
        }

        return new RetrievedMOIDocuments(mathDocuments, mathElementMap.values(), searchConfig);
    }

    /**
     * Returns the total number of non-empty documents in the given index.
     * Non-empty means, there must be at least one MOI in the document.
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
