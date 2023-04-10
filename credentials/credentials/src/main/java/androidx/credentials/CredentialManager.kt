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

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

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
     * @param request the request for getting the credential
     * @param activity the activity used to potentially launch any UI needed
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    // TODO(helenqin): support failure flow.
    suspend fun executeGetCredential(
        request: GetCredentialRequest,
        activity: Activity,
    ): GetCredentialResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<GetCredentialResponse,
            GetCredentialException> {
            override fun onResult(result: GetCredentialResponse) {
                continuation.resume(result)
            }

            override fun onError(e: GetCredentialException) {
                continuation.resumeWithException(e)
            }
        }

        executeGetCredentialAsync(
            request,
            activity,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Registers a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * @param request the request for creating the credential
     * @param activity the activity used to potentially launch any UI needed
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    suspend fun executeCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity,
    ): CreateCredentialResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<CreateCredentialResponse,
            CreateCredentialException> {
            override fun onResult(result: CreateCredentialResponse) {
                continuation.resume(result)
            }

            override fun onError(e: CreateCredentialException) {
                continuation.resumeWithException(e)
            }
        }

        executeCreateCredentialAsync(
            request,
            activity,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Clears the current user credential state from all credential providers.
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
     */
    suspend fun clearCredentialState(
        request: ClearCredentialStateRequest
    ): Unit = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<Void?, ClearCredentialException> {
            override fun onResult(result: Void?) {
                continuation.resume(Unit)
            }

            override fun onError(e: ClearCredentialException) {
                continuation.resumeWithException(e)
            }
        }

        clearCredentialStateAsync(
            request,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Java API for requesting a credential from the user.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * @param request the request for getting the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    fun executeGetCredentialAsync(
        request: GetCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    ) {
        val provider: CredentialProvider? = CredentialProviderFactory
            .getBestAvailableProvider(context)
        if (provider == null) {
            // TODO (Update with the right error code when ready)
            callback.onError(
                GetCredentialProviderConfigurationException(
                    "executeGetCredentialAsync no provider dependencies found - please ensure " +
                        "the desired provider dependencies are added")
            )
            return
        }
        provider.onGetCredential(request, activity, cancellationSignal, executor, callback)
    }

    /**
     * Java API for registering a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * @param request the request for creating the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    fun executeCreateCredentialAsync(
        request: CreateCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
    ) {
        val provider: CredentialProvider? = CredentialProviderFactory
            .getBestAvailableProvider(context)
        if (provider == null) {
            // TODO (Update with the right error code when ready)
            callback.onError(CreateCredentialProviderConfigurationException(
                "executeCreateCredentialAsync no provider dependencies found - please ensure the " +
                    "desired provider dependencies are added"))
            return
        }
        provider.onCreateCredential(request, activity, cancellationSignal, executor, callback)
    }

    /**
     * Clears the current user credential state from all credential providers.
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
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    fun clearCredentialStateAsync(
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
}