/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.framegraph

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.config.FrameGraphCoroutineScope
import androidx.camera.camera2.pipe.config.FrameGraphScope
import androidx.camera.camera2.pipe.graph.GraphProcessor
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.GraphSessionLock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * This manages the submission of [FrameCapture] requests for the
 * [androidx.camera.camera2.pipe.FrameGraph]. Frame capture requests on FrameGraph are non
 * suspending and take lower priority over an active session.
 *
 * It handles Frame capture requests by immediately returning a [PendingFrameCapture]. The pending
 * results are later fulfilled by attempting to batch and submit requests to the [GraphProcessor]
 * once [SessionLock] is available. This is done on [frameGraphCoroutineScope].
 */
@FrameGraphScope
internal class FrameGraphFrameCaptureQueue
@Inject
constructor(
    private val frameCaptureQueue: FrameCaptureQueue,
    private val graphProcessor: GraphProcessor,
    private val sessionLock: GraphSessionLock,
    @FrameGraphCoroutineScope private val frameGraphCoroutineScope: CoroutineScope,
) : AutoCloseable {
    private val lock = Any()

    @GuardedBy("lock") private var dirty = false
    @GuardedBy("lock") private var closed = false

    @GuardedBy("lock") private val requestBuffer = mutableListOf<PendingFrameCapture>()

    fun enqueue(request: Request): FrameCapture {
        return enqueue(listOf(request)).first()
    }

    fun enqueue(requests: List<Request>): List<FrameCapture> {
        val pendingFrameCaptures = requests.map { PendingFrameCapture(it) }
        var shouldAbort = false
        val triggerUpdate =
            synchronized(lock) {
                if (closed) {
                    shouldAbort = true
                    false
                } else {
                    requestBuffer.addAll(pendingFrameCaptures)
                    if (!dirty) {
                        dirty = true
                        true
                    } else {
                        false
                    }
                }
            }

        if (triggerUpdate) {
            applyUpdate()
        } else if (shouldAbort) {
            pendingFrameCaptures.abortAll()
        }

        return pendingFrameCaptures
    }

    // Assuming that the frameGraphCoroutine is canceled after we close this
    // FrameGraphFrameCaptureQueue we shouldn't need to worry about any PendingFrameCapture(s) never
    // completing. If there is a possibility of close() never being called and only the scope is
    // canceled, we should keep draining the queue until we have completed/aborted all the
    // PendingFrameCapture(s).
    private fun applyUpdate() {
        sessionLock.withTokenIn(frameGraphCoroutineScope) {
            var success = false
            var pendingFrameCaptures: List<PendingFrameCapture>? = null
            var isClosed = false
            try {
                synchronized(lock) {
                    isClosed = closed
                    if (requestBuffer.isNotEmpty()) {
                        pendingFrameCaptures = ArrayList(requestBuffer)
                        requestBuffer.clear()
                    }
                    dirty = false
                }

                if (isClosed) {
                    pendingFrameCaptures?.abortAll()
                    return@withTokenIn
                }

                if (pendingFrameCaptures.isNullOrEmpty()) {
                    return@withTokenIn
                }

                flush(pendingFrameCaptures)

                success = true
            } finally {
                // Completing a PendingFrameCapture is crucial so that the clients do not get stuck
                // waiting for a FrameCapture. This is a defensive block to handle the
                // CancellationException thrown if the frameServerCoroutineScope is canceled or due
                // to any reason the above code block fails.
                if (!success) {
                    pendingFrameCaptures?.abortAll()
                }
            }
        }
    }

    override fun close() {
        val remaining =
            synchronized(lock) {
                if (closed) return
                closed = true
                ArrayList(requestBuffer).also { requestBuffer.clear() }
            }
        remaining.abortAll()
    }

    /**
     * Sends the list of [PendingFrameCapture] to be submitted to the camera device. This is done by
     * extracting the list of underlying [Request]s and then queuing these requests to the
     * [frameCaptureQueue]. We also need to send these requests to the [graphProcessor].
     * [frameCaptureQueue] returns the list of [FrameCapture]s right away, and we fill in those to
     * the corresponding [PendingFrameCapture]. CameraGraph then internally populates the
     * [FrameCapture] objects with the appropriate camera artifacts as they become available at a
     * future point in time.
     */
    private fun flush(pendingFrameCaptures: List<PendingFrameCapture>) {
        val requestList = pendingFrameCaptures.map { it.request }
        val frameCaptures = frameCaptureQueue.enqueue(requestList)

        for (i in requestList.indices) {
            if (graphProcessor.submit(requestList[i])) {
                pendingFrameCaptures[i].setFrameCapture(frameCaptures[i])
            } else {
                frameCaptures[i].close()
                pendingFrameCaptures[i].abort()
            }
        }
    }

    private fun List<PendingFrameCapture>.abortAll() {
        for (pendingFrameCapture in this) {
            pendingFrameCapture.abort()
        }
    }
}
