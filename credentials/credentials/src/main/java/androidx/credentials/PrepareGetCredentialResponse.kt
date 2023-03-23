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

import androidx.annotation.RequiresApi

/**
 * A response object that indicates the get-credential prefetch work is complete and provides
 * metadata about it. It can then be used to issue the full credential retrieval flow via the
 * [CredentialManager.getCredential] (Kotlin) / [CredentialManager.getCredentialAsync] (Java)
 * method to perform the remaining flows such as consent
 * collection and credential selection, to officially retrieve a credential.
 *
 * For now this API requires Android U (level 34). However, it is designed with backward
 * compatibility in mind and can potentially be made accessible <34 if any provider decides to
 * support that.
 *
 * @property pendingGetCredentialHandle a handle that represents this pending get-credential
 * operation; pass this handle to [CredentialManager.getCredential] (Kotlin) /
 * [CredentialManager.getCredentialAsync] (Java) to perform the remaining flows to officially
 * retrieve a credential.
 */
@RequiresApi(34)
class PrepareGetCredentialResponse constructor(
    val pendingGetCredentialHandle: PendingGetCredentialHandle,
) {

    /**
     * Returns true if the user has any candidate credentials for the given {@code credentialType},
     * and false otherwise.
     */
    @Suppress("UNUSED_PARAMETER")
    fun hasCredentialResults(credentialType: String): Boolean {
        TODO("Implement")
    }

    /**
     * Returns true if the user has any candidate authentication actions (locked credential
     * supplier), and false otherwise.
     */
    fun hasAuthenticationResults(): Boolean {
        TODO("Implement")
    }

    /**
     * Returns true if the user has any candidate remote credential results, and false otherwise.
     */
    fun hasRemoteResults(): Boolean {
        TODO("Implement")
    }

    @RequiresApi(34)
    class PendingGetCredentialHandle
}
