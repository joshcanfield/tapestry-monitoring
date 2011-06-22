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

import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

/**
 * MonitorAdvice is used to advise both Service and Component methods. A MonitorAdvice instance is created
 * for each method being monitored.
 */
public class MonitorAdvice implements MethodAdvice {
    private Stopwatch stopwatch;

    public MonitorAdvice(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
    }

    public void run(final MethodInvocation invocation) {
        Split split = stopwatch.start();
        try {
            invocation.proceed();
        } finally {
            split.stop();
        }
    }

    public void advise(MethodInvocation invocation) {
        run(invocation);
    }
}
