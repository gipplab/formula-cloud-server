package com.formulasearchengine.formulacloud.arqmath;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class TasksTests {
    @Test
    public void loadText() throws IOException {
        Path p = Paths.get("/mnt/share/data/arqmath/posts/topic-text.csv");
        Path g = Paths.get("/mnt/share/data/arqmath/goldenData/qrel_task2.tsv");
        Tasks tasks = new Tasks(p, g);
        Map<String, Topic> map = tasks.getTopicMap();

        assertTrue(map.containsKey("B.1"));

        Topic t = map.get("B.1");
        assertEquals("B.1", t.getId());
        assertTrue(t.getText().contains("Finding value of <math id=q_1>c</math> such that the range of the rational function"));

        Map<String, GoldenData> gold = tasks.getGoldenDataMap();
        assertTrue( gold.containsKey("B.1") );
        GoldenData gd = gold.get("B.1");
        assertTrue(gd.containsFIDAtAll("25466142"));
        assertEquals(3, gd.getScoreFID("25466142"));

        GoldenData gd2 = gold.get("B.56");
        assertEquals(0, gd2.getScoreFID("301436"));
        assertEquals(3, gd2.getScoreFID("7988300"));

        List<String> btaskIds = tasks.getOrderedTopicIDsFromGoldenSet();
        System.out.println(btaskIds.size());
        System.out.println(btaskIds);
    }
}
