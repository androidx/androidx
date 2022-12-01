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

package androidx.credentials.provider

import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential

/** A credential count information class that takes in
 * a credential type, and a count of credential stored
 * for that type.
 *
 * Providers add this information to [CreateEntry] and it is
 * displayed with that entry on the selector. This information
 * helps users select this entry over others.
 *
 * @property type the type of the credential
 * @property count the number of credentials stored for this credential type
 *
 * @hide
 */
class CredentialCountInformation constructor(
    val type: String,
    val count: Int
    ) {
    companion object {
        @JvmStatic
        fun createPasswordCountInformation(count: Int): CredentialCountInformation {
            return CredentialCountInformation(PasswordCredential.TYPE_PASSWORD_CREDENTIAL, count)
        }

        @JvmStatic
        fun createPublicKeyCountInformation(count: Int): CredentialCountInformation {
            return CredentialCountInformation(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL, count)
        }
    }
}