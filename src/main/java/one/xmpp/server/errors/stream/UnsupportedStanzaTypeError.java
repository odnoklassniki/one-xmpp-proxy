package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The initiating entity has sent a first-level child of the stream that is not supported by the
 * server
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class UnsupportedStanzaTypeError extends XmppStreamError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("unsupported-stanza-type");

    private static final long serialVersionUID = 1L;

    public UnsupportedStanzaTypeError() {
    }

    public UnsupportedStanzaTypeError(String message) {
        super(message);
    }

    public UnsupportedStanzaTypeError(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedStanzaTypeError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
