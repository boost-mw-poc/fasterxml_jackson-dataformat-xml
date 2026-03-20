package tools.jackson.dataformat.xml.jaxb;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.annotation.*;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.dataformat.xml.*;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformat-xml#510] @XmlValue and @XmlAttribute ignored after upgrade
public class JakartaBindXmlValueWithAttribute510Test extends XmlTestUtil
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Test", propOrder = { "value" })
    @XmlRootElement(name = "Reproducer")
    static class Reproducer510 {
        @XmlValue
        public BigDecimal value;
        @XmlAttribute(name = "node", required = true)
        public String node;

        public Reproducer510() { }
        public Reproducer510(BigDecimal value, String node) {
            this.value = value;
            this.node = node;
        }

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getNode() { return node; }
        public void setNode(String node) { this.node = node; }
    }

    // [dataformat-xml#510]: Serialization
    @Test
    public void testSerializationWithJaxbAnnotations510() throws Exception {
        // Use Jakarta JAXB with workaround for implicit "value" name
        final XmlMapper mapper = XmlMapper.builder()
                .addModule(new tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule()
                        .setNameUsedForXmlValue(null))
                .build();

        Reproducer510 obj = new Reproducer510(new BigDecimal("1.123"), "myNode");
        String xml = mapper.writeValueAsString(obj);
        // Expect: <Reproducer node="myNode">1.123</Reproducer>
        assertEquals("<Reproducer node=\"myNode\">1.123</Reproducer>", xml);
    }

    // [dataformat-xml#510]: Deserialization
    @Test
    public void testDeserializationWithJaxbAnnotations510() throws Exception {
        final XmlMapper mapper = XmlMapper.builder()
                .addModule(new tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule()
                        .setNameUsedForXmlValue(null))
                .build();

        String xml = "<Reproducer node=\"myNode\">1.123</Reproducer>";
        Reproducer510 obj = mapper.readValue(xml, Reproducer510.class);
        assertEquals(new BigDecimal("1.123"), obj.getValue());
        assertEquals("myNode", obj.getNode());
    }

    // Also verify with manual introspector pair setup (common pattern)
    @Test
    public void testWithIntrospectorPair510() throws Exception {
        JakartaXmlBindAnnotationIntrospector jaxbIntr =
                new JakartaXmlBindAnnotationIntrospector();
        jaxbIntr.setNameUsedForXmlValue(null);
        AnnotationIntrospector intr = XmlAnnotationIntrospector.Pair.instance(
                jaxbIntr,
                new JacksonAnnotationIntrospector());
        XmlMapper mapper = XmlMapper.builder()
                .annotationIntrospector(intr)
                .build();

        Reproducer510 obj = new Reproducer510(new BigDecimal("1.123"), "myNode");
        String xml = mapper.writeValueAsString(obj);
        assertEquals("<Reproducer node=\"myNode\">1.123</Reproducer>", xml);

        // Round-trip
        Reproducer510 back = mapper.readValue(xml, Reproducer510.class);
        assertEquals(new BigDecimal("1.123"), back.getValue());
        assertEquals("myNode", back.getNode());
    }
}
