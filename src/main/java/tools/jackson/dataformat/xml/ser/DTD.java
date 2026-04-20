package tools.jackson.dataformat.xml.ser;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Value container to represent XML Document Type Declaration,
 * to be written using {@link XmlGeneratorInitializer}.
 *
 * @since 3.2
 */
public record DTD(String rootName,
        String systemId, String publicId,
        String internalSubset)
    implements XmlPrologDirective
{
    public DTD {
        rootName = _nonEmptyNonNull("rootName", rootName);
        systemId = _emptyToNull(systemId);
        publicId = _emptyToNull(publicId);
        internalSubset = _emptyToNull(internalSubset);
    }

    static String _emptyToNull(String str) {
        return "".equals(str) ? null : str;
    }

    static String _nonEmptyNonNull(String prop, String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Illegal argument for '%s': must be non-empty String"
                    .formatted(prop));
        }
        return str;
    }

    @Override
    public void write(ToXmlGenerator xmlGen, XMLStreamWriter2 xmlWriter)
        throws XMLStreamException
    {
        xmlWriter.writeDTD(rootName(), systemId(), publicId(), internalSubset());
    }
}
