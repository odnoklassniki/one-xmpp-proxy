package one.xmpp.server.network;

import java.nio.ByteBuffer;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public class EchoServer extends AbstractServer {

    @Override
    protected void handleAccepted(AbstractSocket socket) {
        // no op -- waiting for client first packet
        addToReadPoll(socket);
    }

    @Override
    protected void handleRead(AbstractSocket socket, ByteBuffer readBuffer) {
        ByteBuffer byteBuffer = readBuffer.duplicate();
        readBuffer.flip();
        socket.queueWrite(byteBuffer);
    }

    @Override
    protected AbstractSocket newISocket(long clientSocketPointer, boolean secured) throws Exception {
        return new AbstractSocket(clientSocketPointer);
    }

}
