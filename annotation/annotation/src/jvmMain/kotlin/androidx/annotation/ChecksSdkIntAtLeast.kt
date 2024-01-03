/*
 * Copyright (C) 2020 The Android Open Source Project
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

/**
 * Denotes that the annotated method checks if the SDK_INT API level is at least the given value,
 * and either returns it or executes the given lambda in that case (or if it's a field, has the
 * value true).
 *
 * The API level can be specified either as an API level via [.api], or for preview platforms as a
 * codename (such as "R") via [.codename]}, or it can be passed in to the method; in that case, the
 * parameter containing the API level or code name should be specified via [.parameter], where the
 * first parameter is numbered 0.
 *
 * Examples:
 * ```
 * // Simple version check
 * @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
 * public static boolean isAtLeastO() {
 *     return Build.VERSION.SDK_INT >= 26;
 * }
 *
 * // Required API level is passed in as first argument, and function
 * // in second parameter is executed if SDK_INT is at least that high:
 * @ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
 * inline fun fromApi(value: Int, action: () -> Unit) {
 *     if (Build.VERSION.SDK_INT >= value) {
 *         action()
 *     }
 * }
 *
 * // Kotlin property:
 * @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD)
 * val isGingerbread: Boolean
 * get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
 *
 * // Java field:
 * @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
 * public static final boolean SUPPORTS_LETTER_SPACING =
 * Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD
)
public annotation class ChecksSdkIntAtLeast(
    /** The API level is at least the given level */
    val api: Int = -1,
    /** The API level is at least the given codename (such as "R") */
    val codename: String = "",
    /** The API level is specified in the given parameter, where the first parameter is number 0 */
    val parameter: Int = -1,
    /**
     * The parameter number for a lambda that will be executed if the API level is at least the
     * value supplied via [api], [codename] or [parameter]
     */
    val lambda: Int = -1,

    /** The associated Extension SDK id, or 0 if the Android platform */
    val extension: Int = 0
)
