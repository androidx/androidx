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
import androidx.camera.camera2.pipe.internal.FrameImpl
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
    capacity: Int,
) : FrameBuffer, FrameDistributor.FrameStartedListener {
    // This sealed class is used to model the two possible outcomes of a frame acquisition attempt.
    // If a frame was successfully acquired, we should close it at an appropriate time.
    private sealed class BufferEntry(val frameReference: FrameReference) {

        class WithFrame(val frame: Frame) : BufferEntry(frame)

        class WithoutFrame(reference: FrameReference) : BufferEntry(reference)
    }

    private val lock = Any()

    @GuardedBy("lock") private var frameQueue: ArrayDeque<BufferEntry> = ArrayDeque(capacity)

    @GuardedBy("lock") private var closed = false

    private val _frameFlow =
        MutableSharedFlow<FrameReference>(
            replay = 0,
            extraBufferCapacity = FRAME_FLOW_EXTRA_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val frameFlow: SharedFlow<FrameReference> = _frameFlow.asSharedFlow()

    init {
        require(capacity >= 0) { "FrameBuffer capacity must be greater than or equal to 0" }
    }

    private val _size = MutableStateFlow(0)
    override val size: StateFlow<Int> = _size.asStateFlow()

    override var capacity: Int = capacity
        set(newCapacity) {
            require(newCapacity >= 0) { "Capacity cannot be negative" }

            val framesToClose: MutableList<Frame> = mutableListOf()
            synchronized(lock) {
                if (closed) {
                    return
                }

                val previousCapacity = field

                if (newCapacity == previousCapacity) return

                field = newCapacity

                val currentSize = frameQueue.size
                if (newCapacity < currentSize) {
                    val numToTrim = currentSize - newCapacity
                    repeat(numToTrim) {
                        val evictedItem = frameQueue.removeFirst()
                        (evictedItem as? BufferEntry.WithFrame)?.let { framesToClose.add(it.frame) }
                    }
                    // Trim the memory of the ArrayDeque to fit the new smaller capacity
                    val newFrameQueue: ArrayDeque<BufferEntry> = ArrayDeque(frameQueue)
                    frameQueue = newFrameQueue
                }
                _size.value = frameQueue.size
            }

            framesToClose.forEach { it.close() }
        }

    override fun onFrameStarted(frameReference: FrameReference) {
        // If capacity is 0, emit the reference and exit early.
        if (capacity == 0) {
            synchronized(lock) {
                if (!closed) {
                    _frameFlow.tryEmit(frameReference)
                }
            }
            return
        }

        val acquiredFrameForInternalUse = frameReference.tryAcquireForInternalUse()

        val entryToAdd: BufferEntry =
            if (acquiredFrameForInternalUse != null) {
                BufferEntry.WithFrame(acquiredFrameForInternalUse)
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

    @Deprecated("Use removeFirst instead.")
    override fun removeFirstReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.removeFirstOrNull()?.let { entry ->
                _size.value = frameQueue.size
                entry.frameReference
            }
        }

    @Deprecated("Use removeLast instead.")
    override fun removeLastReference(): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            frameQueue.removeLastOrNull()?.let { entry ->
                _size.value = frameQueue.size
                entry.frameReference
            }
        }

    @Deprecated("Use removeAll instead")
    override fun removeAllReferences(): List<FrameReference> =
        synchronized(lock) {
            if (closed) return emptyList()
            val references = frameQueue.map { it.frameReference }
            frameQueue.clear()
            _size.value = 0
            references
        }

    @Deprecated("Use removeFirst instead")
    override fun removeFirstFrameReferenceAndAcquire(
        predicate: (FrameReference) -> Boolean
    ): Frame? = removeAndAcquire(predicate, reversed = false)

    @Deprecated("Use removeLast instead")
    override fun removeLastFrameReferenceAndAcquire(
        predicate: (FrameReference) -> Boolean
    ): Frame? = removeAndAcquire(predicate, reversed = true)

    @Deprecated("Use removeAll instead")
    override fun removeAllFrameReferencesAndAcquire(
        predicate: (FrameReference) -> Boolean
    ): List<Frame> =
        synchronized(lock) {
            if (closed) return emptyList()

            val iterator = frameQueue.iterator()

            val removedFrames = mutableListOf<Frame>()
            while (iterator.hasNext()) {
                val bufferEntry = iterator.next()
                if (predicate(bufferEntry.frameReference)) {
                    iterator.remove()
                    bufferEntry.tryAcquireFrameForExternalUse()?.let { frame ->
                        removedFrames.add(frame)
                    }
                }
            }
            _size.value = frameQueue.size
            return removedFrames
        }

    override fun removeFirst(predicate: ((FrameReference) -> Boolean)?): Frame? =
        removeAndAcquire(predicate ?: { true }, reversed = false)

    override fun removeLast(predicate: ((FrameReference) -> Boolean)?): Frame? =
        removeAndAcquire(predicate ?: { true }, reversed = true)

    override fun removeAll(predicate: ((FrameReference) -> Boolean)?): List<Frame> =
        synchronized(lock) {
            if (closed || frameQueue.isEmpty()) return emptyList()

            if (predicate == null) {
                return clearQueueAndExtractFrames()
            }

            val removedFrames = buildList {
                val iterator = frameQueue.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (predicate(entry.frameReference)) {
                        iterator.remove()
                        if (entry is BufferEntry.WithFrame) {
                            add(entry.frame)
                        }
                    }
                }
            }
            _size.value = frameQueue.size
            return removedFrames
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

    fun trimAll() {
        val framesToClose =
            synchronized(lock) {
                if (closed) return
                clearQueueAndExtractFrames()
            }
        for (frame in framesToClose) {
            frame.close()
        }
    }

    @GuardedBy("lock")
    private fun clearQueueAndExtractFrames(): List<Frame> {
        val frames = frameQueue.mapNotNull { (it as? BufferEntry.WithFrame)?.frame }
        frameQueue.clear()
        _size.value = 0
        return frames
    }

    override fun close() {
        val framesToClose: List<Frame>
        synchronized(lock) {
            if (closed) return
            closed = true
            framesToClose = clearQueueAndExtractFrames()
        }
        for (frame in framesToClose) {
            frame.close()
        }
        frameGraphBuffers.detach(this)
    }

    private fun removeAndAcquire(
        predicate: (FrameReference) -> Boolean,
        reversed: Boolean = false,
    ): Frame? {
        synchronized(lock) {
            if (closed) return null

            val indexToRemove =
                if (reversed) {
                    frameQueue.indexOfLast { predicate(it.frameReference) }
                } else {
                    frameQueue.indexOfFirst { predicate(it.frameReference) }
                }

            if (indexToRemove != -1) {
                val bufferEntry = frameQueue.removeAt(indexToRemove)
                _size.value = frameQueue.size
                return bufferEntry.tryAcquireFrameForExternalUse()
            }
        }
        return null
    }

    private fun FrameReference.tryAcquireForInternalUse(): Frame? =
        (this as? FrameImpl)?.tryAcquireForInternalUse(streams)

    private fun BufferEntry.tryAcquireFrameForExternalUse(): Frame? =
        (this as? BufferEntry.WithFrame)?.frame?.also { (it as FrameImpl).isExternal = true }

    private companion object {
        const val FRAME_FLOW_EXTRA_BUFFER_CAPACITY = 4
    }
}
