package org.apache.tapestry5.monitor;

import org.apache.tapestry5.annotations.Monitor;
import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.TapestryException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Hashtable;

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
     * This method creates an object name "org.example.tapestry:name=Index,type=page,method=onActivate(String, Integer)"
     *
     * @param method being monitored, or null if transformation is provided.
     * @return the object name
     */
    // @Override - not until Java 6
    public ObjectName getJmxObjectName(Class owningClass, Method method) {
        final Hashtable<String, String> properties = new Hashtable<String, String>();

        String domain = owningClass.getPackage().getName();
        if (domain.startsWith(appPackage)) {
            String type = domain.substring(appPackage.length() + 1);
            domain = appPackage;
            properties.put("package", type);
        }

        final String methodDescription = getMediumDescription(method);

        properties.put("class", owningClass.getSimpleName());
        properties.put("method", ObjectName.quote(methodDescription));
        properties.put("type", "Monitor");

        return objectName(domain, properties);
    }

    private ObjectName objectName(String domain, Hashtable<String, String> attributes) {
        try {
            return new ObjectName(domain, attributes);
        } catch (MalformedObjectNameException e) {
            final String message = String.format(
                    "Failed creating JMX object name domain='%s' attributes='%s'",
                    domain, attributes
            );
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
