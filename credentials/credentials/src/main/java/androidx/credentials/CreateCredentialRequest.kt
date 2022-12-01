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
import androidx.annotation.RestrictTo
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.Companion.BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIVILEGED
import androidx.credentials.PublicKeyCredential.Companion.BUNDLE_KEY_SUBTYPE
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base request class for registering a credential.
 *
 * An application can construct a subtype request and call [CredentialManager.executeCreateCredential] to
 * launch framework UI flows to collect consent and any other metadata needed from the user to
 * register a new user credential.
 */
abstract class CreateCredentialRequest internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val data: Bundle,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val requireSystemProvider: Boolean
) {
    /** @hide */
    companion object {
        /** @hide */
        @JvmStatic
        fun createFrom(
            type: String,
            data: Bundle,
            requireSystemProvider: Boolean
        ): CreateCredentialRequest {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        CreatePasswordRequest.createFrom(data)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        when (data.getString(BUNDLE_KEY_SUBTYPE)) {
                            CreatePublicKeyCredentialRequest
                                .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST ->
                                CreatePublicKeyCredentialRequest.createFrom(data)
                            BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST_PRIVILEGED ->
                                CreatePublicKeyCredentialRequestPrivileged.createFrom(data)
                            else -> throw FrameworkClassParsingException()
                        }
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a request with
                // the raw framework values.
                CreateCustomCredentialRequest(type, data, requireSystemProvider)
            }
        }
    }
}
