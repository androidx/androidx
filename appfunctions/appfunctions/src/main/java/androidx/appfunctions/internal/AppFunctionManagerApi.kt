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

package androidx.appfunctions.internal

import android.app.appfunctions.AppFunctionRegistration
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionActivityState
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.RegisterAppFunctionRequest
import androidx.appfunctions.metadata.AppFunctionMetadata

/** Provides the backend to the [android.app.appfunctions.AppFunctionManager] API. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppFunctionManagerApi {
    /**
     * Execute the app function.
     *
     * @param request the app function details and the arguments.
     * @return the result of the attempt to execute the function.
     */
    public suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest,
        functionMetadata: AppFunctionMetadata,
    ): ExecuteAppFunctionResponse

    /**
     * Checks if [functionId] in [packageName] is enabled.
     *
     * @param packageName The package name of the owner of [functionId].
     * @param functionId The identifier of the app function.
     * @throws IllegalArgumentException If the [functionId] is not available under [packageName].
     */
    public suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean

    /**
     * Sets [newEnabledState] to an app function [functionId] owned by the calling package.
     *
     * @param functionId The identifier of the app function.
     * @param newEnabledState The new state of the app function.
     * @throws IllegalArgumentException If the [functionId] is not available.
     */
    public suspend fun setAppFunctionEnabled(
        functionId: String,
        @AppFunctionManager.EnabledState newEnabledState: Int,
    )

    /** Returns the [AppFunctionActivityState]s for the specified activities. */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public suspend fun getAppFunctionActivityStates(
        activityIds: Set<android.app.appfunctions.AppFunctionActivityId>
    ): List<AppFunctionActivityState>

    /** Registers multiple callback-based runtime implementations of app functions. */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public fun registerAppFunctions(
        requests: List<RegisterAppFunctionRequest>
    ): AppFunctionRegistration

    public companion object {
        /**
         * When the AppSearch indexer has finished but the AppFunction metadata adapter is still
         * running, apps calling isAppFunctionEnabled or setAppFunctionEnabled would encounter a
         * runtime exception with [RUNTIME_METADATA_MISSING_ERROR_MESSAGE] as error message.
         * However, that should have been returned as IllegalArgumentException according to the
         * public API documentation.
         */
        public fun applyMissingRuntimeMetadataExceptionFix(
            functionId: String,
            error: Exception,
        ): Exception {
            if (
                error is RuntimeException &&
                    error !is IllegalArgumentException &&
                    error.message?.contains(RUNTIME_METADATA_MISSING_ERROR_MESSAGE) == true
            ) {
                Log.d(Constants.APP_FUNCTIONS_TAG, "Apply missing runtime metadata exception fix")
                return IllegalArgumentException(
                    "Runtime metadata for $functionId is not yet created."
                )
            }
            return error
        }

        /**
         * The RuntimeException error message return by
         * [android.app.appfunctions.AppFunctionManager.isAppFunctionEnabled] and
         * [android.app.appfunctions.AppFunctionManager.setAppFunctionEnabled] when the runtime
         * metadata is not yet created.
         */
        private const val RUNTIME_METADATA_MISSING_ERROR_MESSAGE: String =
            "Expected 1 GenericDocument for runtimeMetadata, found 0"
    }
}
