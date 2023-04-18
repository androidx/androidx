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

/**
 * Allows extending custom versions of BeginGetCredentialOptions for unique use cases.
 *
 * If you get a [BeginGetCustomCredentialOption] instead of a type-safe option class such as
 * [BeginGetPasswordOption], [BeginGetPublicKeyCredentialOption], etc., then you should check if
 * you have any other library at interest that supports this custom [type] of credential option,
 * and if so use its parsing utilities to resolve to a type-safe class within that library.
 *
 * @property type the credential type determined by the credential-type-specific subclass
 * generated for custom use cases
 * @property candidateQueryData the partial request data in the [Bundle] format that will be sent to
 * the provider during the initial candidate query stage, which should not contain sensitive user
 * information
 * @throws IllegalArgumentException If [type] is null or, empty
 */
open class BeginGetCustomCredentialOption constructor(
    id: String,
    type: String,
    candidateQueryData: Bundle,
) : BeginGetCredentialOption(
    id,
    type,
    candidateQueryData
) {
    init {
        require(id.isNotEmpty()) { "id should not be empty" }
        require(type.isNotEmpty()) { "type should not be empty" }
    }

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        /** @hide */
        @JvmStatic
        internal fun createFrom(
            data: Bundle,
            id: String,
            type: String
        ): BeginGetCustomCredentialOption {
            return BeginGetCustomCredentialOption(id, type, data)
        }

        /** @hide */
        @JvmStatic
        internal fun createFromEntrySlice(
            data: Bundle,
            id: String,
            type: String
        ): BeginGetCustomCredentialOption {
            return BeginGetCustomCredentialOption(id, type, data)
        }
    }
}
