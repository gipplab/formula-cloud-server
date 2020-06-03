package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author Andre Greiner-Petter
 */
public class DocumentSearchResult {
    @JsonProperty("title")
    private String title;

    @JsonProperty("moi")
    private DocumentSearchResultMOI[] mois;

    public DocumentSearchResult() {
        this.title = null;
        this.mois = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonGetter("moi")
    public DocumentSearchResultMOI[] getMois() {
        return mois;
    }

    @JsonSetter("moi")
    public void setMois(DocumentSearchResultMOI[] mois) {
        this.mois = mois;
    }
}
