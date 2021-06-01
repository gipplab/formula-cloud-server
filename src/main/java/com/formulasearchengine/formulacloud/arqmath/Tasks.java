package com.formulasearchengine.formulacloud.arqmath;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public class Tasks {

    private Map<String, Topic> topicMap;
    private Map<String, GoldenData> goldenDataMap;

    public Tasks(Path pathToTopicTextFile, Path goldenDataset) throws IOException {
        topicMap = new HashMap<>();
        goldenDataMap = new HashMap<>();
        loadTopics(pathToTopicTextFile);
        loadGoldenData(goldenDataset);
    }

    private void loadGoldenData(Path goldenData) {
        try ( BufferedReader br = new BufferedReader(new FileReader(goldenData.toFile())) ) {
            String line = "";
            while ( (line = br.readLine()) != null ) {
                String[] infos = line.split("\t+");
                String id = infos[0];
                GoldenData gd = goldenDataMap.computeIfAbsent(id, GoldenData::new);
                gd.addScore(infos[2], infos[3]);
            }
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        }
    }

    private void loadTopics(Path pathToTopicTextFile) throws IOException {
        String file = Files.readString(pathToTopicTextFile);
        Matcher idMatcher = Topic.ID_PATTERN.matcher(file);

        String previousID = "";
        while ( idMatcher.find() ) {
            if ( previousID.isBlank() ) {
                previousID = idMatcher.group(1);
                idMatcher.appendReplacement(new StringBuilder(), "");
                continue;
            }

            StringBuilder sbContent = new StringBuilder();
            idMatcher.appendReplacement(sbContent, "");
            String content = sbContent.toString().trim();
            Topic topic = new Topic(previousID, content);
            topicMap.put(topic.getId(), topic);

            previousID = idMatcher.group(1);
        }

        StringBuilder sbC = new StringBuilder();
        idMatcher.appendTail(sbC);
        Topic t = new Topic(previousID, sbC.toString().trim());
        topicMap.put(t.getId(), t);
    }

    public Map<String, Topic> getTopicMap() {
        return topicMap;
    }

    public Map<String, GoldenData> getGoldenDataMap() {
        return goldenDataMap;
    }

    public List<String> getOrderedTopicIDsFromGoldenSet() {
        Set<String> ids = goldenDataMap.keySet();
        List<String> listIDs = new LinkedList<>(ids);
        listIDs.sort((id1, id2) -> {
            int num1 = Integer.parseInt(id1.split("\\.")[1]);
            int num2 = Integer.parseInt(id2.split("\\.")[1]);
            return Integer.compare(num1, num2);
        });
        return listIDs;
    }
}
