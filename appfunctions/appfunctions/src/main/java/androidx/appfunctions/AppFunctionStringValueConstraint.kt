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
 * Specifies constraints applied on the string values for return types, parameters, or properties in
 * app functions.
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
 * @AppFunctionStringValueConstraint(enumValues = ["LOW", "MEDIUM", "HIGH"])
 * fun getPriorityLevel(): String {
 *     // Function body
 * }
 *
 * // Constraining a parameter:
 * @AppFunction
 * fun setMode(
 *     @AppFunctionStringValueConstraint(enumValues = ["AUTO", "MANUAL"])
 *     mode: String
 * ) {
 *     // Function body
 * }
 *
 * // Constraining a property within an AppFunctionSerializable:
 * @AppFunctionSerializable
 * data class AppConfig(
 *     @property:AppFunctionStringValueConstraint(enumValues = ["ENABLED", "DISABLED"])
 *     val featureFlag: String
 * )
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
public annotation class AppFunctionStringValueConstraint(
    /**
     * The list of allowed string values for the annotated element.
     *
     * These values are communicated to the agent or framework that interprets or invokes the
     * `@AppFunction`. If any of the values carry special meaning (e.g., `"AUTO"` means automatic
     * mode), such meanings should be documented clearly in the corresponding property, parameter,
     * or function return KDoc.
     */
    val enumValues: Array<String> = []
)
