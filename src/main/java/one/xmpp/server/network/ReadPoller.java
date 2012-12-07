package one.xmpp.server.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Status;

import one.ejb.NotNullByDefault;
import one.xmpp.server.network.AbstractServer.AbstractSocket;

@NotNullByDefault
class ReadPoller extends AbstractPoller {

    private final NetworkOperationsLogger opLogger;

    private final Queue<AbstractSocket> toScheduleOperationsCycle = new ConcurrentLinkedQueue<AbstractSocket>();

    public ReadPoller(final NetworkOperationsLogger opLogger, int maxConnections, long socketPollTimeoutUs, int index) {
        super("ReadPoller-" + index, Poll.APR_POLLIN, maxConnections, socketPollTimeoutUs);
        this.opLogger = opLogger;
    }

    public boolean addToScheduleOperationsCycle(AbstractSocket socket) {
        final boolean add = toScheduleOperationsCycle.add(socket);
        wakeUpPoll();
        return add;
    }

    @Override
    protected void afterWakeUp() {
        super.afterWakeUp();

        final boolean logTraceEnabled = log.isTraceEnabled();

        AbstractSocket socket;
        while ((socket = toScheduleOperationsCycle.poll()) != null) {
            boolean removed = false;

            // synchronized (pollLock) {
            synchronized (toPollQueue) {
                final int indexOf = toPollQueue.indexOf(socket);
                if (indexOf != -1) {
                    toPollQueue.remove(indexOf);
                    removed = true;

                    if (logTraceEnabled) {
                        log.trace(socket + " is removed from to-poll-queue of poll #" + pollPointer);
                    }
                }
            }

            if (!removed) {

                /*
                 * Poller thread don't need to sync over socket, because it won't "miss" destroying
                 * the socket handler due to single-thread remove() method implementation.
                 * --vlsergey
                 */
                if (socket.isClosed()) {
                    if (logTraceEnabled) {
                        log.trace(socket + " is closed already");
                    }
                    continue;
                }

                int result = Poll.remove(pollPointer, socket.getClientSocketPointer());

                if (result == Status.APR_SUCCESS) {
                    removed = true;
                    if (logTraceEnabled) {
                        log.trace(socket + " is removed from reading poll #" + pollPointer);
                    }
                }
            }

            if (removed) {
                if (logTraceEnabled) {
                    log.trace(socket + " added to async queue to process it's own queue");
                }

                abstractServer.scheduleOperationsCycle(socket);
            }
        }
    }

    @Override
    protected void logSignal(long signal) {
        opLogger.onReadPollerSignal(signal);
    }

    @Override
    protected void onHangUp(long clientSocketPointer) {
        abstractServer.handleHangUp(clientSocketPointer);
    }

    @Override
    protected void onMaintain(long clientSocketPointer) {
        abstractServer.handleMaintain(clientSocketPointer);
    }

    @Override
    protected void onPendingError(long clientSocketPointer) {
        abstractServer.handlePendingError(clientSocketPointer);
    }

    @Override
    protected void onSignal(long clientSocketPointer, long signal) {
        if ((signal & Poll.APR_POLLIN) != 0) {
            this.abstractServer.canReadWithoutBlocking(clientSocketPointer);
        } else {
            log.warn("Unsupported signal from " + clientSocketPointer + ": " + signal + ". Socket is probably 'lost'.");
        }
    }

    public boolean removeFromToScheduleOperationsCycle(AbstractSocket socket) {
        return toScheduleOperationsCycle.remove(socket);
    }

}