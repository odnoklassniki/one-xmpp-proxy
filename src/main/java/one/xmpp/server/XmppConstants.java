package one.xmpp.server;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public interface XmppConstants {

    public static final QName ATTRIBUTE_CLIENT_FROM = new QName("from");
    public static final QName ATTRIBUTE_CLIENT_ID = new QName("id");
    public static final QName ATTRIBUTE_CLIENT_TO = new QName("to");
    public static final QName ATTRIBUTE_CLIENT_TYPE = new QName("type");

    public static final QName ATTRIBUTE_SASL_AUTH_MECHANISM = new QName("mechanism");

    public static final QName ATTRIBUTE_XML_LANG = new QName(XMLConstants.XML_NS_URI, "lang",
            XMLConstants.XML_NS_PREFIX);

    public static final QName ELEMENT_CLIENT_ERROR = new QName(Namespaces.CLIENT, "error", Prefixes.CLIENT);
    public static final QName ELEMENT_CLIENT_IQ = new QName(Namespaces.CLIENT, "iq", Prefixes.CLIENT);
    public static final QName ELEMENT_CLIENT_MESSAGE = new QName(Namespaces.CLIENT, "message", Prefixes.CLIENT);
    public static final QName ELEMENT_CLIENT_MESSAGE_BODY = new QName(Namespaces.CLIENT, "body", Prefixes.CLIENT);
    public static final QName ELEMENT_CLIENT_PRESENCE = new QName(Namespaces.CLIENT, "presence", Prefixes.CLIENT);

    public static final QName ELEMENT_SASL_AUTH = new QName(Namespaces.SASL, "auth", Prefixes.SASL);
    public static final QName ELEMENT_SASL_CHALLENGE = new QName(Namespaces.SASL, "challenge", Prefixes.SASL);
    public static final QName ELEMENT_SASL_MECHANISM = new QName(Namespaces.SASL, "mechanism", Prefixes.SASL);
    public static final QName ELEMENT_SASL_MECHANISMS = new QName(Namespaces.SASL, "mechanisms", Prefixes.SASL);
    public static final QName ELEMENT_SASL_SUCCESS = new QName(Namespaces.SASL, "success", Prefixes.SASL);
    public static final QName ELEMENT_SASL_FAILURE = new QName(Namespaces.SASL, "failure", Prefixes.SASL);

    public static final QName ELEMENT_STREAM = new QName(Namespaces.STREAM, Elements.STREAM_STREAM, Prefixes.STREAM);
    public static final QName ELEMENT_STREAM_ERROR = new QName(Namespaces.STREAM, Elements.STREAM_ERROR,
            Prefixes.STREAM);
    public static final QName ELEMENT_STREAM_FEATURES = new QName(Namespaces.STREAM, Elements.STREAM_FEATURES,
            Prefixes.STREAM);

    public interface Elements {
        public static final String STREAM_ERROR = "error";
        public static final String STREAM_FEATURES = "features";
        public static final String STREAM_STREAM = "stream";
    }

    public interface Namespaces {
        public static final String CLIENT = "jabber:client";
        public static final String SASL = "urn:ietf:params:xml:ns:xmpp-sasl";
        public static final String STANZAS = "urn:ietf:params:xml:ns:xmpp-stanzas";
        public static final String STREAM = "http://etherx.jabber.org/streams";
        public static final String STREAMERROR = "urn:ietf:params:xml:ns:xmpp-streams";
    }

    public interface Prefixes {
        public static final String CLIENT = XMLConstants.DEFAULT_NS_PREFIX;
        public static final String SASL = XMLConstants.DEFAULT_NS_PREFIX;
        public static final String STANZAS = XMLConstants.DEFAULT_NS_PREFIX;
        public static final String STREAM = "stream";
        public static final String STREAMERROR = XMLConstants.DEFAULT_NS_PREFIX;
    }

}
