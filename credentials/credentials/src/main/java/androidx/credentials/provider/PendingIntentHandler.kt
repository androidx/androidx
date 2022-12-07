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

import android.content.Intent
import android.credentials.CreateCredentialResponse
import android.credentials.Credential
import android.service.credentials.CredentialProviderService
import android.util.Log
import android.app.PendingIntent
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.GetCredentialsRequest
import androidx.annotation.RequiresApi

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
                ApplicationInfo(
                    frameworkReq.callingPackage,
                    arrayOf()
                ))
        }

        /**
         * Set the [CreateCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on
         * [CreateEntry]
         *
         * @hide
         */
        fun setCreateCredentialResponse(
            intent: Intent,
            response: androidx.credentials.CreateCredentialResponse
        ) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREATE_CREDENTIAL_RESULT,
                CreateCredentialResponse(response.data))
        }

        /**
         * Extract the [GetCredentialProviderRequest] from the provider's
         * [PendingIntent] invoked by the Android system.
         *
         * @hide
         */
        fun getGetCredentialsRequest(intent: Intent):
            GetCredentialProviderRequest? {
            val frameworkReq = intent.getParcelableExtra(
                "EXTRA_GET_CREDENTIAL_REQUEST",
                GetCredentialsRequest::class.java
            )
            if (frameworkReq == null) {
                Log.i(TAG, "Get request from framework is null")
            }
            // TODO("Implement")
            return null
        }

        /**
         * Set the [android.credentials.GetCredentialResponse] on the result of the
         * activity invoked by the [PendingIntent] set on
         * [CreateEntry]
         *
         * @hide
         */
        // TODO ("Update to GetCredentialResponse when latest framework SDK is dropped")
        fun setGetCredentialResponse(
            intent: Intent,
            response: androidx.credentials.Credential
        ) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREDENTIAL_RESULT,
                Credential(response.type, response.data)
            )
        }
    }
}
