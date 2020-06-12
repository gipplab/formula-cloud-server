package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class TFIDFOptions {
    @JsonProperty("termFrequencyOption")
    private TermFrequencies tfOption;

    @JsonProperty("inverseDocumentFrequency")
    private InverseDocumentFrequencies idfOption;

    @JsonProperty("k1")
    private double k1 = 1.2;

    @JsonProperty("b")
    private double b = 0.95;

    public TFIDFOptions() {
        this.tfOption = TermFrequencies.mBM25;
        this.idfOption = InverseDocumentFrequencies.IDF;
    }

    public TFIDFOptions(TermFrequencies tf, InverseDocumentFrequencies idf){
        this.tfOption = tf;
        this.idfOption = idf;
    }

    public void setTfOption(TermFrequencies tfOption) {
        this.tfOption = tfOption;
    }

    public void setIdfOption(InverseDocumentFrequencies idfOption) {
        this.idfOption = idfOption;
    }

    public double getK1() {
        return k1;
    }

    public void setK1(double k1) {
        this.k1 = k1;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public TermFrequencies getTfOption() {
        return tfOption;
    }

    public InverseDocumentFrequencies getIdfOption() {
        return idfOption;
    }

    public static TFIDFOptions getDefaultTFIDFOption(){
        return new TFIDFOptions();
    }
}
