package com.joshcanfield.tapestry5.internal.monitor;

import com.joshcanfield.tapestry5.annotations.Monitor;
import com.joshcanfield.tapestry5.monitor.DefaultMonitorNameGenerator;
import com.joshcanfield.tapestry5.monitor.MonitorNameGenerator;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.util.StrategyRegistry;

import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Choose a monitor name generator by the interface of the service.
 */
public class MonitorNameGeneratorImpl implements MonitorNameGenerator {

    private final StrategyRegistry<MonitorNameGenerator> strategyRegistry;
    private final DefaultMonitorNameGenerator defaultMonitorNameGenerator;

    public MonitorNameGeneratorImpl(
            Map<Class, MonitorNameGenerator> source,
            @Autobuild
            DefaultMonitorNameGenerator defaultMonitorNameGenerator
    ) {
        this.defaultMonitorNameGenerator = defaultMonitorNameGenerator;
        strategyRegistry = StrategyRegistry.newInstance(MonitorNameGenerator.class, source, true);
    }

    public String getMonitorName(Monitor monitor, Class owningClass, Method method) {
        return getNameGenerator(owningClass).getMonitorName(monitor, owningClass, method);
    }

    public ObjectName getJmxObjectName(Monitor monitor, Class owningClass, Method method) {
        return getNameGenerator(owningClass).getJmxObjectName(monitor, owningClass, method);
    }

    private MonitorNameGenerator getNameGenerator(Class owningClass) {
        MonitorNameGenerator nameGenerator = strategyRegistry.get(owningClass);
        if (nameGenerator == null) nameGenerator = defaultMonitorNameGenerator;
        return nameGenerator;
    }
}
