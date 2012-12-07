package one.xmpp.server;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import one.ejb.NotNullByDefault;

@Component
@ManagedResource
@NotNullByDefault
public class DefaultXmppProxyConfiguration implements XmppProxyConfiguration {

    private static final int DEFAULT_SOCKET_MAX_WRITE_BYTES = 4048;

    private int socketMaxWriteBytes = DEFAULT_SOCKET_MAX_WRITE_BYTES;

    @Override
    @ManagedAttribute
    public int getMaxConnections() {
        return 1100000;
    }

    @Override
    public int getReadPollersCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    @ManagedAttribute()
    public int getSocketMaxWriteBytes() {
        return socketMaxWriteBytes;
    }

    @Override
    @ManagedAttribute
    public long getSocketReadPollTimeout() {
        // 5 minutes
        return 5 * 60 * 1000 * 1000;
    }

    @Override
    @ManagedAttribute
    public long getSocketWritePollTimeout() {
        // 1 minute
        return 1 * 60 * 1000 * 1000;
    }

    @Override
    public int getWritePollersCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public void setSocketMaxWriteBytes(int socketMaxWriteBytes) {
        this.socketMaxWriteBytes = socketMaxWriteBytes;
    }

    @Override
    @ManagedAttribute
    public int socketListenBacklog() {
        return 8192;
    }

}
