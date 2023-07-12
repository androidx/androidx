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

package androidx.credentials.exceptions.publickeycredential

import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A subclass of CreateCredentialException for unique exceptions thrown specific only to
 * PublicKeyCredentials. See [CredentialManager] for more details on how Credentials work for
 * Credential Manager flows.
 *
 * @throws NullPointerException if [type] is null
 * @throws IllegalArgumentException if [type] is empty
 */
open class GetPublicKeyCredentialException @JvmOverloads internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val type: String,
    errorMessage: CharSequence? = null
) : GetCredentialException(type, errorMessage) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
    }

    internal companion object {
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        fun createFrom(type: String, msg: String?): GetCredentialException {
            return try {
               with(type) {
                   when {
                       startsWith(GetPublicKeyCredentialDomException
                           .TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION) ->
                           GetPublicKeyCredentialDomException.createFrom(type, msg)
                       else -> { throw FrameworkClassParsingException() }
                   }
               }
            } catch (t: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a response
                // with the raw framework values.
                GetCredentialCustomException(type, msg)
            }
        }
    }
}
