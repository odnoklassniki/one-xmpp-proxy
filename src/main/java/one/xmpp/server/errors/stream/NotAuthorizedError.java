package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has attempted to send data before the stream has been authenticated, or otherwise is
 * not authorized to perform an action related to stream negotiation; the receiving entity MUST NOT
 * process the offending stanza before sending the stream error.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class NotAuthorizedError extends XmppStreamError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("not-authorized");

    private static final long serialVersionUID = 1L;

    public NotAuthorizedError() {
    }

    public NotAuthorizedError(String message) {
        super(message);
    }

    public NotAuthorizedError(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAuthorizedError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
