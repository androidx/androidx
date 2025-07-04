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

package androidx.credentials.playservices.controllers

import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.CONTROLLER_REQUEST_CODE
import androidx.credentials.playservices.controllers.CredentialProviderController.Companion.cancelOrCallbackExceptionOrResult
import androidx.credentials.playservices.controllers.CredentialProviderController.Companion.maybeReportErrorResultCodeGet
import androidx.credentials.provider.PendingIntentHandler
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.M)
internal class ResponseUtils {
    companion object {
        private const val TAG = "GetCredentialController"

        @JvmStatic
        fun handleGetCredentialResponse(
            uniqueRequestCode: Int,
            resultCode: Int,
            data: Intent?,
            executor: Executor,
            callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
            cancellationSignal: CancellationSignal?,
        ) {
            if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
                Log.w(
                    TAG,
                    "Returned request code $CONTROLLER_REQUEST_CODE which " +
                        " does not match what was given $uniqueRequestCode",
                )
                return
            }

            if (
                maybeReportErrorResultCodeGet(
                    resultCode,
                    { s, f -> cancelOrCallbackExceptionOrResult(s, f) },
                    { e -> executor.execute { callback.onError(e) } },
                    cancellationSignal,
                )
            ) {
                return
            }

            if (data == null) {
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute {
                        callback.onError(
                            GetCredentialUnknownException("No provider data returned.")
                        )
                    }
                }
            } else {
                val response = PendingIntentHandler.retrieveGetCredentialResponse(data)
                if (response != null) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute { callback.onResult(response) }
                    }
                } else {
                    val providerException =
                        PendingIntentHandler.retrieveGetCredentialException(data)
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute {
                            callback.onError(
                                providerException
                                    ?: GetCredentialUnknownException("No provider data returned")
                            )
                        }
                    }
                }
            }
        }
    }
}
