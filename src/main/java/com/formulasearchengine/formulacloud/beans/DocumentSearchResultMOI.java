package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Andre Greiner-Petter
 */
public class DocumentSearchResultMOI {
    @JsonProperty("moiMD5")
    private String moiMD5;

    @JsonProperty("localTermFrequency")
    private Integer localTF;

    public DocumentSearchResultMOI() {
        this.moiMD5 = null;
        this.localTF = null;
    }

    public String getMoiMD5() {
        return moiMD5;
    }

    public void setMoiMD5(String moiMD5) {
        this.moiMD5 = moiMD5;
    }

    @JsonGetter("localTermFrequency")
    public Integer getLocalTF() {
        return localTF;
    }

    @JsonSetter("localTermFrequency")
    public void setLocalTF(String localTF) {
        this.localTF = Integer.parseInt(localTF);
    }
}
