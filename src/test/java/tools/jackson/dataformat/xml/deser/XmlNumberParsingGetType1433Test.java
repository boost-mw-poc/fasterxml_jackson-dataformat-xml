package tools.jackson.dataformat.xml.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import tools.jackson.dataformat.xml.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlNumberParsingGetType1433Test
    extends XmlTestUtil
{
    private final XmlMapper XML_MAPPER = xmlMapper(false);

    // Bit different for XML as there's rarely "native" number tokens
    @Test
    void getNumberType() throws Exception
    {
       JsonParser p;

        p = _createParser("<root>123</root>");
        _verifyGetNumberTypeFail(p, "null");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        _verifyGetNumberTypeFail(p, "START_OBJECT");
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        _verifyGetNumberTypeFail(p, "PROPERTY_NAME");
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertTrue(p.isExpectedNumberIntToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        _verifyGetNumberTypeFail(p, "END_OBJECT");
        p.close();
        _verifyGetNumberTypeFail(p, "null");
    }

    private void _verifyGetNumberTypeFail(JsonParser p, String token) throws Exception
    {
        // In 2.x got exception; in 3.x null
        assertNull(p.getNumberType());
    }

    private JsonParser _createParser(String text) throws Exception {
        return XML_MAPPER.createParser(text);
    }
}
