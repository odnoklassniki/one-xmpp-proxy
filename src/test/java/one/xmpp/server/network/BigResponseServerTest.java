package one.xmpp.server.network;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import one.xmpp.AbstractSpringTest;

public class BigResponseServerTest extends AbstractSpringTest {

    private static SocketFactory socketFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractSpringTest.beforeClass(true, false, BigResponseServer.class);
        Thread.sleep(1000);

        socketFactory = SocketFactory.getDefault();
    }

    private static void testLongImpl() throws Exception {
        final Socket socket = socketFactory.createSocket("localhost", 5222);
        try {

            final OutputStream outputStream = socket.getOutputStream();
            try {
                final InputStream inputStream = socket.getInputStream();

                outputStream.write(new byte[] { 50 });

                try {
                    byte[] buffer = new byte[BigResponseServer.SIZE];
                    int read = 0;
                    while (read < buffer.length) {
                        read += inputStream.read(buffer, read, buffer.length - read);
                    }

                    System.out.println("Total passed: " + read + " of  " + BigResponseServer.SIZE);
                    Assert.assertEquals(BigResponseServer.SIZE, read);
                } finally {
                    inputStream.close();
                }
            } finally {
                outputStream.close();
            }
        } finally {
            socket.close();
        }
    }

    @Test
    public void testLong() throws Exception {
        testLongImpl();
    }
}
