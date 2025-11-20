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
import androidx.credentials.providerevents.exception.RegisterExportException
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import java.util.concurrent.Executor

/** Provider interface to be implemented by the OEMs that will support provider events APIs. */
public interface ProviderEventsApiProvider {
    /** Returns true if the provider is available on this device, or otherwise false. */
    public fun isAvailable(): Boolean

    /**
     * Invoked on a request to import credentials.
     *
     * @param context the client calling context used to potentially launch any UI
     * @param request the request for importing the credentials
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun onImportCredentials(
        context: Context,
        request: ImportCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<ProviderImportCredentialsResponse, ImportCredentialsException>,
    )

    /**
     * Invoked on a request to register accounts capable for exporting credentials.
     *
     * @param request the request for registering export entries
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun onRegisterExport(
        request: RegisterExportRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<RegisterExportResponse, RegisterExportException>,
    )
}
