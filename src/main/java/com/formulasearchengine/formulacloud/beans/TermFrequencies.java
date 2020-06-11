package com.formulasearchengine.formulacloud.beans;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
public enum TermFrequencies {
    BINARY(     (r,t) -> r > 0 ? 1 : 0),
    RAW(        (r,t) -> r),
    RELATIVE(   (r,t) -> r/(double)t),
    LOG(        (r,t) -> Math.log(1+r)),
    NORM(       (r,t) -> 0.5+0.5*(r/(double)t)), // note that t isn't total but max in this case
    BM25(       (r, t, avgDL, k1, b) ->
            r * (k1+1) / (r + k1*(1-b + b*t/avgDL))
    ),
    mBM25(      (tf, maxCpc, avgC, avgDL, l, k1, b) ->
            (InverseDocumentFrequencies.IDF.calculate(tf, l) * tf * (k1+1)) /
                    (maxCpc+ k1*(1-b + b*(avgDL/(l*avgC))))
    );

    private final ITermFrequencyCalculator calculator;

    TermFrequencies(ITermFrequencyCalculator calculator){
        this.calculator = calculator;
    }

    TermFrequencies(IBM25TermFrequencyCalculator calculator){
        this.calculator = calculator;
    }

    TermFrequencies(ImBM25TermFrequencyCalculator calculator){
        this.calculator = calculator;
    }

    public double calculate(long raw, long total){
        return this.calculator.calculate(raw, total);
    }

    public double calculate(long raw, long total, double avgDL, double k1, double b) {
        if ( calculator instanceof IBM25TermFrequencyCalculator ) {
            return ((IBM25TermFrequencyCalculator)calculator).calculate(raw, total, avgDL, k1, b);
        } else return calculate(raw, total);
    }

    public double calculate(long tf, int maxCPc, double avgC, double avgDL, int docLength, double k1, double b) {
        if ( calculator instanceof ImBM25TermFrequencyCalculator ) {
            return ((ImBM25TermFrequencyCalculator)calculator).calculate(tf, maxCPc, avgC, avgDL, docLength, k1, b);
        } else {
            return calculate(tf, docLength, avgDL, k1, b);
        }
    }

    public static TermFrequencies getTermFrequencyByTag(String name) {
        return Arrays.stream(TermFrequencies.values())
                .filter( tf -> tf.name().toLowerCase().equals(name.toLowerCase()) )
                .findAny()
                .orElse(null);
    }
}
