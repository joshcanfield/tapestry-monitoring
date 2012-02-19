// Copyright 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.internal.monitor;

import org.apache.tapestry5.annotations.Monitor;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.internal.util.InheritanceSearch;
import org.apache.tapestry5.jmx.MBeanSupport;
import org.apache.tapestry5.monitor.MonitorAdviser;
import org.apache.tapestry5.monitor.MonitorNameGenerator;
import org.apache.tapestry5.plastic.MethodParameter;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.javasimon.jmx.StopwatchMXBeanFactory;
import org.slf4j.Logger;

import javax.inject.Named;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MonitorAdviserImpl implements MonitorAdviser {
    private final Logger logger;
    private final MonitorNameGenerator monitorNameGenerator;
    // TODO : Refactor into local interface
    private final MBeanSupport mBeanSupport;

    public MonitorAdviserImpl(
            Logger logger,
            @Named("MonitorNameGenerator") MonitorNameGenerator monitorNameGenerator,
            @Named("MBeanSupport") MBeanSupport mBeanSupport) {
        this.logger = logger;
        this.monitorNameGenerator = monitorNameGenerator;
        this.mBeanSupport = mBeanSupport;
    }

    /**
     * Locate and advise the monitored methods of this service.
     * <p/>
     * This will search the interface hierarchy for methods that have the @Monitor annotation.
     *
     * @param receiver of the advice
     */
    public void monitor(MethodAdviceReceiver receiver) {
        final Class owningClass = receiver.getInterface();
        for (Class o : new InheritanceSearch(owningClass)) {
            if (o.equals(Object.class)) continue;
            final Method[] methods = o.getDeclaredMethods();
            for (Method method : methods) {
                final Monitor monitor = receiver.getMethodAnnotation(method, Monitor.class);
                if (monitor == null) continue;
                logger.trace("Monitoring method: {}.{}", owningClass.getSimpleName(), method.getName());

                advise(monitor, owningClass, method, receiver);
            }
        }
    }

    /**
     * Locate and advise the monitored methods of this component.
     *
     * @param transformation to be monitored
     */
    public void monitor(PlasticClass transformation) {
        final List<PlasticMethod> methods = transformation.getMethodsWithAnnotation(Monitor.class);

        for (PlasticMethod monitoredMethod : methods) {
            final Class<?> aClass;
            try {
                aClass = Class.forName(transformation.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class should be available.", e);
            }
            Method method = null;

            METHODS:
            for (Method m : aClass.getDeclaredMethods()) {
                if (!m.getName().equals(monitoredMethod.getDescription().methodName))
                    continue; // move on to the next possible match

                final Class<?>[] types = m.getParameterTypes();
                final List<MethodParameter> typeNames = monitoredMethod.getParameters();

                if (types.length != typeNames.size()) continue; // move on to the next possible match

                for (int i = 0, typeNamesLength = typeNames.size(); i < typeNamesLength; i++) {
                    if (!types[i].getName().equals(typeNames.get(i).getType()))
                        continue METHODS; // move on to the next possible match
                }
                // we have our method!
                method = m;
                break;
            }

            if (method != null) {
                logger.trace("Monitoring method: {}.{}", aClass.getSimpleName(), method.getName());

                advise(method.getAnnotation(Monitor.class), aClass, method, monitoredMethod);
            }
        }
    }

    private void advise(Monitor monitor, Class<?> owningClass, Method method, MethodAdviceReceiver receiver) {
        MonitorAdvice advice = createAdvice(monitor, owningClass, method);

        receiver.adviseMethod(method, advice);
    }

    private void advise(Monitor monitor, Class<?> owningClass, Method method, PlasticMethod monitoredMethod) {
        MonitorAdvice advice = createAdvice(monitor, owningClass, method);

        monitoredMethod.addAdvice(advice);
    }

    private MonitorAdvice createAdvice(Monitor monitor, Class<?> owningClass, Method method) {

        final String name = monitorNameGenerator.getMonitorName(monitor, owningClass, method);
        final Stopwatch stopwatch = SimonManager.getStopwatch(name);

        // TODO: Consider making JMX optional, add JSON endpoint
        final ObjectName objectName = monitorNameGenerator.getJmxObjectName(monitor, owningClass, method);
        mBeanSupport.register(StopwatchMXBeanFactory.create(stopwatch), objectName);

        // Add exception stopwatch/mxbeans

        HashSet<String> names = new HashSet<String>();
        for (Monitor.ExceptionFilter exception : monitor.exceptions()) {
            // This gets appended to the StopWatch name
            // How does this affect the ObjectName?
            final String suffix = exception.name();
            names.add(suffix);
        }

        Map<String, Stopwatch> exceptionToStopwatchMap = new HashMap<String, Stopwatch>();
        for (String exceptionName : names) {

            Stopwatch exceptionStopwatch = SimonManager.getStopwatch(name + "." + exceptionName);
            mBeanSupport.register(StopwatchMXBeanFactory.create(stopwatch), getExceptionObjectName(objectName, exceptionName));

            exceptionToStopwatchMap.put(exceptionName, exceptionStopwatch);
        }

        MonitorAdvice advice = new MonitorAdvice(stopwatch, monitor, exceptionToStopwatchMap);


        return advice;
    }

    private ObjectName getExceptionObjectName(ObjectName objectName, String s) {
        try {
            return new ObjectName(objectName.getCanonicalName() + "," + s);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


}
