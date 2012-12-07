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
public class WritePollers extends AbstractPollers {

    @Autowired
    private NetworkOperationsLogger networkOperationsLogger;

    @Autowired
    private XmppProxyConfiguration xmppProxyConfiguration;

    public WritePollers() {
        super("writePoller-");
    }

    @Override
    public WritePoller getPoller(AbstractSocket socket) {
        return ((WritePoller) super.getPoller(socket));
    }

    @Override
    protected AbstractPoller newPoller(int maxConnectionsPerPoller, int pollerIndex) {
        return new WritePoller(networkOperationsLogger, maxConnectionsPerPoller,
                xmppProxyConfiguration.getSocketWritePollTimeout(), pollerIndex);
    }

}
