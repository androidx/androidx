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
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import java.util.Objects

/**
 * The outcome of a batch passkey creation attempt for a relying party, delivered to
 * [RelyingPartyEventsService.onPublicKeyCredentialCreationResult] with either the created
 * [credential] or the [exception] describing why creation failed.
 *
 * Exactly one of [credential] or [exception] is non-null.
 *
 * @property credential the created credential, carrying the WebAuthn registration response JSON, or
 *   `null` if creation failed
 * @property exception the exception describing why passkey creation failed, or `null` if creation
 *   succeeded
 * @property isSuccess `true` if passkey creation succeeded ([credential] is non-null), or `false`
 *   if it failed ([exception] is non-null)
 */
// TODO(b/436712597): Add sessionId once IRelyingPartyEventsService passes a session identifier.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PublicKeyCredentialCreationResult
private constructor(
    public val credential: CreatePublicKeyCredentialResponse?,
    public val exception: CreateCredentialException?,
) {
    /** Whether passkey creation succeeded ([credential] is non-null). */
    public val isSuccess: Boolean
        get() = credential != null

    /**
     * Constructs a [PublicKeyCredentialCreationResult] for a successfully created passkey.
     *
     * @param credential the created credential, carrying the WebAuthn registration response JSON
     */
    public constructor(
        credential: CreatePublicKeyCredentialResponse
    ) : this(credential = credential, exception = null)

    /**
     * Constructs a [PublicKeyCredentialCreationResult] for a passkey creation that failed.
     *
     * @param exception the exception describing why passkey creation failed
     */
    public constructor(
        exception: CreateCredentialException
    ) : this(credential = null, exception = exception)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PublicKeyCredentialCreationResult) return false
        return credential == other.credential && exception == other.exception
    }

    override fun hashCode(): Int = Objects.hash(credential, exception)

    override fun toString(): String =
        "PublicKeyCredentialCreationResult(credential=$credential, exception=$exception)"
}
