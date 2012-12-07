package one.xmpp.server.network;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;

import one.ejb.NotNullByDefault;
import one.xmpp.server.XmppProxyConfiguration;
import one.xmpp.utils.AsyncOperationsExecutor;
import one.xmpp.utils.CharSequenceUtils;

/**
 * Abstract server, not linked to any particular protocol.
 * 
 * Partially based on SSL Server server example by Mladen Turk, released under ASL 2.0 license.
 */
@Component
@ManagedResource
@NotNullByDefault
public abstract class AbstractServer {

    private static final Log log = LogFactory.getLog(AbstractServer.class);

    static {
        TomcatNativeLibrary.load();
    }

    @Autowired
    private AsyncOperationsExecutor asyncOperationsExecutor;

    @Autowired
    private NetworkOperationsLogger opLogger;

    @Autowired
    private ReadPollers readPollers;

    private final AtomicLong receivedBytes = new AtomicLong(0);

    private final AtomicLong sentBytes = new AtomicLong(0);

    private final AtomicLong sentPackets = new AtomicLong(0);

    private Map<Long, AbstractSocket> sockets;

    private volatile boolean stop = false;

    @Autowired
    private WritePollers writePollers;

    @Autowired
    private XmppProxyConfiguration xmppProxyConfiguration;

    void accepted(final long clientSocketPointer, boolean secured) throws Exception {
        AbstractSocket iSocket = newISocket(clientSocketPointer, secured);

        Socket.optSet(clientSocketPointer, Socket.APR_SO_REUSEADDR, 1);
        Socket.optSet(clientSocketPointer, Socket.APR_SO_NONBLOCK, 1);
        Socket.optSet(clientSocketPointer, Socket.APR_TCP_NODELAY, 1);
        Socket.timeoutSet(clientSocketPointer, 0);

        handleAccepted(iSocket);

        log.info("Connection from " + iSocket + " accepted. Total " + sockets.size() + " sockets.");
    }

    protected void addToReadPoll(AbstractSocket socket) {
        socket.lastReadPollAddTime = System.currentTimeMillis();
        readPollers.addToPollQueue(socket);
    }

    protected void addToWritePoll(AbstractSocket socket) {
        socket.lastWritePollAddTime = System.currentTimeMillis();
        writePollers.addToPollQueue(socket);
    }

    final void canReadWithoutBlocking(final long clientSocketPointer) {
        try {
            asyncOperationsExecutor.submit(new CanReadTask(clientSocketPointer));
        } catch (RejectedExecutionException exc) {
            if (stop) {
                log.debug("Rejected read task scheduling for socket #" + clientSocketPointer
                        + " because shutdown in progress");
                return;
            }
            throw exc;
        }
    }

    final void canWriteWithoutBlocking(final long clientSocketPointer) {
        try {
            asyncOperationsExecutor.submit(new CanWriteTask(clientSocketPointer));
        } catch (RejectedExecutionException exc) {
            if (stop) {
                log.debug("Rejected write task scheduling for socket #" + clientSocketPointer
                        + " because shutdown in progress");
                return;
            }
            throw exc;
        }
    }

    @ManagedOperation
    public String dumpSocketInfo(long clientSocketId) {
        AbstractSocket socket = sockets.get(Long.valueOf(clientSocketId));
        if (socket == null) {
            return "no such socket";
        }
        return socket.dump();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getActiveSockets() {
        return sockets.size();
    }

    public ISocket[] getActiveSocketsArray(ISocket[] sockets) {
        return this.sockets.values().toArray(sockets);
    }

    /**
     * @return internal array of open sockets. Use with care.
     */
    protected Map<Long, AbstractSocket> getSocketsInternal() {
        return this.sockets;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getReceivedBytes() {
        return receivedBytes.longValue();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getSentBytes() {
        return sentBytes.longValue();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getSentPackets() {
        return sentPackets.longValue();
    }

    protected abstract void handleAccepted(AbstractSocket socket);

    void handleHangUp(final long clientSocketPointer) {
        asyncOperationsExecutor.submit(new OnHangupTask(clientSocketPointer));
    }

    void handleMaintain(final long clientSocketPointer) {
        asyncOperationsExecutor.submit(new OnMaintainTask(clientSocketPointer));
    }

    void handlePendingError(final long clientSocketPointer) {
        asyncOperationsExecutor.submit(new OnPendingErrorTask(clientSocketPointer));
    }

    protected abstract void handleRead(AbstractSocket socket, final ByteBuffer readBuffer);

    protected abstract AbstractSocket newISocket(final long clientSocketPointer, boolean secured) throws Exception;

    /**
     * Process (in sync) any previously scheduled operations for socket.
     */
    protected void processSocketOperations(AbstractSocket socket) {
        boolean removed = readPollers.removeFromToScheduleOperationsCycle(socket);
        if (removed && log.isTraceEnabled()) {
            log.trace(socket + " removed from 'stop-polling-queue' set because processSocketOperations() is started");
        }

        SocketOperation socketOperation;
        do {

            if (socket.isClosed()) {
                if (log.isTraceEnabled()) {
                    log.trace(socket + " is closed. Stop processing.");
                }
                return;
            }

            synchronized (socket.operationsLock) {
                socketOperation = socket.operations.peekFirst();
            }

            if (socketOperation != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Executing " + socketOperation + " for " + socket);
                }

                boolean stopCycle = socketOperation.run(socket);

                if (socketOperation.isComplete()) {
                    // remove from queue
                    synchronized (socket.operationsLock) {
                        socket.operations.remove(socketOperation);
                    }
                }

                if (stopCycle) {
                    if (log.isTraceEnabled()) {
                        log.trace("Operation " + socketOperation + " asked not to continue operations cycle for "
                                + socket + " (something is scheduled)");
                    }
                    return;
                }
            }
        } while (socketOperation != null);

        if (socket.isClosed()) {
            if (log.isTraceEnabled()) {
                log.trace(socket + " is closed. Stop processing.");
            }
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("No operations left in queue for " + socket + ". Return to read queue");
        }
        addToReadPoll(socket);

        return;
    }

    protected void scheduleOperationsCycle(final AbstractSocket iSocket) {
        iSocket.assertNotClosed();
        asyncOperationsExecutor.submit(new ProcessSocketOperationsTask(iSocket));
    }

    protected void scheduleOperationsCycleLater(AbstractSocket socket) {
        if (socket.processingAsyncOperation) {
            // not needed, we already in cycle
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Adding " + socket + " to 'stop-polling-queue' set");
        }

        readPollers.addToScheduleOperationsCycle(socket);
    }

    void setAsyncOperationsExecutor(AsyncOperationsExecutor asyncOperationsExecutor) {
        this.asyncOperationsExecutor = asyncOperationsExecutor;
    }

    @PostConstruct
    public void start() {
        this.sockets = new ConcurrentHashMap<Long, AbstractSocket>(xmppProxyConfiguration.getMaxConnections(),
                XmppProxyConfiguration.CONCURRENCY_LEVEL);
    }

    @PreDestroy
    public void stop() {
        this.stop = true;

        for (AbstractSocket iSocket : sockets.values()) {
            iSocket.queueClose(true);
        }
    }

    protected class AbstractSocket implements ISocket {

        private final long clientSocketPointer;

        private final Long clientSockId;

        private volatile boolean closed = false;

        private final long connectionTime = System.currentTimeMillis();

        private long lastCanReadTime;

        private long lastCanWriteTime;

        private final AtomicLong lastDataReadTime = new AtomicLong(System.currentTimeMillis());

        private final AtomicLong lastDataSentTime = new AtomicLong(System.currentTimeMillis());

        private long lastReadPollAddTime;

        private long lastWritePollAddTime;

        /**
         * Socket operations queue. Usefull for writing and other operations. All operations are
         * executed in specified order, if no operations left and socket is not closed -- it is
         * returned to READ poll.
         */
        protected final LinkedList<SocketOperation> operations = new LinkedList<SocketOperation>();

        protected final Object operationsLock = new Object();

        /**
         * If this flag is set, that means socket is NOT in poll, and not need to be added to
         * stop-poll-queue to execute another operation (it will be started after the current one)
         */
        private volatile boolean processingAsyncOperation = false;

        // 1 Kb read buffer
        private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1 << 10);

        protected final AtomicLong readBytes = new AtomicLong(0);

        /**
         * Client IP address (in numeric address string format)
         */
        private final InetAddress remoteIp;

        private final int remotePort;

        private final AtomicLong sentBytes = new AtomicLong(0);

        protected AbstractSocket(long clientSock) throws Exception {
            this.clientSockId = Long.valueOf(clientSock);
            this.clientSocketPointer = clientSock;

            final long socketRemoteAddressPointer = Address.get(Socket.APR_REMOTE, clientSock);
            final Sockaddr socketAddress = new Sockaddr();
            if (!Address.fill(socketAddress, socketRemoteAddressPointer)) {
                throw new UnsupportedOperationException("Socket address for client socket handler #" + clientSock
                        + " can't be found");
            }
            this.remotePort = socketAddress.port;
            this.remoteIp = InetAddress.getByName(Address.getip(socketRemoteAddressPointer));

            // set internal buffer to be used for all read operations
            Socket.setrbb(clientSock, readBuffer);

            sockets.put(clientSockId, this);
        }

        protected final void assertNotClosed() {
            if (closed) {
                throw new IllegalStateException(this + " is closed already");
            }
        }

        protected synchronized void close() {
            if (closed) {
                return;
            }

            if (log.isInfoEnabled()) {
                log.info("Closing " + this);
            }

            // set flag first so noone will try to access it using sockets array
            closed = true;

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Removing " + this + " from read poll");
                }
                readPollers.remove(this);
            } catch (Throwable exc) {
                log.error("Unable to remove " + this + " from read poll: " + exc, exc);
            }

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Removing " + this + " from write poll");
                }
                writePollers.remove(this);
            } catch (Throwable exc) {
                log.error("Unable to remove " + this + " from write poll: " + exc, exc);
            }

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Removing " + this + " from sockets collection");
                }
                sockets.remove(clientSockId);
            } catch (Throwable exc) {
                log.error("Unable to remove " + this + " from sockets collection: " + exc, exc);
            }

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Nativelly close and destroy " + this);
                }
                AprUtils.closeAndDestroySocketSafe(clientSocketPointer);
            } catch (Throwable exc) {
                log.error("Unable to nativelly close and destroy " + this + ": " + exc, exc);
            }

            // just for nice logs
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Cleanup operation queue for " + this);
                }
                synchronized (operationsLock) {
                    operations.clear();
                }
            } catch (Throwable exc) {
                log.error("Unable to cleanup operation queue for " + this + ": " + exc, exc);
            }

            if (log.isInfoEnabled()) {
                log.info("Closed and destroyed " + this);
            }
        }

        @Override
        public String dump() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this).append("\n");

            stringBuilder.append("\tClosed: ").append(closed).append("\n");
            stringBuilder.append('\t').append(clientSocketPointer).append('\n');
            stringBuilder.append('\t').append(getRemoteIp()).append(':').append(getRemotePort()).append('\n');
            stringBuilder.append("\tRead bytes: \t").append(readBytes).append('\n');
            stringBuilder.append("\tSent bytes: \t").append(sentBytes).append('\n');
            stringBuilder.append("\tLast data read time: \t").append(lastDataReadTime.get()).append('\t')
                    .append(new Date(lastDataReadTime.get())).append('\n');
            stringBuilder.append("\tLast data sent time: \t").append(lastDataSentTime.get()).append('\t')
                    .append(new Date(lastDataSentTime.get())).append('\n');

            stringBuilder.append("\tLast read poll add time: \t").append(lastReadPollAddTime).append('\t')
                    .append(new Date(lastReadPollAddTime)).append('\n');
            stringBuilder.append("\tLast can read time: \t").append(lastCanReadTime).append('\t')
                    .append(new Date(lastCanReadTime)).append('\n');
            stringBuilder.append("\tLast write poll add time: \t").append(lastWritePollAddTime).append('\t')
                    .append(new Date(lastWritePollAddTime)).append('\n');
            stringBuilder.append("\tLast can write time: \t").append(lastCanWriteTime).append('\t')
                    .append(new Date(lastCanWriteTime)).append('\n');

            dumpImpl(stringBuilder);

            stringBuilder.append("\tProcessing async operation flag: \t").append(processingAsyncOperation).append('\n');
            stringBuilder.append("\tOperations queue: \t").append(operations).append('\n');

            return stringBuilder.toString();
        }

        @SuppressWarnings("unused")
        protected void dumpImpl(StringBuilder stringBuilder) {
            // no default implementation
        }

        public long getClientSocketPointer() {
            return clientSocketPointer;
        }

        @Override
        public long getConnectionTime() {
            return connectionTime;
        }

        @Override
        public long getLastDataReadTime() {
            return lastDataReadTime.get();
        }

        @Override
        public long getLastDataSentTime() {
            return lastDataSentTime.get();
        }

        /**
         * @return client IP address
         */
        public InetAddress getRemoteIp() {
            return remoteIp;
        }

        public int getRemotePort() {
            return remotePort;
        }

        @Override
        public long getSocketId() {
            return clientSocketPointer;
        }

        void handleCanRead() {
            readBuffer.clear();

            // just to be sure it still set
            Socket.optSet(clientSocketPointer, Socket.APR_SO_NONBLOCK, 1);
            Socket.timeoutSet(clientSocketPointer, 1000);

            int read = Socket.recvbb(clientSocketPointer, 0, readBuffer.capacity());

            if (read == 0) {
                opLogger.onSocketReadZeroBytes();
                log.info("Socket::read for " + this + " returned 0. Closing it");
                this.queueClose(true);
                return;
            }

            if (read < 0) {
                opLogger.onSocketReadError(-read);

                if (-read == Status.TIMEUP) {
                    // need to return back to poller
                    return;
                }

                log.warn("Socket::read returned " + read + " for " + this + " (" + Error.strerror(-read)
                        + "). Closing it.");
                this.queueClose(true);
                return;
            }

            opLogger.onSocketReadBytes(read);

            readBuffer.position(0);
            readBuffer.limit(read);

            if (log.isTraceEnabled()) {
                log.trace("Received " + read + " bytes from " + this);
                if (read > 0) {
                    log.trace("\t'" + CharSequenceUtils.decodeFromUtf8Safe(readBuffer) + "'");
                }
            }

            AbstractServer.this.receivedBytes.addAndGet(read);
            this.readBytes.addAndGet(read);
            updateLastDataReadTime();

            handleRead(this, readBuffer);
        }

        int handleCanWrite() {
            // extract WriteOperation from queue
            SocketOperation socketOperation = peekFirstSocketOperation();
            if (!(socketOperation instanceof SocketWriteOpeation)) {
                throw new IllegalStateException("Queued operation is not SocketWriteOpeation");
            }

            final ByteBuffer byteBuffer = ((SocketWriteOpeation) socketOperation).byteBuffer;
            final int position = byteBuffer.position();
            final int remaining = byteBuffer.remaining();
            final int toSend = Math.min(remaining, xmppProxyConfiguration.getSocketMaxWriteBytes());

            if (log.isTraceEnabled()) {
                log.trace("Sending " + toSend + " bytes of (" + remaining + ") from " + byteBuffer + " to " + this);
            }

            // just to be sure it still set
            Socket.optSet(clientSocketPointer, Socket.APR_SO_NONBLOCK, 1);
            Socket.timeoutSet(clientSocketPointer, 1000);

            int sent;
            if (byteBuffer.isDirect()) {
                sent = Socket.sendb(clientSocketPointer, byteBuffer, position, toSend);
            } else {
                sent = Socket.send(clientSocketPointer, byteBuffer.array(), position, toSend);
            }

            if (sent == 0) {
                opLogger.onSocketWriteZeroBytes();
            }

            if (sent < 0) {
                opLogger.onSocketWriteError(-sent);

                log.error("Unable to sent data to " + this + ", error #" + (-sent) + ": " + Error.strerror(-sent));
                queueClose(true);
                return sent;
            }

            opLogger.onSocketWriteBytes(sent);

            if (log.isTraceEnabled()) {
                log.trace("Sent " + sent + " bytes to " + this);
            }

            if (sent > 0) {
                this.sentBytes.addAndGet(sent);
                AbstractServer.this.sentBytes.addAndGet(sent);
                updateLastDataSentTime();

                byteBuffer.position(position + sent);
            }

            if (byteBuffer.remaining() == 0) {
                // task complete
                synchronized (operationsLock) {
                    operations.remove(socketOperation);
                }
                sentPackets.incrementAndGet();
            }

            return sent;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public boolean isQueueEmpty() {
            synchronized (operationsLock) {
                return operations.isEmpty();
            }
        }

        private SocketOperation peekFirstSocketOperation() {
            synchronized (operationsLock) {
                return operations.peekFirst();
            }
        }

        @Override
        public void queue(SocketOperation operation) {
            queueImpl(operation, false);
        }

        @Override
        public void queueClose(boolean next) {
            if (closed) {
                return;
            }
            queueImpl(SocketCloseOperation.INSTANCE, true);
        }

        private void queueImpl(SocketOperation operation, boolean next) {
            if (log.isTraceEnabled()) {
                log.trace("Queue operation for " + this + ": " + operation);
            }

            synchronized (operationsLock) {
                if (next) {
                    operations.clear();
                }
                operations.add(operation);
            }

            scheduleOperationsCycleLater(this);
        }

        @Override
        public void queueWrite(ByteBuffer data) {
            queueWriteImpl(data.duplicate());
        }

        protected void queueWriteImpl(ByteBuffer data) {
            assertNotClosed();

            if (data.remaining() <= 0) {
                throw new IllegalArgumentException("Buffer has no bytes remaining");
            }

            if (log.isTraceEnabled()) {
                try {
                    log.trace("Adding " + data.remaining() + " bytes to write queue of " + this);
                    if (!data.isDirect()) {
                        log.trace("\t'" + new String(data.array(), data.position(), data.remaining(), "utf-8") + "'");
                    }
                } catch (UnsupportedEncodingException e) {
                    // no op
                }
            }

            queue(new SocketWriteOpeation(data));
        }

        @Override
        public String toString() {
            return "ISocketImpl [#" + clientSocketPointer + "; " + getRemoteIp() + ":" + getRemotePort() + "]";
        }

        @Override
        public void updateLastDataReadTime() {
            lastDataReadTime.set(System.currentTimeMillis());
        }

        @Override
        public void updateLastDataSentTime() {
            lastDataSentTime.set(System.currentTimeMillis());
        }
    }

    private abstract class AbstractSocketTask implements Runnable {

        protected final long clientSocketPointer;

        private AbstractSocketTask(long clientSocketPointer) {
            this.clientSocketPointer = clientSocketPointer;
        }

        @Override
        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("Executing " + this);
            }

            final AbstractSocket socket = sockets.get(Long.valueOf(clientSocketPointer));
            if (socket == null) {
                log.warn("Not processing " + this + ", referenced Socket object not found (seems already closed)");
                return;
            }

            try {
                socket.assertNotClosed();

                socket.processingAsyncOperation = true;
                try {
                    run(socket);
                } finally {
                    socket.processingAsyncOperation = false;
                }

                // we are not in operations cycle here, start new one
                processSocketOperations(socket);
            } catch (Throwable exc) {
                log.error("Unable to execute " + this + " for " + socket + ": " + exc, exc);
            }
        }

        protected abstract void run(AbstractSocket socket) throws Exception;

    }

    private final class CanReadTask extends AbstractSocketTask {

        private CanReadTask(long clientSocketPointer) {
            super(clientSocketPointer);
        }

        @Override
        protected void run(AbstractSocket socket) throws Exception {
            socket.lastCanReadTime = System.currentTimeMillis();
            socket.handleCanRead();
        }

        @Override
        public String toString() {
            return "CanReadTask [" + clientSocketPointer + "]";
        }
    }

    private final class CanWriteTask extends AbstractSocketTask {

        private CanWriteTask(long clientSocketPointer) {
            super(clientSocketPointer);
        }

        @Override
        protected void run(AbstractSocket socket) throws Exception {
            socket.lastCanWriteTime = System.currentTimeMillis();
            socket.handleCanWrite();
        }

        @Override
        public String toString() {
            return "CanWriteTask [" + clientSocketPointer + "]";
        }
    }

    private final class OnHangupTask extends AbstractSocketTask {

        private OnHangupTask(long clientSocketPointer) {
            super(clientSocketPointer);
        }

        @Override
        protected void run(AbstractSocket socket) throws Exception {
            socket.queueClose(true);
        }

        @Override
        public String toString() {
            return "OnHangupTask [" + clientSocketPointer + "]";
        }
    }

    private final class OnMaintainTask extends AbstractSocketTask {

        private OnMaintainTask(long clientSocketPointer) {
            super(clientSocketPointer);
        }

        @Override
        protected void run(AbstractSocket socket) throws Exception {
            socket.queueClose(true);
        }

        @Override
        public String toString() {
            return "OnMaintainTask [" + clientSocketPointer + "]";
        }
    }

    private final class OnPendingErrorTask extends AbstractSocketTask {

        private OnPendingErrorTask(long clientSocketPointer) {
            super(clientSocketPointer);
        }

        @Override
        protected void run(AbstractSocket socket) throws Exception {
            socket.queueClose(true);
        }

        @Override
        public String toString() {
            return "OnPendingErrorTask [" + clientSocketPointer + "]";
        }
    }

    private final class ProcessSocketOperationsTask implements Runnable {
        private final AbstractSocket iSocket;

        private ProcessSocketOperationsTask(AbstractSocket iSocket) {
            this.iSocket = iSocket;
        }

        @Override
        public void run() {
            processSocketOperations(iSocket);
        }

        @Override
        public String toString() {
            return "ProcessSocketOperationsTask [" + iSocket + "]";
        }
    }

    private class SocketWriteOpeation implements SocketOperation {

        private final ByteBuffer byteBuffer;

        SocketWriteOpeation(ByteBuffer byteBuffer) {
            super();
            this.byteBuffer = byteBuffer;
        }

        @Override
        public boolean isComplete() {
            return byteBuffer.remaining() == 0;
        }

        @Override
        public boolean run(ISocket socket) {

            /*
             * This was very nice idea to optimize writing single packets, skipping
             * add-to-poll-wakeup-poll-async steps, but it turns out socket badly handles
             * "no more buffer space" situation, resulting in "error #20014: Internal error". You
             * can try to uncomment those lines and repeat the following test: Search 100 accounts
             * in Psi, select them and ask for VCard (yep, for 100 VCard at the same time). --
             * vlsergey
             */

            // final AbstractSocket abstractSocket = (AbstractSocket) socket;
            // we try to send data once,
            // int sent;
            // do {
            // sent = abstractSocket.handleCanWrite();
            // } while (sent > 0 && abstractSocket.peekFirstSocketOperation() instanceof
            // SocketWriteOpeation);
            //
            // if (sent == 0 && abstractSocket.peekFirstSocketOperation() instanceof
            // SocketWriteOpeation) {
            addToWritePoll((AbstractSocket) socket);
            return true;
            // }
            // return false;
        }

        @Override
        public String toString() {
            return "SocketWriteOpeation [" + byteBuffer + "]";
        }
    }

}
