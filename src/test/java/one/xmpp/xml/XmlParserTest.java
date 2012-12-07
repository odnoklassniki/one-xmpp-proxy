package one.xmpp.xml;


import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import javax.xml.namespace.QName;

import org.junit.Test;

import one.ejb.NotNullByDefault;
import one.xmpp.server.errors.stream.BadFormatError;
import one.xmpp.utils.CharSequenceUtils;

public class XmlParserTest {

    @Test
    public void test() throws Exception {

        final ByteBuffer data = CharSequenceUtils
                .encodeToUtf8("<?xml version=\"1.0\"?>\n"
                        + "<stream:stream to=\"vlsergey\" xml:lang=\"en\" version=\"1.0\" xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\">\n"
                        + "<iq type=\"set\" id=\"550776446\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"><resource>rumosodkl-0168768531000</resource></bind></iq>\n"
                        + "</stream:stream>");

        new XmlParser(new XmlConstructor(new EmpryStanzaListener()), new ErrorHandlerImpl()).parse(data);

    }

    @Test
    public void testUtf8ByBytes() throws Exception {

        final ByteBuffer data = CharSequenceUtils
                .encodeToUtf8("<?xml version=\"1.0\"?>\n"
                        + "<stream:stream to=\"vlsergey\" xml:lang=\"en\" version=\"1.0\" xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\">\n"
                        + "<iq type=\"set\" id=\"550776446\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"><resource>Это ID-ресурса</resource></bind></iq>\n"
                        + "</stream:stream>");

        XmlParser xmlParser = new XmlParser(new XmlConstructor(new EmpryStanzaListener()), new ErrorHandlerImpl());

        for (int i = 0; i < data.limit(); i++) {
            xmlParser.parse(ByteBuffer.wrap(new byte[] { data.get(i) }));
        }

    }

    @NotNullByDefault
    private static final class EmpryStanzaListener implements StanzaListener {
        @Override
        public void onStanza(XmlElement xmlElement) {
            // no op
        }

        @Override
        public void onStreamBegin(XmlElement xmlElement) {
            // no op
        }

        @Override
        public void onStreamEnd(QName qName) {
            // no op
        }
    }

    private static final class ErrorHandlerImpl implements XmlParsingErrorHandler {

        @Override
        public void onXmlParsingError(BadFormatError exc) {
            fail(exc.toString());
        }

    }

}
