package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class SingleGetRequest {
    @JsonProperty("_index")
    private String index;

    @JsonProperty("_id")
    private String id;

    public SingleGetRequest() {}

    public SingleGetRequest(String index, String id) {
        this.index = index;
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
