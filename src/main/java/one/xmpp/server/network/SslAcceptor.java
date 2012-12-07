package one.xmpp.server.network;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import one.ejb.NotNullByDefault;
import one.xmpp.server.XmppProxyConfiguration;
import one.xmpp.utils.AsyncOperationsExecutor;

/**
 * Accepts new connections AND handles SSL handshakes
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@Component
@NotNullByDefault
public class SslAcceptor implements AcceptorThread.SocketAcceptedListener {

    private static final String ADDRESS = "0.0.0.0";

    private static final Log logger = LogFactory.getLog(SslAcceptor.class);

    private static final int PORT = 5223;

    static {
        TomcatNativeLibrary.load();
    }

    private AcceptorThread acceptorThread;

    @Autowired
    private AsyncOperationsExecutor asyncOperationsExecutor;

    private long inetAddress = 0;

    private long poolPointer;

    @Resource
    private AbstractServer server;

    private long serverSocketPointer;

    @Autowired
    private SslConfiguration sslConfiguration;

    public long sslServerContextPointer;

    @Autowired
    private XmppProxyConfiguration xmppServiceConfiguration;

    AbstractServer getServer() {
        return server;
    }

    @Override
    public void onAcceptedSocket(long clientSocketPointer) {
        asyncOperationsExecutor.submit(new SslSocketAcceptTask(clientSocketPointer));
    }

    void setAsyncOperationsExecutor(AsyncOperationsExecutor asyncOperationsExecutor) {
        this.asyncOperationsExecutor = asyncOperationsExecutor;
    }

    void setServer(AbstractServer sslServer) {
        this.server = sslServer;
    }

    void setSslConfiguration(SslConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    void setXmppServiceConfiguration(XmppProxyConfiguration xmppServiceConfiguration) {
        this.xmppServiceConfiguration = xmppServiceConfiguration;
    }

    @PostConstruct
    public void start() throws Exception {

        poolPointer = Pool.create(0);

        /* Create SSL Context, one for each Virtual Host */
        sslServerContextPointer = SSLContext.make(poolPointer, SSL.SSL_PROTOCOL_SSLV2 | SSL.SSL_PROTOCOL_SSLV3
                | SSL.SSL_PROTOCOL_TLSV1, SSL.SSL_MODE_SERVER);
        sslConfiguration.setupSslContext(sslServerContextPointer);

        logger.info("Accepting: " + ADDRESS + ":" + PORT);

        inetAddress = Address.info(ADDRESS, Socket.APR_INET, PORT, 0, poolPointer);
        serverSocketPointer = Socket.create(Socket.APR_INET, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP, poolPointer);
        Socket.optSet(serverSocketPointer, Socket.APR_TCP_DEFER_ACCEPT, 1);

        int status = Socket.bind(serverSocketPointer, inetAddress);
        if (status != Status.APR_SUCCESS) {

            if (status == 730048) {
                throw (new Exception("Can't create Acceptor. IP address and port " + ADDRESS + ":" + PORT
                        + " is already occupied by other application."));
            }

            throw (new Exception("Can't create Acceptor. Error #" + status + ": " + Error.strerror(status)));
        }
        Socket.listen(serverSocketPointer, xmppServiceConfiguration.socketListenBacklog());

        acceptorThread = new AcceptorThread("AcceptorThread-" + PORT, serverSocketPointer, this);
        acceptorThread.start();
    }

    @PreDestroy
    public void stop() {
        acceptorThread.setStop(true);
        Socket.destroy(serverSocketPointer);
        Pool.destroy(poolPointer);
    }

    private final class SslSocketAcceptTask implements Runnable {
        private final long clientSocketPointer;

        private SslSocketAcceptTask(long clientSocketPointer) {
            this.clientSocketPointer = clientSocketPointer;
        }

        @Override
        public void run() {
            boolean done = false;
            try {
                Socket.timeoutSet(clientSocketPointer, 10 * 1000 * 1000);
                SSLSocket.attach(sslServerContextPointer, clientSocketPointer);
                final int sslStatus = SSLSocket.handshake(clientSocketPointer);

                if (sslStatus != Status.APR_SUCCESS) {
                    logger.debug("SSL handshake failed: Error #" + sslStatus + ": " + SSL.getLastError());
                    return;
                }

                server.accepted(clientSocketPointer, true);
                done = true;

            } catch (Exception exc) {

                logger.error("Socket not accepted: " + exc);

            } finally {
                if (!done) {
                    AprUtils.closeAndDestroySocketSafe(clientSocketPointer);
                }
            }
        }

        @Override
        public String toString() {
            return "SslSocketAcceptTask [" + clientSocketPointer + "]";
        }
    }

}