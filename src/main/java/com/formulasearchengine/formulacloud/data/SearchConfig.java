package com.formulasearchengine.formulacloud.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.beans.MathMergeFunctions;
import com.formulasearchengine.formulacloud.beans.TFIDFOptions;

/**
 * @author Andre Greiner-Petter
 */
public class SearchConfig {
    @JsonProperty("database")
    private Databases db = Databases.ARQMATH;

    @JsonProperty("minGlobalTF")
    private int minGlobalTF = 1;

    @JsonProperty("minGlobalDF")
    private int minGlobalDF = 1;

    @JsonProperty("minComplexity")
    private int minComplexity = 1;

    @JsonProperty("maxGlobalTF")
    private int maxGlobalTF = Integer.MAX_VALUE;

    @JsonProperty("maxGlobalDF")
    private int maxGlobalDF = Integer.MAX_VALUE;

    @JsonProperty("maxComplexity")
    private int maxComplexity = Integer.MAX_VALUE;

    @JsonProperty("numberOfDocsToRetrieve")
    private int numberOfDocsToRetrieve = 10;

    // minimum number of hits a MOI shall appear in
    @JsonProperty("minNumberOfDocHitsPerMOI")
    private int minNumberOfDocHitsPerMOI = 1;

    @JsonProperty("maxNumberOfResults")
    private int maxNumberOfResults = 10;

    @JsonProperty("tfidfOptions")
    private TFIDFOptions tfidfOptions = TFIDFOptions.getDefaultTFIDFOption();

    @JsonProperty("scoreMerger")
    private MathMergeFunctions scoreMerger = MathMergeFunctions.MAX;

    @JsonProperty("enableMathML")
    private boolean enableMathML = false;

    @JsonProperty("query")
    private final String searchQuery;

    public SearchConfig() {
        this.searchQuery = "";
    }

    public SearchConfig(String query) {
        this.searchQuery = query;
    }

    public Databases getDb() {
        return db;
    }

    public void setDb(Databases db) {
        this.db = db;
    }

    public int getMinGlobalTF() {
        return minGlobalTF;
    }

    public void setMinGlobalTF(int minGlobalTF) {
        this.minGlobalTF = minGlobalTF;
    }

    public int getMinGlobalDF() {
        return minGlobalDF;
    }

    public void setMinGlobalDF(int minGlobalDF) {
        this.minGlobalDF = minGlobalDF;
    }

    public int getMinComplexity() {
        return minComplexity;
    }

    public void setMinComplexity(int minComplexity) {
        this.minComplexity = minComplexity;
    }

    public int getMaxGlobalTF() {
        return maxGlobalTF;
    }

    public void setMaxGlobalTF(int maxGlobalTF) {
        this.maxGlobalTF = maxGlobalTF;
    }

    public int getMaxGlobalDF() {
        return maxGlobalDF;
    }

    public void setMaxGlobalDF(int maxGlobalDF) {
        this.maxGlobalDF = maxGlobalDF;
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public void setMaxComplexity(int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    public int getNumberOfDocsToRetrieve() {
        return numberOfDocsToRetrieve;
    }

    public void setNumberOfDocsToRetrieve(int numberOfDocsToRetrieve) {
        this.numberOfDocsToRetrieve = numberOfDocsToRetrieve;
    }

    public int getMinNumberOfDocHitsPerMOI() {
        return minNumberOfDocHitsPerMOI;
    }

    public void setMinNumberOfDocHitsPerMOI(int minNumberOfDocHitsPerMOI) {
        this.minNumberOfDocHitsPerMOI = minNumberOfDocHitsPerMOI;
    }

    public int getMaxNumberOfResults() {
        return maxNumberOfResults;
    }

    public void setMaxNumberOfResults(int maxNumberOfResults) {
        this.maxNumberOfResults = maxNumberOfResults;
    }

    public TFIDFOptions getTfidfOptions() {
        return tfidfOptions;
    }

    public void setTfidfOptions(TFIDFOptions tfidfOptions) {
        this.tfidfOptions = tfidfOptions;
    }

    public MathMergeFunctions getScoreMerger() {
        return scoreMerger;
    }

    public void setScoreMerger(MathMergeFunctions scoreMerger) {
        this.scoreMerger = scoreMerger;
    }

    public boolean isEnableMathML() {
        return enableMathML;
    }

    public void setEnableMathML(boolean enableMathML) {
        this.enableMathML = enableMathML;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}
