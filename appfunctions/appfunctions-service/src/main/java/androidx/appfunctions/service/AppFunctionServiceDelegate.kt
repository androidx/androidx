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

import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.internal.Translator
import androidx.appfunctions.internal.TranslatorSelector
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.service.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.service.internal.AggregatedAppFunctionInvoker
import androidx.appfunctions.service.internal.unsafeBuildReturnValue
import androidx.appfunctions.service.internal.unsafeGetParameterValue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionServiceDelegate(
    context: Context,
    workerCoroutineContext: CoroutineContext,
    private val mainCoroutineContext: CoroutineContext,
    private val aggregatedInventory: AggregatedAppFunctionInventory,
    private val aggregatedInvoker: AggregatedAppFunctionInvoker,
    private val translatorSelector: TranslatorSelector,
) {

    private val job = Job()
    private val workerCoroutineScope = CoroutineScope(workerCoroutineContext + job)
    private val appContext = context.applicationContext

    public fun onExecuteFunction(
        executeAppFunctionRequest: ExecuteAppFunctionRequest,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>,
    ): Job =
        workerCoroutineScope.launch {
            try {
                val appFunctionMetadata =
                    aggregatedInventory.functionIdToMetadataMap[
                            executeAppFunctionRequest.functionIdentifier]
                if (appFunctionMetadata == null) {
                    Log.d(
                        APP_FUNCTIONS_TAG,
                        "${executeAppFunctionRequest.functionIdentifier} is not available",
                    )
                    callback.onError(
                        AppFunctionFunctionNotFoundException(
                            "${executeAppFunctionRequest.functionIdentifier} is not available"
                        )
                    )
                    return@launch
                }
                val translator =
                    getTranslator(executeAppFunctionRequest, appFunctionMetadata.schema)

                val parameters =
                    extractParameters(executeAppFunctionRequest, appFunctionMetadata, translator)
                callback.onResult(
                    unsafeInvokeFunction(
                        executeAppFunctionRequest,
                        appFunctionMetadata,
                        parameters,
                        translator,
                    )
                )
            } catch (e: CancellationException) {
                Log.d(
                    APP_FUNCTIONS_TAG,
                    "Invocation of ${executeAppFunctionRequest.functionIdentifier} was cancelled",
                    e,
                )
                callback.onError(AppFunctionCancelledException(e.message))
            } catch (e: AppFunctionException) {
                Log.d(
                    APP_FUNCTIONS_TAG,
                    "Failed to invoke ${executeAppFunctionRequest.functionIdentifier}",
                    e,
                )
                callback.onError(e)
            } catch (e: Exception) {
                Log.d(
                    APP_FUNCTIONS_TAG,
                    "Failed to invoke ${executeAppFunctionRequest.functionIdentifier}",
                    e,
                )
                callback.onError(AppFunctionAppUnknownException(e.message))
            }
        }

    internal fun onDestroy() {
        job.cancel()
    }

    private fun getTranslator(
        request: ExecuteAppFunctionRequest,
        schemaMetadata: AppFunctionSchemaMetadata?,
    ): Translator? {
        if (request.useJetpackSchema) {
            return null
        }
        return schemaMetadata?.let { translatorSelector.getTranslator(it) }
    }

    private fun extractParameters(
        request: ExecuteAppFunctionRequest,
        appFunctionMetadata: CompileTimeAppFunctionMetadata,
        translator: Translator?,
    ): Map<String, Any?> {
        // Upgrade the parameters from the agents, if they are using the old format.
        val translatedParameters =
            translator?.upgradeRequest(request.functionParameters) ?: request.functionParameters

        return buildMap {
            for (parameterMetadata in appFunctionMetadata.parameters) {
                this[parameterMetadata.name] =
                    translatedParameters.unsafeGetParameterValue(parameterMetadata)
            }
        }
    }

    private suspend fun unsafeInvokeFunction(
        request: ExecuteAppFunctionRequest,
        appFunctionMetadata: CompileTimeAppFunctionMetadata,
        parameters: Map<String, Any?>,
        translator: Translator?,
    ): ExecuteAppFunctionResponse {
        val result =
            withContext(mainCoroutineContext) {
                aggregatedInvoker.unsafeInvoke(
                    buildAppFunctionContext(),
                    request.functionIdentifier,
                    parameters,
                )
            }
        val returnValue = appFunctionMetadata.response.unsafeBuildReturnValue(result)
        // Downgrade the return value from the agents, if they are using the old format.
        val translatedReturnValue = translator?.downgradeResponse(returnValue) ?: returnValue
        return ExecuteAppFunctionResponse.Success(translatedReturnValue)
    }

    private fun buildAppFunctionContext(): AppFunctionContext {
        return object : AppFunctionContext {
            override val context: Context
                get() = appContext
        }
    }
}
