package one.xmpp.server.network;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Socket;

import one.ejb.NotNullByDefault;

@NotNullByDefault
class AcceptorThread extends Thread {

    private static final Log log = LogFactory.getLog(AcceptorThread.class);

    private final AtomicLong counter = new AtomicLong(1);

    private final SocketAcceptedListener listener;

    private final long serverSocketPointer;

    private volatile boolean stop = false;

    public AcceptorThread(final String name, final long serverSocketPointer, final SocketAcceptedListener listener) {
        super(name);
        setDaemon(true);

        this.serverSocketPointer = serverSocketPointer;
        this.listener = listener;
    }

    @Override
    public void run() {
        while (!stop) {
            try {

                final long clientSocketPointer = Socket.accept(serverSocketPointer);

                if (log.isDebugEnabled()) {
                    log.debug("Accepted connection #" + counter.getAndIncrement() + " at client socket "
                            + clientSocketPointer);
                }

                listener.onAcceptedSocket(clientSocketPointer);

            } catch (org.apache.tomcat.jni.Error exc) {

                if (730004 == exc.getError() && stop) {
                    // shutdown in progress
                    break;
                }

                log.error("Unable to accept connection from server socket: Error #" + exc.getError() + ": " + exc, exc);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            } catch (Exception exc) {
                log.error("Unable to accept connection from server socket: " + exc, exc);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
        }
    }

    void setStop(boolean stop) {
        this.stop = stop;
    }

    public interface SocketAcceptedListener {
        void onAcceptedSocket(long clientSocketPointer);
    }

}