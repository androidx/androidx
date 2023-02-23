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

/**
 * Allows extending custom versions of GetCredentialOptions for unique use cases.
 *
 * If you get a [GetCustomCredentialOption] instead of a type-safe option class such as
 * [GetPasswordOption], [GetPublicKeyCredentialOption], etc., then you should check if
 * you have any other library at interest that supports this custom [type] of credential option,
 * and if so use its parsing utilities to resolve to a type-safe class within that library.
 *
 * Note: The Bundle keys for [requestData] and [candidateQueryData] should not be in the form of
 * `androidx.credentials.*` as they are reserved for internal use by this androidx library.
 *
 * @property type the credential type determined by the credential-type-specific subclass
 * generated for custom use cases
 * @property requestData the request data in the [Bundle] format, generated for custom use cases
 * @property candidateQueryData the partial request data in the [Bundle] format that will be sent to
 * the provider during the initial candidate query stage, which should not contain sensitive user
 * information
 * @property isSystemProviderRequired true if must only be fulfilled by a system provider and false
 * otherwise
 * @property isAutoSelectAllowed defines if a credential entry will be automatically chosen if it is
 * the only one available option, false by default
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [requestData] or [type] is null
 */
open class GetCustomCredentialOption @JvmOverloads constructor(
    final override val type: String,
    final override val requestData: Bundle,
    final override val candidateQueryData: Bundle,
    final override val isSystemProviderRequired: Boolean,
    final override val isAutoSelectAllowed: Boolean = false,
) : CredentialOption(
    type = type,
    requestData = requestData,
    candidateQueryData = candidateQueryData,
    isSystemProviderRequired = isSystemProviderRequired,
    isAutoSelectAllowed = isAutoSelectAllowed
) {

    init {
        if (!requestData.containsKey(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED)) {
            requestData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, isAutoSelectAllowed)
        }
        require(type.isNotEmpty()) { "type should not be empty" }
    }
}