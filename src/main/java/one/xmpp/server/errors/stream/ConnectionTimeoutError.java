package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has not generated any traffic over the stream for some period of time (configurable
 * according to a local service policy).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@NotNullByDefault
public class ConnectionTimeoutError extends XmppStreamError {

    private static final long serialVersionUID = 1L;

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("connection-timeout");

    public ConnectionTimeoutError() {
        super();
    }

    public ConnectionTimeoutError(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionTimeoutError(String message) {
        super(message);
    }

    public ConnectionTimeoutError(Throwable cause) {
        super(cause);
    }

    @Override
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
