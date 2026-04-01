package tools.jackson.dataformat.xml.ser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import tools.jackson.databind.cfg.GeneratorInitializer;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [dataformat-xml#315]: allow custom encoding in XML declaration via GeneratorInitializer
public class GeneratorInitializer315Test extends XmlTestUtil
{
    @Test
    public void testCustomEncodingViaGeneratorInitializer() throws Exception {
        GeneratorInitializer initializer = (config, gen) -> {
            ToXmlGenerator xmlGen = (ToXmlGenerator) gen;
            XMLStreamWriter sw = xmlGen.getStaxWriter();
            try {
                sw.writeStartDocument("ISO-8859-1", "1.0");
            } catch (XMLStreamException e) {
                throw new RuntimeException("Failed to write StartDocument event", e);
            }
        };
        XmlMapper mapper = XmlMapper.builder()
                .generatorInitializer(initializer)
                .build();
        String xml = mapper.writeValueAsString(new StringBean("test"));
        assertTrue(xml.contains("encoding='ISO-8859-1'") || xml.contains("encoding=\"ISO-8859-1\""),
                "Expected ISO-8859-1 encoding in XML declaration, got: " + xml);
    }
}
