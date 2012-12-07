package one.xmpp.server.network;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Component;

import one.ejb.NotNullByDefault;
import one.xmpp.server.XmppProxyConfiguration;
import one.xmpp.server.network.AbstractServer.AbstractSocket;

@Component
@ManagedResource
@NotNullByDefault
public abstract class AbstractPollers {

    /**
     * 64 bit to 32 bit Hash Function by Thomas Wang. Released as part of free library by Apple.
     * 
     * @see http://www.concentric.net/~ttwang/tech/inthash.htm
     * @see http://opensource.apple.com/source/JavaScriptCore/JavaScriptCore-521/wtf/HashFunctions.h
     */
    private static int hash6432shift(long key) {
        key = (~key) + (key << 18); // key = (key << 18) - key - 1;
        key = key ^ (key >>> 31);
        key = key * 21; // key = (key + (key << 2)) + (key << 4);
        key = key ^ (key >>> 11);
        key = key + (key << 6);
        key = key ^ (key >>> 22);
        return (int) key;
    }

    private final String beanNamePrefix;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private XmppProxyConfiguration xmppProxyConfiguration;

    private final AbstractPoller[] pollers;

    private final int segments;

    public AbstractPollers(String beanNamePrefix) {
        this.beanNamePrefix = beanNamePrefix;

        // only at startup -- do not adjust in runtime
        this.segments = Runtime.getRuntime().availableProcessors();
        this.pollers = new AbstractPoller[segments];
    }

    public void addToPollQueue(AbstractSocket socket) {
        getPoller(socket).addToPollQueue(socket);
    }

    @PreDestroy
    public void destroy() {
        for (int i = 0; i < segments; i++) {
            pollers[i].stop();
        }
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getAddToPollCalls() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getAddToPollCalls();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getHangUps() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getHangUps();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getInternalSignals() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getInternalSignals();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getPendingErrors() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getPendingErrors();
        }
        return result;
    }

    public AbstractPoller getPoller(AbstractSocket socket) {
        return pollers[getSegmentIndex(socket)];
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getPoolsetSize() {
        int result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getPoolsetSize();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getRemoveFromPollCalls() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getRemoveFromPollCalls();
        }
        return result;
    }

    private int getSegmentIndex(AbstractSocket socket) {
        return Math.abs(hash6432shift(socket.getSocketId())) % segments;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getSignals() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getSignals();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getWakeUpCalls() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getWakeUpCalls();
        }
        return result;
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getWakeUps() {
        long result = 0;
        for (AbstractPoller poller : pollers) {
            result += poller.getWakeUps();
        }
        return result;
    }

    @PostConstruct
    public void init() {

        final AutowireCapableBeanFactory autowireCapableBeanFactory = context.getAutowireCapableBeanFactory();
        final int maxConnectionsPerPoller = (xmppProxyConfiguration.getMaxConnections() / segments) + 1;

        // so array won't be changed after object initialization
        for (int i = 0; i < segments; i++) {
            pollers[i] = newPoller(maxConnectionsPerPoller, i);
            autowireCapableBeanFactory.autowireBean(pollers[i]);
            autowireCapableBeanFactory.initializeBean(pollers[i], beanNamePrefix + i);
        }
    }

    protected abstract AbstractPoller newPoller(int maxConnectionsPerPoller, int pollerIndex);

    void remove(AbstractSocket socket) throws InterruptedException {
        getPoller(socket).remove(socket);
    }

}
