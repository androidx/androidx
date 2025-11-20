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

package androidx.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor
import androidx.pdf.service.PdfDocumentServiceImpl
import androidx.pdf.service.connect.PdfSandboxHandleImpl
import androidx.pdf.service.connect.PdfServiceConnection
import androidx.pdf.service.connect.PdfServiceConnectionImpl
import androidx.pdf.utils.openFileDescriptor
import java.io.IOException
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/**
 * A [PdfLoader] implementation that opens PDF documents through a sandboxed Android service.
 *
 * This class establishes a connection with the [PdfDocumentServiceImpl] bound service and attempts
 * to load a PDF document using the [PdfRenderer]. If successful, it returns a
 * [SandboxedPdfDocument] instance, which provides a remote interface for interacting with the
 * document.
 *
 * The loading process involves:
 * 1. Establishing a connection with the service using a [PdfServiceConnection].
 * 2. Opening a [ParcelFileDescriptor] for the PDF document.
 * 3. Passing the file descriptor to the service for loading and rendering.
 * 4. Creating a [SandboxedPdfDocument] instance upon successful loading.
 *
 * @param context The [Context] required for accessing system services.
 * @param coroutineContext The [CoroutineContext] used for asynchronous operations. This context is
 *   resolved internally to ensure an appropriate [ContinuationInterceptor] is present.
 * - If the provided `coroutineContext` already contains a [ContinuationInterceptor], that
 *   interceptor will be used.
 * - If the provided `coroutineContext` does not contain a [ContinuationInterceptor],
 *   [Dispatchers.IO] will be automatically added to it to handle I/O-bound tasks such as opening
 *   file descriptors and interacting with the PDF service.
 * - Providing a [Job] in this `coroutineContext` is an error.
 *
 * @constructor Creates a new [SandboxedPdfLoader] instance.
 */
public class SandboxedPdfLoader(
    context: Context,
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : PdfLoader {
    private val context = context.applicationContext

    internal var testingConnection: PdfServiceConnection? = null

    override suspend fun openDocument(uri: Uri, password: String?): PdfDocument {
        val connection = connect(uri)

        return withContext(resolveCoroutineContext(coroutineContext)) {
            val pfd = openFileDescriptor(context, uri, "r")
            openDocumentInternal(uri, pfd, password, connection)
        }
    }

    override suspend fun openDocument(
        uri: Uri,
        fileDescriptor: ParcelFileDescriptor,
        password: String?,
    ): PdfDocument {
        val connection = connect(uri)

        return withContext(resolveCoroutineContext(coroutineContext)) {
            openDocumentInternal(uri, fileDescriptor, password, connection)
        }
    }

    private suspend fun connect(uri: Uri): PdfServiceConnection {
        val connection: PdfServiceConnection =
            testingConnection ?: PdfServiceConnectionImpl(context)
        if (!connection.isConnected) {
            connection.connect(uri)
        }
        return connection
    }

    private fun openDocumentInternal(
        uri: Uri,
        pfd: ParcelFileDescriptor,
        password: String?,
        connection: PdfServiceConnection,
    ): PdfDocument {
        val binder =
            connection.documentBinder
                ?: throw IllegalStateException(
                    "Binder interface not available for loading the document!"
                )
        val status = PdfLoadingStatus.entries[binder.openPdfDocument(pfd, password)]

        if (status != PdfLoadingStatus.SUCCESS) {
            handlePdfLoadingError(pfd, status)
        }

        return SandboxedPdfDocument(
            uri,
            connection,
            password,
            pfd,
            coroutineContext,
            binder.numPages(),
            binder.isPdfLinearized(),
            binder.getFormType(),
            annotationsProcessor = BatchPdfAnnotationsProcessor(binder),
        )
    }

    private fun handlePdfLoadingError(
        pfd: ParcelFileDescriptor,
        status: PdfLoadingStatus,
    ): Exception {
        // The PdfDocument is not created in case of any error, so close the file descriptor
        // here only to release resources and prevent leaks.
        pfd.close()

        when (status) {
            PdfLoadingStatus.WRONG_PASSWORD -> throw PdfPasswordException("Incorrect password")
            PdfLoadingStatus.PDF_ERROR -> throw IOException("Unable to process the PDF document")
            PdfLoadingStatus.LOADING_ERROR -> throw RuntimeException("Loading failed")
            else -> throw IllegalStateException("Unknown loading status: $status")
        }
    }

    public companion object {
        private fun resolveCoroutineContext(coroutineContext: CoroutineContext): CoroutineContext {
            return when {
                coroutineContext[Job] != null -> error("coroutineContext may not contain a Job")
                coroutineContext[ContinuationInterceptor] == null ->
                    coroutineContext + Dispatchers.IO
                else -> coroutineContext
            }
        }

        /**
         * Prepares sandboxing PDF resources ahead of any document operations, to reduce latency
         * during the interaction with the [SandboxedPdfLoader] or [PdfDocument].
         *
         * The returned [PdfSandboxHandle] represents a session and must be closed by the caller
         * when no longer needed.
         *
         * Calling this method is optional. Any document operation via [SandboxedPdfLoader] and
         * [PdfDocument] will initialize the resources internally on demand, but may experience
         * increased startup time.
         *
         * @param context A [Context] of component to be associated with pdf session.
         * @return A [PdfSandboxHandle] representing an active pdf session.
         * @see PdfSandboxHandle
         */
        @JvmStatic
        public fun startInitialization(context: Context): PdfSandboxHandle {
            return PdfSandboxHandleImpl(context).also { it.connect() }
        }
    }
}

/** Represents the loading status of a PDF file. */
// TODO(b/425827955): Clean up status codes and handle runtime exceptions directly
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class PdfLoadingStatus {
    SUCCESS, // The PDF was loaded successfully.
    WRONG_PASSWORD, // Incorrect password was provided for a password-protected PDF.
    PDF_ERROR, // Invalid or Corrupt pdf file was provided
    LOADING_ERROR, // A general error occurred while trying to load the PDF
    UNKNOWN,
}
