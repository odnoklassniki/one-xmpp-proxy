package one.xmpp.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Utility class for dynamic adjusting thread pool size as soon as we have unfinished queue tasks.
 * Do not wait until queue reaches it's limit.
 * 
 * @author Sergey Vladimirov ( sergey {dot} vladimirov {at} odnoklassniki {dot} ru )
 */
@Component
public class ThreadPoolExecutorAdjuster implements Runnable {

    private static final Log log = LogFactory.getLog(ThreadPoolExecutorAdjuster.class);

    @Autowired
    private AsyncOperationsExecutor executor;

    @Override
    @Scheduled(fixedDelay = 100)
    public void run() {
        final int corePoolSize = executor.getCorePoolSize();
        final long queueSize = executor.getQueueSize();
        final int poolSize = executor.getPoolSize();
        final int activeCount = executor.getActiveCount();

        if (poolSize == activeCount && queueSize > 0) {
            if (corePoolSize < executor.getMaxThreads()) {
                executor.setCorePoolSize(corePoolSize + 1);

                if (log.isDebugEnabled()) {
                    log.debug("Number of core thread for " + executor + " increased to " + (corePoolSize + 1));
                }
            }
        } else {
            if (corePoolSize > executor.getMinThreads()) {
                executor.setCorePoolSize(corePoolSize - 1);

                if (log.isDebugEnabled()) {
                    log.debug("Number of core thread for " + executor + " decreased to " + (corePoolSize - 1));
                }

            }
        }
    }

}
