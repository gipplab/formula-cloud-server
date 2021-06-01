package com.formulasearchengine.formulacloud.arqmath;

import com.formulasearchengine.formulacloud.FormulaCloudSearcher;
import com.formulasearchengine.formulacloud.beans.DocumentSearchResult;
import com.formulasearchengine.formulacloud.beans.SearchResults;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.MOIResult;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.es.ElasticsearchConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class Evaluator {
    private static final Logger LOG = LogManager.getLogger(Evaluator.class.getName());

    private final Tasks tasks;
    private final Map<String, Topic> textMapping;
    private final Map<String, GoldenData> goldenDataMapping;

    private FormulaCloudSearcher server;
    private SearchConfig searchConfig;

    private double overallScore;

    private Path resultOutput = Paths.get("test/results.csv");
    private String resultString = "";
    private String summary = "";

    private int truePositives = 0;
    private int selectedElements = 0;
    private int relevantElements = 0;
    private int totalRelevanceScore = 0;

    private String headerLine = String.format(
            "%4s\t%5s\t%5s\t%5s\t%5s\n" +
                    "-----------------------------------",
            "T-ID", "Prec.", "Rec.", "F1", "AvgR."
    );

    private final Path outputPath = Paths.get("/mnt/share/data/arqmath/results/");

    public Evaluator(ElasticsearchConfig config, boolean taskA) throws IOException {
        Path p = Paths.get("/mnt/share/data/arqmath/posts/topic-text.csv");
        Path g = null;
        if (taskA) g = Paths.get("/mnt/share/data/arqmath/goldenData/qrel_task1.tsv");
        if (!taskA) g = Paths.get("/mnt/share/data/arqmath/goldenData/qrel_task2.tsv");
        tasks = new Tasks(p, g);

        textMapping = tasks.getTopicMap();
        goldenDataMapping = tasks.getGoldenDataMap();

        LOG.info("Start formula cloud search server");
        server = new FormulaCloudSearcher(config);
        server.start();

        this.searchConfig = generateDefaultSearchConfig();
        this.overallScore = 0.0;
    }

    private SearchConfig generateDefaultSearchConfig() {
        SearchConfig sc = new SearchConfig();
        sc.setDb(Databases.ARQMATH);
        sc.setMinGlobalTF(1);
        sc.setMinGlobalDF(1);
        sc.setMaxGlobalDF(Databases.ARQMATH.TOTAL_DOCS / 4);

        sc.setNumberOfDocsToRetrieve(200);
        sc.setMinNumberOfDocHitsPerMOI(1);

        sc.setMaxNumberOfResults(Integer.MAX_VALUE);
        sc.setEnableMathML(true);
        return sc;
    }

    public void performAllTests() {
        List<String> requestIds = tasks.getOrderedTopicIDsFromGoldenSet();
        LOG.info("Start testing " + requestIds.size() + " topic IDs.");
        int counter = 0;
        int total = requestIds.size();
        while (!requestIds.isEmpty()) {
            performSingleTest(requestIds.remove(0));
            counter++;
            LOG.info("Tested " + counter + "/" + total);
        }
    }

    public void evaluateTaskA() {
        List<String> requestIds = tasks.getOrderedTopicIDsFromGoldenSet();
        LOG.info("Start testing " + requestIds.size() + " topic IDs.");
        int counter = 0;
        int total = requestIds.size();
        while (!requestIds.isEmpty()) {
            performSingleATaskTest(requestIds.remove(0));
            counter++;
            LOG.info("Tested " + counter + "/" + total);
        }
    }

    public void performSingleATaskTest(String id) {
        String text = textMapping.get(id).getText();
        if (text.isBlank()) {
            LOG.warn("Skip testing empty text for ID: " + id);
            return;
        }
        GoldenData gold = goldenDataMapping.get(id);
        this.relevantElements += gold.getNumberOfRelevantHits();

        searchConfig.setSearchQuery(text);
        List<DocumentSearchResult> resultList = server.searchForDocuments(searchConfig);

        Set<String> relevantHitsMem = new HashSet<>();
        Set<String> totalNumberOfIDs = new HashSet<>();

        int relevanceSum = 0;

        for (int i = 0; i < resultList.size(); i++) {
            DocumentSearchResult result = resultList.get(i);
            String docID = result.getTitle();

            totalNumberOfIDs.add(docID);
            if (gold.containsFIDAtAll(docID)) {
                LOG.warn("Found relevant DOC for TopicID: " + id +
                        "; Relevance: " + gold.getScoreFID(docID) +
                        "; Pos: " + (i + 1) +
                        "; extDocID: " + docID);
                if (gold.getScoreFID(docID) > 0) {
                    relevantHitsMem.add(docID);
                }
                relevanceSum += gold.getScoreFID(docID);
            }
        }

        updateScoresAndSummary(id, relevantHitsMem, totalNumberOfIDs, relevanceSum, gold);
    }

    public void updateScoresAndSummary(String id, Set<String> relevantHitsMem, Set<String> totalNumberOfIDs, int relevanceSum, GoldenData gold) {
        this.truePositives += relevantHitsMem.size();
        this.selectedElements += totalNumberOfIDs.size();
        this.totalRelevanceScore += relevanceSum;

        double precision = totalNumberOfIDs.size() > 0 ? relevantHitsMem.size() / (double) totalNumberOfIDs.size() : 0;
        double recall = gold.getNumberOfRelevantHits() > 0 ? relevantHitsMem.size() / (double) gold.getNumberOfRelevantHits() : 0;
        double f1 = precision + recall != 0 ? 2 * (precision * recall) / (precision + recall) : 0;

        double avgRelevance = gold.getNumberOfRelevantHits() > 0 ? relevanceSum / (double) gold.getNumberOfRelevantHits() : 0;

        String line = String.format(
                "%4s\t%5.3f\t%5.3f\t%5.3f\t%5.3f\n",
                id,
                precision,
                recall,
                f1,
                avgRelevance
        );

        System.out.println(headerLine);
        System.out.println(line);
        summary += line;
    }

    public void performSingleTest(String id) {
        String text = textMapping.get(id).getText();
        if (text.isBlank()) {
            LOG.warn("Skip testing empty text for ID: " + id);
            return;
        }

        searchConfig.setSearchQuery(text);
        SearchResults sr = server.search(searchConfig);

        LOG.info("Retrieved results for " + id + ". Start calculating score.");
        double score = searchScore(id, sr);
        this.overallScore += score;

        LOG.info("Total score for " + id + ": " + score);
        if (score > 0) LOG.warn("A score over 0! Score: " + score);

        resultString += "Total score for " + id + ": " + score + "\n\n";
    }

    public double searchScore(String topicID, SearchResults results) {
        List<MOIResult> moiResults = results.getResults();
        GoldenData gold = goldenDataMapping.get(topicID);

        double score = 0;

        String lineStart = topicID + ",";
        int topHitsFilter = 5;

        Set<String> relevantHitsMem = new HashSet<>();
        Set<String> totalNumberOfIDs = new HashSet<>();

        HashMap<String, LinkedList<Double>> mappings = new HashMap<>();

        int relevanceSum = 0;

        for (int i = 0; i < moiResults.size(); i++, topHitsFilter--) {
            MOIResult moiResult = moiResults.get(i);
            List<String> fIDs = moiResult.getFormulaID();

            for (String fID : fIDs) {
                LinkedList<Double> internalScores = mappings.computeIfAbsent(fID, id -> new LinkedList<>());
                internalScores.add(moiResult.getScore());

                if (gold.containsFIDAtAll(fID)) {
                    totalNumberOfIDs.add(fID);
                    LOG.warn("Found fID in Results: [TopicID: " + topicID +
                            ", fID: " + fID +
                            ", Relevance: " + gold.getScoreFID(fID) +
                            "; MOIPos: " + (i + 1) +
                            "; MOI: " + moiResult.getMoiMD5() + " / " + moiResult.toString() + "]");
                    if (gold.getScoreFID(fID) > 0) {
                        relevantHitsMem.add(fID);
                    }
                    relevanceSum += gold.getScoreFID(fID);
                    score += gold.getScoreFID(fID) * (1 / (double) (i + 1));
                }
            }

            if (topHitsFilter > 0) {
                String line = lineStart + score + "," + moiResult;
                resultString += line + "\n";
            }
        }

        if (totalNumberOfIDs.size() > 0) {
            this.relevantElements += gold.getNumberOfRelevantHits();
        }

        List<Result> totalScores = mappings
                .entrySet()
                .stream()
                .map(entry -> {
                    double v = entry.getValue()
                            .stream()
                            .sorted()
                            .limit(3)
                            .mapToDouble(val -> val)
                            .average()
                            .orElse(0);
                    Result r = new Result();
                    r.fID = Integer.parseInt(entry.getKey());
                    r.topicID = topicID;
                    r.score = v;
                    return r;
                })
                .sorted(Comparator.comparingDouble(r -> r.score))
                .collect(Collectors.toList());

        Collections.reverse(totalScores);

        writeResults(totalScores);

        updateScoresAndSummary(topicID, relevantHitsMem, totalNumberOfIDs, relevanceSum, gold);
        return score;
    }

    private void writeResults(List<Result> scores) {
        if ( scores.isEmpty() ) return;
        Path out = outputPath.resolve(scores.get(0).topicID + ".csv");
        try {
            Files.deleteIfExists(out);
        } catch (IOException e) {
            LOG.error("Unable to delete old results file " + out.toString(), e);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out.toFile()))) {
            int i = 1;
            for (Result res : scores) {
                writer.write(String.format(
                        "%s,%d,%f,%d\n",
                        res.topicID,
                        res.fID,
                        res.score,
                        i
                ));
                i++;
            }
        } catch (IOException ioe) {
            LOG.error("Unable to write output file " + out.toString(), ioe);
        }
    }

    private static class Result {
        private String topicID;
        private int fID;
        private double score;
    }

    public void stop() {
        server.stop();
    }

    public static void main(String[] args) throws IOException {
        ElasticsearchConfig config = ElasticsearchConfig.loadConfig(args);
        Evaluator evaluator = new Evaluator(config, false);

//        evaluator.performSingleTest("B.1");
        evaluator.performAllTests();
//        evaluator.evaluateTaskA();
        System.out.println(evaluator.resultString);
        System.out.println(evaluator.overallScore);

        System.out.println(evaluator.headerLine);
        System.out.println(evaluator.summary);
        System.out.println("----------------------------------");

        double precision = evaluator.truePositives / (double) evaluator.selectedElements;
        double recall = evaluator.truePositives / (double) evaluator.relevantElements;
        double f1 = precision + recall > 0 ? 2 * (precision * recall) / (precision + recall) : 0;

        String total = String.format(
                "%4s\t%5.3f\t%5.3f\t%5.3f\t%5.3f\n",
                "All",
                precision,
                recall,
                f1,
                evaluator.totalRelevanceScore / (double) evaluator.relevantElements
        );
        System.out.println(total);

        evaluator.stop();
    }
}
