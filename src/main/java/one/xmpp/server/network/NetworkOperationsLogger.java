package one.xmpp.server.network;

public interface NetworkOperationsLogger {

    void onReadPollerSignal(long signal);

    void onSocketReadBytes(int read);

    void onSocketReadError(int i);

    void onSocketReadZeroBytes();

    void onSocketWriteBytes(int read);

    void onSocketWriteError(int i);

    void onSocketWriteZeroBytes();

    void onWritePollerSignal(long signal);

}
