package com.formulasearchengine.formulacloud.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MathElement {
    @JsonProperty(value = "md5", required = true)
    private String moiMD5;

    @JsonProperty("moi")
    private String moi;

    @JsonProperty("globalDocumentFrequency")
    private int globalDF;

    @JsonProperty("globalTermFrequency")
    private int globalTF;

    @JsonProperty("complexity")
    private short complexity;

    @JsonIgnore
    private final Map<String, Integer> localFrequencies;

    private MathElement() {
        this.localFrequencies = new HashMap<>();
    }

    public MathElement(String moiMD5) {
        this();
        this.moiMD5 = moiMD5;
    }

    public MathElement(String moiMD5, String moi, int globalTF, int globalDF, short complexity) {
        this(moiMD5);
        this.moi = moi;
        this.globalTF = globalTF;
        this.globalDF = globalDF;
        this.complexity = complexity;
    }

    public MathElement(MathElement copy) {
        this.moiMD5 = copy.moiMD5;
        this.moi = copy.moi;
        this.globalTF = copy.globalTF;
        this.globalDF = copy.globalDF;
        this.complexity = copy.complexity;
        this.localFrequencies = copy.localFrequencies;
    }

    public void addLocalFrequency(String docID, int localFrequency) {
        localFrequencies.put(docID, localFrequency);
    }

    public void setMOI(String moi) {
        this.moi = moi;
    }

    public void setGlobalDocumentFrequency(int globalDF) {
        this.globalDF = globalDF;
    }

    public void setGlobalTermFrequency(int globalTF) {
        this.globalTF = globalTF;
    }

    public void setComplexity(short complexity) {
        this.complexity = complexity;
    }

    public String getMoiMD5() {
        return moiMD5;
    }

    public String getMoi() {
        return moi;
    }

    public int getGlobalDF() {
        return globalDF;
    }

    public int getGlobalTF() {
        return globalTF;
    }

    public short getComplexity() {
        return complexity;
    }

    public int getLocalTF(String docID) {
        return localFrequencies.getOrDefault(docID, 0);
    }

    @JsonIgnore
    public Map<String, Integer> getAllLocalTF() {
        return localFrequencies;
    }

    @Override
    public String toString() {


        return moi + " [C: " + getComplexity() +
                "; GTF: " + getGlobalTF() +
                "; GDF: " + getGlobalDF() +
                "; MD5: " + moiMD5 + "]";
    }
}
