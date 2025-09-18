package com.fasterxml.jackson.dataformat.xml.records.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.xml.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.fasterxml.jackson.dataformat.xml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [dataformat-xml#735]
public class XmlRecordDeser735Test extends XmlTestUtil
{
    record Amount(@JacksonXmlText String value,
                  @JacksonXmlProperty(isAttribute = true, localName = "Ccy") String currency) {}

    static class Pojo735 {
        String value;
        String currency;

        public Pojo735(@JacksonXmlText String value,
                @JacksonXmlProperty(isAttribute = true, localName = "Ccy") String currency)
        {
            this.value = value;
            this.currency = currency;
        }
    }
    
    private final String XML = "<Amt Ccy='EUR'>1</Amt>";

    private final XmlMapper MAPPER = newMapper();

    @JacksonTestFailureExpected
    @Test
    public void testPojoDeser() throws Exception {
        Pojo735 amt = MAPPER.readValue(XML, Pojo735.class);
        assertEquals("1", amt.value);
        assertEquals("EUR", amt.currency);
    }

    @JacksonTestFailureExpected
    @Test
    public void testRecordDeser() throws Exception {
        Amount amt = MAPPER.readValue(XML, Amount.class);
        assertEquals("1", amt.value);
        assertEquals("EUR", amt.currency);
    }
}
