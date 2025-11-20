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
 * Annotates a class to indicate that it can be serialized and transferred between processes using
 * AppFunction.
 *
 * When a class is annotated with `@AppFunctionSerializable` and is used as a parameter or return
 * type (directly or as a nested entity) in an AppFunction, the shape of the entity defined within
 * its primary constructor will be exposed to the caller as part of an
 * [androidx.appfunctions.metadata.AppFunctionMetadata]. This information allows the caller to
 * construct the structured input to call an AppFunction or understand what properties are provided
 * in the structured output.
 *
 * **Constraints for Classes Annotated with `@AppFunctionSerializable`:**
 * * **Primary Constructor Parameters:** Only properties declared in the primary constructor that
 *   expose a getter method are eligible for inclusion in the AppFunctionMetadata. Critically, it is
 *   a **requirement** to place properties with a getter in primary constructor. Attempting to
 *   include constructor parameters that are not declared as properties (and thus don't have a
 *   getter) will result in a compiler error.
 * * **Supported Property Types:** The properties in the primary constructor must be of one of the
 *   following types:
 *     * `Int`
 *     * `Long`
 *     * `Float`
 *     * `Double`
 *     * `Boolean`
 *     * `String`
 *     * `IntArray`
 *     * `LongArray`
 *     * `FloatArray`
 *     * `DoubleArray`
 *     * `BooleanArray`
 *     * `List<String>`
 *     * Another class annotated with `@AppFunctionSerializable` (enabling nested structures) or a
 *       list of a class annotated with `@AppFunctionSerializable`
 * * **Public Primary Constructor:** The primary constructor of the annotated class must have public
 *   visibility to allow instantiation.
 * * **
 *
 * **IMPORTANT:** When the default value is set in the constructor parameter, the field would be
 * exposed to the caller as optional in the
 * [androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata]. However, the default value is
 * ignored when the value is missing from the function calling request. Instead, the specified rule
 * is always used:
 * * For non-nullable primitive types, the JVM default for that type is used (e.g., `0` for `Int`).
 * * For non-nullable collection types, an empty collection is provided.
 * * For nullable types, the value will be `null`.
 * * Any other types are not allowed to be optional.
 *
 * **Example:**
 *
 * ```
 * @AppFunctionSerializable
 * class Location(val latitude: Double, val longitude: Double)
 *
 * @AppFunctionSerializable
 * class Place(
 *     val name: String,
 *     val location: Location, // Nested AppFunctionSerializable
 *     // Nullable String is allowed, if missing, will be null. The default value will not be used
 *     // when the value is missing
 *     val notes: String? = "default"
 * )
 *
 * @AppFunctionSerializable
 * class SearchPlaceResult(
 *     val places: List<Place> // If missing, will be an empty list
 * )
 *
 * @AppFunctionSerializable
 * class Attachment(
 *   uri: String // Putting constructor parameter without getter will result in compiler error
 * )
 * ```
 *
 * @see androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
 */
// Use BINARY here so that the annotation is kept around at the aggregation stage.
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSerializable(
    /**
     * Whether to use the serializable's KDoc as the type's description to the agent. The default
     * value is `false`.
     *
     * If set to `true`, the serializable's KDoc will be set as the type's
     * [androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.description]. The class's
     * properties' KDoc will also be set as their corresponding descriptions. The caller will use
     * the descriptions to interpret when and how to use the class and its properties.
     */
    public val isDescribedByKdoc: Boolean = false
)
