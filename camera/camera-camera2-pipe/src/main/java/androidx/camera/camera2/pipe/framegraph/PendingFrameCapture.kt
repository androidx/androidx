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
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Placeholder for [FrameCapture] of a Frame request that may be fulfilled at a later point in time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PendingFrameCapture(override val request: Request) : FrameCapture {
    private val lock = Any()

    // The type is chosen to be null in order to allow cancellation and not throw exception when
    // waiting for it to complete. This choice is made to keep the usage of FrameCapture via
    // PendingFrameCapture clean for the clients. The status of close/abort can be always queried
    // from the status field of PendingFrameCapture.
    private val deferred = CompletableDeferred<FrameCapture?>()

    @GuardedBy("lock") private var closed = false
    @GuardedBy("lock") private var aborted = false
    @GuardedBy("lock") private var pendingListeners: MutableList<Frame.Listener>? = mutableListOf()

    fun setFrameCapture(frameCapture: FrameCapture) {
        val listeners: List<Frame.Listener>?
        synchronized(lock) {
            if (closed || aborted) {
                frameCapture.close()
                return
            }

            if (!deferred.complete(frameCapture)) {
                frameCapture.close()
                return
            }
            listeners = pendingListeners
            pendingListeners = null
        }

        if (listeners != null) {
            for (listener in listeners) {
                frameCapture.addListener(listener)
            }
        }
    }

    override val status: OutputStatus
        get() =
            synchronized(lock) {
                if (deferred.isCompleted) {
                    deferred.getCompleted()?.let {
                        return it.status
                    }
                }
                return when {
                    aborted -> OutputStatus.ERROR_OUTPUT_ABORTED
                    closed -> OutputStatus.UNAVAILABLE
                    else -> OutputStatus.PENDING
                }
            }

    override suspend fun awaitFrame(): Frame? {
        return deferred.await()?.awaitFrame()
    }

    override fun getFrame(): Frame? {
        return if (deferred.isCompleted) deferred.getCompleted()?.getFrame() else null
    }

    override fun addListener(listener: Frame.Listener) {
        val frameCapture: FrameCapture?
        synchronized(lock) {
            if (closed || aborted) {
                listener.onFrameComplete()
                return
            }
            if (!deferred.isCompleted) {
                pendingListeners?.add(listener)
                return
            }
            frameCapture = deferred.getCompleted()
        }
        // If deferred FrameCapture was completed, then attach the listener directly to the source.
        // In case it is null, the (closed || aborted) state must be true and listener will be
        // completed in the above.
        frameCapture?.addListener(listener)
    }

    override fun close() {
        terminate(asClosed = true)
    }

    fun abort() {
        terminate(asClosed = false)
    }

    private fun terminate(asClosed: Boolean) {
        val frameCapture: FrameCapture?
        val listeners: List<Frame.Listener>?
        synchronized(lock) {
            if (closed || aborted) return
            if (asClosed) closed = true else aborted = true

            // Complete the deferred with null so that awaitFrame method doesn't block when this
            // PendingFrameCapture is closed before the actual FrameCapture is set.
            deferred.complete(null)

            listeners = pendingListeners.also { pendingListeners = null }
            frameCapture = deferred.getCompleted()
        }
        if (listeners != null) {
            for (listener in listeners) {
                listener.onFrameComplete()
            }
        }
        frameCapture?.close()
    }
}
