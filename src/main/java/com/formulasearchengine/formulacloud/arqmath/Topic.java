package com.formulasearchengine.formulacloud.arqmath;

import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class Topic {
    public static final Pattern ID_PATTERN = Pattern.compile("([AB]\\.\\d+),");

    private String id;

    private String text;

    public Topic(String id, String text) {
        this.id = id;

        if ( text.startsWith("\"") && text.endsWith("\"") )
            text = text.substring(1, text.length()-1);
        text = text.replace("==", "");
        text = text.replace("\r\n", " ");
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
