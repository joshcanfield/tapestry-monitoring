package org.example.testapp.services.impl;

import org.apache.tapestry5.annotations.Monitor;
import org.example.testapp.services.NotMonitored;

/**
 * User: josh_canfield
 * Date: 7/21/11
 */
public class NotMonitoredImpl implements NotMonitored {

    @Monitor("MonitoredImpl.MethodOne")
    public void methodOne() {
        // do nothing...
    }
}
