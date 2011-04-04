package org.apache.tapestry5.internal.monitor;

import org.apache.tapestry5.ioc.util.StrategyRegistry;
import org.apache.tapestry5.monitor.MonitorNameGenerator;

import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Choose a monitor name generator by the interface of the service.
 */
public class MonitorNameGeneratorImpl implements MonitorNameGenerator {
    private final StrategyRegistry<MonitorNameGenerator> strategyRegistry;

    public MonitorNameGeneratorImpl(Map<Class, MonitorNameGenerator> source) {
        strategyRegistry = StrategyRegistry.newInstance(MonitorNameGenerator.class, source);
    }

    public String getMonitorName(Class owningClass, Method method) {
        final MonitorNameGenerator nameGenerator = strategyRegistry.get(owningClass);
        return nameGenerator.getMonitorName(owningClass, method);
    }

    public ObjectName getJmxObjectName(Class owningClass, Method method) {
        return strategyRegistry.get(owningClass).getJmxObjectName(owningClass, method);
    }
}
