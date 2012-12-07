package one.xmpp.server.network;

import java.nio.ByteBuffer;

import org.apache.commons.lang.StringUtils;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public class BigResponseServer extends AbstractServer {

    static final int SIZE = 1 << 20;

    @Override
    protected void handleAccepted(AbstractSocket socket) {
        // no op -- waiting for client first packet
        addToReadPoll(socket);
    }

    @Override
    protected void handleRead(AbstractSocket socket, ByteBuffer readBuffer) {
        try {
            // sent 1 Mb in response
            socket.queueWrite(ByteBuffer.wrap(StringUtils.repeat("+", SIZE).getBytes("utf-8")));
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    protected AbstractSocket newISocket(long clientSocketPointer, boolean secured) throws Exception {
        return new AbstractSocket(clientSocketPointer);
    }

}
