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
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base response class for the credential creation operation made with the
 * [CreateCredentialRequest].
 *
 * @property type the credential type determined by the credential-type-specific subclass (e.g.
 * the type for [CreatePasswordResponse] is [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] and for
 * [CreatePublicKeyCredentialResponse] is [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL])
 * @property data the response data in the [Bundle] format
 */
abstract class CreateCredentialResponse internal constructor(
    val type: String,
    val data: Bundle,
) {
    /** @hide */
    companion object {
        /** @hide */
        @JvmStatic
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
    }
}
