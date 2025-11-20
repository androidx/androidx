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
import androidx.credentials.providerevents.internal.RequestValidationHelper

/**
 * a request to import the provider's credentials
 *
 * @property requestJson the request according to the
 *   [Fido Credential Exchange Protocol format](https://fidoalliance.org/specs/cx/cxp-v1.0-wd-20240522.html)
 * @throws IllegalArgumentException If [requestJson] is empty, or if it is not a valid JSON
 */
public class ImportCredentialsRequest(public val requestJson: String) {
    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }

    public companion object {
        private const val REQUEST_JSON_KEY = "androidx.credentials.import.REQUEST_JSON"

        /**
         * Creates a [ImportCredentialsRequest] from a bundle.
         *
         * @throws IllegalArgumentException if the bundle does not contain the request parameters.
         */
        @JvmStatic
        public fun createFrom(bundle: Bundle): ImportCredentialsRequest {
            val requestJson = bundle.getString(REQUEST_JSON_KEY)
            if (requestJson == null) {
                throw IllegalArgumentException("The bundle does not contain requestJson")
            }
            return ImportCredentialsRequest(requestJson)
        }

        /** Wraps the request into a bundle. */
        @JvmStatic
        public fun toBundle(request: ImportCredentialsRequest): Bundle =
            Bundle().apply { putString(REQUEST_JSON_KEY, request.requestJson) }
    }
}
