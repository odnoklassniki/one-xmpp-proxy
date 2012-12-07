package one.xmpp.xml;

import one.xmpp.server.errors.stream.BadFormatError;

public interface XmlParsingErrorHandler {

    void onXmlParsingError(BadFormatError exc);

}
