package one.xmpp.utils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import one.ejb.NotNull;
import one.ejb.NotNullByDefault;

@NotNullByDefault
public class AsyncOperationsExecutor extends AsyncOperationsExecutorMBean implements RejectedExecutionHandler {

    public static int DEFAULT_MAX_THREADS = 250;

    public static int DEFAULT_MIN_THREADS = 10;

    public static int DEFAULT_QUEUE_SIZE = 250000;

    private static final Log log = LogFactory.getLog(AsyncOperationsExecutor.class);

    private static final String THREAD_NAME_PREFIX = "AsyncWorker-";

    public int maxQueueSize = DEFAULT_QUEUE_SIZE;

    private int maxThreads = DEFAULT_MAX_THREADS;

    private int minThreads = DEFAULT_MIN_THREADS;

    private ArrayBlockingQueue<Runnable> queue;

    private ThreadGroup threadGroup;

    private ThreadPoolExecutor threadPoolExecutor;

    public AsyncOperationsExecutor() {
    }

    @ManagedOperation
    public String dumpThreadTasks() {
        Thread[] threads = new Thread[threadGroup.activeCount() + 1];
        final int count = this.threadGroup.enumerate(threads, false);

        StringBuilder result = new StringBuilder();
        for (int t = 0; t < count; t++) {
            Thread thread = threads[t];
            result.append(thread);
            result.append(" \t=> \t");
            result.append(thread.getState());
            result.append(" \t=> \t");
            if (thread instanceof ThreadWithTask) {
                result.append(((ThreadWithTask) thread).lastTask.get());
            }
            result.append('\n');
        }

        return result.toString();
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    @Override
    public long getQueueSize() {
        return queue.size();
    }

    @Override
    protected ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    @PostConstruct
    public void init() {
        this.threadGroup = new ThreadGroup(THREAD_NAME_PREFIX + "-Group");
        ThreadFactory threadFactory = new ThreadFactoryImpl(threadGroup, THREAD_NAME_PREFIX);
        queue = new ArrayBlockingQueue<Runnable>(getMaxQueueSize());
        threadPoolExecutor = new ThreadPoolExecutorImpl(DEFAULT_MIN_THREADS, DEFAULT_MAX_THREADS, 60L,
                TimeUnit.SECONDS, queue, threadFactory);
    }

    @Override
    @NotNullByDefault(false)
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn("Cannot add new task: " + r + " (threads = " + executor.getActiveCount() + ", tasks = "
                + executor.getTaskCount() + ")");
    }

    public void setMaxQueueSize(int queueSize) {
        if (queue != null) {
            throw new IllegalStateException("Queue is already initialized, unable to change queue size");
        }
        this.maxQueueSize = queueSize;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        if (threadPoolExecutor != null) {
            threadPoolExecutor.setMaximumPoolSize(maxThreads);
        }
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;

        if (threadPoolExecutor != null && threadPoolExecutor.getCorePoolSize() < minThreads) {
            threadPoolExecutor.setCorePoolSize(minThreads);
        }
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS);
        threadPoolExecutor.shutdownNow();
    }

    public void submit(Runnable socketAcceptTask) {
        getThreadPoolExecutor().submit(socketAcceptTask);

        if (log.isTraceEnabled()) {
            log.trace("Added new task to " + this + ": " + socketAcceptTask + ". " + getQueueSize()
                    + " tasks in queue; " + getActiveCount() + " active threads.");
        }
    }

    @Override
    public String toString() {
        return "AsyncOperationsExecutor";
    }

    private static final class FutureTaskImpl<V> extends FutureTask<V> {

        private final Object task;

        public FutureTaskImpl(Callable<V> callable) {
            super(callable);
            this.task = callable;
        }

        public FutureTaskImpl(Runnable runnable, V result) {
            super(runnable, result);
            this.task = runnable;
        }

        @Override
        public String toString() {
            return "FutureTask [" + task + "]";
        }
    }

    private static final class ThreadFactoryImpl extends CustomizableThreadFactory {
        private static final long serialVersionUID = 1L;

        private ThreadFactoryImpl(ThreadGroup threadGroup, String threadNamePrefix) {
            super(threadNamePrefix);
            setThreadGroup(threadGroup);
        }

        @Override
        @NotNullByDefault(false)
        public ThreadWithTask createThread(Runnable runnable) {
            ThreadWithTask thread = new ThreadWithTask(getThreadGroup(), runnable, nextThreadName());
            thread.setPriority(getThreadPriority());
            thread.setDaemon(isDaemon());
            return thread;
        }
    }

    private static final class ThreadPoolExecutorImpl extends ThreadPoolExecutor {

        private ThreadPoolExecutorImpl(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        @NotNullByDefault(false)
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);

            if (t instanceof ThreadWithTask) {
                ((ThreadWithTask) t).lastTask = new WeakReference<Runnable>(r);
            }
        }

        @Override
        @NotNullByDefault(false)
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new FutureTaskImpl<T>(callable);
        }

        @Override
        @NotNullByDefault(false)
        protected <T extends Object> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new FutureTaskImpl<T>(runnable, value);
        }
    }

    private static final class ThreadWithTask extends Thread {
        @NotNull
        private volatile WeakReference<Runnable> lastTask = new WeakReference<Runnable>(null);

        public ThreadWithTask(ThreadGroup group, Runnable target, String name) {
            super(group, target, name);
        }
    }
}
