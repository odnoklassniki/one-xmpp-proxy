package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The authentication failed because the initiating entity did not provide valid credentials (this
 * includes but is not limited to the case of an unknown username); sent in reply to a <response/>
 * element or an <auth/> element with initial response data.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class SaslNotAuthorizedError extends SaslError {

    private static final ByteBuffer MESSAGE = getSaslErrorMessage("not-authorized");

    private static final long serialVersionUID = 1L;

    public SaslNotAuthorizedError(String message) {
        super(message);
    }

    public SaslNotAuthorizedError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslNotAuthorizedError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
