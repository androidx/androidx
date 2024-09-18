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
import androidx.credentials.CredentialManagerCallback
import java.util.concurrent.Executor

internal class RegistryManagerImpl(private val context: Context) : RegistryManager() {
    override fun registerCredentialsAsync(
        request: RegisterCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<RegisterCredentialsResponse, RegisterCredentialsException>
    ) {
        val provider: RegistryManagerProvider? =
            RegistryManagerProviderFactory(context).getBestAvailableProvider()
        if (provider == null) {
            callback.onError(
                RegisterCredentialsConfigurationException(
                    "registerCredentials: no provider dependencies found - please ensure " +
                        "the desired provider dependencies are added"
                )
            )
            return
        }
        provider.onRegisterCredentials(request, cancellationSignal, executor, callback)
    }
}
