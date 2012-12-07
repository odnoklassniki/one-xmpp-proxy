package one.xmpp.server;

import one.ejb.NotNullByDefault;
import one.xmpp.server.network.ISocket;
import one.xmpp.server.network.SocketOperation;

/**
 * Async operation wrapper for restarting stream, i.e. cleaning XML parsing stack and waiting for
 * stream:stream packet from client
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@NotNullByDefault
public class RestartStreamOperation implements SocketOperation {

    public static RestartStreamOperation INSTANCE = new RestartStreamOperation();

    private RestartStreamOperation() {
        // no op;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean run(ISocket socket) {
        ((AbstractXmppProxyServer.AbstractXmppProxySocket) socket).restart();
        return false;
    }

    @Override
    public String toString() {
        return "RestartStreamOperation";
    }

}
