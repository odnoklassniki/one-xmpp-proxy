package one.xmpp.server.errors.stream;

import java.nio.ByteBuffer;

import javax.xml.namespace.QName;

import one.xmpp.server.XmppConstants;
import one.xmpp.utils.CharSequenceUtils;
import one.xmpp.xml.XmlElement;

public abstract class SaslError extends XmppStreamError {

    private static final long serialVersionUID = 1L;

    protected static ByteBuffer getSaslErrorMessage(String elementName) {
        final QName qName = new QName(XmppConstants.Namespaces.SASL, elementName, XmppConstants.Prefixes.SASL);

        return CharSequenceUtils.encodeToUtf8(//
                new XmlElement(XmppConstants.ELEMENT_SASL_FAILURE,//
                        new XmlElement(qName)//
                ).toXml());
    }

    public SaslError() {
        super();
    }

    public SaslError(String message) {
        super(message);
    }

    public SaslError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslError(Throwable cause) {
        super(cause);
    }

}
