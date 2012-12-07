package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has violated some local service policy; the server MAY choose to specify the policy in
 * the <text/> element or an application-specific condition element.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class PolicyViolationError extends XmppStreamError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("policy-violation");

    private static final long serialVersionUID = 1L;

    public PolicyViolationError() {
    }

    public PolicyViolationError(String message) {
        super(message);
    }

    public PolicyViolationError(String message, Throwable cause) {
        super(message, cause);
    }

    public PolicyViolationError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
