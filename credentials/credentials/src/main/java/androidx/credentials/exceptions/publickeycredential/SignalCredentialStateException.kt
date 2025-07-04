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
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class SignalCredentialStateException
internal constructor(val type: String, val errorMessage: String? = null) : Exception(errorMessage) {
    companion object {
        const val ERROR_TYPE_UNKNOWN = "error_type_unknown"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun createFrom(type: String, msg: String?): SignalCredentialStateException {
            return SignalCredentialStateException(type, msg)
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun createFrom(msg: String?): SignalCredentialStateException {
            return SignalCredentialStateException(ERROR_TYPE_UNKNOWN, msg)
        }
    }
}
