package com.formulasearchengine.formulacloud.data;

import com.formulasearchengine.formulacloud.beans.InverseDocumentFrequencies;
import com.formulasearchengine.formulacloud.beans.SearchResults;
import com.formulasearchengine.formulacloud.beans.TermFrequencies;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class ScoreCalculationTests {

    /**
     * Testing MOI:
     *  mrow(mi:ζ,mo:ivt,mrow(mo:(,mrow(mi:σ,mo:+,mrow(mi:i,mo:ivt,mi:t)),mo:)))
     *  Bbtc4XYaieURFIfMzgYR1g==
     *
     *  "complexity" :   "5"
     *          "tf" : "132"
     *          "df" :  "97"
     */
    @Test
    public void calculateBestTest() {
        // doc 2271804 test
        double tf = TermFrequencies.mBM25.calculate(
                1,
                1,
                1.82,
                45.47,
                128,
                1.2,
                0.95
        );

        double idf = InverseDocumentFrequencies.IDF.calculate(97, 1_349_297);
        double score = tf*idf;
        System.out.println(tf);
        System.out.println(idf);
        System.out.println(score);
    }

    @Test
    public void testToString() {
        List<MOIResult> results = new LinkedList<>();
        results.add(
                new MOIResult(
                        new MathElement(
                                "Bbtc4XYaieURFIfMzgYR1g==",
                                "mrow(mi:ζ,mo:ivt,mrow(mo:(,mrow(mi:σ,mo:+,mrow(mi:i,mo:ivt,mi:t)),mo:)))",
                                132,
                                97,
                                (short)5
                        ),
                        "Doc1",
                        "Formula1.1",
                        85.3141314314123
                )
        );
        SearchResults r = new SearchResults("Search test", results);
        System.out.println(r);
    }
}
