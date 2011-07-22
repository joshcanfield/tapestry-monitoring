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

package org.example.testapp.services;

import org.apache.tapestry5.annotations.Monitor;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.ServiceBuilder;
import org.apache.tapestry5.ioc.ServiceResources;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.monitor.MonitorModule;
import org.apache.tapestry5.monitor.MonitorNameGenerator;
import org.example.testapp.services.impl.HelloServiceImpl;
import org.example.testapp.services.impl.NotMonitoredImpl;
import org.example.testapp.services.impl.SubMonitoredImpl;
import org.slf4j.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;

@SubModule(MonitorModule.class)
public class AppModule {

    public static void bind(ServiceBinder binder) {
        binder.bind(HelloService.class, HelloServiceImpl.class);
        binder.bind(SubMonitored.class, SubMonitoredImpl.class);
        binder.bind(NotMonitored.class, NotMonitoredImpl.class);

        binder.bind(Renamed.class, new ServiceBuilder<Renamed>() {
            public Renamed buildService(final ServiceResources resources) {
                return new Renamed() {
                    public void monitoredMethod() {
                        resources.getLogger().debug("Calling Renamed.monitoredMethod() for Two");
                    }

                    public void notMonitoredMethod() {
                        resources.getLogger().debug("Calling Renamed.notMonitoredMethod() for Two");
                    }
                };
            }
        }).withId("One");
    }

    public static Renamed buildTwo(final Logger logger) {
        return new Renamed() {
            public void monitoredMethod() {
                logger.debug("Calling Renamed.monitoredMethod()");
            }

            public void notMonitoredMethod() {
                logger.debug("Calling Renamed.notMonitoredMethod()");
            }
        };
    }


    @Contribute(MonitorNameGenerator.class)
    public static void provideRenamedMonitorNameGenerator(MappedConfiguration<Class, MonitorNameGenerator> configuration) {
        configuration.add(SubMonitored.class, new ExtendedNameGenerator());
        configuration.add(Renamed.class, new OneNameGenerator());
    }

    private static class OneNameGenerator extends RenamedNameGenerator {
        private OneNameGenerator() {
            super("OneNameGenerator");
        }
    }

    private static class TwoNameGenerator extends RenamedNameGenerator {
        private TwoNameGenerator() {
            super("TWoNameGenerator");
        }
    }

    private static class ExtendedNameGenerator extends RenamedNameGenerator {
        private ExtendedNameGenerator() {
            super("ExtendedNameGenerator");
        }
    }

    private static class RenamedNameGenerator implements MonitorNameGenerator {

        private final String name;

        private RenamedNameGenerator(String name) {
            this.name = name;
        }

        public String getMonitorName(Monitor monitor, Class owningClass, Method method) {
            return this.name + "_Renamed_Service";
        }

        public ObjectName getJmxObjectName(Monitor monitor, Class owningClass, Method method) {
            try {
                return new ObjectName(this.name + "_Renamed_Service:type=hello");
            } catch (MalformedObjectNameException e) {
                throw new RuntimeException(e);
            }
        }

    }
}


