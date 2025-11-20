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

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DISABLED
import androidx.appfunctions.AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_ENABLED
import androidx.appfunctions.AppFunctionSystemUnknownException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import com.android.extensions.appfunctions.AppFunctionManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine

/** Provides the AppFunctionManager backend through the sidecar extension. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class ExtensionAppFunctionManagerApi(private val context: Context) :
    AppFunctionManagerApi {

    private val appFunctionManager: AppFunctionManager by lazy { AppFunctionManager(context) }

    override suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
        return suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }
            appFunctionManager.executeAppFunction(
                request.toPlatformExtensionClass(),
                Runnable::run,
                cancellationSignal,
                object :
                    OutcomeReceiver<
                        com.android.extensions.appfunctions.ExecuteAppFunctionResponse,
                        com.android.extensions.appfunctions.AppFunctionException,
                    > {
                    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                    override fun onResult(
                        result: com.android.extensions.appfunctions.ExecuteAppFunctionResponse
                    ) {
                        cont.resume(
                            ExecuteAppFunctionResponse.Success.fromPlatformExtensionClass(result)
                        )
                    }

                    override fun onError(
                        error: com.android.extensions.appfunctions.AppFunctionException
                    ) {
                        val exception =
                            fixAppFunctionExceptionErrorType(
                                AppFunctionException.fromPlatformExtensionsClass(error)
                            )
                        cont.resume(ExecuteAppFunctionResponse.Error(exception))
                    }
                },
            )
        }
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
        val platformExtensionEnabledState = convertToPlatformExtensionEnabledState(newEnabledState)
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

    private fun fixAppFunctionExceptionErrorType(
        exception: AppFunctionException
    ): AppFunctionException {
        // Platform throws IllegalArgumentException when function not found during function
        // execution and ends up being AppFunctionSystemUnknownException instead of
        // AppFunctionFunctionNotFoundException.
        // The bug was already shipped in some Android version, so we have to keep this fix.
        if (
            exception is AppFunctionSystemUnknownException &&
                exception.errorMessage == "IllegalArgumentException: App function not found."
        ) {
            return AppFunctionFunctionNotFoundException("App function not found.")
        }
        return exception
    }

    @AppFunctionManager.EnabledState
    private fun convertToPlatformExtensionEnabledState(
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
