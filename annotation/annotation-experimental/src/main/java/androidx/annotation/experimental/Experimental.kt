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

package androidx.annotation.experimental

import kotlin.annotation.Retention
import kotlin.annotation.Target

/**
 * Denotes that the annotated element is a marker of an experimental API.
 *
 * Any declaration annotated with this marker is considered part of an unstable API surface and its
 * call sites should accept the experimental aspect of it either by using [UseExperimental],
 * or by being annotated with that marker themselves, effectively causing further propagation of
 * that experimental aspect.
 */
@Deprecated(
    "This annotation has been replaced by `@RequiresOptIn`",
    ReplaceWith("RequiresOptIn", "androidx.annotation.RequiresOptIn")
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class Experimental(
    /**
     * Defines the reporting level for incorrect usages of this experimental API.
     */
    public val level: Level = Level.ERROR
) {
    /**
     * Severity of the diagnostic that should be reported on usages of experimental API which did
     * not explicitly accept the experimental aspect of that API either by using
     * [UseExperimental] or by being annotated with the corresponding marker annotation.
     */
    public enum class Level {
        /**
         * Specifies that a warning should be reported on incorrect usages of this experimental API.
         */
        WARNING,

        /**
         * Specifies that an error should be reported on incorrect usages of this experimental API.
         */
        ERROR
    }
}
