package org.example.testapp.services.impl;

import org.example.testapp.services.HelloService;
import org.example.testapp.services.SubMonitored;

import javax.inject.Inject;

/**
 * User: josh_canfield
 * Date: 2/14/11
 */
public class SubMonitoredImpl implements SubMonitored {

    @Inject
    private HelloService helloService;

    public void monitoredMethod() {
    }

    public void notMonitoredMethod() {
    }

    public void callsHelloServiceMonitoredMethod() {
        helloService.monitoredMethod();
    }
}
