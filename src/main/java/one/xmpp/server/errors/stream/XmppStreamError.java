package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;
import one.xmpp.server.XmppConstants;
import one.xmpp.utils.CharSequenceUtils;
import one.xmpp.xml.XmlElement;

/**
 * The root stream element MAY contain an <error/> child element that is prefixed by the streams
 * namespace prefix. The error child MUST be sent by a compliant entity (usually a server rather
 * than a client) if it perceives that a stream-level error has occurred.
 * 
 * The following rules apply to stream-level errors:
 * 
 * <ul>
 * 
 * <li>It is assumed that all stream-level errors are unrecoverable; therefore, if an error occurs
 * at the level of the stream, the entity that detects the error MUST send a stream error to the
 * other entity, send a closing &lt;/stream&gt; tag, and terminate the underlying TCP connection.
 * 
 * <li>If the error occurs while the stream is being set up, the receiving entity MUST still send
 * the opening <stream> tag, include the <error/> element as a child of the stream element, send the
 * closing </stream> tag, and terminate the underlying TCP connection. In this case, if the
 * initiating entity provides an unknown host in the 'to' attribute (or provides no 'to' attribute
 * at all), the server SHOULD provide the server's authoritative hostname in the 'from' attribute of
 * the stream header sent before termination.
 * 
 * </ul>
 * 
 * http://www.ietf.org/rfc/rfc3920.txt
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public abstract class XmppStreamError extends Exception {

    private static final long serialVersionUID = 1L;

    protected static ByteBuffer getStandardErrorMessage(String elementName) {
        final QName errorElementName = new QName(XmppConstants.Namespaces.STREAMERROR, elementName,
                XMLConstants.DEFAULT_NS_PREFIX);

        return CharSequenceUtils.encodeToUtf8(//
                new XmlElement(XmppConstants.ELEMENT_STREAM_ERROR, //
                        new XmlElement(errorElementName) //
                ).toXml());
    }

    public XmppStreamError() {
        super();
    }

    public XmppStreamError(String message) {
        super(message);
    }

    public XmppStreamError(String message, Throwable cause) {
        super(message, cause);
    }

    public XmppStreamError(Throwable cause) {
        super(cause);
    }

    @NotNullByDefault
    public abstract void queueWrite(AbstractXmppProxySocket xmppSocket);
}
