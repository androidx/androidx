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

import androidx.annotation.RestrictTo

/**
 * Specifies constraints applied on [android.net.Uri] values for parameters, return types, or
 * properties in app functions.
 *
 * This annotation can be applied to:
 * - parameters of an `@AppFunction` (of type [android.net.Uri]),
 * - functions annotated with `@AppFunction` (for the return value of type [android.net.Uri]),
 * - or properties within an `@AppFunctionSerializable` (of type [android.net.Uri]).
 *
 * At compile time, the compiler translates this constraint into an
 * [androidx.appfunctions.metadata.AppFunctionStringTypeMetadata] object with:
 * - [androidx.appfunctions.metadata.AppFunctionStringTypeMetadata.format] set to
 *   [androidx.appfunctions.metadata.AppFunctionStringTypeMetadata.FORMAT_URI].
 * - [androidx.appfunctions.metadata.AppFunctionStringTypeMetadata.pattern] set to a regular
 *   expression matching the specified [allowedSchemes].
 *
 * ### Usage Example:
 * ```
 * // Constraining a parameter:
 * @AppFunction
 * fun updateWallpaper(
 *     @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
 *     wallpaperUri: Uri
 * ) {
 *     // Function body
 * }
 *
 * // Constraining a function return value:
 * @AppFunction
 * @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
 * fun getProfilePictureUri(): Uri {
 *     // Function body
 * }
 *
 * // Constraining a property within an AppFunctionSerializable:
 * @AppFunctionSerializable
 * data class WallpaperConfig(
 *     @property:AppFunctionUriValueConstraint(allowedSchemes = ["content"])
 *     val wallpaperUri: Uri
 * )
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class AppFunctionUriValueConstraint(
    /**
     * The list of allowed URI schemes for the annotated element defined by the developer (e.g.
     * `["content"]` or custom schemes such as `["photo", "video"]`).
     *
     * At compile time, this list is translated into a regular expression matching the specified
     * schemes (for example, `^content:.*` when `allowedSchemes = ["content"]`). If empty, any URI
     * scheme is permitted.
     */
    val allowedSchemes: Array<String> = []
)
