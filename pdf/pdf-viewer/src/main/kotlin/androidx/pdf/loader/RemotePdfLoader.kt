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

package androidx.pdf.loader

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.pdf.data.PdfConnectionStatus
import androidx.pdf.data.PdfLoadingStatus
import androidx.pdf.exceptions.PdfPasswordException
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * A loader that facilitates opening remote PDF documents using a bound service.
 *
 * This loader manages the connection to a `PdfDocumentService` and handles the process of opening a
 * PDF document from a URI, potentially with a password.
 *
 * @param context The Android context for binding the service.
 * @param dispatcher The dispatcher to use for coroutine operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RemotePdfLoader(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) : PdfLoader {
    /**
     * Opens a PDF document from the specified [Uri]. Currently, only content URIs (content://) are
     * supported.
     *
     * @param uri The URI of the PDF document to open.
     * @param password (Optional) The password to unlock the document if it is encrypted.
     * @return The opened [PdfDocument].
     * @throws [PdfPasswordException] If the provided password is incorrect.
     * @throws [IOException] If an error occurs while opening the document.
     * @throws [IllegalStateException] For unknown loading status codes.
     * @throws [RemoteException] For communication errors with the service.
     */
    override suspend fun openDocument(uri: Uri, password: String?): PdfDocument =
        withContext(dispatcher) {
            check(uri.scheme == ContentResolver.SCHEME_CONTENT) {
                "Only content:// URIs are supported"
            }
            val connection = PdfConnection(context)
            connection.connect(uri)
            // Wait till remote binder object is ready
            connection.serviceConnectedFlow.first { it == PdfConnectionStatus.CONNECTED }
            openDocument(uri, password, connection)
        }

    /**
     * Opens a PDF document using an established connection to the `PdfDocumentService`.
     *
     * This overload is used internally after the service connection is ready.
     *
     * @param uri The URI of the PDF document to open.
     * @param password The password to unlock the document (if encrypted).
     * @param connection The established connection to the `PdfDocumentService`.
     * @return The opened `PdfDocument`.
     * @throws [PdfPasswordException] If the password is incorrect.
     * @throws [IOException] If an error occurs during opening.
     * @throws [IllegalStateException] For unknown loading status codes.
     * @throws [RemoteException] For communication errors with the service.
     */
    private suspend fun openDocument(
        uri: Uri,
        password: String?,
        connection: PdfConnection
    ): PdfDocument {
        val pdfRemote = connection.pdfDocumentProvider
        val pfd =
            context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IOException("Failed to open PDF file")
        when (val result = pdfRemote.openPdfDocument(pfd, password)) {
            PdfLoadingStatus.SUCCESS.ordinal -> {
                val remotePdf =
                    RemotePdfDocument(
                        connection,
                        dispatcher,
                        pfd,
                        password,
                        pdfRemote.numPages(),
                        pdfRemote.isPdfLinearized(),
                        pdfRemote.getFormType()
                    )
                return remotePdf
            }
            PdfLoadingStatus.WRONG_PASSWORD.ordinal ->
                throw PdfPasswordException("Incorrect password")
            PdfLoadingStatus.PDF_ERROR.ordinal -> throw IOException("INVALID PDF FILE")
            PdfLoadingStatus.LOADING_ERROR.ordinal -> throw RuntimeException("Loading failed")
            else ->
                throw IllegalStateException(
                    "Unknown loading status: $result"
                ) // should never reach here
        }
    }
}
