package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.data.MOIResult;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SearchResults {
    @JsonProperty("results")
    private List<MOIResult> results;

    @JsonProperty("query")
    private String searchQuery;

    protected SearchResults() {
        results = new LinkedList<>();
    }

    public SearchResults(String query, List<MOIResult> results) {
        this.searchQuery = query;
        this.results = results;
    }

    public List<MOIResult> getResults() {
        return results;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Search results for the query: '");
        sb.append(searchQuery.length() > 50 ? searchQuery.substring(0, 50) + "..." : searchQuery);
        sb.append("'\n");

        sb.append("  #   Score   moiMD5                     C   GTF   GDF MOI / DocIDs / FormulaIDs\n");
        int[] counter = new int[]{0};
        results.stream()
                .peek( m -> counter[0]++ )
                .forEach( m -> {
                    String c = String.format("% 3d: ", counter[0]);
                    sb.append(c).append(m.toString()).append("\n");
                } );
        return sb.toString();
    }
}
