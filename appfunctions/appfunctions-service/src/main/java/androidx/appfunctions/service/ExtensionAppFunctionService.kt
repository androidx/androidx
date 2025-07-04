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

package androidx.appfunctions.service

import android.annotation.SuppressLint
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.Dispatchers
import androidx.appfunctions.service.internal.ServiceDependencies
import com.android.extensions.appfunctions.AppFunctionException as ExtensionAppFunctionException
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest as ExtensionExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse as ExtensionExecuteAppFunctionResponse

/** The implementation of [AppFunctionService] from extension library. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("NewApi")
public class ExtensionAppFunctionService : AppFunctionService() {

    private lateinit var delegate: AppFunctionServiceDelegate

    override fun onCreate() {
        super.onCreate()
        delegate =
            AppFunctionServiceDelegate(
                this@ExtensionAppFunctionService,
                Dispatchers.Worker,
                Dispatchers.Main,
                ServiceDependencies.aggregatedAppFunctionInventory,
                ServiceDependencies.aggregatedAppFunctionInvoker,
                Dependencies.translatorSelector,
            )
    }

    override fun onExecuteFunction(
        request: ExtensionExecuteAppFunctionRequest,
        callingPackage: String,
        cancellationSignal: CancellationSignal,
        callback:
            OutcomeReceiver<ExtensionExecuteAppFunctionResponse, ExtensionAppFunctionException>,
    ) {
        val executionJob =
            delegate.onExecuteFunction(
                ExecuteAppFunctionRequest.fromPlatformExtensionClass(request),
                object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                    override fun onResult(result: ExecuteAppFunctionResponse) {
                        when (result) {
                            is ExecuteAppFunctionResponse.Success -> {
                                callback.onResult(result.toPlatformExtensionClass())
                            }
                            is ExecuteAppFunctionResponse.Error -> {
                                callback.onError(result.error.toPlatformExtensionsClass())
                            }
                        }
                    }

                    override fun onError(error: AppFunctionException) {
                        callback.onError(error.toPlatformExtensionsClass())
                    }
                },
            )
        cancellationSignal.setOnCancelListener { executionJob.cancel() }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.onDestroy()
    }
}
