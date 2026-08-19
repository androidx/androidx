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

/**
 * Marks a functional interface as an AppFunction signature for runtime registration using
 * [AppFunctionManager.handleAppFunction] or [AppFunctionManager.handleAppFunctions].
 *
 * Interfaces annotated with [AppFunctionSignature] allow the AppFunction compiler to generate
 * metadata XML files and adapters that bridge the gap between platform requests and your Kotlin
 * implementation.
 *
 * ## Usage Example: Step-by-Step Registration
 *
 * ### Step 1: Define the Signature
 *
 * ```kotlin
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
 * ### Step 2: Declare in AndroidManifest
 *
 * Add a property tag in the `<application>` section. The value must include the `.xml` extension
 * and match [appFunctionXmlFileName].
 *
 * ```xml
 * <application ...>
 *   <property
 *       android:name="android.app.appfunctions"
 *       android:value="cart_functions.xml" />
 * </application>
 * ```
 *
 * ### Step 3: Register the Implementation
 *
 * Retrieve the adapter via [AppFunctionManager.getHandleAppFunctionRequestAdapter] and register
 * your logic using [AppFunctionManager.handleAppFunction]:
 * ```kotlin
 * val addToCartAppFunctionAdapter =
 *     appFunctionManager.getHandleAppFunctionRequestAdapter(AddCurrentItemToCart::class.java)
 *
 * coroutineScope.launch {
 *     appFunctionManager.handleAppFunction(
 *         addToCartAppFunctionAdapter.adapt { quantity ->
 *             val newCartSize = cart.addItem(quantity)
 *             newCartSize
 *         }
 *     )
 * }
 * ```
 *
 * ## Best Practices
 * - **Adapter Caching:** Because [AppFunctionManager.getHandleAppFunctionRequestAdapter] uses
 *   reflection under the hood to instantiate the adapter, we recommend loading it in advance to
 *   avoid runtime latency.
 * - **Lifecycle Scope:** Since `handleAppFunction` is a suspending function, registration remains
 *   active only while its coroutine is running. We recommend using a scope that is alive between
 *   `onResume` and `onPause` (such as `Lifecycle.State.RESUMED`), or if registering within a
 *   composable, using a Compose side-effect (such as `LaunchedEffect` combined with
 *   `repeatOnLifecycle`).
 *
 * ## Generated Content
 *
 * The AppFunction compiler processes classes marked with this annotation to generate two key
 * artifacts:
 * - **An AppFunction XML file:** The compiler generates an XML file named after the
 *   [appFunctionXmlFileName] parameter. This file is placed in the application's `assets` directory
 *   and describes the AppFunctions exposed by this entry point:
 * ```xml
 * <appfunctions>
 *      <appfunction>
 *          <id>com.example.AddCurrentItemToCart#addToCart</id>
 *          <scope>activity</scope>
 *          <parameters>...</parameters>
 *          <response>...</response>
 *      </appfunction>
 *  </appfunctions>
 * ```
 * - **A concrete adapter class:** The compiler generates an implementation of
 *   [HandleAppFunctionRequestAdapter] in the same package as your annotated interface. This adapter
 *   maps the function string identifier to your interface call and handles request/response data
 *   conversions:
 * ```kotlin
 * appFunctionManager.getHandleAppFunctionRequestAdapter(AddCurrentItemToCart::class.java)
 * // Returns:
 * object : HandleAppFunctionRequestAdapter<AddCurrentItemToCart> {
 *     override val functionId: String = "com.example.AddCurrentItemToCart#addToCart"
 *
 *     override fun adapt(implementation: AddCurrentItemToCart): HandleAppFunctionRequest =
 *         HandleAppFunctionRequest(functionId) { request: ExecuteAppFunctionRequest ->
 *             val quantity = request.functionParameters.getInt("quantity")
 *             val result = implementation.addToCart(quantity)
 *             val responseData =
 *                 AppFunctionData.Builder("")
 *                     .setInt(
 *                         ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE,
 *                         result,
 *                     )
 *                     .build()
 *             ExecuteAppFunctionResponse.Success(responseData)
 *         }
 * }
 * ```
 *
 * ## Supported Types
 *
 * See [androidx.appfunctions.AppFunctionSerializable] for supported parameter and return types.
 *
 * @see HandleAppFunctionRequestAdapter
 * @see AppFunctionManager.getHandleAppFunctionRequestAdapter
 */
@ExperimentalAppFunctionsApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSignature(
    /**
     * The scope of the app function.
     *
     * The scope determines the function's lifecycle and uniqueness rules. Depending on the scope,
     * there could be at most one or multiple functions registered in the system with the same
     * [androidx.appfunctions.metadata.AppFunctionName].
     *
     * Possible values:
     * - [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_GLOBAL]
     * - [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_ACTIVITY]
     */
    @AppFunctionScope public val scope: Int,

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
