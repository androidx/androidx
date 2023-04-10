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

/**
 * Encapsulates a request to get a user credential.
 *
 * An application can construct such a request by adding one or more types of [GetCredentialOption],
 * and then call [CredentialManager.executeGetCredential] to launch framework UI flows to allow the user
 * to consent to using a previously saved credential for the given application.
 *
 * @property getCredentialOptions the list of [GetCredentialOption] from which the user can choose
 * one to authenticate to the app
 * @property isAutoSelectAllowed defines if a credential entry will be automatically chosen if it is
 * the only one, false by default
 * @throws IllegalArgumentException If [getCredentialOptions] is empty
 */
class GetCredentialRequest @JvmOverloads constructor(
    val getCredentialOptions: List<GetCredentialOption>,
    val isAutoSelectAllowed: Boolean = false,
) {

    init {
        require(getCredentialOptions.isNotEmpty()) { "credentialRequests should not be empty" }
    }

    /** A builder for [GetCredentialRequest]. */
    class Builder {
        private var getCredentialOptions: MutableList<GetCredentialOption> = mutableListOf()
        private var autoSelectAllowed: Boolean = false

        /** Adds a specific type of [GetCredentialOption]. */
        fun addGetCredentialOption(getCredentialOption: GetCredentialOption): Builder {
            getCredentialOptions.add(getCredentialOption)
            return this
        }

        /** Sets the list of [GetCredentialOption]. */
        fun setGetCredentialOptions(getCredentialOptions: List<GetCredentialOption>): Builder {
            this.getCredentialOptions = getCredentialOptions.toMutableList()
            return this
        }

        /**
         * Sets [autoSelectAllowed], which by default, is false.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAutoSelectAllowed(autoSelectAllowed: Boolean): Builder {
            this.autoSelectAllowed = autoSelectAllowed
            return this
        }

        /**
         * Builds a [GetCredentialRequest].
         *
         * @throws IllegalArgumentException If [getCredentialOptions] is empty
         */
        fun build(): GetCredentialRequest {
            return GetCredentialRequest(getCredentialOptions.toList(),
                autoSelectAllowed)
        }
    }
}