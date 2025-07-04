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

package androidx.pdf.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocumentRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY)
/**
 * A [Service] that provides functionality to interact with PDF document.
 *
 * This service runs in the background. It exposes a [PdfDocumentRemote] interface for clients to
 * interact with, enabling them to access and render PDF documents. The service uses [PdfRenderer]
 * and [PdfRendererPreV] internally to process the documents based on the Android OS version.
 *
 * Clients can bind to this service to perform operations such as rendering pages, extracting text,
 * and accessing document metadata.
 */
public class PdfDocumentServiceImpl : Service() {

    /** Counter representing the number of clients bound to the service currently. */
    private var clientCount = 0

    /**
     * Coroutine job for scheduling the stop of the service. This will be scheduled once all client
     * unbinds from the service.
     */
    private var stopSelfJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var lastStartId = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Save the startId so we can safely stop the service later using stopSelfResult(startId).
        // This ensures we only stop the service if it's the same instance that was originally
        // started, preventing accidental shutdown if the service was restarted in the meantime.
        lastStartId = startId
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        clientCount++
        cancelStopSelfJob()
        return PdfDocumentRemoteImpl()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clientCount--
        if (clientCount <= 0) {
            scheduleStopSelfJob()
            // Defensive approach, if ever onUnbind called more than onBind
            clientCount = 0
        }
        return super.onUnbind(intent)
    }

    private fun scheduleStopSelfJob() {
        stopSelfJob =
            mainScope.launch {
                delay(KEEP_ALIVE_MILLIS)
                if (clientCount <= 0) stopSelfResult(lastStartId)
            }
    }

    private fun cancelStopSelfJob() {
        stopSelfJob?.cancel()
        stopSelfJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelStopSelfJob()
    }

    internal companion object {
        private const val KEEP_ALIVE_MILLIS = 30_000L
    }
}
