/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

/**
 * Specifies constraints applied on the integer values for return types, parameters, or properties
 * in app functions.
 *
 * This annotation can be applied to:
 * - functions annotated with `@AppFunction` (to constrain the return value),
 * - parameters of `@AppFunction`,
 * - or properties within an `@AppFunctionSerializable`.
 *
 * ### Usage Example:
 * ```
 * // Constraining a function return value:
 * @AppFunction
 * @AppFunctionIntValueConstraint(enumValues = [0, 1, 2])
 * fun getDisplayMode(): Int {
 *     // Function body
 * }
 *
 * // Constraining a parameter:
 * @AppFunction
 * fun adjustVolume(
 *     @AppFunctionIntValueConstraint(enumValues = [10, 15, 20, 25])
 *     volumeLevel: Int
 * ) {
 *     // Function body
 * }
 *
 * // Constraining a property within an AppFunctionSerializable:
 * @AppFunctionSerializable
 * data class MySettings(
 *     @property:AppFunctionIntValueConstraint(enumValues = [0, 1])
 *     val darkMode: Int
 * )
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
public annotation class AppFunctionIntValueConstraint(
    /**
     * The list of allowed integer values for the annotated element.
     *
     * These values are communicated to the agent or framework that interprets or invokes the
     * `@AppFunction`. If any of the values carry special meaning (e.g., `0` means "off", `1` means
     * "on"), such meanings should be documented clearly in the corresponding property, parameter,
     * or function return KDoc.
     */
    public val enumValues: IntArray = []
)
