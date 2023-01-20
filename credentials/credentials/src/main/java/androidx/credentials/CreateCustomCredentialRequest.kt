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
 * Base custom create request class for registering a credential.
 *
 * An application can construct a subtype custom request and call
 * [CredentialManager.createCredential] to launch framework UI flows to collect consent and
 * any other metadata needed from the user to register a new user credential.
 *
 * If you get a [CreateCustomCredentialRequest] instead of a type-safe request class such as
 * [CreatePasswordRequest], [CreatePublicKeyCredentialRequest], etc., then you should check if you
 * have any other library at interest that supports this custom [type] of credential request,
 * and if so use its parsing utilities to resolve to a type-safe class within that library.
 *
 * Note: The Bundle keys for [credentialData] and [candidateQueryData] should not be in the form
 * of androidx.credentials.*` as they are reserved for internal use by this androidx library.
 *
 * @property type the credential type determined by the credential-type-specific subclass for
 * custom use cases
 * @property credentialData the data of this [CreateCustomCredentialRequest] in the [Bundle]
 * format (note: bundle keys in the form of `androidx.credentials.*` are reserved for internal
 * library use)
 * @property candidateQueryData the partial request data in the [Bundle] format that will be sent
 * to the provider during the initial candidate query stage, which should not contain sensitive
 * user credential information (note: bundle keys in the form of `androidx.credentials.*` are
 * reserved for internal library use)
 * @property isSystemProviderRequired true if must only be fulfilled by a system provider and
 * false otherwise
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [type], [credentialData], or [candidateQueryData] is null
 */
open class CreateCustomCredentialRequest(
    final override val type: String,
    final override val credentialData: Bundle,
    final override val candidateQueryData: Bundle,
    final override val isSystemProviderRequired: Boolean,
    displayInfo: DisplayInfo,
) : CreateCredentialRequest(
    type,
    credentialData,
    candidateQueryData,
    isSystemProviderRequired,
    displayInfo
) {
    init {
        require(type.isNotEmpty()) { "type should not be empty" }
    }
}