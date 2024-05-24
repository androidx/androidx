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

package androidx.camera.camera2.pipe.internal

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithFailure
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithOutput
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputOrNull
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputStatus
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * FrameCaptureQueue manages the list of requests that are expected to produce a [Frame] that needs
 * to be returned when the [Frame] that matches the [Request] is started.
 */
@CameraGraphScope
internal class FrameCaptureQueue @Inject constructor() : AutoCloseable {
    private val lock = Any()

    @GuardedBy("lock") private val queue = ArrayDeque<FrameCaptureImpl>()

    @GuardedBy("lock") private var closed = false

    fun remove(request: Request): FrameCaptureImpl? =
        synchronized(lock) {
            if (closed) return null

            // If an item matching this request exists, remove it from the queue and return it.
            queue.firstOrNull { it.request == request }?.also { queue.remove(it) }
        }

    /**
     * Tell the [FrameDistributor] that a specific request will be submitted to the camera and
     * create a placeholder that will be completed when that specific request starts exposing.
     */
    fun enqueue(request: Request): FrameCaptureImpl =
        synchronized(lock) {
            FrameCaptureImpl(request).also {
                if (!closed) {
                    queue.add(it)
                } else {
                    it.close()
                }
            }
        }

    /**
     * Tell the [FrameDistributor] that a specific list of requests will be submitted to the camera
     * and to create placeholders.
     */
    fun enqueue(requests: List<Request>): List<FrameCapture> =
        synchronized(lock) {
            requests
                .map { FrameCaptureImpl(it) }
                .also {
                    if (!closed) {
                        queue.addAll(it)
                    } else {
                        for (result in it) {
                            result.close()
                        }
                    }
                }
        }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }

        // Note: This happens outside the synchronized block, but is safe since all modifications
        // above happen within the synchronized block, and all modifications check the closed value
        // before modifying the list.
        for (pendingOutputFrame in queue) {

            // Any pending frame in the queue is guaranteed to not hold a real result.
            pendingOutputFrame.completeWithFailure(OutputStatus.ERROR_OUTPUT_ABORTED)
        }
        queue.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal inner class FrameCaptureImpl(override val request: Request) : FrameCapture {
        private val closed = atomic(false)
        private val result = CompletableDeferred<OutputResult<Frame>>()

        @GuardedBy("this")
        private var frameListeners: MutableList<Frame.Listener>? = mutableListOf()

        /** Complete this [FrameCapture] with the provide [Frame]. */
        fun completeWith(frame: Frame) {
            if (!result.completeWithOutput(frame)) {
                // Close the frame if the result was non-null and we failed to complete this frame
                frame.close()
            } else {
                val listeners: List<Frame.Listener>?
                synchronized(this) {
                    listeners = frameListeners
                    frameListeners = null
                }

                if (listeners != null) {
                    for (i in listeners.indices) {
                        frame.addListener(listeners[i])
                    }
                }
            }
        }

        /** Cancel this [FrameCapture] with a specific [OutputStatus]. */
        fun completeWithFailure(failureStatus: OutputStatus) {
            if (result.completeWithFailure(failureStatus)) {
                val listeners: List<Frame.Listener>?
                synchronized(this) {
                    listeners = frameListeners
                    frameListeners = null
                }

                // Ensure listeners always receive the onFrameCompleted event, since it will not be
                // attached to a real frame.
                if (listeners != null) {
                    for (i in listeners.indices) {
                        listeners[i].onFrameComplete()
                    }
                }
            }
        }

        override fun getFrame(): Frame? {
            if (closed.value) return null
            return result.outputOrNull()?.tryAcquire()
        }

        override val status: OutputStatus
            get() {
                if (closed.value) return OutputStatus.UNAVAILABLE
                return result.outputStatus()
            }

        override suspend fun awaitFrame(): Frame? {
            if (closed.value) return null
            return result.await().output?.tryAcquire()
        }

        override fun addListener(listener: Frame.Listener) {
            val success = synchronized(this) { frameListeners?.add(listener) == true }
            // If the list of listeners is null, then we've already completed this deferred output
            // frame.
            if (!success) {
                val frame = result.outputOrNull()
                if (frame != null) {
                    frame.addListener(listener)
                } else {
                    listener.onFrameComplete()
                }
            }
        }

        override fun close() {
            if (closed.compareAndSet(expect = false, update = true)) {
                completeWithFailure(OutputStatus.UNAVAILABLE)
                result.outputOrNull()?.close()

                // We should close all of the object if we successfully remove it from the list.
                // Otherwise, this operation is a no-op.
                synchronized(lock) { queue.remove(this) }
            }
        }
    }
}
