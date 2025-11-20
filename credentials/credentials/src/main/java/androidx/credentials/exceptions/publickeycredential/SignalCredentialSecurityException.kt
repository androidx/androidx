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

/**
 * This is thrown when the signal credential state operation failed because it was not allowed or a
 * permission was missing
 *
 * @see androidx.credentials.CredentialManager
 */
class SignalCredentialSecurityException
@JvmOverloads
constructor(errorMessage: CharSequence? = null) :
    SignalCredentialStateException(TYPE_SIGNAL_CREDENTIAL_STATE_SECURITY_EXCEPTION, errorMessage) {
    internal companion object {
        internal const val TYPE_SIGNAL_CREDENTIAL_STATE_SECURITY_EXCEPTION =
            "androidx.credentials.SignalCredentialStateException.TYPE_SECURITY"
    }
}
