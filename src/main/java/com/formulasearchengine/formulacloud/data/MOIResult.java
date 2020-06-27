package com.formulasearchengine.formulacloud.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.beans.MathMergeFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@SuppressWarnings("unused")
public class MOIResult extends MathElement implements Comparable<MOIResult> {
    @JsonProperty("docID")
    private final List<String> docID = new LinkedList<>();

    @JsonProperty("formulaID")
    private final List<String> formulaID = new LinkedList<>();

    @JsonProperty(value = "score", required = true)
    private double score;

    private MOIResult() {
        super("");
    }

    public MOIResult(MathElement copy, String docID, String formulaID, double score) {
        super(copy);
        this.docID.add(docID);
        this.formulaID.add(formulaID);
        this.score = score;
    }

    public List<String> getDocID() {
        return docID;
    }

    public List<String> getFormulaID() {
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
                "% 6.4f %22s % 3d % 5d % 5d %s  /  %s  /  %s",
                score,
                super.getMoiMD5(),
                super.getComplexity(),
                super.getGlobalTF(),
                super.getGlobalDF(),
                super.getMoi(),
                docID,
                formulaID
        );
    }

    public MOIResult merge(MOIResult reference, MathMergeFunctions mergeFunction) {
        this.score = mergeFunction.calculate(this.score, reference.score);
        reference.formulaID.forEach( fid -> {
            if ( !formulaID.contains(fid) ) formulaID.add(fid);
        });
        reference.docID.forEach( fid -> {
            if ( !docID.contains(fid) ) docID.add(fid);
        });
        reference.getAllLocalTF().forEach(super.getAllLocalTF()::putIfAbsent);
        return this;
    }
}
