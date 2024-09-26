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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.pdf.data.PdfConnectionStatus
import androidx.pdf.loader.service.PdfDocumentService
import androidx.pdf.models.PdfDocumentProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the connection between your app and the PDF rendering service.
 *
 * Key responsibilities:
 * * Establishes and controls the lifecycle of the service connection.
 * * Provides access to the [PdfDocumentProvider] interface to interact with the service.
 * * Emits signals through a [SharedFlow] to notify when the service is connected.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfConnection internal constructor(private val context: Context) :
    PdfServiceConnection {
    private lateinit var remoteDocumentService: PdfDocumentProvider
    private var connected = false
    private val _serviceConnectedFlow = MutableStateFlow(PdfConnectionStatus.DISCONNECTED)
    override val serviceConnectedFlow: StateFlow<PdfConnectionStatus> =
        _serviceConnectedFlow.asStateFlow()

    /**
     * Obtains a [PdfDocumentProvider] if the service is bound. Note that the document might still
     * be loading.
     */
    override val pdfDocumentProvider: PdfDocumentProvider
        get() =
            if (::remoteDocumentService.isInitialized) {
                remoteDocumentService
            } else {
                throw IllegalStateException("Could not connect to service")
            }

    /** Called when the service is successfully connected. */
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        connected = true
        remoteDocumentService = PdfDocumentProvider.Stub.asInterface(binder)
        // Used by RemotePdfDocument to try reloading the document
        _serviceConnectedFlow.tryEmit(PdfConnectionStatus.CONNECTED)
    }

    /** Called when the service is unexpectedly disconnected. */
    override fun onServiceDisconnected(name: ComponentName?) {
        _serviceConnectedFlow.tryEmit(PdfConnectionStatus.DISCONNECTED)
    }

    /** Disconnects from the service and closes the associated PDF document. */
    override fun disconnect() {
        if (connected) {
            context.unbindService(this)
            connected = false
            remoteDocumentService.closePdfDocument()
        }
    }

    /**
     * Initiates a connection to the PDF service for the specified URI.
     *
     * @param uri The URI of the PDF document to connect to.
     */
    override fun connect(uri: Uri) {
        if (connected) {
            return
        }

        val intent = Intent(context, PdfDocumentService::class.java)
        context.bindIsolatedService(
            intent,
            Context.BIND_AUTO_CREATE,
            uri.toString(),
            context.mainExecutor,
            this
        )
    }
}
