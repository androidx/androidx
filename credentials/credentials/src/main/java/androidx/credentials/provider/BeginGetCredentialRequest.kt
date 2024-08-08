/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo.Companion.extractCallingAppInfo
import androidx.credentials.provider.CallingAppInfo.Companion.setCallingAppInfo
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * Query stage request for getting user's credentials from a given credential provider.
 *
 * This request contains a list of [BeginGetCredentialOption] that have parameters to be used to
 * query credentials, and return a [BeginGetCredentialResponse], containing a list [CredentialEntry]
 * that are presented to the user on an selector.
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 *
 * @param beginGetCredentialOptions the list of type specific credential options to to be processed
 *   in order to produce a [BeginGetCredentialResponse]
 * @param callingAppInfo info pertaining to the app requesting credentials
 * @constructor constructs an instance of [BeginGetCredentialRequest]
 * @throws NullPointerException If [beginGetCredentialOptions] is null
 */
class BeginGetCredentialRequest
@JvmOverloads
constructor(
    val beginGetCredentialOptions: List<BeginGetCredentialOption>,
    val callingAppInfo: CallingAppInfo? = null,
) {
    @RequiresApi(34)
    private object Api34Impl {
        private const val REQUEST_KEY = "androidx.credentials.provider.BeginGetCredentialRequest"

        @JvmStatic
        fun asBundle(bundle: Bundle, request: BeginGetCredentialRequest) {
            bundle.putParcelable(
                REQUEST_KEY,
                BeginGetCredentialUtil.convertToFrameworkRequest(request)
            )
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialRequest? {
            val frameworkRequest =
                bundle.getParcelable(
                    REQUEST_KEY,
                    android.service.credentials.BeginGetCredentialRequest::class.java
                )
            if (frameworkRequest != null) {
                return BeginGetCredentialUtil.convertToJetpackRequest(frameworkRequest)
            }
            return null
        }
    }

    @RequiresApi(23)
    private object Api23Impl {
        private const val EXTRA_BEGIN_GET_CREDENTIAL_OPTION_SIZE =
            "androidx.credentials.provider.extra.EXTRA_BEGIN_GET_CREDENTIAL_OPTION_SIZE"
        private const val EXTRA_BEGIN_GET_CREDENTIAL_OPTION_ID_PREFIX =
            "androidx.credentials.provider.extra.EXTRA_BEGIN_GET_CREDENTIAL_OPTION_ID_"
        private const val EXTRA_BEGIN_GET_CREDENTIAL_OPTION_TYPE_PREFIX =
            "androidx.credentials.provider.extra.EXTRA_BEGIN_GET_CREDENTIAL_OPTION_TYPE_"
        private const val EXTRA_BEGIN_GET_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX =
            "androidx.credentials.provider.extra.EXTRA_BEGIN_GET_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_"

        @JvmStatic
        fun asBundle(bundle: Bundle, request: BeginGetCredentialRequest) {
            val optionSize = request.beginGetCredentialOptions.size
            bundle.putInt(EXTRA_BEGIN_GET_CREDENTIAL_OPTION_SIZE, optionSize)
            for (i in 0 until optionSize) {
                bundle.putString(
                    "$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_ID_PREFIX$i",
                    request.beginGetCredentialOptions[i].id
                )
                bundle.putString(
                    "$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_TYPE_PREFIX$i",
                    request.beginGetCredentialOptions[i].type
                )
                bundle.putBundle(
                    "$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX$i",
                    request.beginGetCredentialOptions[i].candidateQueryData
                )
                request.callingAppInfo?.let { bundle.setCallingAppInfo(it) }
            }
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialRequest? {
            val callingAppInfo = extractCallingAppInfo(bundle)

            val optionSize = bundle.getInt(EXTRA_BEGIN_GET_CREDENTIAL_OPTION_SIZE, -1)
            if (optionSize < 0) {
                return null
            }

            val options = mutableListOf<BeginGetCredentialOption>()
            for (i in 0 until optionSize) {
                val id =
                    bundle.getString("$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_ID_PREFIX$i")
                        ?: return null
                val type =
                    bundle.getString("$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_TYPE_PREFIX$i")
                        ?: return null
                val candidateQueryData =
                    bundle.getBundle(
                        "$EXTRA_BEGIN_GET_CREDENTIAL_OPTION_CANDIDATE_QUERY_DATA_PREFIX$i"
                    ) ?: return null
                options.add(BeginGetCredentialOption.createFrom(id, type, candidateQueryData))
            }
            return BeginGetCredentialRequest(options, callingAppInfo)
        }
    }

    companion object {
        /**
         * Helper method to convert the class to a parcelable [Bundle], in case the class instance
         * needs to be sent across a process. Consumers of this method should use [fromBundle] to
         * reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        fun asBundle(request: BeginGetCredentialRequest): Bundle {
            val bundle = Bundle()
            if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.asBundle(bundle, request)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.asBundle(bundle, request)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [BeginGetCredentialRequest].
         */
        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginGetCredentialRequest? {
            return if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.fromBundle(bundle)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.fromBundle(bundle)
            } else {
                null
            }
        }
    }
}
