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

package org.javasimon.jmx;

import org.javasimon.Stopwatch;

/**
 * Factory class for the Stopwatch MXBeans.
 * <p/>
 * While javasimon comes with a mxbean generating callback it does not offer the naming freedom that I'd like for tapestry.
 * The StopwatchMXBean implementation has a protected constructor so we need a class that lives in the same package
 * in order to build them. The javasimon dev has suggested that the next version will be more flexible.
 */
public class StopwatchMXBeanFactory {

    public static StopwatchMXBean create(Stopwatch stopwatch) {
        return new StopwatchMXBeanImpl(stopwatch);
    }
}
