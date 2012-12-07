package one.xmpp.server;

import org.apache.tomcat.jni.Socket;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import one.ejb.NotNullByDefault;

@ManagedResource
@NotNullByDefault
public interface XmppProxyConfiguration {

    /**
     * The estimated number of concurrently threads. The implementation performs internal sizing to
     * try to accommodate this many threads.
     */
    public static final int CONCURRENCY_LEVEL = 256;

    /**
     * Maximum number of connection that server SHOULD be able to handle. Using at starttime to
     * allocate memory and internal structures, so select a bit more than required. 1.1M is okay to
     * specify.
     */
    @ManagedAttribute
    public abstract int getMaxConnections();

    /**
     * @return number of read pollers (i.e. Poll wrappers) to create and maintain
     */
    @ManagedAttribute
    public abstract int getReadPollersCount();

    /**
     * @return maximum number of bytes that shall be written to socket using
     *         {@link Socket#sendib(long, java.nio.ByteBuffer, int, int)} operation
     */
    @ManagedAttribute
    public abstract int getSocketMaxWriteBytes();

    /**
     * @return maximum time in microseconds that socket can be in read poll without any operation.
     *         Default behavior is to close socket if timeout is reached. Counter is zeroes if ANY
     *         data is received from socket or queued to write to socket (because socket will be
     *         removed from poll and readded after write operation).
     */
    @ManagedAttribute
    public abstract long getSocketReadPollTimeout();

    /**
     * @return maximum time in microseconds that socket can be in write poll without any operation.
     *         Default behavior is to close socket if timeout is reached. Counter is zeroes if ANY
     *         signal is received from poll for this socket.
     */
    @ManagedAttribute
    public abstract long getSocketWritePollTimeout();

    /**
     * @return number of write pollers (i.e. Poll wrappers) to create and maintain
     */
    @ManagedAttribute
    public abstract int getWritePollersCount();

    /**
     * <p>
     * The number of outstanding connections allowed in the sockets listen queue. If this value is
     * less than zero, the listen queue size is set to zero. (APR documentaion)
     * 
     * <p>
     * The backlog argument provides a hint to the implementation which the implementation shall use
     * to limit the number of outstanding connections in the socket’s listen queue. Implementations
     * may impose a limit on backlog and silently reduce the specified value. Normally, a larger
     * backlog argument value shall result in a larger or equal length of the listen queue.
     * Implementations shall support values of backlog up to SOMAXCONN, defined in socket.h. The
     * implementation may include incomplete connections in its listen queue. The limits on the
     * number of incomplete connections and completed connections queued may be different. The
     * implementation may have an upper limit on the length of the listen queue-either global or per
     * accepting socket. If backlog exceeds this limit, the length of the listen queue is set to the
     * limit. If listen() is called with a backlog argument value that is less than 0, the function
     * behaves as if it had been called with a backlog argument value of 0. A backlog argument of 0
     * may allow the socket to accept connections, in which case the length of the listen queue may
     * be set to an implementation-defined minimum value. (POSIX documentation)
     * 
     * @see Socket#listen(long, int)
     */
    @ManagedAttribute
    int socketListenBacklog();
}
