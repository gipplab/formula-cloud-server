package com.formulasearchengine.formulacloud.beans;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class TermFrequencyCalculatorTests {

    @Test
    public void getRightCalculatorTest() {
        assertEquals(TermFrequencies.RELATIVE, TermFrequencies.getTermFrequencyByTag("Relative"));
        assertEquals(TermFrequencies.BM25, TermFrequencies.getTermFrequencyByTag("BM25"));
        assertEquals(TermFrequencies.mBM25, TermFrequencies.getTermFrequencyByTag("mBM25"));
    }

}
