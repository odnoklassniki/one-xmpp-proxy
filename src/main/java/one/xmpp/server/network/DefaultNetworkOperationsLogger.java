package one.xmpp.server.network;

import one.ejb.Nullable;

/**
 * Default no-op implementation of {@link NetworkOperationsLogger}
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
public class DefaultNetworkOperationsLogger implements NetworkOperationsLogger {

    @Override
    public void onReadPollerSignal(long signal) {
        // no op
    }

    @Override
    public void onSocketClose(@Nullable String reason) {
        // no op
    }

    @Override
    public void onSocketReadBytes(int read) {
        // no op
    }

    @Override
    public void onSocketReadError(int i) {
        // no op
    }

    @Override
    public void onSocketReadZeroBytes() {
        // no op
    }

    @Override
    public void onSocketWriteBytes(int read) {
        // no op
    }

    @Override
    public void onSocketWriteError(int i) {
        // no op
    }

    @Override
    public void onSocketWriteZeroBytes() {
        // no op
    }

    @Override
    public void onWritePollerSignal(long signal) {
        // no op
    }

}
