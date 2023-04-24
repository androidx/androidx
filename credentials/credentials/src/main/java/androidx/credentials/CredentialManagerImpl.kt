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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import java.util.concurrent.Executor

/**
 * Manages user authentication flows.
 *
 * An application can call the CredentialManager apis to launch framework UI flows for a user to
 * register a new credential or to consent to a saved credential from supported credential
 * providers, which can then be used to authenticate to the app.
 *
 * This class contains its own exception types.
 * They represent unique failures during the Credential Manager flow. As required, they
 * can be extended for unique types containing new and unique versions of the exception - either
 * with new 'exception types' (same credential class, different exceptions), or inner subclasses
 * and their exception types (a subclass credential class and all their exception types).
 *
 * For example, if there is an UNKNOWN exception type, assuming the base Exception is
 * [ClearCredentialException], we can add an 'exception type' class for it as follows:
 * TODO("Add in new flow with extensive 'getType' function")
 * ```
 * class ClearCredentialUnknownException(
 *     errorMessage: CharSequence? = null
 * ) : ClearCredentialException(TYPE_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION, errorMessage) {
 *  // ...Any required impl here...//
 *  companion object {
 *       private const val TYPE_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION: String =
 *       "androidx.credentials.TYPE_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION"
 *   }
 * }
 * ```
 *
 * Furthermore, the base class can be subclassed to a new more specific credential type, which
 * then can further be subclassed into individual exception types. The first is an example of a
 * 'inner credential type exception', and the next is a 'exception type' of this subclass exception.
 *
 * ```
 * class UniqueCredentialBasedOnClearCredentialException(
 *     type: String,
 *     errorMessage: CharSequence? = null
 * ) : ClearCredentialException(type, errorMessage) {
 *  // ... Any required impl here...//
 * }
 * // .... code and logic .... //
 * class UniqueCredentialBasedOnClearCredentialUnknownException(
 *     errorMessage: CharSequence? = null
 * ) : ClearCredentialException(TYPE_UNIQUE_CREDENTIAL_BASED_ON_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION,
 * errorMessage) {
 * // ... Any required impl here ... //
 *  companion object {
 *       private const val
 *       TYPE_UNIQUE_CREDENTIAL_BASED_ON_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION: String =
 *       "androidx.credentials.TYPE_CLEAR_CREDENTIAL_UNKNOWN_EXCEPTION"
 *   }
 * }
 * ```
 *
 *
 */
@RequiresApi(16)
@SuppressLint("ObsoleteSdkInt")
internal class CredentialManagerImpl internal constructor(
    private val context: Context
) : CredentialManager {
    companion object {
        /**
         * An intent action that shows a screen that let user enable a Credential Manager provider.
         */
        private const val
        INTENT_ACTION_FOR_CREDENTIAL_PROVIDER_SETTINGS: String =
        "android.settings.CREDENTIAL_PROVIDER"
    }

    /**
     * Requests a credential from the user.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param request the request for getting the credential
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    override fun getCredentialAsync(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    ) {
        val provider: CredentialProvider? = CredentialProviderFactory
            .getBestAvailableProvider(this.context)
        if (provider == null) {
            // TODO (Update with the right error code when ready)
            callback.onError(
                GetCredentialProviderConfigurationException(
                    "getCredentialAsync no provider dependencies found - please ensure " +
                        "the desired provider dependencies are added")
            )
            return
        }
        provider.onGetCredential(context, request, cancellationSignal, executor, callback)
    }

    /**
     * Requests a credential from the user.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * Different from the other `getCredentialAsync(GetCredentialRequest, Activity)` API, this API
     * launches the remaining flows to retrieve an app credential from the user, after the
     * completed prefetch work corresponding to the given `pendingGetCredentialHandle`. Use this
     * API to complete the full credential retrieval operation after you initiated a request through
     * the [prepareGetCredentialAsync] API.
     *
     * The execution can potentially launch UI flows to collect user consent to using a
     * credential, display a picker when multiple credentials exist, etc.
     *
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param pendingGetCredentialHandle the handle representing the pending operation to resume
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    @RequiresApi(34)
    override fun getCredentialAsync(
        context: Context,
        pendingGetCredentialHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    ) {
        val provider = CredentialProviderFactory.getUAndAboveProvider(context)
        provider.onGetCredential(
            context, pendingGetCredentialHandle, cancellationSignal, executor, callback)
    }

    /**
     * Prepares for a get-credential operation. Returns a [PrepareGetCredentialResponse]
     * that can later be used to launch the credential retrieval UI flow to finalize a user
     * credential for your app.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * This API doesn't invoke any UI. It only performs the preparation work so that you can
     * later launch the remaining get-credential operation (involves UIs) through the
     * [getCredentialAsync] API which incurs less latency than executing the whole operation in one
     * call.
     *
     * @param request the request for getting the credential
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    @RequiresApi(34)
    override fun prepareGetCredentialAsync(
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<PrepareGetCredentialResponse, GetCredentialException>,
    ) {
        val provider = CredentialProviderFactory.getUAndAboveProvider(context)
        provider.onPrepareCredential(request, cancellationSignal, executor, callback)
    }

    /**
     * Registers a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param request the request for creating the credential
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    override fun createCredentialAsync(
        context: Context,
        request: CreateCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
    ) {
        val provider: CredentialProvider? = CredentialProviderFactory
            .getBestAvailableProvider(this.context)
        if (provider == null) {
            // TODO (Update with the right error code when ready)
            callback.onError(CreateCredentialProviderConfigurationException(
                "createCredentialAsync no provider dependencies found - please ensure the " +
                    "desired provider dependencies are added"))
            return
        }
        provider.onCreateCredential(context, request, cancellationSignal, executor, callback)
    }

    /**
     * Clears the current user credential state from all credential providers.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * You should invoked this api after your user signs out of your app to notify all credential
     * providers that any stored credential session for the given app should be cleared.
     *
     * A credential provider may have stored an active credential session and use it to limit
     * sign-in options for future get-credential calls. For example, it may prioritize the active
     * credential over any other available credential. When your user explicitly signs out of your
     * app and in order to get the holistic sign-in options the next time, you should call this API
     * to let the provider clear any stored credential session.
     *
     * @param request the request for clearing the app user's credential state
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    override fun clearCredentialStateAsync(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>,
    ) {
        val provider: CredentialProvider? = CredentialProviderFactory
            .getBestAvailableProvider(context)
        if (provider == null) {
            // TODO (Update with the right error code when ready)
            callback.onError(ClearCredentialProviderConfigurationException(
                "clearCredentialStateAsync no provider dependencies found - please ensure the " +
                    "desired provider dependencies are added"))
            return
        }
        provider.onClearCredential(request, cancellationSignal, executor, callback)
    }

    /**
     * Returns a pending intent that shows a screen that lets a user enable a Credential Manager provider.
     * @return the pending intent that can be launched
     */
    @RequiresApi(34)
    override fun createSettingsPendingIntent(): PendingIntent {
        val intent: Intent = Intent(INTENT_ACTION_FOR_CREDENTIAL_PROVIDER_SETTINGS)
        intent.setData(Uri.parse("package:" + context.getPackageName()))
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}