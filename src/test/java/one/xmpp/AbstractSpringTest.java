package one.xmpp;

import org.junit.AfterClass;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import one.xmpp.server.DefaultXmppProxyConfiguration;
import one.xmpp.server.network.DefaultNetworkOperationsLogger;
import one.xmpp.server.network.PlainAcceptor;
import one.xmpp.server.network.ReadPollers;
import one.xmpp.server.network.SslAcceptor;
import one.xmpp.server.network.WritePollers;
import one.xmpp.utils.AsyncOperationsExecutor;
import one.xmpp.utils.ThreadPoolExecutorAdjuster;

public abstract class AbstractSpringTest {

    protected static AnnotationConfigApplicationContext context;

    @AfterClass
    public static void afterClass() throws Exception {
        Thread.sleep(1000);

        context.destroy();
    }

    protected static void beforeClass(boolean loadPlainAcceptor, boolean loadSslAcceptor, Class<?> serverClass,
            Class<?>... additionalBeans) throws Exception {

        context = new AnnotationConfigApplicationContext();

        context.register(AsyncOperationsExecutor.class);
        context.register(DefaultNetworkOperationsLogger.class);
        context.register(DefaultXmppProxyConfiguration.class);
        context.register(ReadPollers.class);
        context.register(ThreadPoolExecutorAdjuster.class);
        context.register(WritePollers.class);

        context.register(serverClass);

        if (loadPlainAcceptor) {
            context.register(PlainAcceptor.class);
        }
        if (loadSslAcceptor) {
            context.register(SslAcceptor.class);
            context.register(TestSslConfiguration.class);
        }

        context.register(additionalBeans);

        context.register(ScheduledAnnotationBeanPostProcessor.class);
        // context.register(MBeanServerFactoryBean.class);

        {
            final BeanDefinition beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(AnnotationMBeanExporter.class).addPropertyValue("autodetect", Boolean.TRUE)
                    .getBeanDefinition();
            context.registerBeanDefinition("mBeanExporter", beanDefinition);
        }

        context.refresh();
        context.start();
    }
}
