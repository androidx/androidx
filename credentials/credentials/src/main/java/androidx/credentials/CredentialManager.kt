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
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
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
@RequiresApi(16)
@SuppressLint("ObsoleteSdkInt")
interface CredentialManager {
    companion object {
        /**
         * Creates a [CredentialManager] based on the given [context].
         *
         * @param context the context with which the CredentialManager should be associated
         */
        @JvmStatic
        fun create(context: Context): CredentialManager = CredentialManagerImpl(context)
    }

    /**
     * Requests a credential from the user.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param request the request for getting the credential
     * @throws GetCredentialException If the request fails
     */
    suspend fun getCredential(
        context: Context,
        request: GetCredentialRequest,
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

        getCredentialAsync(
            context,
            request,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Requests a credential from the user.
     *
     * Different from the other `getCredential(GetCredentialRequest, Activity)` API, this API
     * launches the remaining flows to retrieve an app credential from the user, after the
     * completed prefetch work corresponding to the given `pendingGetCredentialHandle`. Use this
     * API to complete the full credential retrieval operation after you initiated a request through
     * the [prepareGetCredential] API.
     *
     * The execution can potentially launch UI flows to collect user consent to using a
     * credential, display a picker when multiple credentials exist, etc.
     *
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param pendingGetCredentialHandle the handle representing the pending operation to resume
     * @throws GetCredentialException If the request fails
     */
    @RequiresApi(34)
    suspend fun getCredential(
        context: Context,
        pendingGetCredentialHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle,
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

        getCredentialAsync(
            context,
            pendingGetCredentialHandle,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Prepares for a get-credential operation. Returns a [PrepareGetCredentialResponse]
     * that can later be used to launch the credential retrieval UI flow to finalize a user
     * credential for your app.
     *
     * This API doesn't invoke any UI. It only performs the preparation work so that you can
     * later launch the remaining get-credential operation (involves UIs) through the
     * [getCredential] API which incurs less latency than executing the whole operation in one call.
     *
     * @param request the request for getting the credential
     * @throws GetCredentialException If the request fails
     */
    @RequiresApi(34)
    suspend fun prepareGetCredential(
        request: GetCredentialRequest,
    ): PrepareGetCredentialResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<PrepareGetCredentialResponse,
            GetCredentialException> {
            override fun onResult(result: PrepareGetCredentialResponse) {
                continuation.resume(result)
            }

            override fun onError(e: GetCredentialException) {
                continuation.resumeWithException(e)
            }
        }

        prepareGetCredentialAsync(
            request,
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
     * @param context the context used to launch any UI needed; use an activity context to make
     * sure the UI will be launched within the same task stack
     * @param request the request for creating the credential
     * @throws CreateCredentialException If the request fails
     */
    suspend fun createCredential(
        context: Context,
        request: CreateCredentialRequest,
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

        createCredentialAsync(
            context,
            request,
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
     * @throws ClearCredentialException If the request fails
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
    fun getCredentialAsync(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    )

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
    fun getCredentialAsync(
        context: Context,
        pendingGetCredentialHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    )

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
    fun prepareGetCredentialAsync(
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<PrepareGetCredentialResponse, GetCredentialException>,
    )

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
    fun createCredentialAsync(
        context: Context,
        request: CreateCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
    )

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
    fun clearCredentialStateAsync(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>,
    )

    /**
     * Returns a pending intent that shows a screen that lets a user enable a Credential Manager provider.
     * @return the pending intent that can be launched
     */
    @RequiresApi(34)
    fun createSettingsPendingIntent(): PendingIntent
}