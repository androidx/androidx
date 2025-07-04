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

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DISABLED
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_ENABLED
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine

/** Provides the AppFunctionManager backend through the platform API. */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class PlatformAppFunctionManagerApi(private val context: Context) : AppFunctionManagerApi {

    private val appFunctionManager: AppFunctionManager by lazy {
        context.getSystemService(AppFunctionManager::class.java)
    }

    override suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            appFunctionManager.isAppFunctionEnabled(
                functionId,
                packageName,
                Runnable::run,
                object : OutcomeReceiver<Boolean, Exception> {
                    override fun onResult(result: Boolean?) {
                        if (result == null) {
                            cont.resumeWithException(IllegalStateException("Something went wrong"))
                        } else {
                            cont.resume(result)
                        }
                    }

                    override fun onError(error: Exception) {
                        cont.resumeWithException(error)
                    }
                },
            )
        }
    }

    override suspend fun setAppFunctionEnabled(
        functionId: String,
        @AppFunctionManagerCompat.EnabledState newEnabledState: Int,
    ) {
        val platformExtensionEnabledState = convertToPlatformEnabledState(newEnabledState)
        return suspendCancellableCoroutine { cont ->
            appFunctionManager.setAppFunctionEnabled(
                functionId,
                platformExtensionEnabledState,
                Runnable::run,
                object : OutcomeReceiver<Void, Exception> {
                    override fun onResult(result: Void?) {
                        cont.resume(Unit)
                    }

                    override fun onError(error: Exception) {
                        cont.resumeWithException(error)
                    }
                },
            )
        }
    }

    override suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
        return suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }
            appFunctionManager.executeAppFunction(
                request.toPlatformClass(),
                Runnable::run,
                cancellationSignal,
                object :
                    OutcomeReceiver<
                        android.app.appfunctions.ExecuteAppFunctionResponse,
                        android.app.appfunctions.AppFunctionException,
                    > {
                    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
                    override fun onResult(
                        result: android.app.appfunctions.ExecuteAppFunctionResponse
                    ) {
                        cont.resume(ExecuteAppFunctionResponse.Success.fromPlatformClass(result))
                    }

                    override fun onError(error: android.app.appfunctions.AppFunctionException) {
                        cont.resume(
                            ExecuteAppFunctionResponse.Error(
                                AppFunctionException.fromPlatformClass(error)
                            )
                        )
                    }
                },
            )
        }
    }

    private fun convertToPlatformEnabledState(
        @AppFunctionManagerCompat.EnabledState enabledState: Int
    ): Int {
        return when (enabledState) {
            APP_FUNCTION_STATE_DEFAULT -> AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
            APP_FUNCTION_STATE_ENABLED -> AppFunctionManager.APP_FUNCTION_STATE_ENABLED
            APP_FUNCTION_STATE_DISABLED -> AppFunctionManager.APP_FUNCTION_STATE_DISABLED
            else -> throw IllegalArgumentException("Unknown enabled state $enabledState")
        }
    }
}
