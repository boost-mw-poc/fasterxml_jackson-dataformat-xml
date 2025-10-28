package tools.jackson.dataformat.xml.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformat-xml#114]: Support for STRICT_DUPLICATE_DETECTION
 */
public class StrictDuplicateDetection114Test extends XmlTestUtil
{
    public static class TestBean114 {
        public String field1;
        public String field2;
    }

    private final XmlMapper STRICT_MAPPER = XmlMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    // [dataformat-xml#114]
    @Test
    public void testStrictDuplicateDetectionWithPOJO() throws Exception
    {
        // Test XML mapper should also reject duplicates
        final String xmlWithDup = "<TestBean><field1>value1</field1><field1>value2</field1></TestBean>";

        StreamReadException e = assertThrows(StreamReadException.class, () -> {
            STRICT_MAPPER.readValue(xmlWithDup, TestBean114.class);
        });
        final String MATCH = "Duplicate Object property \"field1\"";
        assertTrue(e.getMessage().contains(MATCH),
                "Expected ["+MATCH+"] error, got: " + e.getMessage());
    }

    @Test
    public void testNoDuplicatesShouldWork() throws Exception
    {
        final String xml = "<TestBean><field1>value1</field1><field2>value2</field2></TestBean>";

        TestBean114 bean = STRICT_MAPPER.readValue(xml, TestBean114.class);
        assertNotNull(bean);
        assertEquals("value1", bean.field1);
        assertEquals("value2", bean.field2);
    }

    @Test
    public void testDuplicateDetectionDisabledByDefault() throws Exception
    {
        XmlMapper mapper = newMapper(); // default mapper without strict duplicate detection

        // Should allow duplicates by default (last value wins)
        final String xmlWithDup = "<TestBean><field1>value1</field1><field1>value2</field1></TestBean>";

        TestBean114 bean = mapper.readValue(xmlWithDup, TestBean114.class);
        assertNotNull(bean);
        assertEquals("value2", bean.field1); // last value wins
    }
}
