/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.exceptions.publickeycredential

import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager

/**
 * Represents an error thrown during a signal credential state flow with Credential Manager. See
 * [CredentialManager] for more details on how Exceptions work for Credential Manager flows.
 *
 * @see CredentialManager
 * @see SignalCredentialUnknownException
 * @see SignalCredentialSecurityException
 * @see SignalCredentialStateProviderConfigurationException
 */
abstract class SignalCredentialStateException
@JvmOverloads
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) errorMessage: CharSequence? = null,
) : Exception(errorMessage?.toString()) {
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun createFrom(type: String, msg: String?): SignalCredentialStateException {
            return when (type) {
                SignalCredentialSecurityException.TYPE_SIGNAL_CREDENTIAL_STATE_SECURITY_EXCEPTION ->
                    SignalCredentialSecurityException(msg)
                SignalCredentialUnknownException.TYPE_SIGNAL_CREDENTIAL_STATE_UNKNOWN_EXCEPTION ->
                    SignalCredentialUnknownException(msg)
                SignalCredentialStateProviderConfigurationException
                    .TYPE_SIGNAL_CREDENTIAL_STATE_PROVIDER_CONFIGURATION_EXCEPTION ->
                    SignalCredentialStateProviderConfigurationException(msg)
                else -> SignalCredentialUnknownException(msg)
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun createFrom(msg: String?): SignalCredentialStateException {
            return SignalCredentialUnknownException(msg)
        }
    }
}
