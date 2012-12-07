package one.xmpp.utils;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;

@ManagedResource
public abstract class AsyncOperationsExecutorMBean {

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getActiveCount() {
        return getThreadPoolExecutor().getActiveCount();
    }

    @ManagedMetric(metricType = MetricType.COUNTER)
    public long getCompletedTaskCount() {
        return getThreadPoolExecutor().getCompletedTaskCount();
    }

    @ManagedAttribute
    public int getCorePoolSize() {
        return getThreadPoolExecutor().getCorePoolSize();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getLargestPoolSize() {
        return getThreadPoolExecutor().getLargestPoolSize();
    }

    @ManagedAttribute
    public int getMaximumPoolSize() {
        return getThreadPoolExecutor().getMaximumPoolSize();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public int getPoolSize() {
        return getThreadPoolExecutor().getPoolSize();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public long getQueueSize() {
        return getThreadPoolExecutor().getQueue().size();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public long getRemainingCapacity() {
        return getThreadPoolExecutor().getQueue().remainingCapacity();
    }

    @ManagedMetric(metricType = MetricType.GAUGE)
    public long getTaskCount() {
        return getThreadPoolExecutor().getTaskCount();
    }

    protected abstract ThreadPoolExecutor getThreadPoolExecutor();

    @ManagedAttribute
    public boolean isShutdown() {
        return getThreadPoolExecutor().isShutdown();
    }

    @ManagedAttribute
    public boolean isTerminated() {
        return getThreadPoolExecutor().isTerminated();
    }

    @ManagedAttribute
    public boolean isTerminating() {
        return getThreadPoolExecutor().isTerminating();
    }

    @ManagedOperation
    public int prestartAllCoreThreads() {
        return getThreadPoolExecutor().prestartAllCoreThreads();
    }

    @ManagedOperation
    public boolean prestartCoreThread() {
        return getThreadPoolExecutor().prestartCoreThread();
    }

    @ManagedOperation
    public void purge() {
        getThreadPoolExecutor().purge();
    }

    @ManagedAttribute
    public void setCorePoolSize(int corePoolSize) {
        getThreadPoolExecutor().setCorePoolSize(corePoolSize);
    }
}
