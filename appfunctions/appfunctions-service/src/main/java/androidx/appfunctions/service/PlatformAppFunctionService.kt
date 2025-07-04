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

import android.app.appfunctions.AppFunctionException as PlatformAppFunctionException
import android.app.appfunctions.AppFunctionService
import android.app.appfunctions.ExecuteAppFunctionRequest as PlatformExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse as PlatformExecuteAppFunctionResponse
import android.content.pm.SigningInfo
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.Dispatchers
import androidx.appfunctions.service.internal.ServiceDependencies

/** The implementation of [AppFunctionService] from the platform. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(36)
public class PlatformAppFunctionService : AppFunctionService() {

    private lateinit var delegate: AppFunctionServiceDelegate

    override fun onCreate() {
        super.onCreate()
        delegate =
            AppFunctionServiceDelegate(
                this@PlatformAppFunctionService,
                Dispatchers.Worker,
                Dispatchers.Main,
                ServiceDependencies.aggregatedAppFunctionInventory,
                ServiceDependencies.aggregatedAppFunctionInvoker,
                Dependencies.translatorSelector,
            )
    }

    override fun onExecuteFunction(
        request: PlatformExecuteAppFunctionRequest,
        callingPackage: String,
        signingInfo: SigningInfo,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<PlatformExecuteAppFunctionResponse, PlatformAppFunctionException>,
    ) {
        val executionJob =
            delegate.onExecuteFunction(
                ExecuteAppFunctionRequest.fromPlatformClass(request),
                object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                    override fun onResult(result: ExecuteAppFunctionResponse) {
                        when (result) {
                            is ExecuteAppFunctionResponse.Success -> {
                                callback.onResult(result.toPlatformClass())
                            }
                            is ExecuteAppFunctionResponse.Error -> {
                                callback.onError(result.error.toPlatformClass())
                            }
                        }
                    }

                    override fun onError(error: AppFunctionException) {
                        callback.onError(error.toPlatformClass())
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
