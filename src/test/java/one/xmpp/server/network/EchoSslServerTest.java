package one.xmpp.server.network;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;

import one.xmpp.TestSslConfiguration;

import one.xmpp.AbstractSpringTest;

public class EchoSslServerTest extends AbstractSpringTest {

    private static SSLSocketFactory socketFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractSpringTest.beforeClass(false, true, EchoServer.class);
        Thread.sleep(1000);

        CertificateFactory x509CertificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate;
        final FileInputStream certStream = new FileInputStream(TestSslConfiguration.CERT);
        try {
            certificate = x509CertificateFactory.generateCertificate(certStream);
        } finally {
            certStream.close();
        }

        KeyStore keyStore = KeyStore.Builder.newInstance(KeyStore.getDefaultType(), null,
                new PasswordProtection("1234".toCharArray())).getKeyStore();
        keyStore.setCertificateEntry("example", certificate);

        final SSLContext sslContext = SSLContext.getInstance("SSLv3");
        sslContext.init(null, new TrustManager[] { new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // okay
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // okay
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } }, null);

        socketFactory = sslContext.getSocketFactory();

    }

    private static void test() throws Exception {
        final SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket("localhost", 5223);
        // final Socket sslSocket = socketFactory.createSocket("localhost", 5222);
        try {
            sslSocket.setEnabledProtocols(sslSocket.getSupportedProtocols());
            sslSocket.setUseClientMode(true);

            final OutputStream outputStream = sslSocket.getOutputStream();
            try {
                final InputStream inputStream = sslSocket.getInputStream();

                try {
                    // send "hi!"
                    outputStream.write("Hello!".getBytes());

                    byte[] buffer = new byte[100];

                    final long start = System.currentTimeMillis();
                    int read = 0;
                    while (read < 6 && (System.currentTimeMillis() - start) < 10000) {
                        read += inputStream.read(buffer, read, buffer.length - read);
                    }
                } finally {
                    inputStream.close();
                }
            } finally {
                outputStream.close();
            }
        } finally {
            sslSocket.close();
        }
    }

    @Test
    public void testConnect() throws Exception {
        test();
    }

    @Test
    public void testMultiple() throws Exception {
        for (int i = 0; i < 1000; i++) {
            test();
        }
    }

}
