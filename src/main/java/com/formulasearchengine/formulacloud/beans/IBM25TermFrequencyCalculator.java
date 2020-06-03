package com.formulasearchengine.formulacloud.beans;

public interface IBM25TermFrequencyCalculator extends ITermFrequencyCalculator {
    double calculate(long raw, long total, double avgDL, double k1, double b);

    default double calculate(long raw, long total) {
        return 0;
    }
}
