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
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base class for getting a specific type of credentials.
 *
 * [GetCredentialRequest] will be composed of a list of [GetCredentialOption] subclasses to indicate
 * the specific credential types and configurations that your app accepts.
 */
abstract class GetCredentialOption internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val data: Bundle,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val requireSystemProvider: Boolean,
) {
    /** @hide */
    companion object {
        /** @hide */
        @JvmStatic
        fun createFrom(
            type: String,
            data: Bundle,
            requireSystemProvider: Boolean
        ): GetCredentialOption {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        GetPasswordOption.createFrom(data)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        when (data.getString(PublicKeyCredential.BUNDLE_KEY_SUBTYPE)) {
                            GetPublicKeyCredentialOption
                                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION ->
                                GetPublicKeyCredentialOption.createFrom(data)
                            GetPublicKeyCredentialOptionPrivileged
                                .BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION_PRIVILEGED ->
                                GetPublicKeyCredentialOptionPrivileged.createFrom(data)
                            else -> throw FrameworkClassParsingException()
                        }
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a request with
                // the raw framework values.
                GetCustomCredentialOption(type, data, requireSystemProvider)
            }
        }
    }
}
