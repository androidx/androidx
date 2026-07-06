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

/**
 * Encapsulates the result of a user credential request.
 *
 * Typically, a response contains a single user credential in [credential] (such as a passkey or a
 * password). However, for [DigitalCredential]s, a single request may yield multiple credentials
 * returned in [credentials].
 *
 * @property credentials the list of user credentials returned by the provider(s) that can be used
 *   to authenticate to your app
 * @property credential the user credential that can be used to authenticate to your app. When
 *   multiple credentials are returned in [credentials], this returns the first credential.
 * @throws NullPointerException If [credentials] or [credential] is null
 * @throws IllegalArgumentException If [credentials] is empty
 */
class GetCredentialResponse(val credentials: List<Credential>) {

    init {
        require(credentials.isNotEmpty()) { "credentials must not be empty" }
    }

    /**
     * Constructs a [GetCredentialResponse] containing a single credential.
     *
     * @param credential the user credential that can be used to authenticate to your app
     * @throws NullPointerException If [credential] is null
     */
    constructor(credential: Credential) : this(listOf(credential))

    /**
     * The user credential that can be used to authenticate to your app.
     *
     * When multiple credentials are returned in [credentials], this returns the first credential.
     */
    val credential: Credential
        get() = credentials.first()

    internal companion object {
        private const val EXTRA_CREDENTIAL_TYPE =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_TYPE"
        private const val EXTRA_CREDENTIAL_DATA =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA"

        internal const val EXTRA_CREDENTIAL_LIST_SIZE =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_LIST_SIZE"
        private const val EXTRA_CREDENTIAL_TYPE_PREFIX =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_TYPE_"
        private const val EXTRA_CREDENTIAL_DATA_PREFIX =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA_"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun asBundle(response: GetCredentialResponse): Bundle =
            Bundle().apply {
                putString(EXTRA_CREDENTIAL_TYPE, response.credential.type)
                putBundle(EXTRA_CREDENTIAL_DATA, response.credential.data)
                putInt(EXTRA_CREDENTIAL_LIST_SIZE, response.credentials.size)
                response.credentials.forEachIndexed { index, cred ->
                    putString("$EXTRA_CREDENTIAL_TYPE_PREFIX$index", cred.type)
                    putBundle("$EXTRA_CREDENTIAL_DATA_PREFIX$index", cred.data)
                }
            }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): GetCredentialResponse? {
            val size = bundle.getInt(EXTRA_CREDENTIAL_LIST_SIZE, -1)
            if (size > 0) {
                val list = mutableListOf<Credential>()
                for (i in 0 until size) {
                    val type = bundle.getString("$EXTRA_CREDENTIAL_TYPE_PREFIX$i") ?: return null
                    val data = bundle.getBundle("$EXTRA_CREDENTIAL_DATA_PREFIX$i") ?: return null
                    list.add(Credential.createFrom(type, data))
                }
                return GetCredentialResponse(list)
            }
            val type = bundle.getString(EXTRA_CREDENTIAL_TYPE) ?: return null
            val data = bundle.getBundle(EXTRA_CREDENTIAL_DATA) ?: return null
            return GetCredentialResponse(Credential.createFrom(type, data))
        }
    }
}
