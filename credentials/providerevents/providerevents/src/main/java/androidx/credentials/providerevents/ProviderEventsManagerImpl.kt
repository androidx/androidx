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

package androidx.credentials.providerevents

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.exception.RegisterExportException
import androidx.credentials.providerevents.exception.RegisterExportProviderConfigurationException
import androidx.credentials.providerevents.internal.ProviderEventsApiProviderFactory
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import java.util.concurrent.Executor

internal class ProviderEventsManagerImpl(private val context: Context) : ProviderEventsManager {
    override fun importCredentialsAsync(
        context: Context,
        request: ImportCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<ProviderImportCredentialsResponse, ImportCredentialsException>,
    ) {
        val provider: ProviderEventsApiProvider? =
            ProviderEventsApiProviderFactory().getBestAvailableProvider(this.context)
        if (provider == null) {
            callback.onError(
                ImportCredentialsProviderConfigurationException(
                    "importCredentialsAsync no provider dependencies found - please ensure " +
                        "the desired provider dependencies are added"
                )
            )
            return
        }
        provider.onImportCredentials(context, request, cancellationSignal, executor, callback)
    }

    override fun registerExportAsync(
        request: RegisterExportRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<RegisterExportResponse, RegisterExportException>,
    ) {
        val provider: ProviderEventsApiProvider? =
            ProviderEventsApiProviderFactory().getBestAvailableProvider(context)
        if (provider == null) {
            callback.onError(
                RegisterExportProviderConfigurationException(
                    "registerCredentials: no provider dependencies found - please ensure " +
                        "the desired provider dependencies are added"
                )
            )
            return
        }
        provider.onRegisterExport(request, cancellationSignal, executor, callback)
    }
}
