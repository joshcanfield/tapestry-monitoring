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

package org.example.testapp.services.impl;


import org.example.testapp.services.HelloService;
import org.slf4j.Logger;

/**
 */
public class HelloServiceImpl implements HelloService {

    private Logger logger;

    public HelloServiceImpl(Logger logger) {
        this.logger = logger;
    }

    public void monitoredMethod() {
        logger.info("hello monitoredMethod");
    }

    public void monitoredMethod(String str, String str1) {
        logger.info("hello monitoredWithParams({},{})", str, str1);
    }

    public void notMonitoredMethod() {
        logger.info("hello notMonitoredMethod");
    }

    public void callsMonitoredMethod() {
        logger.info("hello callsMonitoredMethod");

        /**
         * !!! This does not trigger the monitoring advice!!!
         */
        monitoredMethod();
    }
}
