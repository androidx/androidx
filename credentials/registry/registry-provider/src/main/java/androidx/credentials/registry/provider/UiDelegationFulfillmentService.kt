/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.provider

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.internal.UiDelegationFulfillmentProviderFactory

/**
 * A service that credential providers must implement to support background fulfillment of
 * credential retrieval requests (for example, [DelegationType.FULL]).
 *
 * Credential Manager binds directly to this background service to fulfill credential retrieval
 * requests silently, without launching any user-facing UI, dialogs, or provider activity windows.
 *
 * If a request contains a set of credentials with mixed delegation types (for example, some
 * credentials specify [DelegationType.FULL] while others specify [DelegationType.NONE]), Credential
 * Manager will fall back to standard interactive UI fulfillment (see
 * [RegistryManager.ACTION_GET_CREDENTIAL]) unless all selected credentials in the set support
 * [DelegationType.FULL] delegation.
 *
 * ### Registration & Discovery
 * - Extend this class and implement the required methods.
 * - To enable Credential Manager to discover and bind to this service, the provider must declare it
 *   in their `AndroidManifest.xml` with an `<intent-filter>` handling the service action specified
 *   during credential registration (see [RegisterCredentialsRequest.serviceAction], defaulting to
 *   [RegistryManager.ACTION_GET_CREDENTIAL_SERVICE]).
 */
public abstract class UiDelegationFulfillmentService : Service() {

    private val factory = UiDelegationFulfillmentProviderFactory()

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
     * Called by Credential Manager to fulfill a credential retrieval request in the background.
     *
     * The provider must fulfill this request silently without showing any UI or authentication
     * prompts.
     *
     * @param request The request details, including selected credentials.
     * @param callback Callback to return the result or failure.
     */
    public abstract fun onGetCredentialRequest(
        request: ProviderGetCredentialRequest,
        callback: OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>,
    )
}
