package org.example.testapp.services;

import org.apache.tapestry5.annotations.Monitor;

/**
 * User: josh_canfield
 * Date: 2/14/11
 */
public interface Renamed {

    @Monitor
    void monitoredMethod();

    void notMonitoredMethod();
}
