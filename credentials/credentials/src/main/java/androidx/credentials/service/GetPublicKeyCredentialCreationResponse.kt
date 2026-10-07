/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.service

import androidx.annotation.RestrictTo
import androidx.credentials.CreatePublicKeyCredentialRequest

/**
 * The relying party's answer to
 * [RelyingPartyEventsService.onGetPublicKeyCredentialCreationRequest], carrying the
 * [WebAuthn passkey registration options](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
 * that the credential provider should use.
 *
 * @property createPublicKeyCredentialRequest the [CreatePublicKeyCredentialRequest] holding the
 *   WebAuthn challenge, rpId and user entity
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GetPublicKeyCredentialCreationResponse(
    public val createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetPublicKeyCredentialCreationResponse) return false
        return createPublicKeyCredentialRequest == other.createPublicKeyCredentialRequest
    }

    override fun hashCode(): Int = createPublicKeyCredentialRequest.hashCode()

    override fun toString(): String =
        "GetPublicKeyCredentialCreationResponse(" +
            "createPublicKeyCredentialRequest=$createPublicKeyCredentialRequest)"
}
