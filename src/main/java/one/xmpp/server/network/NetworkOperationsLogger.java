package one.xmpp.server.network;

import one.ejb.NotNullByDefault;
import one.ejb.Nullable;

@NotNullByDefault
public interface NetworkOperationsLogger {

    void onReadPollerSignal(long signal);

    void onSocketClose(@Nullable String reason);

    void onSocketReadBytes(int read);

    void onSocketReadError(int i);

    void onSocketReadZeroBytes();

    void onSocketWriteBytes(int read);

    void onSocketWriteError(int i);

    void onSocketWriteZeroBytes();

    void onWritePollerSignal(long signal);

}
