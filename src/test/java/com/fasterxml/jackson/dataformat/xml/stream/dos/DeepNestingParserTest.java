package com.fasterxml.jackson.dataformat.xml.stream.dos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.exc.StreamReadException;

import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.XmlTestBase;

public class DeepNestingParserTest extends XmlTestBase {

    public void testDeepDoc() throws Exception
    {
        final XmlMapper xmlMapper = newMapper();
        final String XML = createDeepNestedDoc(1050);
        try (JsonParser p = xmlMapper.createParser(XML)) {
            while (p.nextToken() != null) { }
            fail("expected StreamReadException");
        } catch (StreamReadException e) {
            assertTrue(e.getMessage().contains("Maximum Element Depth limit (1000) Exceeded"));
        }
    }

    // jackson-core's StreamReadConstraints.maxNestingDepth is now enforced on the
    // XML read path (previously ignored); verify with a low configured limit that
    // stays well within the default Stax element-depth limit (1000), so the
    // rejection is unambiguously due to the nesting-depth check.
    public void testDeepDocWithLowNestingLimit() throws Exception
    {
        final XmlMapper xmlMapper = mapperBuilder(XmlFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(10).build())
                .build()).build();
        final String XML = createDeepNestedDoc(50);
        try (JsonParser p = xmlMapper.createParser(XML)) {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                    e.getMessage().contains("nesting depth"));
        }
    }

    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("<root>");
        for (int i = 0; i < depth; i++) {
            sb.append("<leaf>");
        }
        sb.append("abc");
        for (int i = 0; i < depth; i++) {
            sb.append("</leaf>");
        }
        sb.append("</root>");
        return sb.toString();
    }
}
