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

package androidx.credentials.providerevents

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.RegisterExportException
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Provides APIs for credential providers to participate in provider events, such as same-device
 * credential transfer.
 *
 * This manager supports two primary roles for credential transfer:
 * - **Importing (Client Role):** A provider can initiate a flow to import credentials by calling
 *   [importCredentials]. This will display a UI allowing the user to select another provider to
 *   import from.
 * - **Exporting (Source Role):** A provider must first register its ability to export credentials
 *   by calling [registerExport]. Once registered, it will appear in the UI when other providers
 *   initiate an import flow.
 */
public interface ProviderEventsManager {
    public companion object {
        /**
         * Creates a [ProviderEventsManager] based on the given [context].
         *
         * @param context the context with which the ProviderEventsManager should be associated
         */
        @JvmStatic
        public fun create(context: Context): ProviderEventsManager =
            ProviderEventsManagerImpl(context)
    }

    /**
     * Initiates a flow to import credentials from another credential provider.
     *
     * This method launches a provider selector UI, allowing the user to choose a source provider
     * from a list of those registered via [registerExport]. The request is then forwarded to the
     * selected provider.
     *
     * <p>Under the hood, this API facilitates the transfer by creating a temporary file in the
     * caller's cache directory. The framework handles the file I/O, permissions, and cleanup,
     * providing a seamless transfer medium between the two providers.
     *
     * @param context the activity context required to launch the UI.
     * @param request the request detailing the credentials to be imported.
     * @return a [ProviderImportCredentialsResponse] with the imported credential data.
     * @throws ImportCredentialsException on failure, with a subclass indicating the error type.
     */
    public suspend fun importCredentials(
        context: Context,
        request: ImportCredentialsRequest,
    ): ProviderImportCredentialsResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }
        val callback =
            object :
                CredentialManagerCallback<
                    ProviderImportCredentialsResponse,
                    ImportCredentialsException,
                > {
                override fun onResult(result: ProviderImportCredentialsResponse) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onError(e: ImportCredentialsException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }

        importCredentialsAsync(
            context,
            request,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback,
        )
    }

    /**
     * Initiates a flow to import credentials from another credential provider.
     *
     * This method launches a provider selector UI, allowing the user to choose a source provider
     * from a list of those registered via [registerExport]. The request is then forwarded to the
     * selected provider.
     *
     * <p>Under the hood, this API facilitates the transfer by creating a temporary file in the
     * caller's cache directory. The framework handles the file I/O, permissions, and cleanup,
     * providing a seamless transfer medium between the two providers.
     *
     * @param context the activity context required to launch the UI.
     * @param request request the request detailing the credentials to be imported.
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun importCredentialsAsync(
        context: Context,
        request: ImportCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<ProviderImportCredentialsResponse, ImportCredentialsException>,
    )

    /**
     * Registers the calling credential provider as a source for exporting credentials.
     *
     * Once registered, the provider's [ExportEntry] will be displayed in the provider selector UI
     * when another provider initiates an [importCredentials] flow.
     *
     * <p>In addition to calling this method, a provider that wishes to export credentials must
     * declare an [Activity] in its manifest that can handle the transfer request. This activity
     * must have an intent filter configured with the following:
     * <ul>
     * <li>An intent action of "androidx.identitycredentials.action.IMPORT_CREDENTIALS"</li>
     * <li>A data element with a "content" scheme</li>
     * </ul>
     *
     * The framework will invoke this activity with an intent containing a content URI, which serves
     * as the medium for transferring the credential data. The activity can retrieve the request
     * from the intent by calling [IntentHandler.retrieveProviderImportCredentialsRequest]. In
     * addition to the [ImportCredentialsRequest], the [ProviderImportCredentialsRequest] contains a
     * few security measures. The importer's [CallingAppInfo] is provided to verify the importer.
     * The 'credId' is provided to validate the selected [ExportEntry].
     *
     * After the activity processes the request, the activity should return the
     * [ImportCredentialsResponse] through the 'uri' of [ProviderImportCredentialsRequest] to
     * successfully return the credentials or set an exception by calling
     * [IntentHandler.setImportCredentialsException].
     *
     * @param request the request containing the provider data to register
     * @return a [RegisterExportResponse] on successful registration.
     * @throws RegisterExportException if the registration fails.
     */
    public suspend fun registerExport(request: RegisterExportRequest): RegisterExportResponse =
        suspendCancellableCoroutine { continuation ->
            // Any Android API that supports cancellation should be configured to propagate
            // coroutine cancellation as follows:
            val canceller = CancellationSignal()
            continuation.invokeOnCancellation { canceller.cancel() }

            val callback =
                object :
                    CredentialManagerCallback<RegisterExportResponse, RegisterExportException> {
                    override fun onResult(result: RegisterExportResponse) {
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }

                    override fun onError(e: RegisterExportException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

            registerExportAsync(
                request,
                canceller,
                // Use a direct executor to avoid extra dispatch. Resuming the continuation will
                // handle getting to the right thread or pool via the ContinuationInterceptor.
                Runnable::run,
                callback,
            )
        }

    /**
     * Registers the calling credential provider as a source for exporting credentials.
     *
     * Once registered, the provider's [ExportEntry] will be displayed in the provider selector UI
     * when another provider initiates an [importCredentials] flow.
     *
     * <p>In addition to calling this method, a provider that wishes to export credentials must
     * declare an [Activity] in its manifest that can handle the transfer request. This activity
     * must have an intent filter configured with the following:
     * <ul>
     * <li>An intent action of "androidx.identitycredentials.action.IMPORT_CREDENTIALS"</li>
     * <li>A data element with a "content" scheme</li>
     * </ul>
     *
     * The framework will invoke this activity with an intent containing a content URI, which serves
     * as the medium for transferring the credential data. The activity can retrieve the request
     * from the intent by calling [IntentHandler.retrieveProviderImportCredentialsRequest]. In
     * addition to the [ImportCredentialsRequest], the [ProviderImportCredentialsRequest] contains a
     * few security measures. The importer's [CallingAppInfo] is provided to verify the importer.
     * The 'credId' is provided to validate the selected [ExportEntry].
     *
     * After the activity processes the request, the activity should return the
     * [ImportCredentialsResponse] through the 'uri' of [ProviderImportCredentialsRequest] to
     * successfully return the credentials or set an exception by calling
     * [IntentHandler.setImportCredentialsException].
     *
     * @param request the request containing the provider data to register
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun registerExportAsync(
        request: RegisterExportRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<RegisterExportResponse, RegisterExportException>,
    )
}
