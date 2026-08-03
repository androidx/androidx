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

import android.app.appfunctions.AppFunctionManager as PlatformAppFunctionManager
import android.app.appfunctions.AppFunctionRegistration
import android.content.Context
import android.os.Build
import android.os.UserManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionManagerApi
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.internal.AppSearchAppFunctionReader
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.ExtensionAppFunctionManagerApi
import androidx.appfunctions.internal.NullTranslatorSelector
import androidx.appfunctions.internal.PlatformAppFunctionManagerApi
import androidx.appfunctions.internal.PlatformAppFunctionReader
import androidx.appfunctions.internal.Translator
import androidx.appfunctions.internal.TranslatorSelector
import androidx.appfunctions.internal.findImpl
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import java.util.concurrent.Executor
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Provides access to interact with App Functions. This is a backward-compatible wrapper for the
 * platform class [android.app.appfunctions.AppFunctionManager].
 */
public class AppFunctionManager
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    private val context: Context,
    private val appFunctionReader: AppFunctionReader,
    private val appFunctionManagerApi: AppFunctionManagerApi,
    private val translatorSelector: TranslatorSelector = NullTranslatorSelector(),
) {

    /**
     * Checks if [functionId] in the caller's package is enabled.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.isAppFunctionEnabled].
     *
     * @param functionId The identifier of the app function.
     * @throws IllegalArgumentException If the [functionId] is not available in caller's package.
     */
    // TODO(b/539865222): Remove this API completely after migrating usages.
    public suspend fun isAppFunctionEnabled(functionId: String): Boolean {
        return isAppFunctionEnabled(packageName = context.packageName, functionId = functionId)
    }

    /**
     * Checks if [functionId] in [packageName] is enabled.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.isAppFunctionEnabled].
     *
     * @param packageName The package name of the owner of [functionId].
     * @param functionId The identifier of the app function.
     * @throws IllegalArgumentException If the [functionId] is not available under [packageName].
     */
    // TODO(b/539865222): Remove this API completely after migrating usages.
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean {
        return appFunctionManagerApi.isAppFunctionEnabled(
            packageName = packageName,
            functionId = functionId,
        )
    }

    /**
     * Sets [newEnabledState] to an app function [functionId] owned by the calling package.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.setAppFunctionEnabled].
     *
     * @param functionId The identifier of the app function.
     * @param newEnabledState The new state of the app function.
     * @throws IllegalArgumentException If the [functionId] is not available.
     */
    public suspend fun setAppFunctionEnabled(
        functionId: String,
        @EnabledState newEnabledState: Int,
    ) {
        return appFunctionManagerApi.setAppFunctionEnabled(functionId, newEnabledState)
    }

    /**
     * Execute the app function.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.executeAppFunction].
     *
     * @param request the app function details and the arguments.
     * @return the result of the attempt to execute the function.
     */
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
        val functionMetadata: AppFunctionMetadata =
            try {
                appFunctionReader.getAppFunctionMetadata(
                    functionId = request.functionIdentifier,
                    packageName = request.targetPackageName,
                )
                    ?: throw AppFunctionFunctionNotFoundException(
                        "App function not found with identifier: ${request.functionIdentifier} under package: ${request.targetPackageName}"
                    )
            } catch (ex: AppFunctionFunctionNotFoundException) {
                return ExecuteAppFunctionResponse.Error(ex)
            } catch (ex: Exception) {
                return ExecuteAppFunctionResponse.Error(
                    AppFunctionSystemUnknownException(
                        "Something went wrong when querying the app function from AppSearch: ${ex.message}"
                    )
                )
            }

        // Translate the request when necessary by looking into the target schema version.
        val translator =
            if (functionMetadata?.schema?.version == LEGACY_SDK_GLOBAL_SCHEMA_VERSION) {
                translatorSelector.getTranslator(functionMetadata.schema)
            } else {
                null
            }
        val translatedRequest: ExecuteAppFunctionRequest =
            if (translator != null) {
                val functionParametersToExecute =
                    translator.downgradeRequest(request.functionParameters)
                request.copy(functionParameters = functionParametersToExecute)
            } else {
                request
            }

        val executeAppFunctionResponse =
            appFunctionManagerApi.executeAppFunction(translatedRequest, functionMetadata)

        return processResponse(translator, functionMetadata, executeAppFunctionResponse)
    }

    @Suppress("NewApi") // AppFunctionManager is only available when SDK >= 33
    private fun processResponse(
        translator: Translator?,
        functionMetadata: AppFunctionMetadata?,
        response: ExecuteAppFunctionResponse,
    ): ExecuteAppFunctionResponse {
        if (response !is ExecuteAppFunctionResponse.Success) {
            return response
        }

        val currentVersionReturnValue =
            translator?.upgradeResponse(response.returnValue) ?: response.returnValue

        return if (functionMetadata == null) {
            ExecuteAppFunctionResponse.Success(currentVersionReturnValue)
        } else {
            ExecuteAppFunctionResponse.Success(
                currentVersionReturnValue.replaceSpecWith(
                    functionMetadata.response,
                    functionMetadata.components,
                )
            )
        }
    }

    /**
     * Observes available app functions metadata based on the provided filters.
     *
     * Allows discovering app functions that match the given [searchSpec] criteria and continuously
     * emits updates when relevant metadata changes.
     *
     * Updates to [AppFunctionPackageMetadata] can occur when the app defining the function is
     * updated or when a function's enabled state changes, and if multiple updates happen within a
     * short duration, only the latest update might be emitted.
     *
     * The calling app can observe metadata for:
     * - Functions in its own package (no permission required).
     * - When holding the `android.permission.EXECUTE_APP_FUNCTIONS` permission - functions in other
     *   packages that it is allowed to query via
     *   [android.content.pm.PackageManager.canPackageQuery].
     *
     * @param searchSpec an [AppFunctionSearchSpec] instance specifying the filters for searching
     *   the app function metadata.
     * @return a flow that emits a list of [AppFunctionPackageMetadata] matching the search criteria
     *   and updated versions of this list when underlying data changes.
     */
    // TODO(b/508188326): Remove this API completely after migrating usages.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public fun observeAppFunctions(
        searchSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionPackageMetadata>> {
        return appFunctionReader.searchAppFunctionsPackageMetadata(searchSpec)
    }

    /**
     * Observes changes to app functions within packages the caller can query.
     *
     * The returned flow only emits changes that occur after collection starts. Any changes before
     * collection are not reported.
     *
     * An example usage flow is:
     * 1. Start collecting from the [Flow] to monitor app function changes.
     * 2. Call [searchAppFunctions] and [getAppFunctionStates] to get the initial list of app
     *    functions and their states.
     * 3. When receiving [ObserveAppFunctionsEvent.MetadataChanged], call [searchAppFunctions] with
     *    a [AppFunctionSearchSpec] that matches the changed packages to get the updated metadata.
     * 4. When receiving [ObserveAppFunctionsEvent.StatesChanged], call [getAppFunctionStates] with
     *    the list of [androidx.appfunctions.metadata.AppFunctionName]s matching the changed
     *    functions to get the updated states. Note that this is guaranteed to trigger after
     *    [ObserveAppFunctionsEvent.MetadataChanged] for new functions or functions that also
     *    changed states. There is no need to call [getAppFunctionStates] when receiving
     *    [ObserveAppFunctionsEvent.MetadataChanged].
     *
     * @return a [Flow] emitting [ObserveAppFunctionsEvent]s representing metadata or state changes
     */
    @RequiresPermission(
        anyOf =
            [
                "android.permission.EXECUTE_APP_FUNCTIONS",
                "android.permission.DISCOVER_APP_FUNCTIONS",
                "android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM",
            ],
        conditional = true,
    )
    public fun observeAppFunctions(): Flow<ObserveAppFunctionsEvent> {
        return appFunctionReader.observeAppFunctions()
    }

    /**
     * Searches app function [AppFunctionMetadata]s.
     *
     * Note that the state is not guaranteed to be the latest, as metadata can change between
     * request and execute times when apps are updated.
     *
     * The calling app can search for:
     * - Functions in its own package (no permission required).
     * - Functions in other packages that it is allowed to query via
     *   [android.content.pm.PackageManager.canPackageQuery] when holding the
     *   [android.Manifest.permission.EXECUTE_APP_FUNCTIONS] permission.
     * - Functions in other packages that it is allowed to query via
     *   [android.content.pm.PackageManager.canPackageQuery] when holding either the
     *   `android.Manifest.permission.EXECUTE_APP_FUNCTIONS_SYSTEM` or
     *   `android.Manifest.permission.DISCOVER_APP_FUNCTIONS` permission on
     *   [Build.VERSION_CODES.CINNAMON_BUN] and above.
     *
     * @param searchSpec The spec of app functions to search for.
     */
    @RequiresPermission(
        anyOf =
            [
                "android.permission.EXECUTE_APP_FUNCTIONS",
                "android.permission.DISCOVER_APP_FUNCTIONS",
                "android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM",
            ],
        conditional = true,
    )
    public suspend fun searchAppFunctions(
        searchSpec: AppFunctionSearchSpec
    ): List<AppFunctionMetadata> {
        return appFunctionReader.searchAppFunctionsMetadata(searchSpec)
    }

    /**
     * Retrieves the runtime state of the specified app functions.
     *
     * This includes runtime-changing properties such as whether the functions are currently enabled
     * or disabled. Functions that do not exist or are not visible to the calling application will
     * be silently omitted from the result list.
     *
     * This method follows the same permission rules as [searchAppFunctions].
     *
     * See [android.app.appfunctions.AppFunctionManager.getAppFunctionActivityStates] for retrieving
     * the states of app functions associated with a specific activity.
     *
     * See [searchAppFunctions] on how to retrieve the {@link AppFunctionMetadata} of app functions.
     *
     * See [observeAppFunctions] for observing changes to app functions' {@link AppFunctionMetadata}
     * and {@link AppFunctionState}s.
     *
     * @param appFunctionNames The names of the app functions to request the state for.
     * @return the [AppFunctionState]s of the specified app functions.
     */
    @RequiresPermission(
        anyOf =
            [
                "android.permission.EXECUTE_APP_FUNCTIONS",
                "android.permission.DISCOVER_APP_FUNCTIONS",
                "android.permission.EXECUTE_APP_FUNCTIONS_SYSTEM",
            ],
        conditional = true,
    )
    // TODO(b/494238383): Remove annotation after supporting activityIds in CINNAMON_BUN+.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public suspend fun getAppFunctionStates(
        appFunctionNames: List<AppFunctionName>
    ): List<AppFunctionState> {
        return appFunctionReader.getAppFunctionStates(appFunctionNames)
    }

    /**
     * Registers a runtime implementation for an app function, that can be executed using
     * [executeAppFunction].
     *
     * [executeAppFunction] targeting an app function provided by this method will trigger the
     * [CallbackAppFunction.execute] method of the provided implementation, as long as the process
     * registering it is not frozen, and the [android.content.Context] registering it is not
     * destroyed (at which point the registration will be removed).
     *
     * You must declare the app function in your `AndroidManifest.xml` using an application-level
     * `<property>` named `android.app.appfunctions`. See
     * [androidx.appfunctions.metadata.AppFunctionMetadata] for details on the XML schema
     * (`your_app_functions.xml` in the example below).
     *
     * **Example manifest declaration:**
     *
     * ```xml
     * <application ...>
     *   <property
     *       android:name="android.app.appfunctions"
     *       android:value="your_app_functions.xml" />
     *   ...
     * </application>
     * ```
     *
     * Function implementations can only be registered from [android.app.Activity] or
     * [android.app.Service] contexts. If registering from an [android.app.Activity], strongly
     * consider [androidx.appfunctions.metadata.AppFunctionMetadata.SCOPE_ACTIVITY] for your
     * function definition.
     *
     * The `functionId` must correspond to an app function declared in your app's application-level
     * XML assets. If the identifier is not found, this method will throw an
     * [IllegalArgumentException]. Attempting to register a duplicate function for the same scope
     * will throw an [IllegalStateException].
     *
     * To register multiple functions at once, consider using [registerAppFunctions] as a more
     * efficient alternative.
     *
     * The system holds a strong reference to the provided [CallbackAppFunction] implementation as
     * long as it is registered. To prevent memory leaks and ensure the system is aware that the
     * function is no longer available, you must explicitly call
     * [AppFunctionRegistration.unregister] when the function is no longer relevant (e.g., in
     * [android.app.Activity.onStop] or before [android.app.Service.stopForeground]).
     *
     * @param functionId The unique identifier for the function, which must match an entry in the
     *   app's XML resource declarations.
     * @param executor The [Executor] on which the function will be invoked and the incoming
     *   [ExecuteAppFunctionRequest] will be validated (verifying that the incoming platform request
     *   aligns with the declared [androidx.appfunctions.metadata.AppFunctionMetadata]).
     * @param appFunction The [CallbackAppFunction] implementation to be executed when the function
     *   is triggered.
     * @return A [AppFunctionRegistration] object that can be used to unregister the function.
     * @throws IllegalStateException if a duplicate function is already registered for the same
     *   scope, or if not called from [android.app.Activity] or [android.app.Service] contexts.
     * @throws IllegalArgumentException if the provided [functionId] is not declared in the app's
     *   application-level XML resources or if an activity-scoped function is registered from a
     *   non-Activity context.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun registerAppFunction(
        functionId: String,
        executor: Executor,
        appFunction: CallbackAppFunction,
    ): AppFunctionRegistration {
        return registerAppFunctions(
            listOf(RegisterAppFunctionRequest(functionId, executor, appFunction))
        )
    }

    /**
     * Registers several [CallbackAppFunction] implementations at once, sharing a single lifecycle.
     *
     * This is a more efficient alternative to calling [registerAppFunction] multiple times.
     *
     * ### Behavior and Lifecycle
     *
     * Each function registered through this method follows the same execution and lifecycle rules
     * as those registered with [registerAppFunction].
     *
     * ### Batch Operation and Atomicity
     *
     * The registration is atomic: either all functions in the provided list are registered
     * successfully, or none are. If any function in the list fails validation (e.g., it is already
     * registered or not declared in the manifest), this method will throw an exception, and no
     * functions from the batch will be registered. Each function in the request follows the scoping
     * rules declared in the app's XML resources.
     *
     * A single [AppFunctionRegistration] object is returned, which can be used to unregister the
     * entire batch of functions with one call.
     *
     * @param requests A list of [RegisterAppFunctionRequest] objects, each specifying a function to
     *   be registered.
     * @return A single [AppFunctionRegistration] object that can be used to unregister all the
     *   functions in the batch with one call.
     * @throws IllegalStateException if any function in the `requests` list is already registered by
     *   this app.
     * @throws IllegalArgumentException if any [RegisterAppFunctionRequest.functionIdentifier] is
     *   not declared in the app's application-level XML assets or the `requests` list is empty.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun registerAppFunctions(
        requests: List<RegisterAppFunctionRequest>
    ): AppFunctionRegistration {
        return appFunctionManagerApi.registerAppFunctions(requests)
    }

    /**
     * Registers a runtime implementation of an app function bound to the calling coroutine's
     * lifecycle.
     *
     * This method suspends and keeps the function registered until the calling coroutine scope is
     * cancelled. Under the hood, it delegates the registration to [registerAppFunction] and ensures
     * it is unregistered when the coroutine is cancelled.
     *
     * For a callback-based API that does not require a coroutine scope, see [registerAppFunction].
     *
     * @param functionIdentifier The unique identifier of the app function.
     * @param appFunction The implementation of the app function to handle execution requests.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public suspend fun handleAppFunction(
        functionIdentifier: String,
        appFunction: SuspendingAppFunction,
    ): Nothing = handleAppFunction(HandleAppFunctionRequest(functionIdentifier, appFunction))

    /**
     * Registers a runtime implementation of an app function bound to the calling coroutine's
     * lifecycle.
     *
     * This method suspends and keeps the function registered until the calling coroutine scope is
     * cancelled. Under the hood, it delegates the registration to [registerAppFunction] and ensures
     * it is unregistered when the coroutine is cancelled.
     *
     * For a callback-based API that does not require a coroutine scope, see [registerAppFunction].
     *
     * @param request The request containing the function identifier and implementation.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public suspend fun handleAppFunction(request: HandleAppFunctionRequest): Nothing =
        handleAppFunctions(listOf(request))

    /**
     * Registers multiple runtime implementations of app functions bound to the calling coroutine's
     * lifecycle.
     *
     * This method suspends and keeps the functions registered until the calling coroutine scope is
     * cancelled. Under the hood, it delegates the registration to [registerAppFunctions] and
     * ensures they are unregistered when the coroutine is cancelled.
     *
     * For a callback-based API that does not require a coroutine scope, see [registerAppFunctions].
     *
     * @param requests The list of requests containing the function identifiers and implementations.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public suspend fun handleAppFunctions(requests: List<HandleAppFunctionRequest>): Nothing =
        coroutineScope {
            val dispatcher =
                currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
            val executor = dispatcher?.asExecutor() ?: Executor { it.run() }

            suspendCancellableCoroutine<Nothing> { cont ->
                val callbackRequests =
                    requests.map { request ->
                        val callbackAppFunction =
                            request.appFunction.toCallbackAppFunction(this@coroutineScope)
                        RegisterAppFunctionRequest(
                            request.functionIdentifier,
                            executor,
                            callbackAppFunction,
                        )
                    }

                val registration = registerAppFunctions(callbackRequests)

                cont.invokeOnCancellation { registration.unregister() }
            }
        }

    /**
     * Returns an [AppFunctionAdapter] for an interface annotated with `@AppFunctionSignature`.
     *
     * Retrieves a generated [AppFunctionAdapter] that bridges [ExecuteAppFunctionRequest] and
     * [ExecuteAppFunctionResponse] with the strongly-typed signature of the passed interface.
     *
     * This adapter allows wrapping a concrete implementation of the passed interface into a
     * [HandleAppFunctionRequest]. The resulting request can then be registered using
     * [handleAppFunction] or [handleAppFunctions].
     *
     * ### Example
     *
     * ```kotlin
     * @AppFunctionSignature(
     *     scope = AppFunctionMetadata.SCOPE_GLOBAL,
     *     appFunctionXmlFileName = "media_functions"
     * )
     * fun interface PlayMusic {
     *     suspend fun playSong(title: String)
     * }
     *
     * // Retrieve the adapter and register the implementation
     * val adapter = appFunctionManager.getAppFunctionAdapter(PlayMusic::class.java)
     * val request = adapter.adapt { title -> player.play(title) }
     *
     * coroutineScope.launch {
     *     appFunctionManager.handleAppFunction(request)
     * }
     * ```
     *
     * @param interfaceClass The interface class annotated with `@AppFunctionSignature`.
     * @return The [AppFunctionAdapter] for the [interfaceClass].
     * @throws IllegalArgumentException if the adapter class for [interfaceClass] cannot be found or
     *   instantiated.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : Any> getAppFunctionAdapter(interfaceClass: Class<T>): AppFunctionAdapter<T> {
        try {
            @Suppress("UNCHECKED_CAST")
            return interfaceClass.findImpl(prefix = "$", suffix = "_AppFunctionAdapter")
                as AppFunctionAdapter<T>
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to find or instantiate adapter class for ${interfaceClass.name}. " +
                    "Make sure the interface is annotated with @AppFunctionSignature annotation " +
                    "and the generated xml is referenced by the property within the <application> " +
                    "tag of your AndroidManifest.xml.",
                e,
            )
        }
    }

    @IntDef(
        value =
            [APP_FUNCTION_STATE_DEFAULT, APP_FUNCTION_STATE_ENABLED, APP_FUNCTION_STATE_DISABLED]
    )
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class EnabledState

    public companion object {
        /**
         * The default state of the app function. Call [setAppFunctionEnabled] with this to reset
         * enabled state to the default value.
         */
        public const val APP_FUNCTION_STATE_DEFAULT: Int =
            PlatformAppFunctionManager.APP_FUNCTION_STATE_DEFAULT
        /**
         * The app function is enabled. To enable an app function, call [setAppFunctionEnabled] with
         * this value.
         */
        public const val APP_FUNCTION_STATE_ENABLED: Int =
            PlatformAppFunctionManager.APP_FUNCTION_STATE_ENABLED
        /**
         * The app function is disabled. To disable an app function, call [setAppFunctionEnabled]
         * with this value.
         */
        public const val APP_FUNCTION_STATE_DISABLED: Int =
            PlatformAppFunctionManager.APP_FUNCTION_STATE_DISABLED

        /** The version shared across all schema defined in the legacy SDK. */
        private const val LEGACY_SDK_GLOBAL_SCHEMA_VERSION = 1L

        /**
         * Checks whether the AppFunction extension library is available.
         *
         * @return `true` if the AppFunctions extension library is available on this device, `false`
         *   otherwise.
         */
        private fun isExtensionLibraryAvailable(): Boolean =
            try {
                Class.forName("com.android.extensions.appfunctions.AppFunctionManager")
                true
            } catch (_: ClassNotFoundException) {
                false
            }

        /**
         * Gets an instance of [AppFunctionManager] if the AppFunction feature is supported.
         *
         * The AppFunction feature is supported if the calling user is not a profile and either of
         * the following conditions is met:
         * * SDK version is 36 or higher.
         * * SDK version is 34 or higher, and the device implements the App Function extension
         *   ibrary.
         *
         * @return an instance of [AppFunctionManager] if the AppFunction feature is supported or
         *   `null`.
         */
        @JvmStatic
        public fun getInstance(context: Context): AppFunctionManager? {
            // Required AppSearch is only available on U+.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return null
            }

            val userManager = context.getSystemService(UserManager::class.java)
            if (userManager?.isProfile == true) {
                return null
            }

            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN -> {
                    val reader =
                        PlatformAppFunctionReader(context, Dependencies.schemaAppFunctionInventory)
                    AppFunctionManager(
                        context,
                        reader,
                        PlatformAppFunctionManagerApi(context, reader),
                        Dependencies.translatorSelector,
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> {
                    val reader =
                        AppSearchAppFunctionReader(context, Dependencies.schemaAppFunctionInventory)
                    AppFunctionManager(
                        context,
                        reader,
                        PlatformAppFunctionManagerApi(context, reader),
                        Dependencies.translatorSelector,
                    )
                }
                isExtensionLibraryAvailable() -> {
                    AppFunctionManager(
                        context,
                        AppSearchAppFunctionReader(
                            context,
                            Dependencies.schemaAppFunctionInventory,
                        ),
                        ExtensionAppFunctionManagerApi(context),
                        Dependencies.translatorSelector,
                    )
                }
                else -> {
                    null
                }
            }
        }

        /**
         * Internal exception used to differentiate cancellation triggered explicitly by a platform
         * [android.os.CancellationSignal] from other forms of coroutine cancellation (such as
         * parent scope cancellation).
         */
        private class CancellationSignalTriggeredException(message: String? = null) :
            CancellationException(message)
    }

    /**
     * Wraps this [SuspendingAppFunction] into a [CallbackAppFunction].
     *
     * This bridges the suspending execution model into the callback-based execution model required
     * by the platform API. It handles launching the coroutine, mapping exceptions to the
     * corresponding [ExecuteAppFunctionResponse.Error], and bridging the
     * [android.os.CancellationSignal] into coroutine cancellation.
     *
     * Any unhandled exceptions that are not an [AppFunctionException] will be sent back as an
     * [AppFunctionAppUnknownException] and then re-thrown.
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @OptIn(DelicateCoroutinesApi::class)
    private fun SuspendingAppFunction.toCallbackAppFunction(
        coroutineScope: CoroutineScope
    ): CallbackAppFunction {
        return CallbackAppFunction { executeRequest, cancellationSignal, callback ->
            // ATOMIC guarantees the block executes even if cancelled before dispatch, preventing a
            // hanging callback. Inside, ensureActive() acts as the first suspension point,
            // immediately throwing if cancelled to safely route the error to the catch block.
            val job =
                coroutineScope.launch(start = CoroutineStart.ATOMIC) {
                    try {
                        ensureActive()
                        val response = this@toCallbackAppFunction.executeAppFunction(executeRequest)
                        callback.accept(response)
                    } catch (t: CancellationSignalTriggeredException) {
                        callback.accept(
                            ExecuteAppFunctionResponse.Error(
                                AppFunctionCancelledException(t.message)
                            )
                        )
                    } catch (t: AppFunctionException) {
                        callback.accept(ExecuteAppFunctionResponse.Error(t))
                    } catch (t: Throwable) {
                        callback.accept(
                            ExecuteAppFunctionResponse.Error(
                                AppFunctionAppUnknownException(t.message)
                            )
                        )
                        throw t
                    }
                }
            cancellationSignal.setOnCancelListener {
                job.cancel(CancellationSignalTriggeredException())
            }
        }
    }
}
