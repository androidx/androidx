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

/**
 * Request for [CredentialProviderBaseService.onBeginGetCredentialsRequest] to
 * credential providers.
 *
 * @property beginGetCredentialOptions a list of [BeginGetCredentialOption] where each option
 * contains per credential type request parameters
 * @property applicationInfo information pertaining the calling application
 * @throws IllegalArgumentException If [beginGetCredentialOptions] is empty
 *
 * @hide
 */
class BeginGetCredentialsProviderRequest internal constructor(
    val beginGetCredentialOptions: List<BeginGetCredentialOption>,
    val applicationInfo: ApplicationInfo
    ) {

    init {
        require(beginGetCredentialOptions.isNotEmpty()) {
            "beginGetCredentialOptions must not be empty" }
    }
}