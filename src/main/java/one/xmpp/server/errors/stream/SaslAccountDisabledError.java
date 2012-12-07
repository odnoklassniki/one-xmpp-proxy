package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The account of the initiating entity has been temporarily disabled;
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class SaslAccountDisabledError extends SaslError {

    private static final ByteBuffer MESSAGE = getSaslErrorMessage("account-disabled");

    private static final long serialVersionUID = 1L;

    public SaslAccountDisabledError(String message) {
        super(message);
    }

    public SaslAccountDisabledError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslAccountDisabledError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
