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

import android.service.credentials.CallingAppInfo

/**
 * Query stage request for getting user's credentials from a given credential provider.
 *
 * <p>This request contains a list of [BeginGetCredentialOption] that have parameters
 * to be used to query credentials, and return a list of [CredentialEntry] to be set
 * on the [BeginGetCredentialResponse]. This list is then shown to the user on a selector.
 *
 * @param beginGetCredentialOptions the list of type specific credential options to to be processed
 * in order to produce a [BeginGetCredentialResponse]
 * @param callingAppInfo info pertaining to the app requesting credentials
 */
class BeginGetCredentialRequest @JvmOverloads constructor(
    val beginGetCredentialOptions: List<BeginGetCredentialOption>,
    val callingAppInfo: CallingAppInfo? = null,
)