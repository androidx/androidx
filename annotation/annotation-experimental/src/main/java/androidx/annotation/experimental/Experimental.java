/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.annotation.experimental;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element is a marker of an experimental API.
 *
 * Any declaration annotated with this marker is considered part of an unstable API surface and its
 * call sites should accept the experimental aspect of it either by using {@link UseExperimental},
 * or by being annotated with that marker themselves, effectively causing further propagation of
 * that experimental aspect.
 *
 * Example:
 * <pre><code>
 * // Library code
 * &#64;Retention(CLASS)
 * &#64;Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
 * &#64;Experimental(level = Level.ERROR)
 * public @interface ExperimentalDateTime {}
 *
 * &#64;ExperimentalDateTime
 * public class DateProvider {
 *     // ...
 * }
 * </code></pre>
 *
 * <pre><code>
 * // Client code
 * int getYear() {
 *     DateProvider provider; // Error: DateProvider is experimental
 *     // ...
 * }
 *
 * &#64;ExperimentalDateTime
 * Date getDate() {
 *     DateProvider provider; // OK: the function is marked as experimental
 *     // ...
 * }
 *
 * void displayDate() {
 *     System.out.println(getDate()); // Error: getDate() is experimental, acceptance is required
 * }
 * </code></pre>
 *
 */
@Retention(CLASS)
@Target({ANNOTATION_TYPE})
public @interface Experimental {
    /**
     * Severity of the diagnostic that should be reported on usages of experimental API which did
     * not explicitly accept the experimental aspect of that API either by using
     * {@link UseExperimental} or by being annotated with the corresponding marker annotation.
     */
    enum Level {
        /**
         * Specifies that a warning should be reported on incorrect usages of this experimental API.
         */
        WARNING,

        /**
         * Specifies that an error should be reported on incorrect usages of this experimental API.
         */
        ERROR,
    }

    /**
     * Defines the reporting level for incorrect usages of this experimental API.
     */
    Level level() default Level.ERROR;
}
