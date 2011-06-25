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
package org.apache.tapestry5.monitor;

import org.apache.tapestry5.internal.monitor.MonitorAdviserImpl;
import org.apache.tapestry5.internal.monitor.MonitorNameGeneratorImpl;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.jmx.JmxModule;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.services.TapestryModule;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import java.util.Map;

/**
 * Module for javasimon (http://code.google.com/p/javasimon/)
 *
 * @since 5.3.0
 */
@SubModule({TapestryModule.class, JmxModule.class})
public class MonitorModule {

    public static void bind(ServiceBinder binder) {
        binder.bind(MonitorAdviser.class, MonitorAdviserImpl.class);
        binder.bind(MonitorNameGenerator.class, MonitorNameGeneratorImpl.class);
    }

    /**
     * The @Local on MonitorAdviser prevents prevents it from going to the MasterObjectProvider and causing a
     * recursive service dependency. The @Match criteria prevents MonitorAdviser from trying to advise itself.
     *
     * @param receiver       -
     * @param monitorAdviser -
     */
    @Match("^(?!MonitorAdviser).*")
    public static void adviseForMonitoredServices(
            MethodAdviceReceiver receiver,
            @Local MonitorAdviser monitorAdviser
    ) {
        monitorAdviser.monitor(receiver);
    }


    @Contribute(ComponentClassTransformWorker2.class)
    public static void addMonitorWorker(
            OrderedConfiguration<ComponentClassTransformWorker2> configuration,
            @Local final MonitorAdviser monitorAdviser
    ) {

        configuration.add("monitored", new ComponentClassTransformWorker2() {

            public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model) {
                monitorAdviser.monitor(plasticClass);
            }
        });
    }

}
