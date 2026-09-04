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

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AggregatedAppFunctionInvoker
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.internal.unsafeBuildReturnValue
import androidx.appfunctions.internal.unsafeGetParameterValue
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionServiceDelegate(
    context: Context,
    private val mainCoroutineContext: CoroutineContext,
    private val aggregatedInvoker: AggregatedAppFunctionInvoker,
) {
    private val appContext = context.applicationContext

    public suspend fun executeFunction(
        executeAppFunctionRequest: ExecuteAppFunctionRequest,
        metadata: AppFunctionMetadata,
    ): ExecuteAppFunctionResponse =
        try {
            val parameters = extractParameters(executeAppFunctionRequest, metadata)
            unsafeInvokeFunction(
                executeAppFunctionRequest,
                metadata,
                metadata.components,
                parameters,
            )
        } catch (e: CancellationException) {
            Log.d(
                APP_FUNCTIONS_TAG,
                "Invocation of ${executeAppFunctionRequest.functionIdentifier} was cancelled",
                e,
            )
            throw AppFunctionCancelledException(e.message)
        } catch (e: AppFunctionException) {
            Log.d(
                APP_FUNCTIONS_TAG,
                "Failed to invoke ${executeAppFunctionRequest.functionIdentifier}",
                e,
            )
            throw e
        } catch (e: Exception) {
            Log.d(
                APP_FUNCTIONS_TAG,
                "Failed to invoke ${executeAppFunctionRequest.functionIdentifier}",
                e,
            )
            throw AppFunctionAppUnknownException(e.message)
        }

    private fun extractParameters(
        request: ExecuteAppFunctionRequest,
        appFunctionMetadata: AppFunctionMetadata,
    ): Map<String, Any?> {
        return buildMap {
            for (parameterMetadata in appFunctionMetadata.parameters) {
                this[parameterMetadata.name] =
                    request.functionParameters.unsafeGetParameterValue(parameterMetadata)
            }
        }
    }

    private suspend fun unsafeInvokeFunction(
        request: ExecuteAppFunctionRequest,
        appFunctionMetadata: AppFunctionMetadata,
        componentsMetadata: AppFunctionComponentsMetadata,
        parameters: Map<String, Any?>,
    ): ExecuteAppFunctionResponse {
        val result =
            withContext(mainCoroutineContext) {
                aggregatedInvoker.unsafeInvoke(
                    buildAppFunctionContext(),
                    request.functionIdentifier,
                    parameters,
                )
            }
        val returnValue =
            appFunctionMetadata.response.unsafeBuildReturnValue(result, componentsMetadata)
        return ExecuteAppFunctionResponse.Success(returnValue)
    }

    private fun buildAppFunctionContext(): AppFunctionContext {
        return object : AppFunctionContext {
            override val context: Context
                get() = appContext
        }
    }
}
