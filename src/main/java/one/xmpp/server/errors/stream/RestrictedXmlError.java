package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has attempted to send restricted XML features such as a comment, processing
 * instruction, DTD, entity reference, or unescaped character (see Restrictions (Section 11.1)).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class RestrictedXmlError extends BadFormatError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("restricted-xml");

    private static final long serialVersionUID = 1L;

    public RestrictedXmlError() {
    }

    public RestrictedXmlError(String message) {
        super(message);
    }

    public RestrictedXmlError(String message, Throwable cause) {
        super(message, cause);
    }

    public RestrictedXmlError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
