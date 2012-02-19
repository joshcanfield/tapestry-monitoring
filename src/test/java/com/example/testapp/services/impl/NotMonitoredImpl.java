package com.example.testapp.services.impl;

import com.example.testapp.services.NotMonitored;
import com.joshcanfield.tapestry5.annotations.Monitor;

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
