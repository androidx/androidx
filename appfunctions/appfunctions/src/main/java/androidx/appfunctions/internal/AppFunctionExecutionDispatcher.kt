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

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import java.util.function.Consumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Helper class for generated AppFunction services to execute an AppFunction. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public object AppFunctionExecutionDispatcher {
    /**
     * Dispatches the execution of an AppFunction.
     *
     * @param coroutineScope The [CoroutineScope] used to launch the execution coroutine.
     * @param request The [ExecuteAppFunctionRequest] to execute.
     * @param inventory The [AppFunctionInventory] to look up
     *   [androidx.appfunctions.metadata.AppFunctionMetadata] for [request].
     * @param cancellationSignal The [CancellationSignal] used to cancel the execution.
     * @param callback The [Consumer] to receive the [ExecuteAppFunctionResponse].
     * @param block The block of code to execute. The block will be invoked with a map of parameter
     *   names to their extracted values.
     */
    public fun dispatchExecuteAppFunction(
        coroutineScope: CoroutineScope,
        request: ExecuteAppFunctionRequest,
        inventory: AppFunctionInventory,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>,
        block: suspend (Map<String, Any?>) -> Any?,
    ) {
        val job =
            coroutineScope.launch {
                val response =
                    try {
                        executeAppFunction(inventory, request, block)
                    } catch (e: AppFunctionException) {
                        ExecuteAppFunctionResponse.Error(e)
                    }
                // We don't check isActive here since AppFunction implementation is expected
                // to return ERROR_CANCELLED when the operation is caneled.
                callback.accept(response)
            }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    /**
     * Executes an AppFunction with the given request.
     *
     * @param inventory The inventory to look up
     *   [androidx.appfunctions.metadata.AppFunctionMetadata] for [request].
     * @param request The request to execute.
     * @param block The block of code to execute. The block will be invoked with a map of parameter
     *   names to their extracted values.
     * @return The response of the execution.
     * @throws AppFunctionFunctionNotFoundException if the function is not available in the
     *   inventory.
     * @throws AppFunctionCancelledException if the execution is cancelled.
     * @throws AppFunctionException if an explicit AppFunctionException is thrown during execution.
     * @throws AppFunctionAppUnknownException if any other exception is thrown during execution.
     */
    private suspend fun executeAppFunction(
        inventory: AppFunctionInventory,
        request: ExecuteAppFunctionRequest,
        block: suspend (Map<String, Any?>) -> Any?,
    ): ExecuteAppFunctionResponse {
        try {
            val appFunctionMetadata = inventory.functionIdToMetadataMap[request.functionIdentifier]
            if (appFunctionMetadata == null) {
                throw AppFunctionFunctionNotFoundException(
                    "${request.functionIdentifier} is not available"
                )
            }
            val parameters = buildMap {
                for (parameterMetadata in appFunctionMetadata.parameters) {
                    this[parameterMetadata.name] =
                        request.functionParameters.unsafeGetParameterValue(parameterMetadata)
                }
            }
            val result = block(parameters)
            val returnValue =
                appFunctionMetadata.response.unsafeBuildReturnValue(
                    result,
                    inventory.componentsMetadata,
                )
            return ExecuteAppFunctionResponse.Success(returnValue)
        } catch (e: CancellationException) {
            throw AppFunctionCancelledException(e.message)
        } catch (e: AppFunctionException) {
            throw e
        } catch (e: Exception) {
            throw AppFunctionAppUnknownException(e.message)
        }
    }
}
