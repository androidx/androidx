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

import android.app.appfunctions.AppFunctionException as PlatformAppFunctionException
import android.app.appfunctions.AppFunctionService as PlatformAppFunctionService
import android.app.appfunctions.ExecuteAppFunctionRequest as PlatformExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse as PlatformExecuteAppFunctionResponse
import android.content.pm.SigningInfo
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.toCompatExecuteAppFunctionRequest
import androidx.appfunctions.internal.AppFunctionMetadataUtils.getAppFunctionMetadata
import androidx.appfunctions.internal.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Abstract base class to provide app functions to the system.
 *
 * This class wraps [AppFunctionService] functionalities and provides an API that uses
 * `androidx.appfunctions` classes.
 *
 * Include the following in the manifest:
 * ```
 * <service android:name=".YourService"
 *  android:permission="android.permission.BIND_APP_FUNCTION_SERVICE">
 *  <intent-filter>
 *      <action android:name="android.app.appfunctions.AppFunctionService" />
 *  </intent-filter>
 * </service>
 * ```
 *
 * @see [android.app.appfunctions.AppFunctionService]
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
public abstract class AppFunctionService : PlatformAppFunctionService() {
    private val workerCoroutineScope = CoroutineScope(Dispatchers.Worker)

    /**
     * Implements [AppFunctionService.onExecuteFunction] and delegates the execution to
     * [executeFunction] when called by the system.
     *
     * @param request The function execution request.
     * @param callingPackage The package name of the app that is requesting the execution. It is
     *   strongly recommended that you do not alter your function’s behavior based on this value.
     *   Your function should behave consistently for all callers to ensure a predictable
     *   experience.
     * @param signingInfo The signing information of the app that is requesting the execution. It is
     *   strongly recommended that you do not alter your function’s behavior based on this value.
     *   Your function should behave consistently for all callers to ensure a predictable
     *   experience.
     * @param cancellationSignal A signal to cancel the execution.
     * @param callback A callback to report back the result or error.
     */
    final override fun onExecuteFunction(
        request: PlatformExecuteAppFunctionRequest,
        callingPackage: String,
        signingInfo: SigningInfo,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<PlatformExecuteAppFunctionResponse, PlatformAppFunctionException>,
    ) {
        // Just delegate to the suspend version
        val functionExecutionJob =
            workerCoroutineScope.launch {
                val result =
                    try {
                        val appFunctionMetadata =
                            getAppFunctionMetadata(
                                this@AppFunctionService,
                                request.functionIdentifier,
                            )
                                ?: throw AppFunctionFunctionNotFoundException(
                                    "No function found with identifier: ${request.functionIdentifier} in package: ${this@AppFunctionService.packageName}"
                                )
                        withContext(Dispatchers.Main) {
                            executeFunction(
                                request.toCompatExecuteAppFunctionRequest(appFunctionMetadata)
                            )
                        }
                    } catch (e: AppFunctionException) {
                        ExecuteAppFunctionResponse.Error(e)
                    }

                when (result) {
                    is ExecuteAppFunctionResponse.Success -> {
                        result.grantUriAccess(
                            context = this@AppFunctionService,
                            callingPackageName = callingPackage,
                        )
                        callback.onResult(result.toPlatformExecuteAppFunctionResponse())
                    }
                    is ExecuteAppFunctionResponse.Error ->
                        callback.onError(result.error.toPlatformClass())
                }
            }
        // Handle cancellation
        cancellationSignal.setOnCancelListener { functionExecutionJob.cancel() }
    }

    /**
     * Called by the system to execute a specific app function.
     *
     * This method is the entry point for handling all app function requests in an app. When the
     * system needs your AppFunctionService to perform a function, it will invoke this method.
     *
     * Each function you've registered is identified by a unique identifier. This identifier doesn't
     * need to be globally unique, but it must be unique within your app. For example, a function to
     * order food could be identified as "orderFood".
     *
     * You can determine which function to execute by using
     * [ExecuteAppFunctionRequest.functionIdentifier]. This allows your service to route the
     * incoming request to the appropriate logic for handling the specific function.
     *
     * This method is always triggered in the main thread. You should run heavy tasks on a worker
     * thread.
     *
     * ### Exception Handling
     *
     * When an error occurs during execution, implementations have two options to report the
     * failure:
     * 1. Throw an appropriate [androidx.appfunctions.AppFunctionException].
     * 2. Return an [ExecuteAppFunctionResponse.Error] by wrapping an
     *    [androidx.appfunctions.AppFunctionException].
     *
     * This allows the agent to better understand the cause of the failure. For example, if an input
     * argument is invalid, throw or wrap an
     * [androidx.appfunctions.AppFunctionInvalidArgumentException] with a detailed message
     * explaining why it is invalid.
     *
     * Any unhandled exception other than [androidx.appfunctions.AppFunctionException] will be
     * reported as [androidx.appfunctions.AppFunctionAppUnknownException].
     *
     * ### Cancellation
     * The agent app can cancel the execution of an app function at any time. When this happens, the
     * coroutine executing this `executeFunction` will be cancelled. Implementations should handle
     * the [kotlinx.coroutines.CancellationException] appropriately, for example, by ceasing any
     * ongoing work and releasing resources.
     *
     * @param request The function execution request.
     */
    @MainThread
    public abstract suspend fun executeFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse

    /**
     * Implementing class can override this method to perform cleanup but should always call the
     * superclass implementation.
     */
    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        workerCoroutineScope.cancel()
    }
}
