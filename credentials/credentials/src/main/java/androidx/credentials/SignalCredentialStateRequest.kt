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

package androidx.credentials

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Base request class for sending credential state signals to providers.
 *
 * An application can construct a subtype request and call [CredentialManager.signalCredentialState]
 * to propagate a signal credential state request.
 *
 * @property type the request type representing one of [SignalAllAcceptedCredentialIdsRequest],
 *   [SignalCurrentUserDetailsRequest] and [SignalUnknownCredentialRequest])
 * @property requestJson the signal request data in the expected json format
 * @property requestData the bundle to contain the request json and any additional info
 * @property origin the origin of a different application if the request is being made on behalf of
 *   that application (Note: for API level >=34, setting a non-null value for this parameter will
 *   throw a SecurityException if android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
 */
abstract class SignalCredentialStateRequest
internal constructor(
    val type: String,
    val requestJson: String,
    val requestData: Bundle,
    val origin: String? = null,
) {
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val SIGNAL_REQUEST_JSON_KEY = "androidx.credentials.signal_request_json_key"

        private const val SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE"

        private const val SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE"

        private const val SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE =
            "androidx.credentials.SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun createFrom(
            requestType: String,
            requestData: Bundle,
            origin: String?,
        ): SignalCredentialStateRequest {
            val requestJson: String =
                requestData.getString(SIGNAL_REQUEST_JSON_KEY)
                    ?: throw IllegalArgumentException("Bundle was missing requestJson")
            return createFrom(requestType, requestJson, origin)
        }

        /**
         * Creates a SignalCredentialStateRequest from the given parameters.
         *
         * @param requestType the request type representing one of
         *   [SignalAllAcceptedCredentialIdsRequest], [SignalCurrentUserDetailsRequest] and
         *   [SignalUnknownCredentialRequest])
         * @param requestJson the request json corresponding to the request data
         * @param origin the origin of a different application if the request is being made on
         *   behalf of that application (Note: for API level >=34, setting a non-null value for this
         *   parameter will throw a SecurityException if
         *   android.permission.CREDENTIAL_MANAGER_SET_ORIGIN is not present)
         *     @throws IllegalArgumentException if request json validation fails
         *     @throws SignalCredentialSecurityException if origin is set without having
         *       android.permission.CREDENTIAl_MANAGER_SET_ORIGIN
         */
        @JvmStatic
        fun createFrom(
            requestType: String,
            requestJson: String,
            origin: String?,
        ): SignalCredentialStateRequest {
            return when (requestType) {
                SIGNAL_UNKNOWN_CREDENTIAL_STATE_REQUEST_TYPE ->
                    SignalUnknownCredentialRequest(requestJson, origin)
                SIGNAL_CURRENT_USER_DETAILS_STATE_REQUEST_TYPE ->
                    SignalCurrentUserDetailsRequest(requestJson, origin)
                SIGNAL_ALL_ACCEPTED_CREDENTIALS_REQUEST_TYPE ->
                    SignalAllAcceptedCredentialIdsRequest(requestJson, origin)
                else -> throw IllegalArgumentException("Request type is not supported")
            }
        }
    }
}
