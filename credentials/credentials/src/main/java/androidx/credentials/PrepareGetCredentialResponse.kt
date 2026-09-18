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
import android.annotation.SuppressLint
import android.credentials.PrepareGetCredentialResponse
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * A response object that indicates the get-credential prefetch work is complete and provides
 * metadata about it. It can then be used to issue the full credential retrieval flow via the
 * [CredentialManager.getCredential] (Kotlin) / [CredentialManager.getCredentialAsync] (Java) method
 * to perform the remaining flows such as consent collection and credential selection, to officially
 * retrieve a credential.
 *
 * For now this API requires Android U (level 34). However, it is designed with backward
 * compatibility in mind and can potentially be made accessible <34 if any provider decides to
 * support that.
 *
 * This class should be constructed using the Builder (see below) for tests/prod usage.
 *
 * @property pendingGetCredentialHandle a handle that represents this pending get-credential
 *   operation; pass this handle to [CredentialManager.getCredential] (Kotlin) /
 *   [CredentialManager.getCredentialAsync] (Java) to perform the remaining flows to officially
 *   retrieve a credential.
 * @property hasRemoteResultsDelegate whether the response has remote results
 * @property hasAuthResultsDelegate whether the response has auth results
 * @property credentialTypeDelegate whether the response has a credential result handler
 * @property isNullHandlesForTest whether to support null handles for test
 * @throws NullPointerException If [pendingGetCredentialHandle] is null at API level >= 34.
 */
@RequiresApi(34)
@SuppressLint("MissingGetterMatchingBuilder")
public class PrepareGetCredentialResponse
private constructor(
    public val pendingGetCredentialHandle: PendingGetCredentialHandle?,
    public val hasRemoteResultsDelegate: HasRemoteResultsDelegate?,
    public val hasAuthResultsDelegate: HasAuthenticationResultsDelegate?,
    public val credentialTypeDelegate: HasCredentialResultsDelegate?,
    public val isNullHandlesForTest: Boolean,
) {

    init {
        // We don't have these values when we are testing so we should
        // we should not ensure of their presence. Otherwise, enforce
        // for Android U+.
        if (Build.VERSION.SDK_INT >= 34 && !isNullHandlesForTest) {
            pendingGetCredentialHandle!!
        }
    }

    /**
     * Returns true if the user has any candidate credentials for the given {@code credentialType},
     * and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    public fun hasCredentialResults(credentialType: String): Boolean {
        if (credentialTypeDelegate != null) {
            return credentialTypeDelegate.invoke(credentialType)
        }
        return false
    }

    /**
     * Returns true if the user has any candidate authentication actions (locked credential
     * supplier), and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    public fun hasAuthenticationResults(): Boolean {
        if (hasAuthResultsDelegate != null) {
            return hasAuthResultsDelegate.invoke()
        }
        return false
    }

    /**
     * Returns true if the user has any candidate remote credential results, and false otherwise.
     *
     * Note: this API will always return false at API level < 34.
     */
    @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
    public fun hasRemoteResults(): Boolean {
        if (hasRemoteResultsDelegate != null) {
            return hasRemoteResultsDelegate.invoke()
        }
        return false
    }

    /**
     * A handle that represents a pending get-credential operation. Pass this handle to
     * [CredentialManager.getCredential] or [CredentialManager.getCredentialAsync] to perform the
     * remaining flows to officially retrieve a credential.
     *
     * @param frameworkHandle the framework handle representing this pending operation. Must not be
     *   null at API level >= 34.
     * @throws NullPointerException If [frameworkHandle] is null at API level >= 34.
     */
    @RequiresApi(34)
    public class PendingGetCredentialHandle(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val frameworkHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle?
    ) {
        init {
            if (Build.VERSION.SDK_INT >= 34) { // Android U
                frameworkHandle!!
            }
        }
    }

    /** A builder for [PrepareGetCredentialResponse]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class Builder {
        private var pendingGetCredentialHandle: PendingGetCredentialHandle? = null
        private var hasRemoteResultsDelegate: HasRemoteResultsDelegate? = null
        private var hasAuthResultsDelegate: HasAuthenticationResultsDelegate? = null
        private var hasCredentialResultsDelegate: HasCredentialResultsDelegate? = null
        private var frameworkResponse: PrepareGetCredentialResponse? = null

        /** Sets the framework response. */
        public fun setFrameworkResponse(resp: PrepareGetCredentialResponse?): Builder {
            this.frameworkResponse = resp
            if (resp != null) {
                this.hasCredentialResultsDelegate = this::hasCredentialType
                this.hasAuthResultsDelegate = this::hasAuthenticationResults
                this.hasRemoteResultsDelegate = this::hasRemoteResults
            }
            return this
        }

        @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
        private fun hasCredentialType(credentialType: String): Boolean {
            return this.frameworkResponse!!.hasCredentialResults(credentialType)
        }

        @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
        private fun hasAuthenticationResults(): Boolean {
            return this.frameworkResponse!!.hasAuthenticationResults()
        }

        @RequiresPermission(CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS)
        private fun hasRemoteResults(): Boolean {
            return this.frameworkResponse!!.hasRemoteResults()
        }

        /** Sets the framework handle. */
        public fun setPendingGetCredentialHandle(handle: PendingGetCredentialHandle): Builder {
            this.pendingGetCredentialHandle = handle
            return this
        }

        /** Builds a [PrepareGetCredentialResponse]. */
        public fun build(): androidx.credentials.PrepareGetCredentialResponse {
            return androidx.credentials.PrepareGetCredentialResponse(
                pendingGetCredentialHandle,
                hasRemoteResultsDelegate,
                hasAuthResultsDelegate,
                hasCredentialResultsDelegate,
                false,
            )
        }
    }

    /** A builder for [PrepareGetCredentialResponse] for test use only. */
    @VisibleForTesting
    public class TestBuilder {
        private var hasRemoteResultsDelegate: HasRemoteResultsDelegate? = null
        private var hasAuthResultsDelegate: HasAuthenticationResultsDelegate? = null
        private var hasCredentialResultsDelegate: HasCredentialResultsDelegate? = null

        /** Sets the credential type handler. */
        @VisibleForTesting
        public fun setCredentialTypeDelegate(handler: HasCredentialResultsDelegate): TestBuilder {
            this.hasCredentialResultsDelegate = handler
            return this
        }

        /** Sets the has authentication results bit. */
        @VisibleForTesting
        public fun setHasAuthResultsDelegate(
            handler: HasAuthenticationResultsDelegate
        ): TestBuilder {
            this.hasAuthResultsDelegate = handler
            return this
        }

        /** Sets the has remote results bit. */
        @VisibleForTesting
        public fun setHasRemoteResultsDelegate(handler: HasRemoteResultsDelegate): TestBuilder {
            this.hasRemoteResultsDelegate = handler
            return this
        }

        /** Builds a [PrepareGetCredentialResponse]. */
        public fun build(): androidx.credentials.PrepareGetCredentialResponse {
            return androidx.credentials.PrepareGetCredentialResponse(
                null,
                hasRemoteResultsDelegate,
                hasAuthResultsDelegate,
                hasCredentialResultsDelegate,
                true,
            )
        }
    }
}

@Suppress("TypealiasDefinition")
public typealias HasCredentialResultsDelegate = (String) -> Boolean

@Suppress("TypealiasDefinition")
public typealias HasAuthenticationResultsDelegate = () -> Boolean

@Suppress("TypealiasDefinition")
public typealias HasRemoteResultsDelegate = () -> Boolean
