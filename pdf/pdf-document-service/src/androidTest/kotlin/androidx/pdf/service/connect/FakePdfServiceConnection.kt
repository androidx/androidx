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
import android.net.Uri
import android.os.IBinder
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.adapter.PdfDocumentRendererFactoryImpl
import androidx.pdf.service.PdfDocumentRemoteImpl
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.Job

class FakePdfServiceConnection(
    override val context: Context,
    override val isConnected: Boolean,
    override var documentBinder: PdfDocumentRemote? = null,
    override var needsToReopenDocument: Boolean = false,
    private val onServiceConnected: () -> Unit = {},
) : PdfServiceConnection {

    override val pendingJobs: Queue<Job> = ConcurrentLinkedQueue()

    override suspend fun connect(uri: Uri) {
        documentBinder = PdfDocumentRemoteImpl(PdfDocumentRendererFactoryImpl())
        onServiceConnected(null, null)
    }

    override fun disconnect() {
        documentBinder?.closePdfDocument()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        onServiceConnected()
    }

    override fun onServiceDisconnected(name: ComponentName?) {}
}
