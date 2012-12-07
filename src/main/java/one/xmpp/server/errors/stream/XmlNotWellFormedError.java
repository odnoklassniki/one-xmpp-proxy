package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The initiating entity has sent XML that is not well-formed.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class XmlNotWellFormedError extends BadFormatError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("xml-not-well-formed");

    private static final long serialVersionUID = 1L;

    public XmlNotWellFormedError() {
        super();
    }

    public XmlNotWellFormedError(String message) {
        super(message);
    }

    public XmlNotWellFormedError(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlNotWellFormedError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
