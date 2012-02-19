// Copyright 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.internal.monitor;

import org.apache.tapestry5.annotations.Monitor;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MonitorAdvice is used to advise both Service and Component methods. A MonitorAdvice instance is created
 * for each method being monitored.
 */
public class MonitorAdvice implements MethodAdvice {
    private Stopwatch stopwatch;
    private Monitor monitor;
    private Map<String, Stopwatch> exceptionToStopwatchMap;
    private Logger log = LoggerFactory.getLogger(Monitor.class);


    public MonitorAdvice(Stopwatch stopwatch, Monitor monitor, Map<String, Stopwatch> exceptionToStopwatchMap) {
        this.stopwatch = stopwatch;

        this.monitor = monitor;
        this.exceptionToStopwatchMap = exceptionToStopwatchMap;
    }

    public void advise(MethodInvocation invocation) {
        final long nanoTime = System.nanoTime();
        Exception exception = null;
        try {
            invocation.proceed();
        } catch (RuntimeException e) {
            exception = e;
        }
        long ns = System.nanoTime() - nanoTime;

        boolean threwCheckedException = invocation.didThrowCheckedException();
        if (exception == null && threwCheckedException) {
            //noinspection ThrowableResultOfMethodCallIgnored
            exception = invocation.getCheckedException(Exception.class);
        }

        if (exception == null) {
            stopwatch.addTime(ns);
            return;
        }
        Class<? extends Exception> exceptionClass = exception.getClass();

        Filters:
        for (Monitor.ExceptionFilter filter : monitor.exceptions()) {

            final Class<? extends Exception>[] classes = filter.value();
            for (Class<? extends Exception> aClass : classes) {
                if (!aClass.isAssignableFrom(exceptionClass)) {
                    continue;
                }

                switch (filter.strategy()) {
                    case Ignore:
                        log.trace("Ignoring exception: '{}' for stopwatch '{}'",
                                exceptionClass.getName(), stopwatch.getName()
                        );
                        break;
                    case Include:
                        log.trace("Including Exception: '{}' in stopwatch '{}'",
                                exceptionClass.getName(), stopwatch.getName()
                        );
                        stopwatch.addTime(ns);
                        break;
                    case Segregate:
                        Stopwatch sw = exceptionToStopwatchMap.get(filter.name());
                        if (sw == null) throw new RuntimeException("Unable to determine stopwatch!");
                        sw.addTime(ns);
                        log.trace("Including Exception: '{}' in stopwatch '{}'",
                                exceptionClass.getName(), sw.getName()
                        );

                        break Filters;
                }
            }
        }

        if (threwCheckedException) {
            invocation.rethrow();
        } else {
            throw (RuntimeException) exception;
        }
    }
}
