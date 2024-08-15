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

import android.os.Bundle
import androidx.credentials.provider.CallingAppInfo.Companion.extractCallingAppInfo
import androidx.credentials.provider.CallingAppInfo.Companion.setCallingAppInfo

/**
 * Request class for clearing a user's credential state from the credential providers.
 *
 * @param callingAppInfo info pertaining to the calling app that's making the request
 * @constructor constructs an instance of [ProviderClearCredentialStateRequest]
 * @throws NullPointerException If [callingAppInfo] is null
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderClearCredentialStateRequest constructor(val callingAppInfo: CallingAppInfo) {
    companion object {
        /**
         * Helper method to convert the given [request] to a parcelable [Bundle], in case the
         * instance needs to be sent across a process. Consumers of this method should use
         * [fromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        fun asBundle(request: ProviderClearCredentialStateRequest): Bundle {
            val bundle = Bundle()
            bundle.setCallingAppInfo(request.callingAppInfo)
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [ProviderClearCredentialStateRequest].
         *
         * Throws [IllegalArgumentException] if the conversion fails. This means that the given
         * [bundle] does not contain a `ProviderCreateCredentialRequest`. The bundle should be
         * constructed and retrieved from [asBundle] itself and never be created from scratch to
         * avoid the failure.
         */
        @JvmStatic
        fun fromBundle(bundle: Bundle): ProviderClearCredentialStateRequest {
            val callingAppInfo =
                extractCallingAppInfo(bundle)
                    ?: throw IllegalArgumentException("Bundle was missing CallingAppInfo.")
            return ProviderClearCredentialStateRequest(callingAppInfo)
        }
    }
}
