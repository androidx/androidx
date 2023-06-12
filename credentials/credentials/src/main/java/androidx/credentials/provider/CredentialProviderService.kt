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

import android.app.Activity
import android.app.PendingIntent
import android.credentials.ClearCredentialStateException
import android.credentials.GetCredentialException
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.service.credentials.ClearCredentialStateRequest
import android.service.credentials.CredentialEntry
import android.service.credentials.CredentialProviderService
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.utils.BeginCreateCredentialUtil
import androidx.credentials.provider.utils.BeginGetCredentialUtil
import androidx.credentials.provider.utils.ClearCredentialUtil

/**
 * A [CredentialProviderService] is a service used to save and retrieve credentials for a given
 * user, upon the request of a client app that typically uses these credentials for sign-in flows.
 *
 * The credential retrieval and creation/saving is mediated by the Android System that
 * aggregates credentials from multiple credential provider services, and presents them to
 * the user in the form of a selector UI for credential selections/account selections/
 * confirmations etc.
 *
 * A [CredentialProviderService] is only bound to the Android System for the span
 * of a [androidx.credentials.CredentialManager] get/create API call. The service is bound only
 * if :
 *  1. The service requires the [android.Manifest.permission.BIND_CREDENTIAL_PROVIDER_SERVICE]
 *  permission.
 *  2. The user has enabled this service as a credential provider from the
 *  settings.
 *
 *  ## Basic Usage
 *  The basic Credential Manager flow is as such:
 *  - Client app calls one of the APIs exposed in [androidx.credentials.CredentialManager].
 *  - Android system propagates the developer's request to providers that have been
 *  enabled by the user, and can support the [androidx.credentials.Credential] type
 *  specified in the request. We call this the **query phase** of provider communication.
 *  Developer may specify a different set of request parameters to be sent to the provider
 *  during this phase.
 *  - In this query phase, providers, in most cases, will respond with a list of
 *  [CredentialEntry], and an optional list of [Action] entries (for the get flow), and a list
 *  of [CreateEntry] (for the create flow). No actual credentials will be returned in this phase.
 *  - Provider responses are aggregated and presented to the user in the form of a selector UI.
 *  - User selects an entry on the selector.
 *  - Android System invokes the [PendingIntent] associated with this entry, that belongs to the
 *  corresponding provider. We call this the **final phase** of provider communication. The
 *  [PendingIntent] contains the complete request originally created by the developer.
 *  - Provider finishes the [Activity] invoked by the [PendingIntent] by setting the result
 *  as the activity is finished.
 *  - Android System sends back the result to the client app.
 *
 *  The flow described above minimizes the amount of time a service is bound to the system.
 *  Calls to the service are considered stateless. If a service wishes to maintain state
 *  between the calls, it must do its own state management.
 *  Note: The service's process might be killed by the Android System when unbound, for cases
 *  such as low memory on the device.
 *
 * ## Service Registration
 * In order for Credential Manager to propagate requests to a given provider service, the provider
 * must:
 * - Extend this class and implement the abstract methods.
 * - Declare the [CredentialProviderService.SERVICE_INTERFACE] intent as handled by the service.
 * - Require the [android.Manifest.permission.BIND_CREDENTIAL_PROVIDER_SERVICE] permission.
 * - Declare capabilities that the provider supports. Capabilities are essentially credential types
 * that the provider can handle. Capabilities must be added to the metadata of the service against
 * [CredentialProviderService.SERVICE_META_DATA].
 */
@RequiresApi(34)
abstract class CredentialProviderService : CredentialProviderService() {

    final override fun onBeginGetCredential(
        request: android.service.credentials.BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<
            android.service.credentials.BeginGetCredentialResponse, GetCredentialException>
    ) {
        val structuredRequest = BeginGetCredentialUtil.convertToJetpackRequest(request)
        val outcome = object : OutcomeReceiver<BeginGetCredentialResponse,
            androidx.credentials.exceptions.GetCredentialException> {
            override fun onResult(response: BeginGetCredentialResponse) {
                callback.onResult(
                    BeginGetCredentialUtil
                        .convertToFrameworkResponse(response)
                )
            }

            override fun onError(error: androidx.credentials.exceptions.GetCredentialException) {
                super.onError(error)
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(GetCredentialException(error.type, error.message))
            }
        }
        this.onBeginGetCredentialRequest(structuredRequest, cancellationSignal, outcome)
    }

    final override fun onBeginCreateCredential(
        request: android.service.credentials.BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<android.service.credentials.BeginCreateCredentialResponse,
            android.credentials.CreateCredentialException>
    ) {
        val outcome = object : OutcomeReceiver<
            BeginCreateCredentialResponse, CreateCredentialException> {
            override fun onResult(response: BeginCreateCredentialResponse) {
                callback.onResult(
                    BeginCreateCredentialUtil
                        .convertToFrameworkResponse(response)
                )
            }

            override fun onError(error: CreateCredentialException) {
                super.onError(error)
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(
                    android.credentials.CreateCredentialException(
                        error.type, error.message
                    )
                )
            }
        }
        onBeginCreateCredentialRequest(
            BeginCreateCredentialUtil.convertToJetpackRequest(request),
            cancellationSignal, outcome
        )
    }

    final override fun onClearCredentialState(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void, ClearCredentialStateException>
    ) {
        val outcome = object : OutcomeReceiver<Void?, ClearCredentialException> {
            override fun onResult(response: Void?) {
                callback.onResult(response)
            }

            override fun onError(error: ClearCredentialException) {
                super.onError(error)
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(ClearCredentialStateException(error.type, error.message))
            }
        }
        onClearCredentialStateRequest(ClearCredentialUtil.convertToJetpackRequest(request),
            cancellationSignal, outcome)
    }

    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.clearCredentialState]. A client app typically
     * calls this API on instances like sign-out when the intention is that the providers clear
     * any state that they may have maintained for the given user.
     *
     * You should invoked this api after your user signs out of your app to notify all credential
     * providers that any stored credential session for the given app should be cleared.
     *
     * An example scenario of a state that is maintained and is expected to be cleared on this
     * call, is when an active credential session is being stored to limit sign-in options
     * in the result of subsequent get-request calls. When a user explicitly signs out of the app,
     * the next time, the client app may want their users to see all options and hence will call
     * this API first to make sure credential providers can clear the state maintained previously.
     *
     * @param request the request for the credential provider to handle
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    abstract fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?,
            ClearCredentialException>
    )

    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.getCredential], to get a credential
     * sourced from a credential provider installed on the device.
     *
     * Credential provider services must extend this method in order to handle a
     * [BeginGetCredentialRequest] request. Once processed, the service must call one of the
     * [callback] methods to notify the result of the request.
     *
     * This API call is referred to as the **query phase** of the original get request from
     * the client app. In this phase, provider must go over all the
     * [android.service.credentials.BeginGetCredentialOption], and add corresponding a
     * [CredentialEntry] to the [BeginGetCredentialResponse]. Each [CredentialEntry] should
     * contain meta-data to be shown on the selector UI. In addition, each [CredentialEntry]
     * must contain a [PendingIntent].
     * Optionally, providers can also add [Action] entries for any non-credential related actions
     * that they want to offer to the users e.g. opening app, managing credentials etc.
     *
     * When user selects one of the [CredentialEntry], **final phase** of the original client's
     * get-request flow starts. The Android System attached the complete
     * [androidx.credentials.provider.ProviderGetCredentialRequest] to an intent extra of the
     * activity that is started by the pending intent. The request must be retrieved through
     * [PendingIntentHandler.retrieveProviderGetCredentialRequest]. This final request
     * will only contain a single [androidx.credentials.CredentialOption] that contains the
     * parameters of the credential the user has requested. The provider service must retrieve this
     * credential and return through [PendingIntentHandler.setGetCredentialResponse].
     *
     * **Handling locked provider apps**
     * If the provider app is locked, and the provider cannot provide any meta-data based
     * [CredentialEntry], provider must set an [AuthenticationAction] on the
     * [BeginGetCredentialResponse]. The [PendingIntent] set on this entry must lead the user
     * to an >unlock activity. Once unlocked, the provider must retrieve all credentials,
     * and set the list of [CredentialEntry] and the list of optional [Action] as a result
     * of the >unlock activity through [PendingIntentHandler.setBeginGetCredentialResponse].
     *
     * @see CredentialEntry for how an entry representing a credential must be built
     * @see Action for how a non-credential related action should be built
     * @see AuthenticationAction for how an entry that navigates the user to an unlock flow
     * can be built
     *
     * @param [request] the [ProviderGetCredentialRequest] to handle
     * See [BeginGetCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    abstract fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse,
            androidx.credentials.exceptions.GetCredentialException>
    )

    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.createCredential], to create/save a credential
     * with a credential provider installed on the device.
     *
     * Credential provider services must extend this method in order to handle a
     * [BeginCreateCredentialRequest] request. Once processed, the service must call one of the
     * [callback] methods to notify the result of the request.
     *
     * This API call is referred to as the **query phase** of the original create request from
     * the client app. In this phase, provider must process the request parameters in the
     * [BeginCreateCredentialRequest] and return a list of [CreateEntry] whereby every
     * entry represents an account/group where the user will be storing the credential. Each
     * [CreateEntry] must contain a [PendingIntent] that will lead the user to an activity
     * in the credential provider's app that will complete the actual credential creation.
     *
     * When user selects one of the [CreateEntry], the associated [PendingIntent] will be invoked
     * and the provider will receive the complete request as part of the extras in the resulting
     * activity. Provider must retrieve the request through
     * [PendingIntentHandler.retrieveProviderCreateCredentialRequest].
     * Once the activity is complete, and the credential is created, provider must set back the
     * response through [PendingIntentHandler.setCreateCredentialResponse].
     *
     * @param [request] the [BeginCreateCredentialRequest] to handle
     * See [BeginCreateCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    abstract fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse,
            CreateCredentialException>
    )
}
