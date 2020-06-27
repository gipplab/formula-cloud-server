package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.data.MOIResult;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SearchError extends SearchResults {
    @JsonProperty("errorMessage")
    private String message;

    @JsonIgnore
    private Throwable cause;

    private SearchError(){}

    public SearchError(String errorMessage) {
        this.message = errorMessage;
        this.cause = null;
    }

    public SearchError(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        String str = "Error: " + message;
        if ( cause != null ) str += " (" + cause.toString() + ")";
        return str;
    }
}
