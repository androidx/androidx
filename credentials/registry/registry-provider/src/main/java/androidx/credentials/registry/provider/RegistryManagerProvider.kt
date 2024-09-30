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

import android.os.CancellationSignal
import androidx.credentials.CredentialManagerCallback
import java.util.concurrent.Executor

/**
 * Provider interface to be implemented by a system registry manager service provider that will
 * fulfill [RegistryManager] requests. The implementation **must** have a constructor that takes in
 * a context.
 */
public interface RegistryManagerProvider {

    /**
     * Invoked on a request to get a credential.
     *
     * @param request the request containing the credential data to register
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun onRegisterCredentials(
        request: RegisterCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<RegisterCredentialsResponse, RegisterCredentialsException>
    )

    /** Returns true if the provider is available on this device, or otherwise false. */
    public fun isAvailable(): Boolean
}
