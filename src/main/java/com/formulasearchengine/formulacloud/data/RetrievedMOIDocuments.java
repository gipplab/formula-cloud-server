package com.formulasearchengine.formulacloud.data;

import com.formulasearchengine.formulacloud.beans.MOIMathMLResult;
import com.formulasearchengine.formulacloud.beans.TFIDFOptions;
import com.formulasearchengine.formulacloud.beans.TermFrequencies;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This object represents the results of one search cycle.
 * @author Andre Greiner-Petter
 */
public class RetrievedMOIDocuments {
    private static final Logger LOG = LogManager.getLogger(RetrievedMOIDocuments.class.getName());

    private final Map<String, MathDocument> documents;
    private final Map<String, MathElement> mathElements;

    private final SearchConfig config;
    private final TFIDFOptions tfidfOptions;

    public RetrievedMOIDocuments(
            Collection<MathDocument> documentCollection,
            Collection<MathElement> mathElementCollection,
            SearchConfig config
    ) {
        this.config = config;
        this.tfidfOptions = config.getTfidfOptions();
        this.documents = new HashMap<>();
        this.mathElements = new HashMap<>();
        documentCollection.forEach( d -> this.documents.put(d.getDocID(), d));

        LOG.info("Filter elements by specified boundaries");
        mathElementCollection.stream()
                // filter all global values
                .filter( m -> config.getMinGlobalTF() <= m.getGlobalTF() && m.getGlobalTF() <= config.getMaxGlobalTF() )
                .filter( m -> config.getMinGlobalDF() <= m.getGlobalDF() && m.getGlobalDF() <= config.getMaxGlobalDF() )
                .filter( m -> config.getMinComplexity() <= m.getComplexity() && m.getComplexity() <= config.getMaxComplexity() )
                // filter if a MOI does not appear in enough search hits
                .filter( m -> config.getMinNumberOfDocHitsPerMOI() <= m.getAllLocalTF().keySet().size() )
                // finally put all results together
                .forEach( m -> this.mathElements.put(m.getMoiMD5(), m));
    }

    public List<MOIResult> getOrderedScoredMOIs() {
        LOG.info("Calculate TF-IDF scores");

        return mapToTFIDFElements()
                .flatMap( this::mapToMOIResults )
                .collect(
                        Collectors.groupingBy(
                                MathElement::getMoiMD5,
                                Collectors.reducing( (m1, m2) -> m1.merge(m2, config.getScoreMerger()))
                        )
                )
                .values()
                .stream()
                .filter( Optional::isPresent )
                .map( Optional::get )
                .sorted()
                .limit(config.getMaxNumberOfResults())
                .collect(Collectors.toList());

//        return mapToTFIDFElements()
//                .flatMap( this::mapToMOIResults )
//                .sorted()
//                .sequential() // just make sure we do not
//                .limit(config.getMaxNumberOfResults())
//                .collect(Collectors.toList());
    }

    public Stream<TFIDFMathElement> mapToTFIDFElements() {
        if (mathElements.isEmpty()){
            LOG.warn("No math elements retrieved. Result TFIDF set is empty.");
            return Stream.empty();
        }

        return mathElements.values()
                .parallelStream()
                .map( TFIDFMathElement::new )
                .peek(m -> m
                        .getAllLocalTF()
                        .keySet()
                        .forEach( k -> {
                            double score = calculateScore(m, k);
                            m.addScore(k, score);
                        }));
    }

    protected double calculateScore(MathElement math, String docID) {
        MathDocument doc = documents.get(docID);

        // the total number of math elements in this document or
        // max number of math of one type
        int maxLength = tfidfOptions.getTfOption().equals( TermFrequencies.NORM ) ?
                doc.getMaxTermFrequency() : doc.getDocumentLength();

        double tf = tfidfOptions.getTfOption().calculate(
                math.getLocalTF(docID),
                doc.getMaxCountPerComplexity(math.getComplexity()),
                doc.getAvgComplexity(),
                config.getDb().AVG_DOC_LENGTH,
                maxLength, // tricky, in case of NORM its the max TF, otherwise its the doc length
                config.getTfidfOptions().getK1(),
                config.getTfidfOptions().getB()
        );

        double idf = tfidfOptions.getIdfOption().calculate(
                math.getGlobalDF(),
                config.getDb().TOTAL_DOCS
        );

        // that's magic, isn't it?
        return tf * idf;
    }

    private Stream<MOIResult> mapToMOIResults( TFIDFMathElement tfidfMathElement ) {
        Map<String, Double> scores = tfidfMathElement.getScores();
        Collection<MOIResult> results = new LinkedList<>();
        scores.forEach((docID, score) -> results.add(
                config.isEnableMathML() ?
                        new MOIMathMLResult(tfidfMathElement, docID, "Nan", score) :
                        new MOIResult(tfidfMathElement, docID, "NaN", score)
                )
        );
        return results.stream();
    }
}
