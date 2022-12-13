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
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.CredentialProviderService
import android.util.ArraySet
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialResponse

/**
 * PendingIntentHandler to be used by credential providers to extract requests from
 * [PendingIntent] invoked when a given [CreateEntry], or a [CredentialEntry]
 * is selected by the user.
 *
 * This handler can also be used to set [android.credentials.CreateCredentialResponse] and
 * [android.credentials.GetCredentialResponse] on the result of the activity
 * invoked by the [PendingIntent]
 *
 * @hide
 */
@RequiresApi(34)
class PendingIntentHandler {
    companion object {
        private const val TAG = "PendingIntentHandler"

        /**
         * Extract the [CreateCredentialProviderRequest] from the provider's
         * [PendingIntent] invoked by the Android system.
         *
         * @hide
         */
        @JvmStatic
        fun getCreateCredentialRequest(intent: Intent): CreateCredentialProviderRequest? {
            val frameworkReq: CreateCredentialRequest? =
                intent.getParcelableExtra(
                CredentialProviderService
                    .EXTRA_CREATE_CREDENTIAL_REQUEST, CreateCredentialRequest::class.java
                )
            if (frameworkReq == null) {
                Log.i(TAG, "Request not found in pendingIntent")
                return frameworkReq
            }
            return CreateCredentialProviderRequest(
                androidx.credentials.CreateCredentialRequest
                    .createFrom(
                        frameworkReq.type,
                        frameworkReq.data,
                        frameworkReq.data,
                        requireSystemProvider = false),
                CallingAppInfo(
                    frameworkReq.callingPackage,
                    ArraySet()
                ))
        }

        /**
         * Set the [CreateCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on
         * [CreateEntry]
         *
         * @hide
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
         * Extract the [GetCredentialProviderRequest] from the provider's
         * [PendingIntent] invoked by the Android system.
         *
         * @hide
         */
        @JvmStatic
        fun getGetCredentialsRequest(intent: Intent):
            GetCredentialProviderRequest? {
            val frameworkReq = intent.getParcelableExtra(
                CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST,
                android.service.credentials.GetCredentialRequest::class.java
            )
            if (frameworkReq == null) {
                Log.i(TAG, "Get request from framework is null")
                return null
            }
            return GetCredentialProviderRequest.createFrom(frameworkReq)
        }

        /**
         * Set the [android.credentials.GetCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on [CreateEntry]
         *
         * @hide
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
    }
}
