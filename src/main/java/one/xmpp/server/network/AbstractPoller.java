package one.xmpp.server.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;

import one.ejb.NotNullByDefault;
import one.xmpp.server.network.AbstractServer.AbstractSocket;

@ManagedResource
@NotNullByDefault
abstract class AbstractPoller {

    private static final ByteBuffer SIGNAL = ByteBuffer.allocateDirect(1);

    static {
        SIGNAL.put((byte) 1);
    }

    @Autowired
    protected AbstractServer abstractServer;

    private final AtomicLong addToPollCalls = new AtomicLong(0);

    private final int events;

    private final long[] getPoolsetSizeArray;

    private final AtomicLong hangUps = new AtomicLong(0);

    private final AtomicLong internalSignals = new AtomicLong(0);

    protected final Log log = LogFactory.getLog(getClass());

    private final int maxConnections;

    private final AtomicLong pendingErrors = new AtomicLong(0);

    private long pipeSocketReadPointer;

    private long pipeSocketWritePointer;

    protected long pollPointer;

    private long pollTimeoutMicroseconds = 1 * 1000 * 1000;

    private long poolPointer;

    private final AtomicLong removeFromPollCalls = new AtomicLong(0);

    private final AtomicLong signals = new AtomicLong(0);

    /**
     * Maximum time to live for a particular socket
     */
    private final long socketTtl;

    private volatile boolean stop = false;

    private Thread thread;

    private final String threadName;

    protected final List<AbstractSocket> toPollQueue;

    private final List<RemoveTask> toRemoveQueue;

    private final AtomicLong wakeUpCalls = new AtomicLong(0);

    private final AtomicLong wakeUps = new AtomicLong(0);

    /**
     * @param socketTtl
     *            Maximum time to live for a particular socket
     */
    protected AbstractPoller(String threadName, int events, int expectedMaxConnections, long socketTtl) {
        super();

        this.threadName = threadName;
        this.events = events;
        this.maxConnections = expectedMaxConnections;
        this.socketTtl = socketTtl;
        this.toPollQueue = new ArrayList<AbstractSocket>();
        this.toRemoveQueue = new ArrayList<RemoveTask>();
        this.getPoolsetSizeArray = new long[maxConnections * 2];
    }

    protected void addToPollQueue(AbstractSocket socket) {
        addToPollQueueLater(socket);

        /*
         * async currently doesn't work. In case when some new socket has the same handler address
         * as old one, sometimes it immediately reported as hanged up by poll.
         */
        // addToPollQueueImpl(socket);
        /*
         * in case of async adding we need to wake-up poll manually. Otherwise it will "see" new
         * socket only after ~500ms (at Windows). You can check it using "testLong" test case from
         * EchoServerTest. -- vlsergey
         */
        // wakeUpPoll();
    }

    private void addToPollQueueImpl(AbstractSocket socket) {
        if (Poll.remove(pollPointer, socket.getClientSocketPointer()) == Status.APR_SUCCESS) {
            throw new IllegalStateException("Socket were already in poll");
        }

        /*
         * Poller thread don't need to sync over socket, because it won't "miss" destroying the
         * socket handler due to single-thread remove() method implementation. --vlsergey
         */

        if (!socket.isClosed()) {
            Poll.add(pollPointer, socket.getClientSocketPointer(), events);
            if (log.isTraceEnabled()) {
                log.trace(socket + " added to poll #" + pollPointer);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace(socket + " NOT added to poll #" + pollPointer + " because closed already");
            }
        }
    }

    protected void addToPollQueueLater(AbstractSocket socket) {
        addToPollCalls.incrementAndGet();
        synchronized (toPollQueue) {
            toPollQueue.add(socket);

            if (log.isTraceEnabled()) {
                log.trace(socket + " added to add-to-poll queue for poll #" + pollPointer + ". There are "
                        + toPollQueue.size() + " sockets in queue now");
            }
        }
        wakeUpPoll();
    }

    protected void afterWakeUp() {
        wakeUps.incrementAndGet();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getAddToPollCalls() {
        return addToPollCalls.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getHangUps() {
        return hangUps.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getInternalSignals() {
        return internalSignals.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getPendingErrors() {
        return pendingErrors.get();
    }

    @ManagedAttribute
    public long getPollTimeoutMicroseconds() {
        return pollTimeoutMicroseconds;
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getPoolsetSize() {
        // expensive, thought
        return Poll.pollset(pollPointer, getPoolsetSizeArray);
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getRemoveFromPollCalls() {
        return removeFromPollCalls.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getSignals() {
        return signals.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getWakeUpCalls() {
        return wakeUpCalls.get();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getWakeUps() {
        return wakeUps.get();
    }

    @PostConstruct
    public void init() throws Exception {

        this.poolPointer = Pool.create(0);
        this.pollPointer = Poll.create(maxConnections, poolPointer, 0, socketTtl);
        Poll.setTtl(pollPointer, socketTtl);

        long[] pipe = PipeSocketsHelper.newPair();
        this.pipeSocketReadPointer = pipe[0];
        this.pipeSocketWritePointer = pipe[1];

        Socket.optSet(pipeSocketReadPointer, Socket.APR_TCP_NODELAY, 1);
        Socket.optSet(pipeSocketWritePointer, Socket.APR_TCP_NODELAY, 1);

        Poll.add(pollPointer, pipeSocketReadPointer, Poll.APR_POLLIN);

        thread = new Thread(threadName) {
            @Override
            public void run() {
                AbstractPoller.this.run();
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    protected abstract void logSignal(long signal);

    protected abstract void onHangUp(long clientSocketPointer);

    protected abstract void onMaintain(long clientSocketPointer);

    protected abstract void onPendingError(long clientSocketPointer);

    protected abstract void onSignal(long clientSocketPointer, long signal);

    void remove(AbstractSocket socket) throws InterruptedException {
        removeFromPollCalls.incrementAndGet();
        RemoveTask removeTask = new RemoveTask(socket);
        synchronized (toRemoveQueue) {
            toRemoveQueue.add(removeTask);
        }
        wakeUpPoll();
        removeTask.latch.await();
    }

    private void removeImpl(RemoveTask removeTask) {
        AprUtils.removeFromPollSafe(pollPointer, removeTask.socket.getClientSocketPointer());
        removeTask.latch.countDown();
    }

    void run() {
        final ByteBuffer signalBuffer = ByteBuffer.allocateDirect(1024);
        final long[] pollDescriptors = new long[maxConnections * 2];

        RemoveTask[] removeLocal = new RemoveTask[0];
        AbstractSocket[] toPoolLocal = new AbstractSocket[0];
        while (!stop) {
            // synchronized (pollLock) {
            if (stop) {
                return;
            }

            {
                synchronized (toPollQueue) {
                    if (!toPollQueue.isEmpty()) {
                        if (log.isTraceEnabled()) {
                            log.trace("There are " + toPollQueue.size()
                                    + " client pointers in add-to-poll queue for poll #" + pollPointer);
                        }

                        toPoolLocal = toPollQueue.toArray(toPoolLocal);
                        toPollQueue.clear();
                    }
                }

                for (AbstractSocket socket : toPoolLocal) {
                    if (socket == null) {
                        continue;
                    }

                    addToPollQueueImpl(socket);
                }
                // to remove links to sockets
                Arrays.fill(toPoolLocal, null);
            }

            {
                synchronized (toRemoveQueue) {
                    if (!toRemoveQueue.isEmpty()) {
                        if (log.isTraceEnabled()) {
                            log.trace("There are " + toRemoveQueue.size()
                                    + " client pointers in remove-from-poll queue for poll #" + pollPointer);
                        }

                        removeLocal = toRemoveQueue.toArray(removeLocal);
                        toRemoveQueue.clear();
                    }
                }
                for (RemoveTask removeTask : removeLocal) {
                    if (removeTask == null) {
                        continue;
                    }

                    removeImpl(removeTask);
                }
                // to remove links to sockets
                Arrays.fill(removeLocal, null);
            }

            {
                int result = Poll.poll(pollPointer, pollTimeoutMicroseconds, pollDescriptors, true);

                if (result == 0) {
                    log.trace("Poll returned with 0 result");
                } else if (result > 0) {
                    for (int i = 0; i < result; i++) {
                        try {
                            final long socket = pollDescriptors[2 * i + 1];
                            final long signal = pollDescriptors[2 * i + 0];

                            if (log.isTraceEnabled()) {
                                log.trace("In pool #" + pollPointer + " socket #" + socket + " signalled " + signal);
                            }

                            if (socket == pipeSocketReadPointer) {

                                internalSignals.incrementAndGet();

                                if ((signal & Poll.APR_POLLIN) != 0) {
                                    // it is out special signal socket, read and ignore
                                    log.trace("Poll waked up by receiving a signal");
                                    signalBuffer.clear();
                                    int read = Socket.recvb(socket, signalBuffer, 0, 1024);
                                    if (log.isTraceEnabled()) {
                                        log.trace("Read " + read + " bytes from signal pipe socket");
                                    }
                                    Poll.add(pollPointer, socket, Poll.APR_POLLIN);
                                }

                                if ((signal & Poll.APR_POLLHUP) != 0 || (signal & Poll.APR_POLLNVAL) != 0) {
                                    if (stop) {
                                        // expected, do not return to poll and exit
                                    } else {
                                        log.error("Received hangup signal " + signal + " from pipe stream #"
                                                + pipeSocketReadPointer + " not during server shutdown");
                                    }
                                }

                                continue;
                            }

                            logSignal(signal);

                            if ((signal & Poll.APR_POLLERR) != 0) {
                                if (log.isTraceEnabled()) {
                                    log.trace("Socket #" + socket + " returned from poll with pending error signal");
                                }
                                pendingErrors.incrementAndGet();
                                onPendingError(socket);
                                continue;
                            }

                            if ((signal & Poll.APR_POLLHUP) != 0 || (signal & Poll.APR_POLLNVAL) != 0) {
                                if (log.isTraceEnabled()) {
                                    log.trace("Socket #" + socket + " returned from poll with hangup signal");
                                }
                                hangUps.incrementAndGet();
                                onHangUp(socket);
                                continue;
                            }

                            signals.incrementAndGet();
                            onSignal(socket, signal);
                        } catch (Throwable exc) {
                            exc.printStackTrace();
                        }
                    }

                    Arrays.fill(pollDescriptors, 0, 0, result << 1);
                } else {
                    if (-result != Status.TIMEUP) {
                        log.error("Poll #" + pollPointer + " error #" + (-result) + ": " + Error.strerror(-result));

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // just to be sure it's not filled with bad data
                        Arrays.fill(pollDescriptors, 0);
                    }
                }
            }

            afterWakeUp();

            if (socketTtl > 0) {

                int result = Poll.maintain(pollPointer, pollDescriptors, true);
                for (int i = 0; i < result; i++) {
                    long socket = pollDescriptors[i];

                    if (log.isTraceEnabled()) {
                        log.trace("In pool #" + pollPointer + " socket #" + socket + " returned from maintain");
                    }

                    if (socket == pipeSocketReadPointer) {
                        // return it back

                        if (log.isTraceEnabled()) {
                            log.trace("Return signal pointer back to poll");
                        }

                        Poll.add(pollPointer, socket, Poll.APR_POLLIN);
                        continue;
                    }

                    onMaintain(socket);
                }

            }
        }
    }

    @ManagedAttribute
    public void setPollTimeoutMicroseconds(long pollTimeoutMicroseconds) {
        this.pollTimeoutMicroseconds = pollTimeoutMicroseconds;
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping " + this);
        this.stop = true;
        wakeUpPoll();

        log.trace("Waiting poller thread to stop...");
        try {
            thread.join();
        } catch (InterruptedException e) {
            log.error(e, e);
        }

        log.trace("Closing pipe sockets...");
        Socket.close(pipeSocketWritePointer);
        Socket.close(pipeSocketReadPointer);
        Socket.destroy(pipeSocketWritePointer);
        Socket.destroy(pipeSocketReadPointer);

        log.trace("Destroying poll...");
        int result = Poll.destroy(pollPointer);
        if (result != 0) {
            log.error("Unable to destroy server poll: " + Error.strerror(result));
        }

        log.trace("Destroying pool...");
        Pool.destroy(poolPointer);
    }

    protected void wakeUpPoll() {
        if (log.isTraceEnabled()) {
            log.trace("Waking up " + this);
        }
        wakeUpCalls.incrementAndGet();
        Socket.sendb(pipeSocketWritePointer, SIGNAL, 0, 1);
    }

    private class RemoveTask {
        final CountDownLatch latch = new CountDownLatch(1);

        final AbstractSocket socket;

        RemoveTask(AbstractSocket abstractSocket) {
            this.socket = abstractSocket;
        }
    }
}