/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.annotation

import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * Denotes that the annotated element is a marker of an opt-in API.
 *
 * Any declaration annotated with this marker is considered part of an unstable or otherwise
 * non-standard API surface and its call sites should accept the opt-in aspect of it either
 * by using [OptIn] or by being annotated with that marker themselves, effectively causing
 * further propagation of that opt-in aspect.
 *
 * Example:
 * <pre>`
 * // Library code
 * &#64;Retention(CLASS)
 * &#64;Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
 * &#64;RequiresOptIn(level = Level.ERROR)
 * public @interface ExperimentalDateTime {}
 *
 * &#64;ExperimentalDateTime
 * public class DateProvider {
 *   // ...
 * }
`</pre> *
 *
 * <pre>`
 * // Client code
 * int getYear() {
 *   DateProvider provider; // Error: DateProvider is experimental
 *   // ...
 * }
 *
 * &#64;ExperimentalDateTime
 * Date getDate() {
 *   DateProvider provider; // OK: the function is marked as experimental
 *   // ...
 * }
 *
 * void displayDate() {
 *   System.out.println(getDate()); // Error: getDate() is experimental, acceptance is required
 * }
`</pre> *
 *
 * To configure project-wide opt-in, specify the `opt-in` option value in `lint.xml` as a
 * comma-delimited list of opted-in annotations:
 *
 * <pre>`
 * &#64;lint>
 *   &#64;issue id="$issueId">
 *     &#64;option name="opt-in" value="com.foo.ExperimentalBarAnnotation" />
 *   &#64;/issue>
 * &#64;/lint>
 `</pre> *
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class RequiresOptIn(
    /**
     * Defines the reporting level for incorrect usages of this opt-in API.
     */
    val level: Level = Level.ERROR
) {
    /**
     * Severity of the diagnostic that should be reported on usages of opt-in API which did
     * not explicitly accept the opt-in aspect of that API either by:
     * <ul>
     *     <li>Propagating the opt-in aspect by annotating the usage with the marker annotation,
     *     thus becoming part of the marked opt-in API surface <i>or</i>
     *     <li>Suppressing propagation of the opt-in aspect by annotating the usage with [OptIn]
     *     and specifying the marker annotation
     */
    public enum class Level {
        /**
         * Specifies that a warning should be reported on incorrect usages of this opt-in API.
         */
        WARNING,

        /**
         * Specifies that an error should be reported on incorrect usages of this opt-in API.
         */
        ERROR
    }
}
