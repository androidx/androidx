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
import androidx.annotation.RestrictTo
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential

open class BeginGetCredentialOption internal constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val id: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val candidateQueryData: Bundle
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