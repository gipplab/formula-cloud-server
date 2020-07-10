package com.formulasearchengine.formulacloud;

import com.formulasearchengine.formulacloud.beans.InverseDocumentFrequencies;
import com.formulasearchengine.formulacloud.beans.SearchResults;
import com.formulasearchengine.formulacloud.beans.TFIDFOptions;
import com.formulasearchengine.formulacloud.beans.TermFrequencies;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.es.ElasticsearchConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

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
        sc.setMinNumberOfDocHitsPerMOI(4);
        sc.setNumberOfDocsToRetrieve(200);
        sc.setTfidfOptions(new TFIDFOptions(TermFrequencies.mBM25, InverseDocumentFrequencies.IDF));
        SearchResults results = searcher.search(sc);
        System.out.println(results);
    }

    @Test
    public void searchHugeNumberPerformanceTest() {
        SearchConfig sc = new SearchConfig("Riemann Zeta Function");
        sc.setDb(Databases.ZBMATH);
        sc.setMaxNumberOfResults(10);
        sc.setMaxGlobalDF(10_000);
        sc.setMinGlobalDF(10);
        sc.setMinNumberOfDocHitsPerMOI(100);
        sc.setNumberOfDocsToRetrieve(1_000);
        sc.setTfidfOptions(new TFIDFOptions(TermFrequencies.mBM25, InverseDocumentFrequencies.IDF));

        // all setup, start timer
        Instant start = Instant.now();
        SearchResults results = searcher.search(sc);
        Instant stop = Instant.now();
        Duration elapsedTime = Duration.between(start, stop);

        String timeStr = String.format(
                "%02d:%02d.%3d",
                elapsedTime.toMinutesPart(),
                elapsedTime.toSecondsPart(),
                elapsedTime.toMillisPart()
        );
        System.out.println("Time elapsed: " + timeStr);
        System.out.println(results);
    }
}
