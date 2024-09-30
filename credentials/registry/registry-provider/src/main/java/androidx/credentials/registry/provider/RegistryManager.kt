/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManagerCallback
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * APIs for managing credential registries that are registered with the Credential Manager.
 *
 * Use the APIs by constructing a [RegistryManager] with the [create] factory method.
 */
public abstract class RegistryManager internal constructor() {
    public companion object {
        /**
         * Creates a [RegistryManager] based on the given [context].
         *
         * @param context the context with which the RegistryManager should be associated
         */
        @JvmStatic
        public fun create(context: Context): RegistryManager = RegistryManagerImpl(context)

        /**
         * The intent action name used to find and invoke your activity when the user selects a
         * credential that is part of your registry.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public const val ACTION_GET_CREDENTIAL: String =
            "androidx.credentials.registry.provider.action.GET_CREDENTIAL"
    }

    /**
     * Registers credentials with the Credential Manager.
     *
     * The registries will then be used by the Credential Manager when handling an app calling
     * request (see [androidx.credentials.CredentialManager]). The Credential Manager will determine
     * if the registry contains some credential(s) qualified as a candidate to fulfill the given
     * request, and if so it will surface a user selector UI to collect the user decision for
     * whether to proceed with the operation.
     *
     * @param request the request containing the credential data to register
     */
    public suspend fun registerCredentials(
        request: RegisterCredentialsRequest
    ): RegisterCredentialsResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback =
            object :
                CredentialManagerCallback<
                    RegisterCredentialsResponse,
                    RegisterCredentialsException
                > {
                override fun onResult(result: RegisterCredentialsResponse) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onError(e: RegisterCredentialsException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }

        registerCredentialsAsync(
            request,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback
        )
    }

    /**
     * Registers credentials with the Credential Manager.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * The registries will then be used by the Credential Manager when handling an app calling
     * request (see [androidx.credentials.CredentialManager]). The Credential Manager will determine
     * if the registry contains some credential(s) qualified as a candidate to fulfill the given
     * request, and if so it will surface a user selector UI to collect the user decision for
     * whether to proceed with the operation.
     *
     * @param request the request containing the credential data to register
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public abstract fun registerCredentialsAsync(
        request: RegisterCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<RegisterCredentialsResponse, RegisterCredentialsException>
    )

    // TODO: b/355652174 add clear registry APIs.
}
