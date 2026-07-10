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
package androidx.credentials.playservices.controllers.identitycredentials.signalcredentialstate

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.publickeycredential.SignalCredentialRateLimitExceededException
import androidx.credentials.exceptions.publickeycredential.SignalCredentialStateException
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateResponse
import java.util.concurrent.Executor

/** A controller to handle the signal credential state flow with play services. */
internal class SignalCredentialStateController(private val context: Context) :
    CredentialProviderController<
        androidx.credentials.SignalCredentialStateRequest,
        SignalCredentialStateRequest,
        SignalCredentialStateResponse,
        androidx.credentials.SignalCredentialStateResponse,
        SignalCredentialStateException,
    >(context) {

    override fun invokePlayServices(
        request: androidx.credentials.SignalCredentialStateRequest,
        callback:
            CredentialManagerCallback<
                androidx.credentials.SignalCredentialStateResponse,
                SignalCredentialStateException,
            >,
        executor: Executor,
        cancellationSignal: CancellationSignal?,
    ) {
        val convertedRequest = this.convertRequestToPlayServices(request)
        IdentityCredentialManager.Companion.getClient(context)
            .signalCredentialState(convertedRequest)
            .addOnSuccessListener {
                if (it == null) {
                    executor.execute {
                        callback.onError(
                            SignalCredentialStateException.createFrom(
                                "No SignalCredentialStateResponse received"
                            )
                        )
                    }
                } else {
                    val response = this.convertResponseToCredentialManager(it)
                    executor.execute { callback.onResult(result = response) }
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener { e ->
                var exception: SignalCredentialStateException =
                    SignalCredentialStateException.createFrom(e.message)
                if (
                    e is ApiException &&
                        e.statusCode == CommonStatusCodes.CANCELED &&
                        e.message?.contains(RATE_LIMIT_EXCEPTION_MESSAGE_MATCHER) == true
                ) {
                    exception =
                        SignalCredentialRateLimitExceededException(
                            parseRefillMinutesRegex(e.message),
                            e.message,
                        )
                }
                executor.execute { callback.onError(exception) }
            }
    }

    public override fun convertRequestToPlayServices(
        request: androidx.credentials.SignalCredentialStateRequest
    ): SignalCredentialStateRequest {
        return SignalCredentialStateRequest(request.type, request.origin, request.requestData)
    }

    override fun convertResponseToCredentialManager(
        response: SignalCredentialStateResponse
    ): androidx.credentials.SignalCredentialStateResponse {
        return androidx.credentials.SignalCredentialStateResponse()
    }

    companion object {
        const val SIGNAL_REQUEST_JSON_KEY = "androidx.credentials.signal_request_json_key"
        const val RATE_LIMIT_EXCEPTION_MESSAGE_MATCHER = "called too frequently"
        const val MAX_RETRY_TIME = 600000L // 10 minutes

        fun parseRefillMinutesRegex(exceptionMessage: String?): Long {
            if (exceptionMessage == null) return MAX_RETRY_TIME
            val regex =
                """^SignalCredentialState has been called too frequently\. Please retry later after (\d+) minutes\.$"""
                    .toRegex()
            val matchResult = regex.find(exceptionMessage)

            return matchResult?.groups?.get(1)?.value?.toIntOrNull()?.toLong() ?: MAX_RETRY_TIME
        }

        @JvmStatic
        fun getInstance(context: Context): SignalCredentialStateController {
            return SignalCredentialStateController(context)
        }
    }
}
