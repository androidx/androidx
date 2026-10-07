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

/**
 * A query sent to a [RelyingPartyEventsService] asking for the
 * [WebAuthn passkey registration request](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
 * to use when creating a passkey for the relying party. The relying party answers with a
 * [GetPublicKeyCredentialCreationResponse].
 */
// TODO(b/436712597): Add sessionId once IRelyingPartyEventsService passes a session identifier.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GetPublicKeyCredentialCreationRequest() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is GetPublicKeyCredentialCreationRequest
    }

    override fun hashCode(): Int = 0

    override fun toString(): String = "GetPublicKeyCredentialCreationRequest()"
}
