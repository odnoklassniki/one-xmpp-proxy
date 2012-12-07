package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has sent invalid XML over the stream to a server that performs validation.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class InvalidXmlError extends BadFormatError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("invalid-xml");

    private static final long serialVersionUID = 1L;

    public InvalidXmlError() {
    }

    public InvalidXmlError(String message) {
        super(message);
    }

    public InvalidXmlError(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidXmlError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
