package org.apache.tapestry5.monitor;

import org.apache.tapestry5.annotations.Monitor;
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
    public String getMonitorName(Class owningClass, Method method) {
        Monitor monitor = method.getAnnotation(Monitor.class);

        // Monitors may come from annotation or symbol matches
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
     * This method creates an object name "org.example.tapestry:name=pages.Index,method=onActivate(String, Integer)"
     *
     * @param method being monitored, or null if transformation is provided.
     * @return the object name
     */
    // @Override - not until Java 6
    public ObjectName getJmxObjectName(Class owningClass, Method method) {
        final Monitor monitor = method.getAnnotation(Monitor.class);
        if (!monitor.jmxObjectName().equals("")) return asObjectName(monitor.jmxObjectName());

        final String className = owningClass.getCanonicalName();
        if (!className.startsWith(appPackage)) {
            String monitorName = getMonitorName(owningClass, method);
            return asObjectName(monitorName + ":type=Monitor");
        }

        final int lastDot = className.lastIndexOf('.');
        final String pkg = className.substring(appPackage.length() + 1, lastDot);
        final String cls = className.substring(lastDot + 1);
        // can't use , in the ObjectName
        final String methodName = getMediumDescription(method).replace(',', ';');

        return asObjectName(appPackage + ":package=" + pkg + ",class=" + cls + ",method=" + methodName + ",type=Monitor");
    }

    private ObjectName asObjectName(String s) {
        try {
            return new ObjectName(s);
        } catch (MalformedObjectNameException e) {
            throw new TapestryException("Failed creating JMX object name", e);
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
