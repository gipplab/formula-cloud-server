package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.data.MOIResult;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class MOIMathMLResult extends MOIResult {
    private static final Logger LOG = LogManager.getLogger(MOIMathMLResult.class.getName());

    @JsonProperty("mathml")
    private String mathml;

    public MOIMathMLResult(MathElement copy, String docID, double score) {
        super(copy, docID, score);
        if ( copy.getMoi() != null && !copy.getMoi().isBlank() ) {
            try {
                this.mathml = MOIConverter.stringToMML(copy.getMoi());
            } catch (Exception e) {
                LOG.error("Unable to generate MOI MML Representation for: " + copy.getMoi());
            }
        }
    }

    @Override
    public void setMOI(String moi) {
        if ( moi != null && !moi.isBlank() ) {
            this.mathml = MOIConverter.stringToMML(moi);
        }
    }
}
