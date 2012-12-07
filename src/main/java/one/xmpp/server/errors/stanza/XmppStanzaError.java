package one.xmpp.server.errors.stanza;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;
import one.xmpp.server.XmppConstants;
import one.xmpp.xml.XmlAttribute;
import one.xmpp.xml.XmlElement;

/**
 * Stanza-related errors are handled in a manner similar to stream errors (Section 4.7). However,
 * unlike stream errors, stanza errors are recoverable; therefore error stanzas include hints
 * regarding actions that the original sender can take in order to remedy the error.
 * 
 * The following rules apply to stanza-related errors:
 * 
 * <ul>
 * 
 * <li>The receiving or processing entity that detects an error condition in relation to a stanza
 * MUST return to the sending entity a stanza of the same kind (message, presence, or IQ), whose
 * 'type' attribute is set to a value of "error" (such a stanza is called an "error stanza" herein).
 * 
 * <li>The entity that generates an error stanza SHOULD include the original XML sent so that the
 * sender can inspect and, if necessary, correct the XML before attempting to resend.
 * 
 * <li>An error stanza MUST contain an &lt;error/&gt; child element.
 * 
 * <li>An &lt;error/&gt; child MUST NOT be included if the 'type' attribute has a value other than
 * "error" (or if there is no 'type' attribute).
 * 
 * <li>An entity that receives an error stanza MUST NOT respond to the stanza with a further error
 * stanza; this helps to prevent looping.
 * 
 * </ul>
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class XmppStanzaError extends Exception {

    private static final QName QNAME_ID = new QName("id");
    private static final QName QNAME_TO = new QName("to");
    private static final QName QNAME_TYPE = new QName("type");

    private static final long serialVersionUID = 1L;

    private final String condition;

    private final StanzaErrorType errorType;

    public XmppStanzaError(String condition, StanzaErrorType errorType) {
        this.condition = condition;
        this.errorType = errorType;
    }

    public XmppStanzaError(String message, String condition, StanzaErrorType errorType) {
        super(message);
        this.condition = condition;
        this.errorType = errorType;
    }

    public XmppStanzaError(Throwable cause, String condition, StanzaErrorType errorType) {
        super(cause);
        this.condition = condition;
        this.errorType = errorType;
    }

    public XmlElement toXmlElement(AbstractXmppProxySocket xmppSocket, XmlElement originalStanza) {

        List<XmlAttribute> attributes = new ArrayList<XmlAttribute>(2);

        if (StringUtils.isNotEmpty(xmppSocket.getJid())) {
            attributes.add(new XmlAttribute(QNAME_TO, xmppSocket.getJid()));
        }
        if (StringUtils.isNotEmpty(originalStanza.getAttributeValue(QNAME_ID))) {
            attributes.add(new XmlAttribute(QNAME_ID, originalStanza.getAttributeValue(QNAME_ID)));
        }
        attributes.add(new XmlAttribute(QNAME_TYPE, "error"));

        XmlElement xmlElement = new XmlElement(originalStanza.getQName(), attributes);

        XmlElement errorElement = new XmlElement(new QName(originalStanza.getQName().getNamespaceURI(), "error"),
                new XmlAttribute(QNAME_TYPE, errorType.getType()));
        errorElement.appendChild(new XmlElement(new QName(XmppConstants.Namespaces.STANZAS, condition)));
        xmlElement.appendChild(errorElement);

        return xmlElement;
    }
}
