package com.formulasearchengine.formulacloud;

import com.formulasearchengine.formulacloud.util.MOIConverter;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class MOIConverterTests {

    @Test
    public void strToMMLStandardZetaTest(){
        String str = "mrow(mi:ζ,mo:ivt,mrow(mo:(,mi:z,mo:)))";
        String mml = "<mrow><mi>ζ</mi><mo>\u2062</mo><mrow><mo>(</mo><mi>z</mi><mo>)</mo></mrow></mrow>";
        String mmlConverted = MOIConverter.stringToMML(str);
        assertEquals(mml, mmlConverted);
    }

    @Test
    public void strToMMLStandardGammaTest(){
        String str = "mrow(mi:Γ,mo:fap,mrow(mo:(,mi:a,mo:)))";
        String mml = "<mrow><mi>Γ</mi><mo>\u2061</mo><mrow><mo>(</mo><mi>a</mi><mo>)</mo></mrow></mrow>";
        String mmlConverted = MOIConverter.stringToMML(str);
        assertEquals(mml, mmlConverted);
    }

    @Test
    public void mmlToStrStandardGammaTest(){
        String str = "mrow(mi:Γ,mo:fap,mrow(mo:(,mi:a,mo:)))";
        String mml = "<mrow><mi>Γ</mi><mo>\u2061</mo><mrow><mo>(</mo><mi>a</mi><mo>)</mo></mrow></mrow>";
        String strConverted = MOIConverter.mmlToString(mml);
        assertEquals(str, strConverted);
    }

    @Test
    public void mmlToStrExtraUnnecessaryInfoTest(){
        String str = "mrow(mi:Γ,mo:fap,mrow(mo:(,mi:a,mo:)))";
        String mml = "<math xml:ns=\"myDefinitionOfWebspace\"><mrow><mi>Γ</mi><mo stretched=\"extremeStretch\">\u2061</mo><mrow><mo>(</mo><mi>a</mi><mo>)</mo></mrow></mrow><annotation>blabla</annotation></math>";
        String strConverted = MOIConverter.mmlToString(mml);
        assertEquals(str, strConverted);
    }

    @Test
    public void strToMMLComplexCommaTest(){
        String str = "mrow(mi:M,mo:=,msub(mrow(mo:(,mrow(mi:m,mo:ivt,mrow(mo:(,mi:s,mo:,,mi:t,mo:))),mo:)),mrow(mrow(mi:s,mo:,,mi:t),mo:∈,mi:S)))";
        String mml = "<mrow><mi>M</mi><mo>=</mo><msub><mrow><mo>(</mo><mrow><mi>m</mi><mo>\u2062</mo><mrow><mo>(</mo><mi>s</mi><mo>,</mo><mi>t</mi><mo>)</mo></mrow></mrow><mo>)</mo></mrow><mrow><mrow><mi>s</mi><mo>,</mo><mi>t</mi></mrow><mo>∈</mo><mi>S</mi></mrow></msub></mrow>";
        String mmlConverted = MOIConverter.stringToMML(str);
        assertEquals(mml, mmlConverted);
    }

    @Test
    public void strToMMLComplexDotsTest(){
        String str = "mrow(mrow(mi:W,mo:=,mrow(mrow(mo:⟨,mpadded(mi:S),mo:|),mo:ivt,msup(mrow(mo:(,mrow(mi:s,mo:ivt,mi:t),mo:)),mrow(mi:m,mo:ivt,mrow(mo:(,mi:s,mo:,,mi:t,mo:)))))),mo::,mrow(mrow(mi:s,mo:,,mi:t),mo:∈,mrow(mpadded(mi:S),mo:ivt,mpadded(mtext:and),mo:ivt,mi:m,mo:ivt,mrow(mo:(,mi:s,mo:,,mi:t,mo:)),mo:ivt,mrow(mo:<,mi:∞,mo:⟩))))";
        String mml = "<mrow><mrow><mi>W</mi><mo>=</mo><mrow><mrow><mo>⟨</mo><mpadded><mi>S</mi></mpadded><mo>|</mo></mrow><mo>\u2062</mo><msup><mrow><mo>(</mo><mrow><mi>s</mi><mo>\u2062</mo><mi>t</mi></mrow><mo>)</mo></mrow><mrow><mi>m</mi><mo>\u2062</mo><mrow><mo>(</mo><mi>s</mi><mo>,</mo><mi>t</mi><mo>)</mo></mrow></mrow></msup></mrow></mrow><mo>:</mo><mrow><mrow><mi>s</mi><mo>,</mo><mi>t</mi></mrow><mo>∈</mo><mrow><mpadded><mi>S</mi></mpadded><mo>\u2062</mo><mpadded><mtext>and</mtext></mpadded><mo>\u2062</mo><mi>m</mi><mo>\u2062</mo><mrow><mo>(</mo><mi>s</mi><mo>,</mo><mi>t</mi><mo>)</mo></mrow><mo>\u2062</mo><mrow><mo>&lt;</mo><mi>∞</mi><mo>⟩</mo></mrow></mrow></mrow></mrow>";
        String mmlConverted = MOIConverter.stringToMML(str);
        assertEquals(mml, mmlConverted);
    }

    @Test
    public void roundTripTest(){
        String str = "mrow(mrow(mi:W,mo:=,mrow(mrow(mo:⟨,mpadded(mi:S),mo:|),mo:ivt,msup(mrow(mo:(,mrow(mi:s,mo:ivt,mi:t),mo:)),mrow(mi:m,mo:ivt,mrow(mo:(,mi:s,mo:,,mi:t,mo:)))))),mo::,mrow(mrow(mi:s,mo:,,mi:t),mo:∈,mrow(mpadded(mi:S),mo:ivt,mpadded(mtext:and),mo:ivt,mi:m,mo:ivt,mrow(mo:(,mi:s,mo:,,mi:t,mo:)),mo:ivt,mrow(mo:<,mi:∞,mo:⟩))))";
        String mml = MOIConverter.stringToMML(str);
        System.out.println(mml);
        assertEquals(str, MOIConverter.mmlToString(mml) );
    }

    @Test
    public void fileTest() throws Exception {
        URL url = Resources.getResource("otherTest.mml.txt");
        String str = Resources.toString(url, Charsets.UTF_8);
        String mml = Resources.toString(Resources.getResource("otherTest.html"), Charsets.UTF_8);
        assertEquals(mml, MOIConverter.stringToMML(str));
    }

    @Test
    public void sqrtTest() throws Exception {
        URL url = Resources.getResource("sqrt.mml");
        String sqrtMML = Resources.toString(url, Charsets.UTF_8);
        assertEquals("msqrt(mi:x)", MOIConverter.mmlToString(sqrtMML));
    }

}
