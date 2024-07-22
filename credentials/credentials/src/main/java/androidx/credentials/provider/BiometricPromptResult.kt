/*
 * Copyright 2024 The Android Open Source Project
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

import java.util.Objects

/**
 * The result of a Biometric Prompt authentication flow, that is propagated to the provider if the
 * provider requested for [androidx.credentials.CredentialManager] to handle the authentication
 * flow.
 *
 * An instance of this class will always be part of the final provider request, either the
 * [ProviderGetCredentialRequest] or the [ProviderCreateCredentialRequest] that the provider
 * receives after the user selects a [CredentialEntry] or a [CreateEntry] respectively.
 *
 * @property isSuccessful whether the result is a success result, in which case
 *   [authenticationResult] should be non-null
 * @property authenticationResult the result of the authentication flow, non-null if the
 *   authentication flow was successful
 * @property authenticationError error information, non-null if the authentication flow has
 *   failured, meaning that [isSuccessful] will be false in this case
 */
class BiometricPromptResult
internal constructor(
    val authenticationResult: AuthenticationResult? = null,
    val authenticationError: AuthenticationError? = null
) {
    val isSuccessful: Boolean = authenticationResult != null

    /**
     * An unsuccessful biometric prompt result, denoting that authentication has failed.
     *
     * @param authenticationError the error that caused the biometric prompt authentication flow to
     *   fail
     */
    constructor(
        authenticationError: AuthenticationError
    ) : this(authenticationResult = null, authenticationError = authenticationError)

    /**
     * A successful biometric prompt result, denoting that authentication has succeeded.
     *
     * @param authenticationResult the result after a successful biometric prompt authentication
     *   operation
     */
    constructor(
        authenticationResult: AuthenticationResult
    ) : this(authenticationResult = authenticationResult, authenticationError = null)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is BiometricPromptResult) {
            return this.isSuccessful == other.isSuccessful &&
                this.authenticationResult == other.authenticationResult &&
                this.authenticationError == other.authenticationError
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.isSuccessful, this.authenticationResult, this.authenticationError)
    }
}
