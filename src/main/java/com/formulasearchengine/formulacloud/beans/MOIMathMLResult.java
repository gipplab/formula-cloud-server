package com.formulasearchengine.formulacloud.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.formulacloud.data.MOIResult;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.util.MOIConverter;

/**
 * @author Andre Greiner-Petter
 */
public class MOIMathMLResult extends MOIResult {
    @JsonProperty("mathml")
    private String mathml;

    public MOIMathMLResult(MathElement copy, String docID, String formulaID, double score) {
        super(copy, docID, formulaID, score);
        if ( copy.getMoi() != null && !copy.getMoi().isBlank() )
            this.mathml = MOIConverter.stringToMML(copy.getMoi());
    }

    @Override
    public void setMOI(String moi) {
        if ( moi != null && !moi.isBlank() ) {
            this.mathml = MOIConverter.stringToMML(moi);
        }
    }
}
