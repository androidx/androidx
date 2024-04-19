/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools

/**
 * Annotated values that can be sent to/from SDKs in the Privacy Sandbox.
 *
 * The values should be public Kotlin data classes or enum classes.
 *
 * For data classes, they should only contain immutable properties with types supported by the
 * sandbox (primitives, [PrivacySandboxInterface], [PrivacySandboxValue], or lists of primitives or
 * [PrivacySandboxValue]). [PrivacySandboxCallback] interfaces are not allowed.
 *
 * For enum classes, they should only contain basic enum constants.
 *
 * Values cannot have functions, type parameters or properties with default values.
 *
 * Backwards compatibility: After the data/enum class is first published, no new fields should be
 * added, and existing fields should not be renamed, reordered, or changed to another type, unless
 * the SDK major version is incremented. This is required for backwards compatibility with possibly
 * mismatching SDK versions on the client side.
 *
 * Data class usage example:
 * ```
 * @PrivacySandboxValue
 * data class ComplicatedStructure(
 *   val id: Int,
 *   val separator: Char,
 *   val message: String,
 *   val hugeNumber: Double,
 *   val myInterface: MyInterface,
 *   val numbers: List<Int>,
 *   val maybeNumber: Int?,
 *   val maybeInterface: MyInterface?,
 * )
 * ```
 *
 * Enum class usage example:
 * ```
 * @PrivacySandboxValue
 * enum class Direction(
 *   UP,
 *   DOWN,
 * )
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class PrivacySandboxValue
