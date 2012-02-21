// Copyright 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.joshcanfield.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.testapp.pages;

import com.example.testapp.services.HelloService;
import com.joshcanfield.tapestry5.annotations.Monitor;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;

/**
 * Start page.
 */
public class Index {

    @Inject
    private Logger logger;

    @Inject
    private HelloService hello;

    private String name;
    private String[] names;

    @Monitor
    void onActivate(EventContext args) throws InterruptedException {
        logger.info("Running onActivate");
        names = args.toStrings();
        for (String s : names) {
            if (s.equals("interface.notMonitoredMethod")) {
                hello.notMonitoredMethod();
            } else if (s.equals("interface.monitoredMethod")) {
                hello.monitoredMethod();
            } else if (s.equals("interface.callsMonitoredMethod")) {
                hello.callsMonitoredMethod();
            } else if (s.equals("interface.monitoredWithParams")) {
                hello.monitoredMethod("one", "two");
            }
        }
    }

    @Monitor
    void onMonitoredEvent() {
        logger.info("Monitored event...");
        hello.monitoredMethod();
    }

    @Monitor
    public Collection<String> getNames() {
        return Arrays.asList(names == null ? new String[]{} : names);
    }

    @Monitor
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
