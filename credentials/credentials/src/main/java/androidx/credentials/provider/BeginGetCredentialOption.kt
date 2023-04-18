/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Bundle
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential

/**
 * Base class that a credential provider receives during the query phase of a get-credential flow.
 * Classes derived from this base class contain
 * parameters required to retrieve a specific type of credential. E.g. [BeginGetPasswordOption]
 * contains parameters required to retrieve passwords.
 *
 * [BeginGetCredentialRequest] will be composed of a list of [BeginGetCredentialOption]
 * subclasses to indicate the specific credential types and configurations that the credential
 * provider must include while building the [BeginGetCredentialResponse].
 *
 * @property id unique id representing this particular option. Credential providers must
 * use this Id while constructing the [CredentialEntry] to be set on [BeginGetCredentialResponse]
 * @property type the type of the credential to be retrieved against this option. E.g. a
 * [BeginGetPasswordOption] will have type [PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
 * @property candidateQueryData the parameters needed to retrieve the credentials, in the form of a
 * [Bundle]. Note that this is a 'Begin' request denoting a query phase. In this phase, only
 * sensitive information is included in the [candidateQueryData] bundle.
 */
abstract class BeginGetCredentialOption internal constructor(
    val id: String,
    val type: String,
    val candidateQueryData: Bundle
) {
    /** @hide **/
    companion object {
        @JvmStatic
        internal fun createFrom(id: String, type: String, candidateQueryData: Bundle):
            BeginGetCredentialOption {
            return when (type) {
                PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> {
                    BeginGetPasswordOption.createFrom(candidateQueryData, id)
                }

                PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> {
                    BeginGetPublicKeyCredentialOption.createFrom(candidateQueryData, id)
                }

                else -> {
                    BeginGetCustomCredentialOption(id, type, candidateQueryData)
                }
            }
        }
    }
}