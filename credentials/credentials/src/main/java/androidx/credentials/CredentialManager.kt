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

import android.content.Context

/**
 * Manages user authentication flows.
 *
 * An application can call the CredentialManager apis to launch framework UI flows for a user to
 * register a new credential or to consent to a saved credential from supported credential
 * providers, which can then be used to authenticate to the app.
 */
@Suppress("UNUSED_PARAMETER")
class CredentialManager private constructor(private val context: Context) {
    companion object {
        @JvmStatic
        fun create(context: Context): CredentialManager = CredentialManager(context)
    }

    /**
     * Requests a credential from the user.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    // TODO(helenqin): support failure flow.
    suspend fun executeGetCredential(request: GetCredentialRequest): GetCredentialResponse {
        throw UnsupportedOperationException("Unimplemented")
    }

    /**
     * Registers a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    suspend fun executeCreateCredential(
        request: CreateCredentialRequest
    ): CreateCredentialResponse {
        throw UnsupportedOperationException("Unimplemented")
    }
}