package com.formulasearchengine.formulacloud;

import com.formulasearchengine.formulacloud.data.*;
import com.formulasearchengine.formulacloud.es.ElasticSearchConnector;
import com.formulasearchengine.formulacloud.es.ElasticsearchConfig;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class FormulaCloudSearcher {
    private static final Logger LOG = LogManager.getLogger(FormulaCloudSearcher.class.getName());

    private final ElasticSearchConnector elasticsearch;

    public FormulaCloudSearcher(ElasticsearchConfig config) {
        this.elasticsearch = new ElasticSearchConnector(config);
    }

    public void start() {
        LOG.info("Start elasticsearch client");
        this.elasticsearch.start();
    }

    public void stop() {
        LOG.info("Stop elasticsearch client");
        this.elasticsearch.stop();
    }

    public List<MOIResult> search(SearchConfig searchConfig) {
        RetrievedMOIDocuments retrievedDocs = this.elasticsearch.searchDocuments(searchConfig);
        return retrievedDocs.getOrderedScoredMOIs();
    }

    public String getMathMLFromMOIString(String moiString) {
        return "<math>" + MOIConverter.stringToMML(moiString) + "</math>";
    }

    public String getMOIStringFromMML(String mml) {
        return MOIConverter.mmlToString(mml);
    }

    public MathElement getMOI(String moiString, Databases db) {
        return elasticsearch.getMathElement(db.toESString(), moiString);
    }

    public MathElement getMOIByMD5(String md5, Databases db) {
        return elasticsearch.getMathElementByMD5(db.toESString(), md5);
    }

    public List<Object> searchMOI(String moiString, String index) {
        return null; // TODO
    }

    public List<Object> searchMOIByMML(String moiMML, String index) {
        return null; // TODO
    }

    public static void print(List<?> list, PrintStream out) {
        list.forEach( o -> out.println(o.toString()));
    }

    public static void main(String[] args) {
        ElasticsearchConfig config = ElasticsearchConfig.loadConfig(args);
        FormulaCloudSearcher searcher = new FormulaCloudSearcher(config);
        searcher.start();

        System.out.println("Please enter your search query");
        System.out.print("> ");
        Scanner scanner = new Scanner(System.in);
        String searchQuery = scanner.next();
        List<MOIResult> results = searcher.search( new SearchConfig(searchQuery) );
        FormulaCloudSearcher.print(results, System.out);

        searcher.stop();
    }
}
