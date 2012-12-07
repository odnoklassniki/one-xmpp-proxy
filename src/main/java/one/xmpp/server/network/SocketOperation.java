package one.xmpp.server.network;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public interface SocketOperation {

    boolean isComplete();

    /**
     * @return <tt>true</tt>, if operation cycle shall be stopped (due to poll scheduling or due to
     *         socket close)
     */
    boolean run(ISocket socket);
}
