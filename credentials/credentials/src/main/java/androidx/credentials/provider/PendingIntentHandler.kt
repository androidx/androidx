/*
 * Copyright 2022 The Android Open Source Project
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

import android.app.PendingIntent
import android.content.Intent
import android.credentials.CreateCredentialResponse
import android.service.credentials.BeginGetCredentialRequest
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.CredentialProviderService
import android.service.credentials.CredentialsResponseContent
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * PendingIntentHandler to be used by credential providers to extract requests from
 * [PendingIntent] invoked when a given [CreateEntry], or a [CustomCredentialEntry]
 * is selected by the user.
 *
 * This handler can also be used to set [android.credentials.CreateCredentialResponse] and
 * [android.credentials.GetCredentialResponse] on the result of the activity
 * invoked by the [PendingIntent]
 */
@RequiresApi(34)
class PendingIntentHandler {
    companion object {
        private const val TAG = "PendingIntentHandler"

        /**
         * Extract the [ProviderCreateCredentialRequest] from the provider's
         * [PendingIntent] invoked by the Android system.
         */
        @JvmStatic
        fun retrieveCreateCredentialProviderRequest(intent: Intent):
            ProviderCreateCredentialRequest? {
            val frameworkReq: CreateCredentialRequest? =
                intent.getParcelableExtra(
                CredentialProviderService
                    .EXTRA_CREATE_CREDENTIAL_REQUEST, CreateCredentialRequest::class.java
                )
            if (frameworkReq == null) {
                Log.i(TAG, "Request not found in pendingIntent")
                return frameworkReq
            }
            return ProviderCreateCredentialRequest(
                androidx.credentials.CreateCredentialRequest
                    .createFrom(
                        frameworkReq.type,
                        frameworkReq.data,
                        frameworkReq.data,
                        requireSystemProvider = false) ?: return null,
                frameworkReq.callingAppInfo)
        }

        /**
         * Extract [BeginGetCredentialRequest] from the provider's
         * [PendingIntent] invoked by the Android system when the user
         * selects an [AuthenticationAction].
         */
        @JvmStatic
        fun getBeginGetCredentialRequest(intent: Intent): BeginGetCredentialRequest? {
            val request = intent.getParcelableExtra(
                "android.service.credentials.extra.BEGIN_GET_CREDENTIAL_REQUEST",
                BeginGetCredentialRequest::class.java
            )
            return request?.let { BeginGetCredentialUtil.convertToStructuredRequest(it) }
        }

        /**
         * Set the [CreateCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on
         * [CreateEntry]
         */
        @JvmStatic
        fun setCreateCredentialResponse(
            intent: Intent,
            response: androidx.credentials.CreateCredentialResponse
        ) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREATE_CREDENTIAL_RESPONSE,
                CreateCredentialResponse(response.data))
        }

        /**
         * Extract the [ProviderGetCredentialRequest] from the provider's
         * [PendingIntent] invoked by the Android system.
         */
        @JvmStatic
        fun retrieveGetCredentialProviderRequest(intent: Intent):
            ProviderGetCredentialRequest? {
            val frameworkReq = intent.getParcelableExtra(
                CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST,
                android.service.credentials.GetCredentialRequest::class.java
            )
            if (frameworkReq == null) {
                Log.i(TAG, "Get request from framework is null")
                return null
            }
            return ProviderGetCredentialRequest.createFrom(frameworkReq)
        }

        /**
         * Set the [android.credentials.GetCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on [CreateEntry]
         */
        @JvmStatic
        fun setGetCredentialResponse(
            intent: Intent,
            response: GetCredentialResponse
        ) {
            intent.putExtra(
                CredentialProviderService.EXTRA_GET_CREDENTIAL_RESPONSE,
                android.credentials.GetCredentialResponse(
                    android.credentials.Credential(response.credential.type,
                        response.credential.data))
            )
        }

        /**
         * Set the [android.service.credentials.CredentialsResponseContent] on the result of the
         * activity invoked by the [PendingIntent] set on [AuthenticationAction].
         */
        @JvmStatic
        fun setCredentialsResponseContent(
            intent: Intent,
            response: CredentialsResponseContent
        ) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREDENTIALS_RESPONSE_CONTENT,
                response
            )
        }
    }
}
