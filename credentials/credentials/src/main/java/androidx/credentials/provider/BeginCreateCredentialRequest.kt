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
import android.service.credentials.CallingAppInfo
import androidx.annotation.DoNotInline
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.credentials.provider.utils.BeginCreateCredentialUtil

/**
 * Abstract request class for beginning a create credential request.
 *
 * This class is to be extended by structured create credential requests
 * such as [BeginCreatePasswordCredentialRequest].
 */
abstract class BeginCreateCredentialRequest constructor(
    val type: String,
    val candidateQueryData: Bundle,
    val callingAppInfo: CallingAppInfo?
) {
    @RequiresApi(34)
    private object Api34Impl {
        private const val REQUEST_KEY = "androidx.credentials.provider.BeginCreateCredentialRequest"

        @JvmStatic
        @DoNotInline
        fun writeToBundle(bundle: Bundle, request: BeginCreateCredentialRequest) {
            bundle.putParcelable(
                REQUEST_KEY,
                BeginCreateCredentialUtil.convertToFrameworkRequest(request)
            )
        }

        @JvmStatic
        @DoNotInline
        fun readFromBundle(bundle: Bundle): BeginCreateCredentialRequest? {
            val frameworkRequest = bundle.getParcelable(
                REQUEST_KEY,
                android.service.credentials.BeginCreateCredentialRequest::class.java
            )
            if (frameworkRequest != null) {
                return BeginCreateCredentialUtil.convertToJetpackRequest(frameworkRequest)
            }
            return null
        }
    }

    companion object {
        /**
         * Helper method to convert the class to a parcelable [Bundle], in case the class
         * instance needs to be sent across a process. Consumers of this method should use
         * [readFromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        @OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        fun writeToBundle(request: BeginCreateCredentialRequest): Bundle {
            val bundle = Bundle()
            if (BuildCompat.isAtLeastU()) {
                Api34Impl.writeToBundle(bundle, request)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [writeToBundle], back
         * to an instance of [BeginCreateCredentialRequest].
         */
        @JvmStatic
        @OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        fun readFromBundle(bundle: Bundle): BeginCreateCredentialRequest? {
            if (BuildCompat.isAtLeastU()) {
                Api34Impl.readFromBundle(bundle)
            }
            return null
        }
    }
}