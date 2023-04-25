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
 * Base class for a credential with which the user consented to authenticate to the app.
 *
 * @property type the credential type determined by the credential-type-specific subclass (e.g.
 * [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] for `PasswordCredential` or
 * [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] for `PublicKeyCredential`)
 * @property data the credential data in the [Bundle] format
 */
abstract class Credential internal constructor(
    val type: String,
    val data: Bundle,
) {
    /** @hide */
    companion object {
        /** @hide */
        @JvmStatic
        fun createFrom(type: String, data: Bundle): Credential {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        PasswordCredential.createFrom(data)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        PublicKeyCredential.createFrom(data)
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a response
                // with the raw framework values.
                CustomCredential(type, data)
            }
        }
    }
}
