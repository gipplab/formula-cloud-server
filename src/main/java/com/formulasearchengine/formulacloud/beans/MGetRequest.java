package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MGetRequest {
    @JsonProperty("docs")
    private List<SingleGetRequest> docs;

    public MGetRequest(){}

    public List<SingleGetRequest> getDocs() {
        return docs;
    }

    public void setDocs(List<SingleGetRequest> docs) {
        this.docs = docs;
    }
}
