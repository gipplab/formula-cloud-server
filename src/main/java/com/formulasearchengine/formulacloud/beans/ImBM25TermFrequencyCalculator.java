package com.formulasearchengine.formulacloud.beans;

public interface ImBM25TermFrequencyCalculator extends IBM25TermFrequencyCalculator {
    double calculate(long tf, int maxCPc, double avgC, double avgDL, int docLength, double k1, double b);

    @Override
    default double calculate(long raw, long total, double avgDL, double k1, double b) {
        return 0;
    }
}
