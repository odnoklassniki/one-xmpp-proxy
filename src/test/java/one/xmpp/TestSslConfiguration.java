package one.xmpp;

import java.io.File;

import org.apache.tomcat.jni.SSLContext;
import org.springframework.stereotype.Component;

import one.xmpp.server.network.SslConfiguration;

@Component
public class TestSslConfiguration implements SslConfiguration {

    public static File CHAIN, CERT, KEY;

    public static String PASSWORD;

    static {
        try {
            CHAIN = new File(TestSslConfiguration.class.getResource("/one-xmpp-proxy-example-all.pem").toURI());
            CERT = new File(TestSslConfiguration.class.getResource("/one-xmpp-proxy-example-cert.pem").toURI());
            KEY = new File(TestSslConfiguration.class.getResource("/one-xmpp-proxy-example-key.pem").toURI());
            PASSWORD = "1234";
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    @Override
    public void setupSslContext(long contextPointer) throws Exception {
        SSLContext.setCertificateChainFile(contextPointer, CHAIN.getAbsolutePath(), false);
        SSLContext.setCertificate(contextPointer, CERT.getAbsolutePath(), KEY.getAbsolutePath(), PASSWORD, 0);
    }

}
