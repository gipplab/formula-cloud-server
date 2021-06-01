package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.*;

/**
 * @author Andre Greiner-Petter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentSearchResult {
    @JsonProperty("title")
    private String title;

    @JsonProperty("internalID")
    private String internalID;

    @JsonProperty("url")
    private String url;

    @JsonProperty("moi")
    private DocumentSearchResultMOI[] mois;

    @JsonIgnore
    private double searchScore;

    public DocumentSearchResult() {
        this.title = null;
        this.mois = null;
        this.internalID = null;
        this.url = null;
        this.searchScore = 0.0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInternalID() {
        return internalID;
    }

    public void setInternalID(String internalID) {
        this.internalID = internalID;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getSearchScore() {
        return searchScore;
    }

    public void setSearchScore(double searchScore) {
        this.searchScore = searchScore;
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
