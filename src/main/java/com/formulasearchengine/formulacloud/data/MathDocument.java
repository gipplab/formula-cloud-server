package com.formulasearchengine.formulacloud.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MathDocument {
    private static final Logger LOG = LogManager.getLogger(MathDocument.class.getName());

    private String docID;

    private int documentLength = 0;

    private List<MathElement> mathElements;

    private ArrayList<Integer> maxCountPerDepthTable;
    private int maxComplexity = 0;
    private double avgComplexity = 0;

    private float esSearchPrecision;

    private int maxTermFrequency = 0;

    public MathDocument(String docID, float elasticsearchPrecision, List<MathElement> mathElements) {
        this.docID = docID;
        this.esSearchPrecision = elasticsearchPrecision;
        this.maxCountPerDepthTable = new ArrayList<>();
        this.mathElements = mathElements;
    }

    public void init() {
        mathElements = mathElements.stream()
                .filter( m -> {
                    boolean valid = m.getComplexity() > 0 && m.getMoi() != null && !m.getMoi().isBlank();
                    if ( !valid ) LOG.warn("Filter invalid MOI: " + m.getMoiMD5());
                    return valid;
                })
                .collect(Collectors.toList());

        this.maxComplexity = mathElements.stream().map( MathElement::getComplexity ).max(Short::compareTo).orElse((short)0);
        this.avgComplexity = mathElements.stream().mapToDouble( MathElement::getComplexity ).average().orElse(0);

        maxCountPerDepthTable = new ArrayList<>();
        mathElements.forEach( m -> {
            short c = m.getComplexity();
            if ( c <= 0 ) {
                LOG.warn("Something is strange, a math element with complexity 0 should not exist. " + m);
                return;
            }

            if ( this.maxTermFrequency < m.getLocalTF(docID) )
                this.maxTermFrequency = m.getLocalTF(docID);

            documentLength += m.getLocalTF(docID);

            while ( maxCountPerDepthTable.size() < c )
                maxCountPerDepthTable.add(0);

            if ( maxCountPerDepthTable.get(c-1) < m.getLocalTF(this.docID) )
                maxCountPerDepthTable.set(c-1, m.getLocalTF(this.docID));
        });
    }

    public String getDocID() {
        return docID;
    }

    public double getEsSearchPrecision() {
        return esSearchPrecision;
    }

    public int getDocumentLength() {
        return documentLength;
    }

    public int getMaxCountPerComplexity(int complexity) {
        try {
            return maxCountPerDepthTable.get(complexity-1);
        } catch (IndexOutOfBoundsException ioobe) {
            // if the requested complexity does not exist, silently
            // return 0
            return 0;
        }
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public double getAvgComplexity() {
        return avgComplexity;
    }

    public int getMaxTermFrequency() {
        return maxTermFrequency;
    }

    public static int getMaxFrequency(List<MathElement> elements){
        return elements.stream().map( MathElement::getGlobalTF ).max(Integer::compareTo).orElse(0);
    }

    public static int getSumOfFrequencies(List<MathElement> elements){
        return elements.stream().map( MathElement::getGlobalTF ).reduce(0, Integer::sum);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(docID).append(" [Score: ").append(esSearchPrecision);
        sb.append("]: Math Elements ");
        if (mathElements != null)
            sb.append(mathElements.size());
        return sb.toString();
    }
}
