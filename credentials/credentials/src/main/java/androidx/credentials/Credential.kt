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
import androidx.annotation.Discouraged
import androidx.annotation.RequiresApi
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Base class for a credential with which the user consented to authenticate to the app.
 *
 * @sample androidx.credentials.samples.processCredential
 * @property type the credential type determined by the credential-type-specific subclass (e.g.
 *   [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] for `PasswordCredential` or
 *   [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] for `PublicKeyCredential`)
 * @property data the credential data in the [Bundle] format
 */
abstract class Credential
internal constructor(
    val type: String,
    val data: Bundle,
) {
    companion object {
        /**
         * Parses the raw data into an instance of [Credential].
         *
         * @param type matches [Credential.type], the credential type
         * @param data matches [Credential.data], the credential data in the [Bundle] format; this
         *   should be constructed and retrieved from the a given [Credential] itself and never be
         *   created from scratch
         */
        @JvmStatic
        @Discouraged(
            "It is recommended to construct a Credential by directly instantiating a Credential subclass"
        )
        fun createFrom(type: String, data: Bundle): Credential {
            return try {
                when (type) {
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                        PasswordCredential.createFrom(data)
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL ->
                        PublicKeyCredential.createFrom(data)
                    RestoreCredential.TYPE_RESTORE_CREDENTIAL -> RestoreCredential.createFrom(data)
                    DigitalCredential.TYPE_DIGITAL_CREDENTIAL -> DigitalCredential.createFrom(data)
                    else -> throw FrameworkClassParsingException()
                }
            } catch (e: FrameworkClassParsingException) {
                // Parsing failed but don't crash the process. Instead just output a response
                // with the raw framework values.
                CustomCredential(type, data)
            }
        }

        /**
         * Parses the [credential] into an instance of [Credential].
         *
         * @param credential the framework Credential object
         */
        @JvmStatic
        @RequiresApi(34)
        @Discouraged(
            "It is recommended to construct a Credential by directly instantiating a Credential subclass"
        )
        fun createFrom(credential: android.credentials.Credential): Credential {
            return createFrom(credential.type, credential.data)
        }
    }
}
