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
import static org.apache.tapestry5.ioc.annotations.AnnotationUseContext.*;

/**
 * Adds monitoring advice to the method.
 * <p/>
 * Monitoring advice is currently supported on Service interface and page/component methods.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
@UseWith({COMPONENT, MIXIN, PAGE, SERVICE})
public @interface Monitor {

    /**
     * The name of the Monitor.
     * Must match the regular expression: /[-_\[\]A-Za-z0-9.,@$%()<>]+/
     * Duplicate names will share the same monitor
     *
     * @return the name
     */
    String value() default "";

    /**
     * @return The JMX Object name.
     */
    String jmxObjectName() default "";
}
