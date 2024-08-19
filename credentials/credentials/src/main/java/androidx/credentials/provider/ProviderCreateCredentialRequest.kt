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

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.provider.CallingAppInfo.Companion.EXTRA_CREDENTIAL_REQUEST_ORIGIN
import androidx.credentials.provider.CallingAppInfo.Companion.extractCallingAppInfo
import androidx.credentials.provider.CallingAppInfo.Companion.setCallingAppInfo

/**
 * Final request received by the provider after the user has selected a given [CreateEntry] on the
 * UI.
 *
 * This request contains the actual request coming from the calling app, and the application
 * information associated with the calling app.
 *
 * @constructor constructs an instance of [ProviderCreateCredentialRequest]
 * @property callingRequest the complete [CreateCredentialRequest] coming from the calling app that
 *   is requesting for credential creation
 * @property callingAppInfo information pertaining to the calling app making the request
 * @property biometricPromptResult the result of a Biometric Prompt authentication flow, that is
 *   propagated to the provider if the provider requested for
 *   [androidx.credentials.CredentialManager] to handle the authentication flow
 * @throws NullPointerException If [callingRequest], or [callingAppInfo] is null
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderCreateCredentialRequest
@JvmOverloads
constructor(
    val callingRequest: CreateCredentialRequest,
    val callingAppInfo: CallingAppInfo,
    val biometricPromptResult: BiometricPromptResult? = null
) {
    companion object {
        private const val EXTRA_CREATE_CREDENTIAL_REQUEST_TYPE =
            "androidx.credentials.provider.extra.CREATE_CREDENTIAL_REQUEST_TYPE"
        private const val EXTRA_CREATE_REQUEST_CANDIDATE_QUERY_DATA =
            "androidx.credentials.provider.extra.CREATE_REQUEST_CANDIDATE_QUERY_DATA"

        private const val EXTRA_CREATE_REQUEST_CREDENTIAL_DATA =
            "androidx.credentials.provider.extra.CREATE_REQUEST_CREDENTIAL_DATA"

        /**
         * Helper method to convert the given [request] to a parcelable [Bundle], in case the
         * instance needs to be sent across a process. Consumers of this method should use
         * [fromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        @RequiresApi(23) // Icon dependency
        fun asBundle(request: ProviderCreateCredentialRequest): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_CREATE_CREDENTIAL_REQUEST_TYPE, request.callingRequest.type)
            bundle.putBundle(
                EXTRA_CREATE_REQUEST_CREDENTIAL_DATA,
                request.callingRequest.credentialData
            )
            bundle.putBundle(
                EXTRA_CREATE_REQUEST_CANDIDATE_QUERY_DATA,
                request.callingRequest.candidateQueryData
            )
            bundle.setCallingAppInfo(request.callingAppInfo)
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [ProviderCreateCredentialRequest].
         *
         * Throws [IllegalArgumentException] if the conversion fails. This means that the given
         * [bundle] does not contain a `ProviderCreateCredentialRequest`. The bundle should be
         * constructed and retrieved from [asBundle] itself and never be created from scratch to
         * avoid the failure.
         */
        @RequiresApi(23) // Icon dependency
        @JvmStatic
        fun fromBundle(bundle: Bundle): ProviderCreateCredentialRequest {
            val requestType: String =
                bundle.getString(EXTRA_CREATE_CREDENTIAL_REQUEST_TYPE)
                    ?: throw IllegalArgumentException("Bundle was missing request type.")
            val requestData: Bundle =
                bundle.getBundle(EXTRA_CREATE_REQUEST_CREDENTIAL_DATA) ?: Bundle()
            val candidateQueryData: Bundle =
                bundle.getBundle(EXTRA_CREATE_REQUEST_CANDIDATE_QUERY_DATA) ?: Bundle()
            val origin = bundle.getString(EXTRA_CREDENTIAL_REQUEST_ORIGIN)
            val callingAppInfo =
                extractCallingAppInfo(bundle)
                    ?: throw IllegalArgumentException("Bundle was missing CallingAppInfo.")

            return try {
                ProviderCreateCredentialRequest(
                    callingRequest =
                        CreateCredentialRequest.createFrom(
                            requestType,
                            requestData,
                            candidateQueryData,
                            requireSystemProvider = false,
                            origin
                        ),
                    callingAppInfo = callingAppInfo,
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("Conversion failed with $e")
            }
        }
    }
}
