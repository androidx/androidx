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

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionManagerApi
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.internal.AppSearchAppFunctionReader
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.ExtensionAppFunctionManagerApi
import androidx.appfunctions.internal.NullTranslatorSelector
import androidx.appfunctions.internal.PlatformAppFunctionManagerApi
import androidx.appfunctions.internal.Translator
import androidx.appfunctions.internal.TranslatorSelector
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to interact with App Functions. This is a backward-compatible wrapper for the
 * platform class [android.app.appfunctions.AppFunctionManager].
 */
public class AppFunctionManagerCompat
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
        val functionMetadata: AppFunctionMetadata? =
            try {
                appFunctionReader.getAppFunctionMetadata(
                    functionId = request.functionIdentifier,
                    packageName = request.targetPackageName,
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

        val executeAppFunctionResponse = appFunctionManagerApi.executeAppFunction(translatedRequest)

        return processResponse(translator, functionMetadata, executeAppFunctionResponse)
    }

    @Suppress("NewApi") // AppFunctionManagerCompat is only available when SDK >= 33
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
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public fun observeAppFunctions(
        searchSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionPackageMetadata>> {
        return appFunctionReader.searchAppFunctions(searchSpec)
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
            AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
        /**
         * The app function is enabled. To enable an app function, call [setAppFunctionEnabled] with
         * this value.
         */
        public const val APP_FUNCTION_STATE_ENABLED: Int =
            AppFunctionManager.APP_FUNCTION_STATE_ENABLED
        /**
         * The app function is disabled. To disable an app function, call [setAppFunctionEnabled]
         * with this value.
         */
        public const val APP_FUNCTION_STATE_DISABLED: Int =
            AppFunctionManager.APP_FUNCTION_STATE_DISABLED

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
         * Gets an instance of [AppFunctionManagerCompat] if the AppFunction feature is supported.
         *
         * The AppFunction feature is supported,
         * * If SDK version is greater or equal to 36
         * * If SDK version is greater or equal to 34 and the device implements App Function
         *   extension library.
         *
         * @return an instance of [AppFunctionManagerCompat] if the AppFunction feature is supported
         *   or `null`.
         */
        @JvmStatic
        public fun getInstance(context: Context): AppFunctionManagerCompat? {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> {
                    AppFunctionManagerCompat(
                        context,
                        AppSearchAppFunctionReader(
                            context,
                            Dependencies.schemaAppFunctionInventory,
                        ),
                        PlatformAppFunctionManagerApi(context),
                        Dependencies.translatorSelector,
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    isExtensionLibraryAvailable() -> {
                    AppFunctionManagerCompat(
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
    }
}
