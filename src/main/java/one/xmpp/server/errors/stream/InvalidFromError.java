package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The JID or hostname provided in a 'from' address does not match an authorized JID or validated
 * domain negotiated between servers via SASL or dialback, or between a client and a server via
 * authentication and resource binding.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class InvalidFromError extends XmppStreamError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("invalid-from");

    private static final long serialVersionUID = 1L;

    public InvalidFromError() {
    }

    public InvalidFromError(String message) {
        super(message);
    }

    public InvalidFromError(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFromError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
