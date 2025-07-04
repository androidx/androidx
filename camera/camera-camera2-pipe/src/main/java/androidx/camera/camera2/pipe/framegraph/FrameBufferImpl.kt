/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.camera.camera2.pipe.FrameBuffer
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.internal.FrameDistributor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FrameBufferImpl(
    private val frameGraphBuffers: FrameGraphBuffers,
    override val streams: Set<StreamId>,
    override val parameters: Map<Any, Any?>,
    override val capacity: Int,
) : FrameBuffer, FrameDistributor.FrameStartedListener {
    // This sealed class is used to model the two possible outcomes of a frame acquisition attempt.
    // If a frame was successfully acquired, we should close it at an appropriate time.
    private sealed class BufferEntry(val frameReference: FrameReference) {

        class WithFrame(val frame: Frame) : BufferEntry(frame)

        class WithoutFrame(reference: FrameReference) : BufferEntry(reference)
    }

    private val lock = Any()

    @GuardedBy("lock") private val frameQueue: ArrayDeque<BufferEntry> = ArrayDeque(capacity)

    @GuardedBy("lock") private var closed = false

    private val _frameFlow =
        MutableSharedFlow<FrameReference>(
            replay = 0,
            extraBufferCapacity = FRAME_FLOW_EXTRA_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val frameFlow: SharedFlow<FrameReference> = _frameFlow.asSharedFlow()

    init {
        require(capacity > 0) { "FrameBuffer capacity must be greater than 0" }
    }

    private val _size = MutableStateFlow(0)
    override val size: StateFlow<Int> = _size.asStateFlow()

    override fun onFrameStarted(frameReference: FrameReference) {
        val acquiredFrame = frameReference.tryAcquire()

        val entryToAdd: BufferEntry =
            if (acquiredFrame != null) {
                BufferEntry.WithFrame(acquiredFrame)
            } else {
                BufferEntry.WithoutFrame(frameReference)
            }

        var frameToClose: Frame? = null
        synchronized(lock) {
            if (closed) {
                // If the buffer is closed, close the acquired frame.
                if (entryToAdd is BufferEntry.WithFrame) {
                    frameToClose = entryToAdd.frame
                }
            } else {
                if (frameQueue.size == capacity) {
                    val evictedItem = frameQueue.removeFirst()
                    if (evictedItem is BufferEntry.WithFrame) {
                        frameToClose = evictedItem.frame
                    }
                }

                frameQueue.add(entryToAdd)
                _size.value = frameQueue.size
                _frameFlow.tryEmit(entryToAdd.frameReference)
            }
        }
        frameToClose?.close()
    }

    override fun removeFirstReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.removeFirstOrNull()?.let { entry ->
                _size.value = frameQueue.size
                entry.frameReference
            }
        }

    override fun removeLastReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.removeLastOrNull()?.let { entry ->
                _size.value = frameQueue.size
                entry.frameReference
            }
        }

    override fun removeAllReferences(): List<FrameReference> =
        synchronized(lock) {
            if (closed) return emptyList()
            val references = frameQueue.map { it.frameReference }
            frameQueue.clear()
            _size.value = 0
            references
        }

    override fun peekFirstReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.firstOrNull()?.frameReference
        }

    override fun peekLastReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.lastOrNull()?.frameReference
        }

    override fun peekAllReferences(): List<FrameReference> =
        synchronized(lock) {
            if (closed) return emptyList()
            frameQueue.map { it.frameReference }
        }

    override fun close() {
        val framesToClose: List<Frame>
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            framesToClose =
                frameQueue.mapNotNull { entry -> (entry as? BufferEntry.WithFrame)?.frame }
            frameQueue.clear()
            _size.value = 0
        }
        for (frame in framesToClose) {
            frame.close()
        }
        frameGraphBuffers.detach(this)
    }

    private companion object {
        const val FRAME_FLOW_EXTRA_BUFFER_CAPACITY = 4
    }
}
