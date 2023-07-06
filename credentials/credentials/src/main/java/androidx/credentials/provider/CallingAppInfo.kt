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

import android.content.pm.SigningInfo
import androidx.annotation.RestrictTo
import androidx.credentials.provider.utils.RequestValidationUtil

/**
 * Information pertaining to the calling application.
 *
 * @constructor constructs an instance of [CallingAppInfo]
 *
 * @param packageName the calling package name of the calling app
 * @param signingInfo the signingInfo associated with the calling app
 * @param origin the origin of the calling app. This is only set when a
 * privileged app like a browser, calls on behalf of another application.
 *
 * @throws NullPointerException If [packageName] or [signingInfo] is null
 * @throws IllegalArgumentException If [packageName] is empty
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class CallingAppInfo @JvmOverloads constructor(
    val packageName: String,
    val signingInfo: SigningInfo,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val origin: String? = null
) {

    /**
     * Returns the origin of the calling app. This is only non-null if a
     * privileged app like a browser calls Credential Manager APIs on
     * behalf of another application.
     *
     * Additionally, in order to get the origin, the credential provider must
     * provide an allowlist of privileged browsers/apps that it trusts.
     * This allowlist must be in the form of a valid, non-empty JSON. The
     * origin will only be returned if the [packageName] and [signingInfo]
     * match with an app allowlisted in [privilegedAllowlist].
     *
     * @throws IllegalArgumentException If [privilegedAllowlist] is empty, or an
     * invalid JSON
     */
    fun getOrigin(privilegedAllowlist: String): String? {
        if (!RequestValidationUtil.isValidJSON(privilegedAllowlist)) {
            throw IllegalArgumentException("privilegedAllowlist must not be " +
                "empty, and must be a valid JSON")
        }
        return null
    }

    init {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
    }
}
