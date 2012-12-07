package one.xmpp.server.network;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

import one.ejb.NotNullByDefault;

@NotNullByDefault
class PipeSocketsHelper {

    protected static final Log log = LogFactory.getLog(PipeSocketsHelper.class);

    private static final long poolPointer;

    private static final long serverAddressPointer;

    private static final long serverSocketPointer;

    static {
        TomcatNativeLibrary.load();

        poolPointer = Pool.create(0);

        try {
            long inetAddress = Address.info("127.0.0.1", Socket.APR_INET, 0, 0, poolPointer);
            serverSocketPointer = Socket.create(Socket.APR_INET, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP, poolPointer);

            int status = Socket.bind(serverSocketPointer, inetAddress);
            if (status != Status.APR_SUCCESS) {
                throw (new Exception("Can't bind. Error #" + status + ": " + Error.strerror(status)));
            }

            long serverListeningAddressPointer = Address.get(Socket.APR_LOCAL, serverSocketPointer);
            final Sockaddr socketAddress = new Sockaddr();
            if (!Address.fill(socketAddress, serverListeningAddressPointer)) {
                throw new UnsupportedOperationException("Socket address for pipe socket handler #" + socketAddress
                        + " can't be found");
            }

            int remotePort = socketAddress.port;
            String remoteHost = Address.getip(serverListeningAddressPointer);
            log.info("Pipe sockets server started at " + remoteHost + ":" + remotePort);

            serverAddressPointer = Address.info("127.0.0.1", Socket.APR_INET, remotePort, 0, poolPointer);

            status = Socket.listen(serverSocketPointer, 2);
            if (status != Status.APR_SUCCESS) {
                throw (new Exception("Can't listen. Error #" + status + ": " + Error.strerror(status)));
            }

        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    synchronized static long[] newPair() throws Exception {

        final AtomicLong clientSocket = new AtomicLong();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread("PipeSocketsHelper") {
            @Override
            public void run() {
                try {
                    final long pipeInPointer = Socket.create(Socket.APR_INET, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP,
                            poolPointer);

                    int status = Socket.connect(pipeInPointer, serverAddressPointer);
                    if (status != Status.APR_SUCCESS) {
                        throw (new Exception("Can't connect. Error #" + status + ": " + Error.strerror(status)));
                    }

                    clientSocket.set(pipeInPointer);
                    countDownLatch.countDown();
                } catch (Exception exc) {
                    log.fatal(exc, exc);
                }
            }
        }.start();

        countDownLatch.await();
        long serverSide = Socket.accept(serverSocketPointer);

        log.info("New sockets pair " + clientSocket.get() + "<=>" + serverSide);
        return new long[] { clientSocket.get(), serverSide };
    }
}
