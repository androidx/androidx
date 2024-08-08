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
 * Base response class for the credential creation operation made with the
 * [CreateCredentialRequest].
 *
 * @sample androidx.credentials.samples.processCreateCredentialResponse
 * @property type the credential type determined by the credential-type-specific subclass (e.g. the
 *   type for [CreatePasswordResponse] is [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] and for
 *   [CreatePublicKeyCredentialResponse] is [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL])
 * @property data the response data in the [Bundle] format
 */
abstract class CreateCredentialResponse
internal constructor(
    val type: String,
    val data: Bundle,
) {
    internal companion object {
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        fun createFrom(type: String, data: Bundle): CreateCredentialResponse {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        CreatePasswordResponse.createFrom(data)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        CreatePublicKeyCredentialResponse.createFrom(data)
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a response
                // with the raw framework values.
                CreateCustomCredentialResponse(type, data)
            }
        }

        private const val EXTRA_CREATE_CREDENTIAL_RESPONSE_TYPE =
            "androidx.credentials.provider.extra.CREATE_CREDENTIAL_RESPONSE_TYPE"
        private const val EXTRA_CREATE_CREDENTIAL_RESPONSE_DATA =
            "androidx.credentials.provider.extra.CREATE_CREDENTIAL_REQUEST_DATA"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): CreateCredentialResponse? {
            val type = bundle.getString(EXTRA_CREATE_CREDENTIAL_RESPONSE_TYPE) ?: return null
            val data = bundle.getBundle(EXTRA_CREATE_CREDENTIAL_RESPONSE_DATA) ?: return null
            return createFrom(type, data)
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun asBundle(response: CreateCredentialResponse): Bundle =
            Bundle().apply {
                this.putString(EXTRA_CREATE_CREDENTIAL_RESPONSE_TYPE, response.type)
                this.putBundle(EXTRA_CREATE_CREDENTIAL_RESPONSE_DATA, response.data)
            }
    }
}
