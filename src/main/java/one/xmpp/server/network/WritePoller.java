package one.xmpp.server.network;

import org.apache.tomcat.jni.Poll;

import one.ejb.NotNullByDefault;

@NotNullByDefault
class WritePoller extends AbstractPoller {

    private final NetworkOperationsLogger opLogger;

    public WritePoller(NetworkOperationsLogger opLogger, int maxConnections, long socketPollTimeoutUs, int index) {
        super("WritePoller-" + index, Poll.APR_POLLOUT, maxConnections, socketPollTimeoutUs);
        this.opLogger = opLogger;
    }

    @Override
    protected void logSignal(long signal) {
        opLogger.onWritePollerSignal(signal);
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
        if ((signal & Poll.APR_POLLOUT) != 0) {
            abstractServer.canWriteWithoutBlocking(clientSocketPointer);
        } else {
            log.warn("Unsupported signal from " + clientSocketPointer + ": " + signal + ". Socket is probably 'lost'.");
        }
    }
}