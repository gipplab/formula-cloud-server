package com.formulasearchengine.formulacloud.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * @author Andre Greiner-Petter
 */
public class MOIResult extends MathElement implements Comparable<MOIResult> {
    @JsonProperty("docID")
    private String docID;

    @JsonProperty("formulaID")
    private String formulaID;

    @JsonProperty(value = "score", required = true)
    private double score;

    private MOIResult() {
        super("");
    }

    public MOIResult(MathElement copy, String docID, String formulaID, double score) {
        super(copy);
        this.docID = docID;
        this.formulaID = formulaID;
        this.score = score;
    }

    public String getDocID() {
        return docID;
    }

    public String getFormulaID() {
        return formulaID;
    }

    public double getScore() {
        return score;
    }

    /**
     * Compares in descending order because it compares the scores of each MOI.
     * And a larger score should appear before a lower score.
     * @param ref the reference moi
     * @return comparison of this object with the given reference object (descending order)
     */
    @Override
    public int compareTo(@NotNull MOIResult ref) {
        return Double.compare(ref.score, score);
    }

    @Override
    public String toString() {
        return String.format(
                "docID: %7s; fID: %3s; Score: %6.4f; MOI: %s",
                docID,
                formulaID,
                score,
                super.toString()
        );
    }
}
