package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;
import one.xmpp.server.AbstractXmppProxyServer.AbstractXmppProxySocket;

/**
 * The entity has sent XML that cannot be processed; this error MAY be used instead of the more
 * specific XML-related errors, such as {@link BadNamespacePrefixError}, {@link InvalidXmlError},
 * {@link RestrictedXmlError}, <unsupported-encoding/>, and {@link XmlNotWellFormedError}, although
 * the more specific errors are preferred.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class BadFormatError extends XmppStreamError {

    private static final ByteBuffer MESSAGE = getStandardErrorMessage("bad-format");

    private static final long serialVersionUID = 1L;

    public BadFormatError() {
    }

    public BadFormatError(String message) {
        super(message);
    }

    public BadFormatError(String message, Throwable cause) {
        super(message, cause);
    }

    public BadFormatError(Throwable cause) {
        super(cause);
    }

    @Override
    @NotNullByDefault
    public void queueWrite(AbstractXmppProxySocket xmppSocket) {
        xmppSocket.queueWrite(MESSAGE);
    }
}
