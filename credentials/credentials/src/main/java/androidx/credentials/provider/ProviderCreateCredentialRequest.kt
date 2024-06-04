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
 * @property biometricPromptResult the result of a Biometric Prompt authentication flow, that is
 *   propagated to the provider if the provider requested for
 *   [androidx.credentials.CredentialManager] to handle the authentication flow
 * @throws NullPointerException If [callingRequest], or [callingAppInfo] is null
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class ProviderCreateCredentialRequest
@JvmOverloads
constructor(
    val callingRequest: CreateCredentialRequest,
    val callingAppInfo: CallingAppInfo,
    val biometricPromptResult: BiometricPromptResult? = null
)
