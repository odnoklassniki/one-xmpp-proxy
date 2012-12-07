package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The authentication failed because of a temporary error condition within the receiving entity;
 * sent in reply to an &lt;auth/&gt; element or &lt;response/&gt; element
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class SaslTemporaryAuthFailureError extends SaslError {

    private static final ByteBuffer MESSAGE = getSaslErrorMessage("temporary-auth-failure");

    private static final long serialVersionUID = 1L;

    public SaslTemporaryAuthFailureError() {
        super();
    }

    public SaslTemporaryAuthFailureError(String message) {
        super(message);
    }

    public SaslTemporaryAuthFailureError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslTemporaryAuthFailureError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
