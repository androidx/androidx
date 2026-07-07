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

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionInventoryProvider
import androidx.appfunctions.internal.AppFunctionMetadataUtils.getAppFunctionMetadata
import com.android.extensions.appfunctions.AppFunctionException as ExtensionAppFunctionException
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest as ExtensionExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse as ExtensionExecuteAppFunctionResponse
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Abstract base class to provide app functions to the system for Android versions 14-16
 * (inclusive), if the AppFunctions extensions library is available on the device.
 *
 * This class wraps [com.android.extensions.appfunctions.AppFunctionService] functionalities and
 * provides an API that uses `androidx.appfunctions` classes.
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
 * @see [com.android.extensions.appfunctions.AppFunctionService]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public abstract class ExtensionsAppFunctionService :
    AppFunctionService(), AppFunctionInventoryProvider {
    private lateinit var workerExecutor: ExecutorService
    private lateinit var workerCoroutineScope: CoroutineScope

    /**
     * Implements [AppFunctionService.onExecuteFunction] and delegates the execution to
     * [onExecuteFunction] when called by the system.
     *
     * @param request The function execution request.
     * @param callingPackage The package name of the app that is requesting the execution. It is
     *   strongly recommended that you do not alter your function’s behavior based on this value.
     *   Your function should behave consistently for all callers to ensure a predictable
     *   experience.
     * @param cancellationSignal A signal to cancel the execution.
     * @param callback A callback to report back the result or error.
     */
    final override fun onExecuteFunction(
        request: ExtensionExecuteAppFunctionRequest,
        callingPackage: String,
        cancellationSignal: CancellationSignal,
        callback:
            OutcomeReceiver<ExtensionExecuteAppFunctionResponse, ExtensionAppFunctionException>,
    ) {
        // Create a delegator cancellation signal to ensure that it would propagate to
        // [executeFunction] for subclass to receive too.
        val delegateCancellationSignal = CancellationSignal()
        // Just delegate to the suspend version
        val functionExecutionJob =
            workerCoroutineScope.launch {
                val appFunctionMetadata =
                    getAppFunctionMetadata(
                        this@ExtensionsAppFunctionService,
                        resolveInventory(),
                        request.functionIdentifier,
                    )
                if (appFunctionMetadata == null) {
                    callback.onError(
                        AppFunctionFunctionNotFoundException(
                                "No function found with identifier: " +
                                    "${request.functionIdentifier} in package: " +
                                    "${this@ExtensionsAppFunctionService.packageName}"
                            )
                            .toPlatformExtensionsClass()
                    )
                    return@launch
                }
                this@ExtensionsAppFunctionService.mainExecutor.execute {
                    onExecuteFunction(
                        ExecuteAppFunctionRequest.fromPlatformExtensionClass(
                            request,
                            appFunctionMetadata,
                        ),
                        delegateCancellationSignal,
                    ) { response ->
                        when (response) {
                            is ExecuteAppFunctionResponse.Success -> {
                                response.grantUriAccess(
                                    context = this@ExtensionsAppFunctionService,
                                    callingPackageName = callingPackage,
                                )
                                callback.onResult(response.toPlatformExtensionClass())
                            }
                            is ExecuteAppFunctionResponse.Error ->
                                callback.onError(response.error.toPlatformExtensionsClass())
                        }
                    }
                }
            }
        // Handle cancellation
        cancellationSignal.setOnCancelListener {
            delegateCancellationSignal.cancel()
            functionExecutionJob.cancel()
        }
    }

    /**
     * Called by the system following an [AppFunctionManager.executeAppFunction] call that targets
     * an app function identifier referenced by the XML asset associated with this service.
     *
     * You can determine which function to execute using
     * [ExecuteAppFunctionRequest.functionIdentifier]. This allows your service to route the
     * incoming request to the appropriate logic for handling the specific function. See
     * [ExecuteAppFunctionRequest] for how to retrieve the function's arguments.
     *
     * This method is always triggered in the main thread. You should run heavy tasks on a worker
     * thread and dispatch the result with the given callback. You should always report back the
     * result using the callback, no matter if the execution was successful or not.
     *
     * The implementation should try to respect the provided [CancellationSignal], if possible.
     *
     * @param request The function execution request.
     * @param cancellationSignal A signal to cancel the execution.
     * @param callback A callback to report back the result or error.
     */
    @MainThread
    public abstract fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>,
    )

    /**
     * Implementing class can override this method to perform setup but should always call the
     * superclass implementation.
     */
    @CallSuper
    override fun onCreate() {
        super.onCreate()
        workerExecutor = Executors.newSingleThreadExecutor()
        workerCoroutineScope =
            CoroutineScope(SupervisorJob() + workerExecutor.asCoroutineDispatcher())
    }

    /**
     * Implementing class can override this method to perform cleanup but should always call the
     * superclass implementation.
     */
    @CallSuper
    override fun onDestroy() {
        workerCoroutineScope.cancel()
        workerExecutor.shutdown()
        super.onDestroy()
    }

    internal companion object {
        // Hack to suppress InterfaceConstant lint in the base class.
        @SuppressWarnings("InterfaceConstant")
        const val SERVICE_INTERFACE: String = "android.app.appfunctions.AppFunctionService"
    }
}
