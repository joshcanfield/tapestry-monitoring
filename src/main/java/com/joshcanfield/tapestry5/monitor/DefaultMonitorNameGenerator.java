package com.joshcanfield.tapestry5.monitor;

import com.joshcanfield.tapestry5.annotations.Monitor;
import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.TapestryException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;

/**
 * Provide a service override to replace the default behavior.
 */
public class DefaultMonitorNameGenerator implements MonitorNameGenerator {
    private final String appPackage;

    public DefaultMonitorNameGenerator(
            @Symbol(InternalConstants.TAPESTRY_APP_PACKAGE_PARAM)
            final String appPackage) {
        this.appPackage = appPackage;
    }

    /**
     * Builds a monitor name from the class and method
     *
     * @param owningClass we are monitoring
     * @param method      being monitored
     * @return a name of the format "pages.Index.onActivate(java.lang.String)"
     */
    // @Override - not until Java 6
    public String getMonitorName(Monitor monitor, Class owningClass, Method method) {
        // Monitors may come from annotation (TODO or symbol matches)
        if (monitor != null && !"".equals(monitor.value())) {
            return monitor.value();
        }

        String mediumDescription = owningClass.getCanonicalName() + "." + getMediumDescription(method);
        if (mediumDescription.startsWith(appPackage)) {
            mediumDescription = mediumDescription.substring(appPackage.length() + 1);
        }

        // no spaces in the name
        return mediumDescription.replaceAll("\\s", "").replace('.', '_');
    }

    /**
     * Creates a JMX ObjectName for the method.
     * If the Monitor annotation is present and has a jmxObjectName set that value is used.
     * <p/>
     * Given a method with the signature "org.example.tapestry.pages.Index#onActivate(String, Integer)"
     * Where the apps base package is "org.example.tapestry"
     * This method creates an object name "org.example.tapestry:name=Index,type=page,monitor=onActivate(String, Integer)"
     * <p/>
     * If the @Monitor value is set then the "monitor" attribute is replaced with that value.
     *
     * @param method being monitored, or null if transformation is provided.
     * @return the object name
     */
    // @Override - not until Java 6
    public ObjectName getJmxObjectName(Monitor monitor, Class owningClass, Method method) {
        StringBuilder builder = new StringBuilder();
        String domain = owningClass.getPackage().getName();
        if (domain.startsWith(appPackage)) {
            String pkg = domain.substring(appPackage.length() + 1);
            builder.append(appPackage).append(':');
            builder.append("package=").append(pkg).append(',');
        } else {
            builder.append(domain).append(':');
        }

        builder.append("name=").append(owningClass.getSimpleName()).append(',');

        String desc;
        if (monitor != null && !"".equals(monitor.value())) {
            desc = monitor.value();
        } else {
            desc = getMediumDescription(method);
        }
        builder.append("monitor=").append(ObjectName.quote(desc)).append(',');

        builder.append("type=Monitor");

        return objectName(builder.toString());
    }

    private ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            final String message = String.format("Failed creating JMX object name '%s''", name);
            throw new TapestryException(message, e);
        }
    }

    private String getMediumDescription(Method method) {
        final StringBuilder builder = new StringBuilder();

        builder.append(method.getName()).append('(');
        final Class[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0)
                builder.append(", ");

            builder.append(parameterTypes[i].getCanonicalName());
        }
        builder.append(')');

        return builder.toString();
    }
}
