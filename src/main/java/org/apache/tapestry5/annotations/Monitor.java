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

package org.apache.tapestry5.annotations;

import org.apache.tapestry5.ioc.annotations.UseWith;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.tapestry5.annotations.Monitor.ExceptionFilter.Strategy.Include;
import static org.apache.tapestry5.ioc.annotations.AnnotationUseContext.*;

/**
 * Adds monitoring advice to the method.
 * <p/>
 * Monitoring advice is currently supported on Service interface and page/component methods.
 */
@SuppressWarnings("JavaDoc")
@Target(METHOD)
@Retention(RUNTIME)
@Documented
@UseWith({COMPONENT, MIXIN, PAGE, SERVICE})
public @interface Monitor {

    public @interface ExceptionFilter {

        /**
         * Strategy determines how the monitor should handle exceptional invocations of the monitored method.
         */
        enum Strategy {

            /**
             * You want to create a new monitor for these exceptions
             */
            Segregate,

            /**
             * You want to include these exceptions in your successful invocation monitor
             */
            Include,

            /**
             * You don't want these exceptions to show up in any monitor
             */
            Ignore
        }

        /**
         * Describe how to treat the described exceptions.
         * Defaults to {@link Strategy#Include}
         */
        Strategy strategy() default Include;

        /**
         * The name of the child monitor.
         * Defaults to "errors"
         * <p/>
         * Exceptions within this monitor with the same name will share a child monitor.
         */
        String name() default "errors";

        /**
         * The list of {@link Exception} to handle with this strategy
         */
        Class<? extends Exception>[] value();
    }

    /**
     * The name of the Monitor.
     * Must match the regular expression: /[-_\[\]A-Za-z0-9.,@$%()<>]+/
     * Duplicate names will share the same monitor
     */
    String value() default "";

    /**
     * Defines how exceptions will be handled by the monitor.
     * <p/>
     * Items in the list are evaluated in order and the first match is the only filter applied. This allows you to
     * segregate specific exceptions by including them at the top of the list and placing their parent further down.
     * <p/>
     * Defaults to reporting all exceptions with the successful invocations.
     */
    ExceptionFilter[] exceptions() default {
            @ExceptionFilter(
                    strategy = Include,
                    value = Exception.class
            )
    };

}
