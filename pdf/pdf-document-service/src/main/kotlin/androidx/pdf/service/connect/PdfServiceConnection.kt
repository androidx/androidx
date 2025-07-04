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

import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocumentRemote
import java.util.Queue
import kotlinx.coroutines.Job

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PdfServiceConnection : ServiceConnection {
    /**
     * The [Context] used for binding and unbinding the service
     *
     * Implementations should guarantee this is the application [Context] and not a UI [Context]
     */
    public val context: Context
    /** True if the service is actively bound */
    public val isConnected: Boolean

    /**
     * True if the document needs to be reopened. This is expected to be set when the service
     * connection is re-established after unexpected disconnection.
     */
    public var needsToReopenDocument: Boolean

    /** The [PdfDocumentRemote] instance, if the service is actively bound */
    public val documentBinder: PdfDocumentRemote?

    /**
     * Queue for all the job that are working with document. This does not enforce FIFO executing of
     * the task, but rather works as a list with safe concurrent modifications. see
     * [java.util.concurrent.ConcurrentLinkedQueue]
     */
    public val pendingJobs: Queue<Job>

    /** Initiates binding to the service, and suspends until the service is bound */
    public suspend fun connect(uri: Uri)

    /** Immediately unbinds the service */
    public fun disconnect()
}

@RestrictTo(RestrictTo.Scope.LIBRARY) public sealed class ConnectionState

@RestrictTo(RestrictTo.Scope.LIBRARY) public object Disconnected : ConnectionState()

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Connected(public val document: PdfDocumentRemote) : ConnectionState()
