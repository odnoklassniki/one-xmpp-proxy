package one.xmpp.xml;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class Pregenerator implements BeanPostProcessor {

    private static final Log log = LogFactory.getLog(Pregenerator.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        processFields(bean);
        return bean;
    }

    private void processFields(Object bean) {

        Class<?> cls = bean.getClass();
        while (cls != null) {

            processFields(bean, cls);

            for (Class<?> intrfc : cls.getInterfaces()) {
                processFields(bean, intrfc);
            }
            cls = cls.getSuperclass();
        }
    }

    private void processFields(Object bean, Class<?> cls) {
        for (Field field : cls.getDeclaredFields()) {

            Pregenerate pregenerate = field.getAnnotation(Pregenerate.class);
            if (pregenerate == null) {
                continue;
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            try {
                Object value = field.get(bean);
                if (value instanceof XmlAttribute) {
                    ((XmlAttribute) value).pregenerate();
                }
                if (value instanceof XmlElement) {
                    ((XmlElement) value).pregenerate();
                }

            } catch (Exception exc) {
                log.error(exc, exc);
            }
        }
    }

}
