package one.xmpp.xml;

import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringEscapeUtils;

import one.xmpp.server.XmppConstants;
import one.xmpp.utils.CharSequenceUtils;

public class XmlConstructor {

    private State state = State.BEGIN;

    private final StanzaListener stranzaListener;

    // TODO: may be change to LinkedList to remove sync?
    private final Stack<XmlElement> xmlElements = new Stack<XmlElement>();

    public XmlConstructor(StanzaListener stranzaListener) {
        this.stranzaListener = stranzaListener;
    }

    public void characters(CharSequence string) {
        if (state != State.STRANZA) {
            if (CharSequenceUtils.isEmptyTrimmed(string)) {
                // ignore whitespaces and line breaks between STRANZA's
                return;
            }
            throw new IllegalStateException("Received characters not in STRANZA, but in " + state
                    + " state: (escaped) " + StringEscapeUtils.escapeJava("" + string));
        }
        XmlElement last = xmlElements.peek();
        last.appendCharacters(string);
    }

    public void endDocument() {
        if (state != State.END) {
            throw new IllegalStateException("Not in END state");
        }
    }

    public void endElement(QName qName) {

        switch (state) {
        case STREAM:
            if (!XmppConstants.ELEMENT_STREAM.equals(qName)) {
                throw new IllegalStateException("Only " + XmppConstants.ELEMENT_STREAM
                        + " elements are allowed in STREAM state");
            }
            stranzaListener.onStreamEnd(qName);
            state = State.END;
            break;

        case STRANZA:
            final XmlElement xmlElement = xmlElements.pop();
            if (xmlElements.isEmpty()) {
                stranzaListener.onStanza(xmlElement);
                state = State.STREAM;
            }
            break;

        default:
            throw new IllegalStateException("Not in STREAM or STRANZA state");

        }
    }

    public void restart() {
        state = State.BEGIN;
        xmlElements.clear();
    }

    public void startDocument() {
        if (state != State.BEGIN) {
            throw new IllegalStateException("Not in BEGIN state");
        }
    }

    public void startElement(final QName qName, final List<XmlAttribute> xmlAttributes) {

        final XmlElement newElement = new XmlElement(qName, xmlAttributes);

        switch (state) {
        case BEGIN:
            // expecting only stream element

            if (!XmppConstants.ELEMENT_STREAM.equals(qName)) {
                throw new IllegalStateException("Only " + XmppConstants.ELEMENT_STREAM
                        + " elements are allowed in BEGIN state");
            }

            stranzaListener.onStreamBegin(newElement);
            state = State.STREAM;
            break;

        case STREAM:
            state = State.STRANZA;
            xmlElements.push(newElement);
            break;

        case STRANZA:
            XmlElement parent = xmlElements.peek();
            parent.appendChild(newElement);
            xmlElements.push(newElement);
            break;

        default:
            throw new IllegalStateException("Not in BEGIN, STREAM or STRANZA state");
        }
    }

    private static enum State {
        BEGIN, END, STRANZA, STREAM;
    }

}
