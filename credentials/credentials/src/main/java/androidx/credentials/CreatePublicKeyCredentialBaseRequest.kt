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

package androidx.credentials

import android.os.Bundle
import androidx.annotation.VisibleForTesting

/**
 * Base request class for registering a public key credential.
 *
 * An application can construct a subtype request and call [CredentialManager.executeCreateCredential] to
 * launch framework UI flows to collect consent and any other metadata needed from the user to
 * register a new user credential.
 *
 * @property requestJson The request in JSON format
 * @throws NullPointerException If [requestJson] is null. This is handled by the Kotlin runtime
 * @throws IllegalArgumentException If [requestJson] is empty
 *
 * @hide
 */
abstract class CreatePublicKeyCredentialBaseRequest constructor(
    val requestJson: String,
    type: String,
    data: Bundle,
    requireSystemProvider: Boolean,
) : CreateCredentialRequest(type, data, requireSystemProvider) {

    init {
        require(requestJson.isNotEmpty()) { "request json must not be empty" }
    }

    /** @hide */
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
        const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
    }
}