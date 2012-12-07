package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The initiating entity did not provide a mechanism or requested a mechanism that is not supported
 * by the receiving entity; sent in reply to an <auth/> element.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class SaslInvalidMechanismError extends SaslError {

    private static final ByteBuffer MESSAGE = getSaslErrorMessage("invalid-mechanism");

    private static final long serialVersionUID = 1L;

    public SaslInvalidMechanismError() {
        super();
    }

    public SaslInvalidMechanismError(String message) {
        super(message);
    }

    public SaslInvalidMechanismError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslInvalidMechanismError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
