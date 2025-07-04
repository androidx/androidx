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

package androidx.credentials.provider

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.SignalAllAcceptedCredentialIdsRequest
import androidx.credentials.SignalCredentialStateRequest
import androidx.credentials.SignalUnknownCredentialRequest
import androidx.credentials.provider.CallingAppInfo.Companion.EXTRA_CREDENTIAL_REQUEST_ORIGIN
import androidx.credentials.provider.CallingAppInfo.Companion.extractCallingAppInfo
import androidx.credentials.provider.CallingAppInfo.Companion.setCallingAppInfo

/**
 * Signal credential state request received by the provider
 *
 * This request contains the actual request coming from the calling app, and the application
 * information associated with the calling app.
 *
 * @property callingRequest the complete [SignalCredentialStateRequest] coming from the calling app
 *   that is requesting for credential creation
 * @property callingAppInfo information pertaining to the calling app making the request
 * @throws NullPointerException If [callingRequest], or [callingAppInfo] is null
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ProviderSignalCredentialStateRequest
constructor(val callingRequest: SignalCredentialStateRequest, val callingAppInfo: CallingAppInfo) {
    companion object {
        private const val EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_TYPE =
            "androidx.credentials.provider.extra.SIGNAL_CREDENTIAL_STATE_REQUEST_TYPE"

        private const val EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_JSON =
            "androidx.credentials.signal_request_json_key"

        private const val SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE"

        private const val SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE"

        private const val SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE"

        /**
         * Helper method to convert the given [request] to a parcelable [Bundle], in case the
         * instance needs to be sent across a process. Consumers of this method should use
         * [fromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        @RequiresApi(28) // Passkey support
        fun asBundle(request: ProviderSignalCredentialStateRequest): Bundle {
            val bundle = Bundle()
            bundle.putString(
                EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_TYPE,
                request.callingRequest.type,
            )
            bundle.putString(EXTRA_CREDENTIAL_REQUEST_ORIGIN, request.callingRequest.origin)
            bundle.putString(
                EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_JSON,
                request.callingRequest.requestJson,
            )
            bundle.setCallingAppInfo(request.callingAppInfo)
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [ProviderSignalCredentialStateRequest].
         *
         * Throws [IllegalArgumentException] if the conversion fails. This means that the given
         * [bundle] does not contain a `ProviderSignalCredentialStateRequest`. The bundle should be
         * constructed and retrieved from [asBundle] itself and never be created from scratch to
         * avoid the failure.
         */
        @RequiresApi(28) // Passkey support
        @JvmStatic
        fun fromBundle(bundle: Bundle): ProviderSignalCredentialStateRequest {
            val requestType: String =
                bundle.getString(EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_TYPE)
                    ?: throw IllegalArgumentException("Bundle was missing request type.")
            val requestJson: String =
                bundle.getString(EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_JSON) ?: ""
            val origin = bundle.getString(EXTRA_CREDENTIAL_REQUEST_ORIGIN)
            val callingAppInfo =
                extractCallingAppInfo(bundle)
                    ?: throw IllegalArgumentException("Bundle was missing CallingAppInfo.")
            val callingRequest: SignalCredentialStateRequest =
                when (requestType) {
                    SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE ->
                        SignalUnknownCredentialRequest(requestJson, origin)
                    SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE ->
                        SignalUnknownCredentialRequest(requestJson, origin)
                    SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE ->
                        SignalAllAcceptedCredentialIdsRequest(requestJson, origin)
                    else -> throw IllegalArgumentException("Request type is not supported")
                }

            return try {
                ProviderSignalCredentialStateRequest(
                    callingRequest,
                    callingAppInfo = callingAppInfo,
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("Conversion failed with $e")
            }
        }

        @RequiresApi(28) // Passkey support
        @JvmStatic
        fun createFrom(
            requestType: String,
            requestData: Bundle,
            origin: String?,
        ): ProviderSignalCredentialStateRequest {
            val callingAppInfo =
                extractCallingAppInfo(requestData)
                    ?: throw IllegalArgumentException("Bundle was missing CallingAppInfo.")
            val requestJson: String =
                requestData.getString(EXTRA_SIGNAL_CREDENTIAL_STATE_REQUEST_JSON)
                    ?: throw IllegalArgumentException("Bundle was missing requestJson")
            val signalRequest =
                when (requestType) {
                    SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE ->
                        SignalUnknownCredentialRequest(requestJson, origin)

                    SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE ->
                        SignalUnknownCredentialRequest(requestJson, origin)

                    SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE ->
                        SignalAllAcceptedCredentialIdsRequest(requestJson, origin)

                    else -> throw IllegalArgumentException("Request type is not supported")
                }

            return try {
                ProviderSignalCredentialStateRequest(signalRequest, callingAppInfo)
            } catch (e: Exception) {
                throw IllegalArgumentException("Request type is not supported")
            }
        }
    }
}
