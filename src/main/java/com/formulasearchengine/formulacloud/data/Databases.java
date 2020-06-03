package com.formulasearchengine.formulacloud.data;

/**
 * This class contains all global information about a dataset.
 * It contains the total number of documents (that contains math),
 * the total number of MOI, the unique number of MOI, and the average
 * complexity.
 */
public enum Databases {
    //     NAME       #Math Docs      total # MOI     avg Complexity
    ARXIV("arXiv",       841_000,    2_080_634_554,   4.59                  ),
    ZBMATH("zbMATH",   1_349_297,       61_355_307,   4.89                  ),
    ARQMATH("ARQMath", 2_058_866,      143_317_218,   5.0007810794366305    );

    private final String str;
    public final int TOTAL_DOCS;
    public final int TOTAL_MOI;
    public final double AVG_COMPLEXITY;

    public final double AVG_DOC_LENGTH;

    Databases(String str, int docs, int math, double avgC) {
        this.str = str;
        this.TOTAL_DOCS = docs;
        this.TOTAL_MOI = math;
        this.AVG_DOC_LENGTH = math/(double)docs;
        this.AVG_COMPLEXITY = avgC;
    }

    public String toESString() {
        return str.toLowerCase();
    }

    @Override
    public String toString() {
        return str;
    }
}
