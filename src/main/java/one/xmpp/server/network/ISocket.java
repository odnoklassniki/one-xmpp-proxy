package one.xmpp.server.network;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public interface ISocket {

    String dump();

    long getConnectionTime();

    long getLastDataReadTime();

    long getLastDataSentTime();

    /**
     * @return some internal per-server unique ID. It is unique at every point of time for all
     *         opened sockets, but can be reused if socket is closed.
     */
    long getSocketId();

    boolean isClosed();

    boolean isQueueEmpty();

    void queue(SocketOperation operation);

    void queueClose(String closeReasonCode, boolean next);

    void queueWrite(ByteBuffer buffer);

    void updateLastDataReadTime();

    void updateLastDataSentTime();
}
