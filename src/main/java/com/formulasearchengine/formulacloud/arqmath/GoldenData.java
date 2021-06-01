package com.formulasearchengine.formulacloud.arqmath;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class GoldenData {
    private String topicID;

    private Map<String, Integer> fIDScores;

    public GoldenData(String topicID) {
        this.topicID = topicID;
        this.fIDScores = new HashMap<>();
    }

    public void addScore(String fID, String score) {
        this.addScore(fID, Integer.parseInt(score));
    }

    public void addScore(String fID, Integer score) {
        fIDScores.put(fID, score);
    }

    public int getScoreFID(String fID) {
        return fIDScores.getOrDefault(fID, 0);
    }

    public boolean containsFIDAtAll(String fID) {
        return fIDScores.containsKey(fID);
    }

    public String getTopicID() {
        return topicID;
    }

    public int getNumberOfRelevantHits() {
        return (int)fIDScores.entrySet().stream().filter(e -> e.getValue() > 0).count();
    }
}
