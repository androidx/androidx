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

import android.content.ComponentName
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
 * @param type the credential type determined by the credential-type-specific subclass
 * generated for custom use cases
 * @param requestData the request data in the [Bundle] format, generated for custom use cases
 * (note: bundle keys in the form of `androidx.credentials.*` and `android.credentials.*` are
 * reserved for internal library usage)
 * @param candidateQueryData the partial request data in the [Bundle] format that will be sent to
 * the provider during the initial candidate query stage, which should not contain sensitive user
 * information (note: bundle keys in the form of `androidx.credentials.*` and
 * `android.credentials.*` are reserved for internal library usage)
 * @param isSystemProviderRequired true if must only be fulfilled by a system provider and false
 * otherwise
 * @param isAutoSelectAllowed defines if a credential entry will be automatically chosen if it is
 * the only one available option, false by default
 * @param allowedProviders a set of provider service [ComponentName] allowed to receive this
 * option (Note: a [SecurityException] will be thrown if it is set as non-empty but your app does
 * not have android.permission.CREDENTIAL_MANAGER_SET_ALLOWED_PROVIDERS; for API level < 34,
 * this property will not take effect and you should control the allowed provider via
 * [library dependencies](https://developer.android.com/training/sign-in/passkeys#add-dependencies))
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [requestData] or [type] is null
 */
open class GetCustomCredentialOption @JvmOverloads constructor(
    type: String,
    requestData: Bundle,
    candidateQueryData: Bundle,
    isSystemProviderRequired: Boolean,
    isAutoSelectAllowed: Boolean = false,
    allowedProviders: Set<ComponentName> = emptySet(),
) : CredentialOption(
    type = type,
    requestData = requestData,
    candidateQueryData = candidateQueryData,
    isSystemProviderRequired = isSystemProviderRequired,
    isAutoSelectAllowed = isAutoSelectAllowed,
    allowedProviders = allowedProviders,
) {

    init {
        require(type.isNotEmpty()) { "type should not be empty" }
    }
}