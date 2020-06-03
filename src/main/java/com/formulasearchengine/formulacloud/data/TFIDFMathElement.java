package com.formulasearchengine.formulacloud.data;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class TFIDFMathElement extends MathElement {
    private Map<String, Double> scores;

    public TFIDFMathElement(MathElement parent){
        super(parent);
        scores = new HashMap<>();
    }

    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public void addScore(String docID, double score) {
        this.scores.put(docID, score);
    }

    public double getScore(String docID) {
        return scores.getOrDefault(docID, (double)0);
    }

    @Override
    public String toString(){
        String out = getMoi();
        out += " -> TFIDF Score: " + StringUtils.join(scores);
        out += " [depth: " + getComplexity();
        out += ", TF: " + getLocalTF("any");
        out += ", HDF: " + getGlobalDF();
        out += ", GTF: " + getGlobalTF();
        out += ", GDF: " + getGlobalDF() + "]";
        return out;
    }
}
