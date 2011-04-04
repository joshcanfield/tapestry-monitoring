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
import org.apache.tapestry5.ioc.annotations.PreventServiceDecoration;
import org.apache.tapestry5.ioc.internal.util.InheritanceSearch;
import org.apache.tapestry5.jmx.MBeanSupport;
import org.apache.tapestry5.monitor.MonitorAdviser;
import org.apache.tapestry5.monitor.MonitorNameGenerator;
import org.apache.tapestry5.services.ClassTransformation;
import org.apache.tapestry5.services.TransformMethod;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.javasimon.jmx.StopwatchMXBeanFactory;
import org.slf4j.Logger;

import javax.inject.Named;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.List;

/**
 *
 */
@PreventServiceDecoration // decorators don't get decorated... s/decorator/adviser/g
public class MonitorAdviserImpl implements MonitorAdviser {
    private final Logger logger;
    private final MonitorNameGenerator monitorNameGenerator;
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
                final Monitor monitor = method.getAnnotation(Monitor.class);
                if (monitor == null) continue;

                final Stopwatch stopwatch = createStopwatch(owningClass, method);
                receiver.adviseMethod(method, new MonitorAdvice(stopwatch));
            }
        }
    }

    /**
     * Locate and advise the monitored methods of this component.
     *
     * @param transformation to be monitored
     */
    public void monitor(ClassTransformation transformation) {
        final List<TransformMethod> methods = transformation.matchMethodsWithAnnotation(Monitor.class);

        for (TransformMethod monitoredMethod : methods) {
            final Class<?> aClass;
            try {
                aClass = Class.forName(transformation.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class should be available.", e);
            }
            Method method = null;

            METHODS:
            for (Method m : aClass.getDeclaredMethods()) {
                if (!m.getName().equals(monitoredMethod.getName())) continue; // move on to the next possible match

                final Class<?>[] types = m.getParameterTypes();
                final String[] typeNames = monitoredMethod.getSignature().getParameterTypes();
                if (types.length != typeNames.length) continue; // move on to the next possible match

                for (int i = 0, typeNamesLength = typeNames.length; i < typeNamesLength; i++) {
                    if (!types[i].getName().equals(typeNames[i]))
                        continue METHODS; // move on to the next possible match
                }
                // we have our method!
                method = m;
                break;
            }

            final Stopwatch stopwatch = createStopwatch(aClass, method);
            monitoredMethod.addAdvice(new MonitorAdvice(stopwatch));
        }
    }

    private Stopwatch createStopwatch(Class<?> owningClass, Method method) {
        // need to choose which monitorNameGenerator I'm going to use.
        final String name = monitorNameGenerator.getMonitorName(owningClass, method);

        final ObjectName objectName = monitorNameGenerator.getJmxObjectName(owningClass, method);

        final Stopwatch stopwatch = SimonManager.getStopwatch(name);

        mBeanSupport.register(StopwatchMXBeanFactory.create(stopwatch), objectName);

        return stopwatch;
    }


}
