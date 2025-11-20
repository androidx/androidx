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

package androidx.credentials.providerevents.transfer

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Success response of exporting the credentials to the provider
 *
 * @property exportResults the result of exporting credentials, keyed by the [CredentialTypes]
 */
public class ExportCredentialsResponse(public val exportResults: Map<String, PerTypeExportResult>) {
    public companion object {
        private const val BUNDLE_NUM_SUCCESS_KEY =
            "androidx.credentials.providerevents.BUNDLE_NUM_SUCCESS_KEY"
        private const val BUNDLE_NUM_FAILURE_KEY =
            "androidx.credentials.providerevents.BUNDLE_NUM_FAILURE_KEY"
        private const val BUNDLE_NUM_IGNORED_KEY =
            "androidx.credentials.providerevents.BUNDLE_NUM_IGNORED_KEY"

        /** Wraps the response class into a bundle */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun toBundle(response: ExportCredentialsResponse): Bundle =
            Bundle().apply {
                for (result in response.exportResults.entries) {
                    val bundle = Bundle()
                    bundle.putInt(BUNDLE_NUM_SUCCESS_KEY, result.value.numSuccess)
                    bundle.putInt(BUNDLE_NUM_FAILURE_KEY, result.value.numFailure)
                    bundle.putInt(BUNDLE_NUM_IGNORED_KEY, result.value.numIgnored)
                    putBundle(result.key, bundle)
                }
            }

        /** Unwraps the response class from a bundle */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun fromBundle(bundle: Bundle): ExportCredentialsResponse {
            val exportResults = mutableMapOf<String, PerTypeExportResult>()
            for (key in bundle.keySet()) {
                val resultBundle = bundle.getBundle(key) ?: continue
                val numSuccess = resultBundle.getInt(BUNDLE_NUM_SUCCESS_KEY)
                val numFailure = resultBundle.getInt(BUNDLE_NUM_FAILURE_KEY)
                val numIgnored = resultBundle.getInt(BUNDLE_NUM_IGNORED_KEY)
                exportResults[key] = PerTypeExportResult(key, numSuccess, numFailure, numIgnored)
            }
            return ExportCredentialsResponse(exportResults)
        }
    }
}
