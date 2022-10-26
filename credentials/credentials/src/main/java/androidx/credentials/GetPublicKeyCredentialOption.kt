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
 * A request to get passkeys from the user's public key credential provider.
 *
 * @property requestJson the request in JSON format
 * @property allowHybrid defines whether hybrid credentials are allowed to fulfill this request,
 * true by default
 * @throws NullPointerException If [requestJson] or [allowHybrid] is null. It is handled by the
 * Kotlin runtime
 * @throws IllegalArgumentException If [requestJson] is empty
 *
 * @hide
 */
class GetPublicKeyCredentialOption @JvmOverloads constructor(
    requestJson: String,
    @get:JvmName("allowHybrid")
    val allowHybrid: Boolean = true,
) : GetPublicKeyCredentialBaseOption(
    requestJson,
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    toBundle(requestJson, allowHybrid),
    false
) {
    /** @hide */
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val BUNDLE_KEY_ALLOW_HYBRID = "androidx.credentials.BUNDLE_KEY_ALLOW_HYBRID"

        @JvmStatic
        internal fun toBundle(requestJson: String, allowHybrid: Boolean): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putBoolean(BUNDLE_KEY_ALLOW_HYBRID, allowHybrid)
            return bundle
        }
    }
}