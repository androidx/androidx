/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.service

import android.app.Service
import android.content.Intent
import android.os.CancellationSignal
import android.os.IBinder
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.providerevents.internal.CredentialEventsProviderFactory
import androidx.credentials.providerevents.signal.ProviderSignalCredentialStateCallback
import androidx.credentials.providerevents.signal.ProviderSignalCredentialStateRequest

/**
 * A base service for credential providers to receive advanced requests from
 * [androidx.credentials.CredentialManager].
 *
 * This service builds upon the functionality of [CredentialProviderService] by enabling support for
 * more advanced credential requests, such as conditional creation.
 *
 * Note that this service is distinct from [CredentialProviderService], which handles basic
 * credential saving and retrieval requests.
 *
 * This service is bound only during the duration of an API call. To receive requests, users must
 * enable the corresponding [CredentialProviderService] from within the same package, in Android
 * Settings.
 *
 * ## Basic Usage
 *
 * The interaction between Credential Manager and this service typically involves:
 * - A client application invokes an advanced API in [androidx.credentials.CredentialManager], such
 *   as a conditional credential creation request using
 *   [androidx.credentials.CreatePublicKeyCredentialRequest.isConditional].
 * - The Android system forwards the request to enabled credential providers that support the
 *   requested feature.
 * - Credential providers receive the request, process it, and return an appropriate response.
 * - Depending on the requested feature, providers may display UI elements such as notifications.
 * - The Android system sends the result back to the client application.
 *
 * This flow is designed to minimize the service's lifecycle. Calls to the service are stateless. If
 * a service requires maintaining state between calls, it must implement its own state management.
 * Note that the service's process may be terminated by the Android System when unbound, such as
 * during low-memory conditions.
 *
 * ## Service Registration
 *
 * To enable Credential Manager to send requests to a provider service, the provider must:
 * - Extend this class and implement the required methods.
 * - Declare the name of this service class within the metadata of the corresponding
 *   [CredentialProviderService]. This is done using the
 *   [android.service.credentials.CredentialProviderService.SERVICE_META_DATA] key. The service
 *   class name should be specified along with supported capabilities.
 *
 *   For example: ```xml <credential-provider
 *   xmlns:android="http://schemas.android.com/apk/res/android" android:settingsActivity="xyz">
 *   <capabilities> <capability name="android.credentials.TYPE_PASSWORD_CREDENTIAL" /> <capability
 *   name="androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL" /> </capabilities> <events-service
 *   class-name="MyCredentialProviderEventsService" /> </credential-provider> ```
 */
public abstract class CredentialProviderEventsService() : Service() {
    private val factory = CredentialEventsProviderFactory()

    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            return null
        }
        val provider = factory.getBestAvailableProvider(intent)
        if (provider == null) {
            return null
        }
        return provider.getStubImplementation(this)
    }

    /**
     * Called when a credential provider should create a credential.
     *
     * This method should be extended by credential providers to receive credential creation
     * requests. Note that for now, a credential provider will only receive conditional passkey
     * creation requests through this endpoint (after certain internal conditions are met), and all
     * other regular credential creation requests will still come through
     * [CredentialProviderService.onBeginCreateCredential] API.
     *
     * The difference between [CredentialProviderService.onBeginCreateCredential] and this API is
     * that in the former, providers return entries that are shown on the UI, whereas in this one,
     * providers simply create the passkey and return the response.
     *
     * Because there is no UI from the system in the conditional passkey creation flow, it is
     * required that providers fulfill certain conditions before creating the passkey:
     * 1. Check that the user has a credential stored with the provider already that has been
     *    recently used.
     * 2. Show a notification to notify the user after creating the passkey.
     *
     * Note that the credential provider must be selected as the preferred service for them to
     * receive the conditional passkey create request. When they do receive the request, they must
     * check if the request is of type [androidx.credentials.CreatePublicKeyCredentialRequest] and
     * that [androidx.credentials.CreatePublicKeyCredentialRequest.isConditional] is set to true.
     *
     * @param request The request for creating a credential.
     * @param cancellationSignal A signal to cancel the operation.
     * @param callback The callback to receive the result of the credential creation.
     */
    public open fun onCreateCredentialRequest(
        request: ProviderCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiverCompat<CreateCredentialResponse, CreateCredentialException>,
    ) {}

    /**
     * Called when a credential provider should receive credential state signals from a calling
     * application.
     *
     * This method should be extended by credential providers to receive credential state signal
     * requests. Note there is no required action on receipt of this request if it is not
     * applicable, however there are recommended actions. See
     * [spec](https://w3c.github.io/webauthn/#sctn-signal-methods)
     *
     * @param request The request for signalling a user's credential state.
     * @param callback The callback to receive the result of the credential state signal.
     */
    public open fun onSignalCredentialStateRequest(
        request: ProviderSignalCredentialStateRequest,
        callback: ProviderSignalCredentialStateCallback,
    ) {}
}
