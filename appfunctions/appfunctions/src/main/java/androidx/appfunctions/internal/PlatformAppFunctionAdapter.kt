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

package androidx.appfunctions.internal

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.CallbackAppFunction
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.toCompatExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/** Extension functions to adapt platform AppFunction types. */
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
internal fun CallbackAppFunction.toPlatformAppFunction(
    appFunctionReader: AppFunctionReader,
    executor: Executor,
): android.app.appfunctions.AppFunction {
    return object : android.app.appfunctions.AppFunction {
        override fun onExecuteAppFunction(
            request: android.app.appfunctions.ExecuteAppFunctionRequest,
            cancellationSignal: CancellationSignal,
            callback:
                OutcomeReceiver<
                    android.app.appfunctions.ExecuteAppFunctionResponse,
                    android.app.appfunctions.AppFunctionException,
                >,
        ) {
            val delegateSignal = CancellationSignal()
            // We use GlobalScope here because CallbackAppFunction is a callback-based API with no
            // parent coroutine context or lifecycle scope to bind to. This is safe because the
            // metadata fetch is a short-lived operation.
            // Consider using a separate executor in case there are performance issues with querying
            // metadata on the provided executor.
            val executionJob =
                GlobalScope.launch(executor.asCoroutineDispatcher()) {
                    try {
                        val functionMetadata =
                            appFunctionReader.getAppFunctionMetadata(
                                request.functionIdentifier,
                                request.targetPackageName,
                            )
                                ?: throw AppFunctionFunctionNotFoundException(
                                    "Function ${request.functionIdentifier} not found in " +
                                        "package ${request.targetPackageName}"
                                )
                        val compatRequest =
                            request.toCompatExecuteAppFunctionRequest(functionMetadata)
                        execute(compatRequest, delegateSignal) { response ->
                            when (response) {
                                is ExecuteAppFunctionResponse.Success -> {
                                    callback.onResult(
                                        response.toPlatformExecuteAppFunctionResponse()
                                    )
                                }
                                is ExecuteAppFunctionResponse.Error -> {
                                    callback.onError(response.error.toPlatformClass())
                                }
                            }
                        }
                    } catch (throwable: Throwable) {
                        val compatException =
                            when (throwable) {
                                is CancellationException ->
                                    AppFunctionCancelledException(throwable.message)
                                is AppFunctionException -> throwable
                                else -> AppFunctionAppUnknownException(throwable.message)
                            }
                        callback.onError(compatException.toPlatformClass())
                    }
                }

            cancellationSignal.setOnCancelListener {
                delegateSignal.cancel()
                executionJob.cancel()
            }
        }
    }
}
