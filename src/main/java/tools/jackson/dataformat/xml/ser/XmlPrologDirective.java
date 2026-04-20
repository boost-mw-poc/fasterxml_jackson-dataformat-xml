package tools.jackson.dataformat.xml.ser;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Common API for XML nodes -- Comments, DTDs, Processing Instructions -- to
 * be written <b>before</b> actual XML Document (root node).
 *
 * @since 3.2
 */
public interface XmlPrologDirective
{
    /**
     * Method to call to actually write out the directive.
     */
    public void write(ToXmlGenerator xmlGen, XMLStreamWriter2 sw)
        throws XMLStreamException;
}
