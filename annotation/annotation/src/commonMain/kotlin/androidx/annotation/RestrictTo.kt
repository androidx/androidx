/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.annotation.RestrictTo.Scope

/**
 * Denotes that the annotated element should only be accessed from within a specific scope (as
 * defined by [Scope]).
 *
 * Example of restricting usage within a library (based on Gradle group ID):
 * ```
 * @RestrictTo(GROUP_ID)
 * public void resetPaddingToInitialValues() { ...
 * ```
 *
 * Example of restricting usage to tests:
 * ```
 * @RestrictTo(Scope.TESTS)
 * public abstract int getUserId();
 * ```
 *
 * Example of restricting usage to subclasses:
 * ```
 * @RestrictTo(Scope.SUBCLASSES)
 * public void onDrawForeground(Canvas canvas) { ...
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE
)
public expect annotation class RestrictTo(
    /**
     * The scope(s) to which usage should be restricted.
     */
    vararg val value: Scope
) {
    public enum class Scope {
        /**
         * Restrict usage to code within the same library (e.g. the same Gradle group ID and
         * artifact ID).
         */
        LIBRARY,

        /**
         * Restrict usage to code within the same group of libraries.
         *
         * This corresponds to the Gradle group ID.
         */
        LIBRARY_GROUP,

        /**
         * Restrict usage to code within packages whose Gradle group IDs share the same prefix up to
         * the last `.` separator.
         *
         * For example, libraries `foo.bar:lib1` and `foo.baz:lib2` share the `foo.` prefix and can
         * therefore use each other's APIs that are restricted to this scope. Similar applies to
         * libraries `com.foo.bar:lib1` and `com.foo.baz:lib2`, which share the `com.foo.` prefix.
         *
         * Library `com.bar.qux:lib3`, however, will not be able to use the restricted API because
         * it only shares the prefix `com.` and not all the way until the last `.` separator.
         */
        LIBRARY_GROUP_PREFIX,

        /**
         * Restrict usage to code within the same group ID (based on Gradle group ID).
         *
         * This is an alias for [LIBRARY_GROUP_PREFIX].
         */
        @Deprecated(
            message = "Use @RestrictTo(LIBRARY_GROUP_PREFIX) instead",
            replaceWith = ReplaceWith(
                "LIBRARY_GROUP_PREFIX",
                "androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX"
            )
        )
        GROUP_ID,

        /**
         * Restrict usage to test source sets or code annotated with the [TESTS] restriction scope.
         *
         * This is equivalent to `@VisibleForTesting(NONE)`.
         */
        TESTS,

        /**
         * Restrict usage to subclasses of the enclosing class.
         *
         * **Note:** This scope should not be used to annotate packages.
         */
        SUBCLASSES,
    }
}
