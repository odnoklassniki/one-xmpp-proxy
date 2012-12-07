package one.xmpp.server.network;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import one.xmpp.server.XmppProxyConfiguration;

import one.ejb.NotNullByDefault;
import one.xmpp.server.network.AbstractServer.AbstractSocket;

@Component
@ManagedResource
@NotNullByDefault
public class ReadPollers extends AbstractPollers {

    @Autowired
    private NetworkOperationsLogger networkOperationsLogger;

    @Autowired
    private XmppProxyConfiguration xmppProxyConfiguration;

    public ReadPollers() {
        super("readPoller-");
    }

    public void addToScheduleOperationsCycle(AbstractSocket socket) {
        getPoller(socket).addToScheduleOperationsCycle(socket);
    }

    @Override
    public ReadPoller getPoller(AbstractSocket socket) {
        return ((ReadPoller) super.getPoller(socket));
    }

    @Override
    protected AbstractPoller newPoller(int maxConnectionsPerPoller, int pollerIndex) {
        return new ReadPoller(networkOperationsLogger, maxConnectionsPerPoller,
                xmppProxyConfiguration.getSocketReadPollTimeout(), pollerIndex);
    }

    public boolean removeFromToScheduleOperationsCycle(AbstractSocket socket) {
        return getPoller(socket).removeFromToScheduleOperationsCycle(socket);
    }

}
