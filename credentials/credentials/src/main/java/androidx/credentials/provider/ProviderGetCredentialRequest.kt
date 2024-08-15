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
import android.content.ComponentName
import android.os.Bundle
import androidx.credentials.CredentialOption
import androidx.credentials.provider.CallingAppInfo.Companion.extractCallingAppInfo
import androidx.credentials.provider.CallingAppInfo.Companion.setCallingAppInfo

/**
 * Request received by the provider after the query phase of the get flow is complete i.e. the user
 * was presented with a list of credentials, and the user has now made a selection from the list of
 * [CredentialEntry] presented on the selector UI.
 *
 * This request will be added to the intent extras of the activity invoked by the [PendingIntent]
 * set on the [CredentialEntry] that the user selected. The request must be extracted using the
 * [PendingIntentHandler.retrieveProviderGetCredentialRequest] helper API.
 *
 * @constructor constructs an instance of [ProviderGetCredentialRequest]
 * @property credentialOptions the list of credential retrieval options containing the required
 *   parameters, expected to contain a single [CredentialOption] when this request is retrieved from
 *   the [android.app.Activity] invoked by the [android.app.PendingIntent] set on a
 *   [PasswordCredentialEntry] or a [PublicKeyCredentialEntry], or expected to contain multiple
 *   [CredentialOption] when this request is retrieved from the [android.app.Activity] invoked by
 *   the [android.app.PendingIntent] set on a [RemoteEntry]
 * @property callingAppInfo information pertaining to the calling application
 * @property biometricPromptResult the result of a Biometric Prompt authentication flow, that is
 *   propagated to the provider if the provider requested for
 *   [androidx.credentials.CredentialManager] to handle the authentication flow
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderGetCredentialRequest
@JvmOverloads
constructor(
    val credentialOptions: List<CredentialOption>,
    val callingAppInfo: CallingAppInfo,
    val biometricPromptResult: BiometricPromptResult? = null,
) {
    companion object {
        @JvmStatic
        internal fun createFrom(
            options: List<CredentialOption>,
            callingAppInfo: CallingAppInfo,
            biometricPromptResult: BiometricPromptResult? = null
        ): ProviderGetCredentialRequest {
            return ProviderGetCredentialRequest(options, callingAppInfo, biometricPromptResult)
        }

        private const val EXTRA_CREDENTIAL_OPTION_SIZE =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_SIZE"
        private const val EXTRA_CREDENTIAL_OPTION_TYPE_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_TYPE_"
        private const val EXTRA_CREDENTIAL_OPTION_CREDENTIAL_RETRIEVAL_DATA_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_CREDENTIAL_RETRIEVAL_DATA_"
        private const val EXTRA_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_"
        private const val EXTRA_CREDENTIAL_OPTION_IS_SYSTEM_PROVIDER_REQUIRED_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_IS_SYSTEM_PROVIDER_REQUIRED_"
        private const val EXTRA_CREDENTIAL_OPTION_ALLOWED_PROVIDERS_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_OPTION_ALLOWED_PROVIDERS_"

        /**
         * Helper method to convert the given [request] to a parcelable [Bundle], in case the
         * instance needs to be sent across a process. Consumers of this method should use
         * [fromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        fun asBundle(request: ProviderGetCredentialRequest): Bundle {
            val bundle = Bundle()
            val optionSize = request.credentialOptions.size
            bundle.putInt(EXTRA_CREDENTIAL_OPTION_SIZE, optionSize)
            for (i in 0 until optionSize) {
                val option = request.credentialOptions[i]
                bundle.putString("$EXTRA_CREDENTIAL_OPTION_TYPE_PREFIX$i", option.type)
                bundle.putBundle(
                    "$EXTRA_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX$i",
                    option.candidateQueryData
                )
                bundle.putBundle(
                    "$EXTRA_CREDENTIAL_OPTION_CREDENTIAL_RETRIEVAL_DATA_PREFIX$i",
                    option.requestData
                )
                bundle.putBoolean(
                    "$EXTRA_CREDENTIAL_OPTION_IS_SYSTEM_PROVIDER_REQUIRED_PREFIX$i",
                    option.isSystemProviderRequired
                )
                bundle.putParcelableArray(
                    "$EXTRA_CREDENTIAL_OPTION_ALLOWED_PROVIDERS_PREFIX$i",
                    option.allowedProviders.toTypedArray()
                )
            }
            bundle.setCallingAppInfo(request.callingAppInfo)
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [ProviderGetCredentialRequest].
         *
         * Throws [IllegalArgumentException] if the conversion fails. This means that the given
         * [bundle] does not contain a `ProviderGetCredentialRequest`. The bundle should be
         * constructed and retrieved from [asBundle] itself and never be created from scratch to
         * avoid the failure.
         */
        @JvmStatic
        fun fromBundle(bundle: Bundle): ProviderGetCredentialRequest {
            val callingAppInfo =
                extractCallingAppInfo(bundle)
                    ?: throw IllegalArgumentException("Bundle was missing CallingAppInfo.")
            val optionSize = bundle.getInt(EXTRA_CREDENTIAL_OPTION_SIZE, -1)
            if (optionSize < 0) {
                throw IllegalArgumentException("Bundle had invalid option size as $optionSize.")
            }
            val options = mutableListOf<CredentialOption>()
            for (i in 0 until optionSize) {
                val type =
                    bundle.getString("$EXTRA_CREDENTIAL_OPTION_TYPE_PREFIX$i")
                        ?: throw IllegalArgumentException(
                            "Bundle was missing option type at index $optionSize."
                        )
                val candidateQueryData =
                    bundle.getBundle("$EXTRA_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX$i")
                        ?: throw IllegalArgumentException(
                            "Bundle was missing candidate query data at index $optionSize."
                        )
                val requestData =
                    bundle.getBundle("$EXTRA_CREDENTIAL_OPTION_CREDENTIAL_RETRIEVAL_DATA_PREFIX$i")
                        ?: throw IllegalArgumentException(
                            "Bundle was missing request data at index $optionSize."
                        )
                val isSystemProviderRequired =
                    bundle.getBoolean(
                        "$EXTRA_CREDENTIAL_OPTION_IS_SYSTEM_PROVIDER_REQUIRED_PREFIX$i",
                        false
                    )
                val allowedProviders =
                    try {
                        @Suppress("DEPRECATION")
                        bundle
                            .getParcelableArray(
                                "${EXTRA_CREDENTIAL_OPTION_ALLOWED_PROVIDERS_PREFIX}$i"
                            )
                            ?.mapNotNull { it as ComponentName? }
                            ?.toSet() ?: emptySet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                options.add(
                    CredentialOption.createFrom(
                        type,
                        requestData,
                        candidateQueryData,
                        isSystemProviderRequired,
                        allowedProviders
                    )
                )
            }

            return createFrom(options, callingAppInfo)
        }
    }
}
