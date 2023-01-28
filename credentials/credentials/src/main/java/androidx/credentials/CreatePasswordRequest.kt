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
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A request to save the user password credential with their password provider.
 *
 * @property id the user id associated with the password
 * @property password the password
 */
class CreatePasswordRequest private constructor(
    val id: String,
    val password: String,
    displayInfo: DisplayInfo,
) : CreateCredentialRequest(
    type = PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    credentialData = toCredentialDataBundle(id, password),
    candidateQueryData = toCandidateDataBundle(),
    isSystemProviderRequired = false,
    displayInfo
) {

    /**
     * Constructs a [CreatePasswordRequest] to save the user password credential with their
     * password provider.
     *
     * @param id the user id associated with the password
     * @param password the password
     * @throws NullPointerException If [id] is null
     * @throws NullPointerException If [password] is null
     * @throws IllegalArgumentException If [password] is empty
     */
    constructor(id: String, password: String) : this(id, password, DisplayInfo(id, null))

    init {
        require(password.isNotEmpty()) { "password should not be empty" }
    }

    /** @hide */
    companion object {
        internal const val BUNDLE_KEY_ID = "androidx.credentials.BUNDLE_KEY_ID"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val BUNDLE_KEY_PASSWORD = "androidx.credentials.BUNDLE_KEY_PASSWORD"

        @JvmStatic
        internal fun toCredentialDataBundle(id: String, password: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_ID, id)
            bundle.putString(BUNDLE_KEY_PASSWORD, password)
            return bundle
        }

        // No credential data should be sent during the query phase.
        @JvmStatic
        internal fun toCandidateDataBundle(): Bundle {
            return Bundle()
        }

        @JvmStatic
        @RequiresApi(23)
        internal fun createFrom(data: Bundle): CreatePasswordRequest {
            try {
                val id = data.getString(BUNDLE_KEY_ID)
                val password = data.getString(BUNDLE_KEY_PASSWORD)
                val displayInfo = DisplayInfo.parseFromCredentialDataBundle(data)
                return if (displayInfo == null) CreatePasswordRequest(
                    id!!,
                    password!!
                ) else CreatePasswordRequest(id!!, password!!, displayInfo)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}