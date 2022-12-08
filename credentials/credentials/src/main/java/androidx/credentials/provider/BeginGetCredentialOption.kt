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

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.credentials.PasswordCredential
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.GetPublicKeyCredentialOptionPrivileged
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base class for getting a specific type of credentials, to be used in the query phase of a
 * get flow.
 *
 * [BeginGetCredentialsProviderRequest] will be composed of a list of [BeginGetCredentialOption]
 * subclasses to indicate the specific credential types and configurations that your app accepts.
 *
 * @hide
 */
abstract class BeginGetCredentialOption internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
) {
    /** @hide */
    companion object {
        /** @hide */
        @JvmStatic
        fun createFrom(
            type: String,
            candidateQueryData: Bundle
        ): BeginGetCredentialOption {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        BeginGetPasswordOption.createFrom(candidateQueryData)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        when (candidateQueryData.getString(
                            PublicKeyCredential.BUNDLE_KEY_SUBTYPE)) {
                            GetPublicKeyCredentialOption
                                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION ->
                                BeginGetPublicKeyCredentialOption.createFrom(candidateQueryData)
                            GetPublicKeyCredentialOptionPrivileged
                                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION_PRIVILEGED
                            -> BeginGetPublicKeyCredentialOptionPrivileged.createFrom(
                                    candidateQueryData)
                            else -> throw FrameworkClassParsingException()
                        }
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // BeginGetCustomCredentialOption gets the requestData as is
                BeginGetCustomCredentialOption(type, candidateQueryData)
            }
        }
    }
}
