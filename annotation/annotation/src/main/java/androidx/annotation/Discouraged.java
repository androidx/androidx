/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element, while not disallowed or deprecated, is one that
 * programmers are generally discouraged from using.
 * <p>
 * Example:
 * <pre><code>
 *  &#64;Discouraged(message = "It is much more efficient to retrieve "
 *                           + "resources by identifier than by name.")
 *  public void getValue(String name) {
 *      ...
 *  }
 * </code></pre>
 * </p>
 */
@Retention(SOURCE)
@Target({CONSTRUCTOR, FIELD, METHOD, PARAMETER, TYPE})
public @interface Discouraged {
    /**
     * Defines the message to display when an element marked with this annotation is used. An
     * alternative should be provided in the message.
     */
    String message() default "";
}
