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

package org.apache.tapestry5.monitor.integration;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.dom.Document;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.monitor.MonitorNameGenerator;
import org.apache.tapestry5.test.PageTester;
import org.example.testapp.pages.Index;
import org.example.testapp.services.HelloService;
import org.example.testapp.services.Renamed;
import org.example.testapp.services.SubMonitored;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Collection;

@Test
public class TapestryMonitorIntegrationTests extends Assert {

    private PageTester tester;
    private MonitorNameGenerator monitorNameGenerator;

    @BeforeClass
    public void prepareForTests() {
        tester = new PageTester("org.example.testapp", "app", "src/test/webapp");
        monitorNameGenerator = tester.getRegistry().getService(MonitorNameGenerator.class);
    }

    @BeforeMethod
    public void resetSimon() {
        // Each test assumes we're starting at zero
        Collection<String> strings = SimonManager.simonNames();
        for (String string : strings) {
            SimonManager.getSimon(string).reset();
        }
    }

    @Test
    public void monitor_page_methods() throws NoSuchMethodException {
        final Method onActivateMethod = Index.class.getDeclaredMethod("onActivate", EventContext.class);
        final Method getNamesMethod = Index.class.getDeclaredMethod("getNames");
        final Method getNameMethod = Index.class.getDeclaredMethod("getName");

        final Stopwatch onActivateStopwatch = getStopwatch(Index.class, onActivateMethod);
        final Stopwatch getNamesStopwatch = getStopwatch(Index.class, getNamesMethod);
        final Stopwatch getNameStopwatch = getStopwatch(Index.class, getNameMethod);

        assertEquals(onActivateStopwatch.getCounter(), 0);
        assertEquals(getNamesStopwatch.getCounter(), 0);
        assertEquals(getNameStopwatch.getCounter(), 0);

        tester.renderPage("index/one/two/three");
        assertEquals(onActivateStopwatch.getCounter(), 1);
        assertEquals(getNamesStopwatch.getCounter(), 1);
        // Three items in the list
        assertEquals(getNameStopwatch.getCounter(), 3);

        tester.renderPage("index/one/two");
        assertEquals(onActivateStopwatch.getCounter(), 2);
        // called once per page render
        assertEquals(getNamesStopwatch.getCounter(), 2);
        // two items in the list, plus the first three
        assertEquals(getNameStopwatch.getCounter(), 5);
    }

    @Test
    public void monitor_page_event_methods() throws NoSuchMethodException {
        final Method onMonitoredEvent = Index.class.getDeclaredMethod("onMonitoredEvent");
        final Stopwatch onMonitoredEventStopwatch = getStopwatch(Index.class, onMonitoredEvent);

        // Method should not have been called, no counter increment
        assertEquals(onMonitoredEventStopwatch.getCounter(), 0);

        Document index = tester.renderPage("index");
        // Method should not have been called, no counter increment
        assertEquals(onMonitoredEventStopwatch.getCounter(), 0);

        Element eventLink = index.getElementById("monitored").getContainer();
        index = tester.clickLink(eventLink);

        // Method should be called once.
        assertEquals(onMonitoredEventStopwatch.getCounter(), 1);

        eventLink = index.getElementById("monitored").getContainer();
        tester.clickLink(eventLink);

        assertEquals(onMonitoredEventStopwatch.getCounter(), 2);
    }

    @Test
    public void monitor_not_profiled_service_methods() throws NoSuchMethodException {
        // make sure the page is rendered once so that all the services are loaded and monitoring is created
        tester.renderPage("index");
        assertTrue(SimonManager.simonNames().size() > 0);

        // Methods without the monitor annotation should not be monitored
        final Method method = HelloService.class.getMethod("notMonitoredMethod");
        assertFalse(SimonManager.simonNames().contains(monitorNameGenerator.getMonitorName(HelloService.class, method)));

    }

    @Test
    public void monitor_service_interface_methods() throws NoSuchMethodException {
        /**
         * Service implementations bound to an interface must respect the monitor annotation
         */
        final Method profiledInterfaceMethod = HelloService.class.getMethod("monitoredMethod");
        final Stopwatch profiledStopWatch = getStopwatch(HelloService.class, profiledInterfaceMethod);

        // re-ordering is easier if we don't hard-code the count value...
        int count = 0;
        assertEquals(profiledStopWatch.getCounter(), count);

        HelloService service = tester.getRegistry().getService(HelloService.class);
        service.monitoredMethod();
        assertEquals(profiledStopWatch.getCounter(), ++count);

        service.monitoredMethod();
        assertEquals(profiledStopWatch.getCounter(), ++count);
    }

    /**
     * This is an anti-test. Calling a monitored method through means other than the original proxy does not invoke
     * the advice.
     * <p/>
     * I wish this worked and someday it will fail when it does ;)
     *
     * @throws NoSuchMethodException if the method doesn't exist
     */
    @Test
    public void monitor_indirect_methods() throws NoSuchMethodException {
        final Method profiledInterfaceMethod = HelloService.class.getMethod("monitoredMethod");
        final Stopwatch profiledStopWatch = getStopwatch(HelloService.class, profiledInterfaceMethod);

        HelloService service = tester.getRegistry().getService(HelloService.class);

        service.callsMonitoredMethod();
        assertEquals(profiledStopWatch.getCounter(), 0);
    }

    @Test
    public void monitor_service_interface_methods_with_params() throws NoSuchMethodException {
        /**
         * Service implementations bound to an interface must respect the monitor annotation
         */
        final Method profiledInterfaceMethod = HelloService.class.getMethod("monitoredMethod", String.class, String.class);
        final Method noparams = HelloService.class.getMethod("monitoredMethod");
        final Stopwatch profiledStopWatch = getStopwatch(HelloService.class, profiledInterfaceMethod);
        final Stopwatch noparamsStopWatch = getStopwatch(HelloService.class, noparams);

        // re-ordering is easier if we don't hard-code the count value...
        int count = 0;
        assertEquals(profiledStopWatch.getCounter(), count);
        assertEquals(noparamsStopWatch.getCounter(), 0);

        HelloService service = tester.getRegistry().getService(HelloService.class);
        service.monitoredMethod("one", "two");
        assertEquals(profiledStopWatch.getCounter(), ++count);
        assertEquals(noparamsStopWatch.getCounter(), 0);

        service.monitoredMethod("one", "two");
        assertEquals(profiledStopWatch.getCounter(), ++count);
        assertEquals(noparamsStopWatch.getCounter(), 0);
    }

    @Test
    public void monitor_jmx_beans_created() throws NoSuchMethodException {
        // have to load the page and call the interface in order to get the monitors created.
        tester.renderPage("index/interface.monitoredMethod");

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final Method profiledInterfaceMethod = HelloService.class.getMethod("monitoredMethod");
        final Method onActivateMethod = Index.class.getDeclaredMethod("onActivate", EventContext.class);
        final Method getNamesMethod = Index.class.getDeclaredMethod("getNames");

        assertMonitorRegistered(mBeanServer, HelloService.class, profiledInterfaceMethod);
        assertMonitorRegistered(mBeanServer, Index.class, onActivateMethod);
        assertMonitorRegistered(mBeanServer, Index.class, getNamesMethod);
    }

    @Test
    public void monitor_name_generator() throws NoSuchMethodException {
        testRenamedMonitor(Renamed.class, "One", "OneNameGenerator_Renamed_Service");
    }

    @Test
    public void monitor_name_generator_for_extended_interface() throws NoSuchMethodException {
        testRenamedMonitor(SubMonitored.class, null, "ExtendedNameGenerator_Renamed_Service");
    }

    @Test
    public void monitor_name_second_implementation() throws NoSuchMethodException {
        // It would be nice to be able to provide another name generator for this instance...
        testRenamedMonitor(Renamed.class, "Two", "OneNameGenerator_Renamed_Service");
    }


    private void testRenamedMonitor(Class<? extends Renamed> serviceInterface, String serviceId, String expected) throws NoSuchMethodException {
        final MonitorNameGenerator nameGenerator = tester.getService(MonitorNameGenerator.class);
        // how do I get a service marked service?

        final Renamed service;
        if (serviceId != null) {
            service = tester.getRegistry().getService(serviceId, serviceInterface);
        } else {
            service = tester.getRegistry().getService(serviceInterface);
        }
        final Method monitored = service.getClass().getMethod("monitoredMethod");

        final String monitoredName = nameGenerator.getMonitorName(serviceInterface, monitored);
        assertEquals(monitoredName, expected);
        final Stopwatch monitoredStopwatch = getStopwatch(serviceInterface, monitored);

        assertEquals(monitoredStopwatch.getCounter(), 0);
        service.monitoredMethod();
        assertEquals(monitoredStopwatch.getCounter(), 1);
        // Calling unmonitored doesn't affect
        service.notMonitoredMethod();
        assertEquals(monitoredStopwatch.getCounter(), 1);
    }

    private void assertMonitorRegistered(MBeanServer mBeanServer, Class<?> owningClass, Method method) {
        ObjectName objectName = monitorNameGenerator.getJmxObjectName(owningClass, method);
        assertTrue(mBeanServer.isRegistered(objectName), "Expected " + objectName + " to be registered.");
    }

    private Stopwatch getStopwatch(Class<?> owningClass, Method method) {
        return SimonManager.getStopwatch(monitorNameGenerator.getMonitorName(owningClass, method));
    }


}
