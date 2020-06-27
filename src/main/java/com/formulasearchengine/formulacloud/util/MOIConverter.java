package com.formulasearchengine.formulacloud.util;

import com.formulasearchengine.formulacloud.data.MathElement;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import java.util.Base64;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MOIConverter {
    private MOIConverter(){}

    public static final char FUNCTION_APPLY = '\u2061';
    public static final char INVISIBLE_TIMES = '\u2062';

    private static final Pattern ANY_PATTERN = Pattern.compile("([a-z]+)([(:])");

    /**
     * Converts the string formatted equation to MathML formatted equation.
     * @param str string formatted equation
     * @return MathML formatted equation
     */
    public static String stringToMML(String str){
        StringBuilder sb = new StringBuilder();
        stringToMML(sb, str);
        return sb.toString();
    }

    /**
     * Converts the string math equation to MathML and fills the given StringBuilder
     * with the MathML format. {@link #stringToMML(String)} might be more convenient.
     * @param sb StringBuilder will be filled with MathML formatted {@param str}
     * @param str the math equation in string format that will be formatted to MathML
     */
    public static void stringToMML(StringBuilder sb, String str) {
        LinkedList<String> stack = new LinkedList<>();

        if ( str.isEmpty() ) {
            return;
        }

        Matcher any = ANY_PATTERN.matcher(str);
        while ( any.find() ) {
            StringBuffer restBuffer = new StringBuffer();
            any.appendReplacement(restBuffer, "");
            String prev = restBuffer.toString();

            if ( !stack.isEmpty() && stack.getLast().equals(":mtext") ){
                prev = prev.substring(0, Math.max(0,prev.length()-1));

                if ( prev.contains("(") || prev.contains(")") ) {
                    String tmp = "";
                    LinkedList<String> open = new LinkedList<>();
                    for ( int i = 0; i < prev.length(); i++) {
                        if ( (prev.charAt(i)+"").equals("(") ) {
                            open.addLast("(");
                        }
                        else if ( (prev.charAt(i)+"").equals(")") ) {
                            if ( open.isEmpty() ) {
                                break;
                            }
                            open.removeLast();
                        }
                        tmp += prev.charAt(i);
                    }

                    prev = prev.substring(tmp.length());
                    sb.append(escapeSpecialContent(tmp));
                    closeTag(sb, "mtext");
                    stack.removeLast();
                } else {
                    sb.append(escapeSpecialContent(prev));
                    closeTag(sb, "mtext");
                    stack.removeLast();
                    prev = "";
                }
            }

            if ( prev.length()>0 ) {
                int startIndex = 0;
                String buffer = "";

                if ( !stack.isEmpty() && stack.getLast().startsWith(":") ){
                    if ( stack.getLast().startsWith(":") && prev.matches("^,(?:[^,)].*|$)") ) {
                        // empty mo... very special case
                        String tag = stack.removeLast();
                        closeTag(sb, tag.substring(1));
                        prev = "";
                    } else {
                        startIndex++;
                        buffer += prev.charAt(0);
                    }
                }

                for ( int i = startIndex; i < prev.length(); i++) {
                    String e = ""+prev.charAt(i);
                    if ( e.equals(")") ) {
                        if ( !stack.isEmpty() ){
                            String lastTag = stack.removeLast();
                            if ( lastTag.startsWith(":") ) {
                                sb.append(escapeSpecialContent(buffer));
                                closeTag(sb, lastTag.substring(1));
                                lastTag = stack.removeLast();
                            }
                            closeTag(sb, lastTag);
                        }
                    } else if ( e.equals(",") ) {
                        String lastTag = stack.removeLast();
                        if ( lastTag.startsWith(":") ) {
                            sb.append(escapeSpecialContent(buffer));
                            closeTag(sb, lastTag.substring(1));
                        } else {
                            stack.addLast(lastTag);
                        }
                    } else {
                        buffer += e;
                    }
                }
            }

            String tmp = any.group(2);
            String tag = any.group(1);

            if ( tmp.equals(":") ) {
                // is an element
                stack.addLast(":"+tag);
                openTag(sb, tag);
            } else {
                // is a parent node
                stack.addLast(tag);
                openTag(sb, tag);
            }
        }

        StringBuffer restBuffer = new StringBuffer();
        any.appendTail(restBuffer);
        String rest = restBuffer.toString();

        String lastElement = stack.removeLast();
        if ( lastElement.startsWith(":") ) {
            rest = rest.substring(0, Math.max(0,rest.length()-stack.size()));
            sb.append(escapeSpecialContent(rest));
            closeTag(sb, lastElement.substring(1));
        } else {
            rest = rest.substring(0, Math.max(0,rest.length()-(stack.size()+1)));
            sb.append(escapeSpecialContent(rest));
            closeTag(sb, lastElement);
        }

        while ( !stack.isEmpty() ) {
            String tag = stack.removeLast();
            if ( tag.startsWith(":") ) tag = tag.substring(1);
            closeTag(sb, tag);
        }
    }

    private static String escapeSpecialContent( String content ) {
        content = content.matches("ivt") ? "" + INVISIBLE_TIMES : content;
        content = content.matches("fap") ? "" + FUNCTION_APPLY : content;
        content = StringEscapeUtils.escapeXml11(content);
        return content;
    }

    private static String unescapeSpecialContent( String content ) {
        content = content.matches(INVISIBLE_TIMES+"") ? "ivt" : content;
        content = content.matches(FUNCTION_APPLY+"")  ? "fap" : content;
        content = StringEscapeUtils.unescapeXml(content);
        return content;
    }

    private static void openTag(StringBuilder sb, String tag){
        sb.append("<");
        sb.append(tag);
        sb.append(">");
    }

    private static void closeTag(StringBuilder sb, String tag){
        sb.append("</");
        sb.append(tag);
        sb.append(">");
    }

    private static final Pattern MATH_PATTERN = Pattern.compile("<math.*?>\\s*(.*?)\\s*</math>", Pattern.DOTALL);
    private static final Pattern SEMANTICS_PATTERN = Pattern.compile("<semantics.*?>\\s*(.*?)\\s*</semantics>", Pattern.DOTALL);
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("<annotation.*?>.*?</annotation>", Pattern.DOTALL);

    public static String mmlToString(String mml) {
        mml = preprocessMML(mml);

        Document doc = Jsoup.parse(mml, "", Parser.xmlParser());
        Element root = doc.root();
        return recursiveMMLToString(root.child(0));
    }

    private static String recursiveMMLToString(Element root) {
        if ( isLeaf(root) ) {
            return root.normalName() + ":" + unescapeSpecialContent(root.ownText());
        }

        StringBuilder sb = new StringBuilder(root.normalName());
        sb.append("(");
        for ( int i = 0; i < root.childrenSize(); i++ ) {
            sb.append(recursiveMMLToString(root.child(i)));
            if ( i < root.childrenSize()-1 )
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    private static boolean isLeaf(Element e) {
        if ( e.childNodeSize() == 1 ) {
            return e.childNode(0) instanceof TextNode;
        } else return false;
    }

    private static String preprocessMML(String mml) {
        mml = getContent(MATH_PATTERN, mml);
        mml = getContent(SEMANTICS_PATTERN, mml);

        StringBuilder sb = new StringBuilder();
        Matcher annoMatcher = ANNOTATION_PATTERN.matcher(mml);
        while ( annoMatcher.find() ) annoMatcher.appendReplacement(sb, "");
        annoMatcher.appendTail(sb);
        return sb.toString();
    }

    private static String getContent(Pattern pattern, String mml) {
        Matcher m = pattern.matcher(mml);
        if ( m.find() ) return m.group(1);
        else return mml;
    }

    /**
     * Returns the MD5 representation of the given element.
     * The MD5 is used in elasticsearch to efficiently access
     * an MOI.
     * @param element an MOI
     * @return the MD5 of the MOI (the unique MOI key in the elasticsearch indices)
     */
    public static String getMD5(MathElement element) {
        return getMD5(element.getMoi());
    }

    /**
     * Returns the MD5 representation of the given element.
     * The MD5 is used in elasticsearch to efficiently access
     * an MOI.
     * @param moi the MOI string (the string representation can be retrieved from
     * @return the MD5 of the MOI (the unique MOI key in the elasticsearch indices)
     */
    public static String getMD5(String moi) {
        byte[] bytes = Hashing.md5().hashString(moi, Charsets.UTF_8).asBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            System.out.println("You have to provide a string that should be translated.");
            return;
        }

        String in = args[0];
        String out = stringToMML(in);
        System.out.println(out);
    }
}
