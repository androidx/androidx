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

import android.content.ServiceConnection
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.pdf.data.PdfConnectionStatus
import androidx.pdf.models.PdfDocumentProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * Defines the contract for managing connections to a PDF service.
 *
 * Classes implementing this interface are responsible for:
 * * Establishing and maintaining connections to the PDF service.
 * * Providing access to the [PdfDocumentProvider] to interact with the service.
 * * Signaling successful service connection through the [serviceConnectedFlow].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface PdfServiceConnection : ServiceConnection {

    /**
     * A [StateFlow] emitting signals whenever the service connection is established. This can be
     * used to trigger actions, such as initial loading/reloading a PDF document.
     */
    val serviceConnectedFlow: StateFlow<PdfConnectionStatus>

    /**
     * Obtains a [PdfDocumentProvider] if the service is bound. Note that the document might still
     * be loading.
     */
    val pdfDocumentProvider: PdfDocumentProvider

    /** Disconnects from the service and closes the associated PDF document. */
    fun disconnect()

    /**
     * Initiates a connection to the PDF service for the specified URI.
     *
     * @param uri The URI of the PDF document to connect to.
     */
    fun connect(uri: Uri)
}
