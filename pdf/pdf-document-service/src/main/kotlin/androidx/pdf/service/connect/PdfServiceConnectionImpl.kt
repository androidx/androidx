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

package androidx.pdf.service.connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.service.PdfDocumentServiceImpl
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfServiceConnectionImpl(override val context: Context) : PdfServiceConnection {
    private val _eventStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(Disconnected)

    override val pendingJobs: Queue<Job> = ConcurrentLinkedQueue()

    private val isProcessing: Boolean
        get() = pendingJobs.any { it.isActive }

    override var needsToReopenDocument: Boolean = false

    override val isConnected: Boolean
        get() = _eventStateFlow.value is Connected

    override val documentBinder: PdfDocumentRemote?
        get() = (_eventStateFlow.value as? Connected)?.document

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val documentRemote = PdfDocumentRemote.Stub.asInterface(service)
        _eventStateFlow.update { Connected(documentRemote) }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        needsToReopenDocument = true
        _eventStateFlow.update { Disconnected }

        // By this time, the system has disconnected the service. If we do not call unbind, then
        // it will try to restart the service immediately. If no processing is active, we can unbind
        // to save resources. Android system, otherwise, penalizes service-restarts by delaying
        // them.
        if (!isProcessing) disconnect()
    }

    override suspend fun connect(uri: Uri) {
        val intent =
            Intent(context, PdfDocumentServiceImpl::class.java).apply {
                // Providing a different Intent to the Service per document is required to obtain a
                // different IBinder channel per document. The data here serves no other purpose.
                // See b/380140417
                data = uri
            }
        context.bindService(intent, /* conn= */ this, /* flags= */ Context.BIND_AUTO_CREATE)
        _eventStateFlow.first { it is Connected }
    }

    override fun disconnect() {
        if (isConnected) {
            // Page releases are unnecessary after document closure; resources are released
            // automatically server-side. Attempting a release on a closed document will result in
            // an exception. To prevent such release calls, the connection is marked as disconnected
            // before closing the document.
            _eventStateFlow.update { Disconnected }

            documentBinder?.closePdfDocument()
            context.unbindService(this)
        }
    }
}
