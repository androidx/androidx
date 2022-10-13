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

package androidx.credentials.playservices.controllers

import androidx.credentials.CredentialManagerCallback
import java.util.concurrent.Executor

/**
 * Extensible abstract class for credential controllers. Please implement this class per every
 * request/response credential type. Unique logic is left to the use case of the implementation.
 * If you are building your own version as an OEM, the below can be mimicked to your own
 * credential provider equivalent and whatever internal service you invoke.
 *
 * @param T1 the credential request type from credential manager
 * @param T2 the credential request type converted to play services
 * @param R2 the credential response type from play services
 * @param R1 the credential response type converted back to that used by credential manager
 *
 * @hide
 */
@Suppress("deprecation")
abstract class CredentialProviderController<T1 : Any, T2 : Any, R2 : Any, R1 : Any> : android.app
        .Fragment() {

    /**
     * Invokes the flow that starts retrieving credential data. In this use case, we invoke
     * play service modules.
     *
     * @param request a credential provider request
     * @param callback a credential manager callback with a credential provider response
     * @param executor to be used in any multi-threaded operation calls, such as listenable futures
     */
    abstract fun invokePlayServices(
        request: T1,
        callback: CredentialManagerCallback<R1>,
        executor: Executor
    )

    /**
     * Allows converting from a credential provider request to a play service request.
     *
     * @param request a credential provider request
     * @return a play service request
     */
    protected abstract fun convertToPlayServices(request: T1): T2

    /**
     * Allows converting from a play service response to a credential provider response.
     *
     * @param response a play service response
     * @return a credential provider response
     */
    protected abstract fun convertToCredentialProvider(response: R2): R1
}