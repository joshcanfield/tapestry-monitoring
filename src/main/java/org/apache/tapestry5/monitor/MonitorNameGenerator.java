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

package org.apache.tapestry5.monitor;

import org.apache.tapestry5.annotations.Monitor;

import javax.management.ObjectName;
import java.lang.reflect.Method;

/**
 * Provides the strategy for naming monitors.
 * <p/>
 * An exception will be thrown when building the monitor if the name does not match the regular expression: /[-_\[\]A-Za-z0-9.,@$%()<>]+/
 * If two or more methods share the same name then they will share the same counter.
 */
public interface MonitorNameGenerator {

    /**
     * Name the monitor for a service interface method.
     *
     * @param monitor     from the method or null
     * @param owningClass of the method
     * @param method      being monitored
     * @return the name of the monitored method
     */
    String getMonitorName(Monitor monitor, Class owningClass, Method method);

    /**
     * Get the monitor name for this service method.
     *
     * @param monitor     from the method or null
     * @param owningClass of the method
     * @param method      being monitored
     * @return the object name
     */
    ObjectName getJmxObjectName(Monitor monitor, Class owningClass, Method method);
}
