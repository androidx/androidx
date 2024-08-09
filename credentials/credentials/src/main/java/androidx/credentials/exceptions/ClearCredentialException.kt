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

package androidx.credentials.exceptions

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager

/**
 * Represents an error thrown during a clear flow with Credential Manager. See [CredentialManager]
 * for more details on how Exceptions work for Credential Manager flows.
 *
 * @see CredentialManager
 * @see ClearCredentialInterruptedException
 * @see ClearCredentialUnknownException
 */
abstract class ClearCredentialException
@JvmOverloads
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) open val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) open val errorMessage: CharSequence? = null
) : Exception(errorMessage?.toString()) {
    internal companion object {
        private const val EXTRA_CLEAR_CREDENTIAL_EXCEPTION_TYPE =
            "androidx.credentials.provider.extra.CLEAR_CREDENTIAL_EXCEPTION_TYPE"
        private const val EXTRA_CLEAR_CREDENTIAL_EXCEPTION_MESSAGE =
            "androidx.credentials.provider.extra.CLEAR_CREDENTIAL_EXCEPTION_MESSAGE"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun asBundle(ex: ClearCredentialException): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_CLEAR_CREDENTIAL_EXCEPTION_TYPE, ex.type)
            ex.errorMessage?.let {
                bundle.putCharSequence(EXTRA_CLEAR_CREDENTIAL_EXCEPTION_MESSAGE, it)
            }
            return bundle
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): ClearCredentialException? {
            val type = bundle.getString(EXTRA_CLEAR_CREDENTIAL_EXCEPTION_TYPE) ?: return null
            val msg = bundle.getCharSequence(EXTRA_CLEAR_CREDENTIAL_EXCEPTION_MESSAGE)
            return when (type) {
                ClearCredentialUnknownException.TYPE_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION ->
                    ClearCredentialUnknownException(msg)
                ClearCredentialInterruptedException.TYPE_CLEAR_CREDENTIAL_INTERRUPTED_EXCEPTION ->
                    ClearCredentialInterruptedException(msg)
                ClearCredentialUnsupportedException.TYPE_CLEAR_CREDENTIAL_UNSUPPORTED_EXCEPTION ->
                    ClearCredentialUnsupportedException(msg)
                ClearCredentialProviderConfigurationException
                    .TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION ->
                    ClearCredentialProviderConfigurationException(msg)
                else -> ClearCredentialCustomException(type, msg)
            }
        }
    }
}
