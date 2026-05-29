/*
 * Copyright 2026 The Android Open Source Project
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
 * Provides an explicit instruction for an [AppFunction] or an [AppFunctionSerializable] and their
 * respective components.
 *
 * Using this annotation will provide instructions to agents that receive the
 * `androidx.appfunctions.metadata.AppFunctionMetadata` to understand how to use the AppFunction.
 *
 * It can be applied to the following targets:
 * - A function annotated with `@AppFunction`: Sets the general description of the AppFunction.
 *
 * ```
 * @AppFunction
 * @AppFunctionInstruction("Creates a new calendar event.")
 * fun createEvent(title: String)
 * ```
 * - A parameter of an `@AppFunction`: Sets the description for that specific parameter.
 *
 * ```
 * @AppFunction
 * fun getWeather(
 *   context: AppFunctionContext,
 *   @AppFunctionInstruction("The city to get the weather for, e.g., 'San Francisco'.")
 *   city: String
 * )
 * ```
 * - The return type of `@AppFunction`: Sets the description for the response.
 *
 * ```
 * @AppFunction
 * fun calculateDistance(
 *     context: AppFunctionContext,
 *     start: Location,
 *     destination: Location
 * ): @AppFunctionInstruction("The distance in miles.") Float
 * ```
 * - A class annotated with `@AppFunctionSerializable`: Sets the description of the data type.
 *
 * ```
 * @AppFunctionSerializable
 * @AppFunctionInstruction("Represents a geographical location.")
 * class Location(val lat: Double, val lng: Double)
 * ```
 * - A property of an `@AppFunctionSerializable` class: Sets the description for that property.
 *
 * ```
 * @AppFunctionSerializable
 * class UserProfile(
 *     @AppFunctionInstruction("The user's display name.")
 *     val displayName: String
 * )
 * ```
 *
 * When a component has opted-in to KDoc extraction (e.g., using `isDescribedByKDoc = true`), the
 * [instruction] provided by this annotation will take precedence, overriding the KDoc description
 * for the component in the generated metadata. For example, if the annotation is applied on the
 * parameter, it would override the parameter's KDoc.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.CLASS,
)
public annotation class AppFunctionInstruction(
    /**
     * The explicit instruction to be used in the generated metadata.
     *
     * This text will override any extracted KDoc description for the annotated element.
     */
    val instruction: String
)
