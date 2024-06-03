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

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreateCredentialRequest

/**
 * Final request received by the provider after the user has selected a given [CreateEntry] on the
 * UI.
 *
 * This request contains the actual request coming from the calling app, and the application
 * information associated with the calling app.
 *
 * @constructor constructs an instance of [ProviderCreateCredentialRequest]
 * @property callingRequest the complete [CreateCredentialRequest] coming from the calling app that
 *   is requesting for credential creation
 * @property callingAppInfo information pertaining to the calling app making the request
 * @throws NullPointerException If [callingRequest], or [callingAppInfo] is null
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderCreateCredentialRequest
internal constructor(
    val callingRequest: CreateCredentialRequest,
    val callingAppInfo: CallingAppInfo,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val biometricPromptResult: BiometricPromptResult? = null,
    // TODO: Remove when exposing API and make this the only constructor
    isInternal: Boolean = true
) {
    init {
        // TODO: Remove when exposing API
        Log.i("ProvCrCredRequest", isInternal.toString())
    }

    /**
     * Constructs an instance of this class
     *
     * @param callingRequest the complete [CreateCredentialRequest] coming from the calling app that
     *   is requesting for credential creation
     * @param callingAppInfo information pertaining to the calling app making the request
     */
    constructor(
        callingRequest: CreateCredentialRequest,
        callingAppInfo: CallingAppInfo,
    ) : this(callingRequest = callingRequest, callingAppInfo = callingAppInfo, isInternal = false)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    constructor(
        callingRequest: CreateCredentialRequest,
        callingAppInfo: CallingAppInfo,
        biometricPromptResult: BiometricPromptResult?
    ) : this(callingRequest, callingAppInfo, biometricPromptResult, isInternal = false)
}
