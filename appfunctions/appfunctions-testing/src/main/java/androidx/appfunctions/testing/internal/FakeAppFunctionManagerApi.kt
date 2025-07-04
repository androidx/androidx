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

package androidx.appfunctions.testing.internal

import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionManagerApi
import androidx.appfunctions.internal.NullTranslatorSelector
import androidx.appfunctions.internal.findImpl
import androidx.appfunctions.service.AppFunctionServiceDelegate
import androidx.appfunctions.service.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.service.internal.AggregatedAppFunctionInvoker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class FakeAppFunctionManagerApi(
    private val context: Context,
    private val appFunctionReader: FakeAppFunctionReader,
) : AppFunctionManagerApi {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse = suspendCancellableCoroutine { continuation ->
        AppFunctionServiceDelegate(
                context,
                Dispatchers.Default,
                Dispatchers.Default,
                AggregatedAppFunctionInventory::class.java.findImpl(prefix = "$", suffix = "_Impl"),
                AggregatedAppFunctionInvoker::class.java.findImpl(prefix = "$", suffix = "_Impl"),
                NullTranslatorSelector(),
            )
            .onExecuteFunction(
                request,
                object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                    override fun onResult(response: ExecuteAppFunctionResponse?) {
                        if (response != null) {
                            continuation.resume(response)
                        } else {
                            continuation.resumeWithException(
                                AppFunctionAppUnknownException("Failed to execute appfunction.")
                            )
                        }
                    }

                    override fun onError(error: AppFunctionException) {
                        continuation.resumeWithException(error)
                    }
                },
            )
    }

    override suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean =
        appFunctionReader.getAppFunctionMetadata(functionId, packageName)?.isEnabled
            ?: throw AppFunctionFunctionNotFoundException(
                "No function found with id: $functionId under package: $packageName"
            )

    override suspend fun setAppFunctionEnabled(functionId: String, newEnabledState: Int) {
        val appFunctionStaticAndRuntimeMetadata =
            appFunctionReader.getAppFunctionStaticAndRuntimeMetadata(
                context.packageName,
                functionId,
            )
                ?: throw AppFunctionFunctionNotFoundException(
                    "No function found with id: $functionId"
                )

        appFunctionReader.setAppFunctionStaticAndRuntimeMetadata(
            context.packageName,
            appFunctionStaticAndRuntimeMetadata.copy(
                runtimeMetadata =
                    appFunctionStaticAndRuntimeMetadata.runtimeMetadata.copy(
                        enabled = newEnabledState
                    )
            ),
        )
    }
}
