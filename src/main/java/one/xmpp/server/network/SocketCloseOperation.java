package one.xmpp.server.network;

import one.ejb.NotNullByDefault;
import one.xmpp.server.network.AbstractServer.AbstractSocket;

@NotNullByDefault
public class SocketCloseOperation implements SocketOperation {

    public static final SocketCloseOperation INSTANCE = new SocketCloseOperation();

    private SocketCloseOperation() {
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean run(ISocket socket) {
        ((AbstractSocket) socket).close();
        return true;
    }

    @Override
    public String toString() {
        return "SocketCloseOperation";
    }

}
