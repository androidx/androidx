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

package androidx.credentials.providerevents.playservices.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.os.BundleCompat.getParcelable
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.providerevents.IntentHandler
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.ImportCredentialsSystemErrorException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.credentials.providerevents.internal.UriUtils.Companion.generateCredentialTransferFile
import androidx.credentials.providerevents.playservices.HiddenActivity
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.EXCEPTION_MESSAGE_TAG
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.EXTRA_CREDENTIAL_TRANSFER_INTENT
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.EXTRA_RESULT_RECEIVER
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.FAILURE_RESPONSE_TAG
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.RESULT_DATA_TAG
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.io.File
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.O)
internal class ImportCredentialsController(
    val context: Context,
    val request: ImportCredentialsRequest,
    val cancellationSignal: CancellationSignal?,
    val executor: Executor,
    val callback:
        CredentialManagerCallback<ProviderImportCredentialsResponse, ImportCredentialsException>,
) {
    private lateinit var uri: Uri
    private lateinit var tempFile: File
    private val resultReceiver =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (maybeReportErrorFromResultReceiver(resultData)) {
                    return
                }
                handleImportCredentialsResponse(
                    resultCode,
                    getParcelable(resultData, RESULT_DATA_TAG, Intent::class.java),
                )
            }
        }

    fun invokePlayServices() {
        Log.d(TAG, "Starting import credentials flow")
        val client = IdentityCredentialManager.getClient(context)
        val tryCreateFile = generateCredentialTransferFile(context)
        if (tryCreateFile == null) {
            callback.onError(
                ImportCredentialsSystemErrorException(
                    "Import failed because the file transfer medium failed to be set-up."
                )
            )
            return
        }
        tempFile = tryCreateFile
        uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, tempFile)
        val task = client.importCredentials(convertToPlayServicesRequest(request, uri))
        task.addOnSuccessListener {
            Log.d(TAG, "success import pending intent, starting UI")
            val intent = Intent(context, HiddenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            intent.putExtra(
                EXTRA_IMPORT_CREDENTIALS_REQUEST,
                ImportCredentialsRequest.toBundle(request),
            )
            intent.putExtra(EXTRA_RESULT_RECEIVER, toIpcFriendlyResultReceiver(resultReceiver))
            intent.putExtra(EXTRA_CREDENTIAL_TRANSFER_INTENT, it.pendingIntent)
            context.grantUriPermission(
                it.pendingIntent.creatorPackage,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            context.startActivity(intent)
        }
        task.addOnFailureListener {
            Log.d(TAG, "Failed to retrieve the pending intent to start UI")
            try {
                tempFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Caught exception while trying to clean up the transfer medium.", e)
            }
            callback.onError(
                ImportCredentialsUnknownErrorException(
                    "Pending intent could not be retrieved to start the UI"
                )
            )
        }
    }

    private fun handleImportCredentialsResponse(resultCode: Int, data: Intent?) {
        if (
            maybeReportErrorResultCode(
                resultCode,
                { s, f -> cancelOrCallbackExceptionOrResult(s, f) },
                { e -> cleanUpAndReportError(e) },
                cancellationSignal,
                data,
            )
        ) {
            return
        }
        if (data == null) {
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                cleanUpAndReportError(
                    ImportCredentialsUnknownErrorException("No provider data returned.")
                )
            }
            return
        }
        val providerException = IntentHandler.retrieveImportCredentialsException(data)
        if (providerException != null) {
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                cleanUpAndReportError(providerException)
            }
            return
        }
        val response = IntentHandler.retrieveProviderImportCredentialsResponse(context, data, uri)
        if (response == null) {
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                cleanUpAndReportError(
                    ImportCredentialsUnknownErrorException("No provider data returned")
                )
            }
            return
        }
        cancelOrCallbackExceptionOrResult(cancellationSignal) { cleanUpAndReportResponse(response) }
    }

    private fun maybeReportErrorFromResultReceiver(resultData: Bundle): Boolean {
        val isError = resultData.getBoolean(FAILURE_RESPONSE_TAG)
        if (!isError) {
            return false
        }
        val errMsg = resultData.getString(EXCEPTION_MESSAGE_TAG)
        val exception = ImportCredentialsUnknownErrorException(errMsg)
        cancelOrCallbackExceptionOrResult(
            cancellationSignal = cancellationSignal,
            onResultOrException = { cleanUpAndReportError(exception) },
        )
        return true
    }

    private fun cleanUpAndReportError(e: ImportCredentialsException) {
        try {
            context.revokeUriPermission(
                GMS_PACKAGE_NAME,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Caught exception while trying to clean up the transfer medium.", e)
        }
        executor.execute { callback.onError(e) }
    }

    private fun cleanUpAndReportResponse(response: ProviderImportCredentialsResponse) {
        try {
            context.revokeUriPermission(
                GMS_PACKAGE_NAME,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            tempFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Caught exception while trying to clean up the transfer medium.", e)
        }
        executor.execute { callback.onResult(response) }
    }

    internal companion object {
        const val TAG = "ImportCredentialsController"
        const val EXTRA_IMPORT_CREDENTIALS_REQUEST: String =
            "androidx.credentials.import.IMPORT_CREDENTIALS_REQUEST"
        const val GMS_PACKAGE_NAME = "com.google.android.gms"
        const val AUTHORITY_SUFFIX = ".importexport.provider"

        fun cancellationReviewer(cancellationSignal: CancellationSignal?): Boolean {
            if (cancellationSignal != null) {
                if (cancellationSignal.isCanceled) {
                    Log.d(TAG, "the flow has been canceled")
                    return true
                }
            } else {
                Log.d(TAG, "No cancellationSignal found")
            }
            return false
        }

        fun cancelOrCallbackExceptionOrResult(
            cancellationSignal: CancellationSignal?,
            onResultOrException: () -> Unit,
        ) {
            if (cancellationReviewer(cancellationSignal)) {
                return
            }
            onResultOrException()
        }

        fun maybeReportErrorResultCode(
            resultCode: Int,
            cancelOnError: (CancellationSignal?, () -> Unit) -> Unit,
            onError: (ImportCredentialsException) -> Unit,
            cancellationSignal: CancellationSignal?,
            data: Intent?,
        ): Boolean {
            if (resultCode != Activity.RESULT_OK) {
                var exception: ImportCredentialsException =
                    when (resultCode) {
                        Activity.RESULT_CANCELED ->
                            ImportCredentialsCancellationException(
                                "activity is cancelled by the user."
                            )
                        else ->
                            ImportCredentialsUnknownErrorException(
                                generateErrorStringUnknown(resultCode)
                            )
                    }
                data?.let {
                    IntentHandler.retrieveImportCredentialsException(it)?.let { intentException ->
                        exception = intentException
                    }
                }
                cancelOnError(cancellationSignal) { onError(exception) }
                return true
            }
            return false
        }

        fun <T : ResultReceiver?> toIpcFriendlyResultReceiver(resultReceiver: T): ResultReceiver? {
            val parcel: Parcel = Parcel.obtain()
            resultReceiver!!.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val ipcFriendly = ResultReceiver.CREATOR.createFromParcel(parcel)
            parcel.recycle()
            return ipcFriendly
        }

        fun convertToPlayServicesRequest(
            request: ImportCredentialsRequest,
            uri: Uri,
        ): com.google.android.gms.identitycredentials.ImportCredentialsRequest {
            return com.google.android.gms.identitycredentials.ImportCredentialsRequest(
                request.requestJson,
                uri,
            )
        }

        fun generateErrorStringUnknown(resultCode: Int): String {
            return "activity with result code: $resultCode indicating not RESULT_OK"
        }
    }
}
