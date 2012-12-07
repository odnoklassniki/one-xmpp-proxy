package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has sent a namespace prefix that is unsupported, or has sent no namespace prefix on an
 * element that requires such a prefix (see XML Namespace Names and Prefixes (Section 11.2)).
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@NotNullByDefault
public class BadNamespacePrefixError extends BadFormatError {

    private static final long serialVersionUID = 1L;

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("bad-namespace-prefix");

    public BadNamespacePrefixError() {
        super();
    }

    public BadNamespacePrefixError(String message, Throwable cause) {
        super(message, cause);
    }

    public BadNamespacePrefixError(String message) {
        super(message);
    }

    public BadNamespacePrefixError(Throwable cause) {
        super(cause);
    }

    @Override
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }

}
