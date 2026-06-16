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

import androidx.appfunctions.metadata.AppFunctionMetadata.AppFunctionScope

// TODO(b/501032667): Link androidx registration API here.
/**
 * Annotation to define an AppFunction signature that will have its implementation registered using
 * [android.app.appfunctions.AppFunctionManager.registerAppFunction].
 *
 * ### Example
 *
 * First, define your interface representing the signature:
 * ```
 * import androidx.appfunctions.AppFunctionSignature
 * import androidx.appfunctions.metadata.AppFunctionMetadata
 *
 * @AppFunctionSignature(
 *     scope = AppFunctionMetadata.SCOPE_ACTIVITY,
 *     appFunctionXmlFileName = "cart_functions"
 * )
 * fun interface AddCurrentItemToCart {
 *     /** Adds the item currently shown on screen to the cart. Returns the new cart size. */
 *     suspend fun addToCart(quantity: Int): Int
 * }
 * ```
 *
 * ### Generated content
 *
 * The AppFunction compiler processes classes marked with this annotation to generate an AppFunction
 * XML file named after the [appFunctionXmlFileName] parameter. This file is placed in the
 * application's `assets` directory and describes the AppFunction signatures exposed.
 *
 * ```
 * <appfunctions>
 *      <appfunction>
 *          <id>package.name.AddCurrentItemToCart#addToCart</id>
 *          <scope>activity</scope>
 *          <parameters>...</parameters>
 *          <response>...</response>
 *      </appfunction>
 *  </appfunctions>
 * ```
 *
 * Then, declare the generated XML file name in your `AndroidManifest.xml`:
 * ```xml
 * <application ...>
 *   <property
 *       android:name="android.app.appfunctions"
 *       android:value="cart_functions.xml" />
 *   ...
 * </application>
 * ```
 *
 * To declare mulltiple xml files in the manifest, use comma separated values. For example:
 * ```xml
 * <application ...>
 *   <property
 *       android:name="android.app.appfunctions"
 *       android:value="cart_functions.xml,payments_functions.xml" />
 *   ...
 * </application>
 * ```
 *
 * ### Supported Types
 *
 * For a detailed list of supported types and the rules governing their serialization, see
 * [androidx.appfunctions.AppFunctionSerializable].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSignature(
    /**
     * Specifies the lifecycle scope of this app function. See
     * [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_GLOBAL] and
     * [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_ACTIVITY] for details on how scope
     * affects registration and execution.
     */
    @AppFunctionScope public val scope: String,

    /**
     * The name of the XML resource file containing the app function metadata definition represented
     * by this signature.
     *
     * Multiple signatures can specify the same XML file name to group their metadata definitions
     * into a single XML resource file.
     */
    public val appFunctionXmlFileName: String,

    /**
     * Whether to use the functional interface's abstract method KDoc as a function's description
     * for the agent. The default value is `false`.
     *
     * If set to `true`, the KDoc will be used to populate:
     * - The function's [androidx.appfunctions.metadata.AppFunctionMetadata.description] as the
     *   KDoc, excluding Kotlin's supported tags like `@param`, `@throws`.
     * - The function's parameters'
     *   [androidx.appfunctions.metadata.AppFunctionParameterMetadata.description] from the KDoc's
     *   `@param` tags.
     * - The function's response's
     *   [androidx.appfunctions.metadata.AppFunctionResponseMetadata.description] from the KDoc's
     *   `@return` tags.
     *
     * Note: If an [AppFunctionInstruction] annotation is also present on the method, parameter, or
     * return type, its value will take precedence and override the corresponding KDoc description.
     *
     * Example:
     * ```kotlin
     * @AppFunctionSignature(
     *     scope = AppFunctionMetadata.SCOPE_GLOBAL,
     *     appFunctionXmlFileName = "my_functions",
     *     isDescribedByKDoc = true
     * )
     * fun interface EnableCaptionsSignature {
     *     /**
     *      * Enables closed captions for media playback.
     *      *
     *      * @param language The language code for the captions (e.g., "en", "es").
     *      * @param showBackground Whether to display a dark background behind the caption text.
     *      * @return Whether the captions were successfully enabled.
     *      */
     *     suspend fun enableCaptions(language: String, showBackground: Boolean): Boolean
     * }
     * ```
     *
     * In this example:
     * - [androidx.appfunctions.metadata.AppFunctionMetadata.description] will be: "Enables closed
     *   captions for media playback."
     * - [androidx.appfunctions.metadata.AppFunctionParameterMetadata.description] for `language`
     *   will be: "The language code for the captions (e.g., "en", "es")."
     * - [androidx.appfunctions.metadata.AppFunctionParameterMetadata.description] for
     *   `showBackground` will be: "Whether to display a dark background behind the caption text."
     * - [androidx.appfunctions.metadata.AppFunctionResponseMetadata.description] will be: "Whether
     *   the captions were successfully enabled."
     */
    public val isDescribedByKDoc: Boolean = false,
)
