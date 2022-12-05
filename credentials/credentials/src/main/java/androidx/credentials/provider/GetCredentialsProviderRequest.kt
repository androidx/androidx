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

import androidx.credentials.GetCredentialOption

/**
 * Request for [CredentialProviderBaseService.onGetCredentialsRequest] to credential providers.
 *
 * @property getCredentialOptions a list of [GetCredentialOption] where each option
 * contains per credential type request parameters
 * @property applicationInfo information pertaining the calling application
 * @throws IllegalArgumentException if [getCredentialOptions] is empty
 *
 * @hide
 */
class GetCredentialsProviderRequest internal constructor(
    val getCredentialOptions: List<GetCredentialOption>,
    val applicationInfo: ApplicationInfo
    ) {

    init {
        require(getCredentialOptions.isNotEmpty()) { "getCredentialOptions must not be empty" }
    }

    /** A builder for [GetCredentialsProviderRequest].
     *
     * @property applicationInfo information pertaining to the calling app
     *
     * @hide
     */
    class Builder internal constructor(private val applicationInfo: ApplicationInfo) {
        private var getCredentialOptions: MutableList<GetCredentialOption> = mutableListOf()

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
         * Builds an instance of [GetCredentialsProviderRequest]
         *
         * @throws IllegalArgumentException if [getCredentialOptions] is empty
         */
        fun build(): GetCredentialsProviderRequest {
            return GetCredentialsProviderRequest(
                getCredentialOptions.toList(), applicationInfo)
        }
    }
}