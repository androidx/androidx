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

import android.Manifest.permission.CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS
import android.credentials.PrepareGetCredentialResponse
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo

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
 * @property frameworkResponse the corresponding framework response, guaranteed to be nonnull
 * at API level >= 34
 * @property pendingGetCredentialHandle a handle that represents this pending get-credential
 * operation; pass this handle to [CredentialManager.getCredential] (Kotlin) /
 * [CredentialManager.getCredentialAsync] (Java) to perform the remaining flows to officially
 * retrieve a credential.
 * @throws NullPointerException If [frameworkResponse] is null at API level >= 34.
 */
@RequiresApi(34)
class PrepareGetCredentialResponse internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val frameworkResponse: PrepareGetCredentialResponse?,
    val pendingGetCredentialHandle: PendingGetCredentialHandle,
) {
    init {
        if (Build.VERSION.SDK_INT >= 34) { // Android U
            frameworkResponse!!
        }
    }

    /**
     * Returns true if the user has any candidate credentials for the given {@code credentialType},
     * and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @Suppress("UNUSED_PARAMETER")
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    fun hasCredentialResults(credentialType: String): Boolean {
        return frameworkResponse?.hasCredentialResults(credentialType) ?: false
    }

    /**
     * Returns true if the user has any candidate authentication actions (locked credential
     * supplier), and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    fun hasAuthenticationResults(): Boolean {
        return frameworkResponse?.hasAuthenticationResults() ?: false
    }

    /**
     * Returns true if the user has any candidate remote credential results, and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    fun hasRemoteResults(): Boolean {
        return frameworkResponse?.hasRemoteResults() ?: false
    }

    /**
     * A handle that represents a pending get-credential operation. Pass this handle to
     * [CredentialManager.getCredential] or [CredentialManager.getCredentialAsync] to perform the
     * remaining flows to officially retrieve a credential.
     *
     * @property frameworkHandle the framework handle representing this pending operation. Must not
     * be null at API level >= 34.
     * @throws NullPointerException If [frameworkHandle] is null at API level >= 34.
     */
    @RequiresApi(34)
    class PendingGetCredentialHandle(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val frameworkHandle:
        PrepareGetCredentialResponse.PendingGetCredentialHandle?
    ) {
        init {
            if (Build.VERSION.SDK_INT >= 34) { // Android U
                frameworkHandle!!
            }
        }
    }
}
