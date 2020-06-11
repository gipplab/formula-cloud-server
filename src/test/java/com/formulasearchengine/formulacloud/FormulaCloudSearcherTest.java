package com.formulasearchengine.formulacloud;

import com.formulasearchengine.formulacloud.beans.SearchResults;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.es.ElasticsearchConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Andre Greiner-Petter
 */
public class FormulaCloudSearcherTest {
    private static FormulaCloudSearcher searcher;

    @BeforeAll
    public static void setup() {
        searcher = new FormulaCloudSearcher(new ElasticsearchConfig());
        searcher.start();
    }

    @AfterAll
    public static void cleanup() {
        searcher.stop();
    }

    @Test
    public void searchRiemannZetaFunctionTest() {
        SearchConfig sc = new SearchConfig("Riemann Zeta Function");
        sc.setDb(Databases.ZBMATH);
        sc.setMaxNumberOfResults(10);
        sc.setMaxGlobalDF(10_000);
        sc.setMinGlobalDF(10);
        sc.setMinNumberOfDocHitsPerMOI(7);
        sc.setNumberOfDocsToRetrieve(200);
        SearchResults results = searcher.search(sc);
        FormulaCloudSearcher.print(results.getResults(), System.out);
    }
}
