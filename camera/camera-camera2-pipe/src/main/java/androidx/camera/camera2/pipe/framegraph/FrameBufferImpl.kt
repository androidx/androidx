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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameBuffer
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TagBundle
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.FrameImpl
import androidx.camera.camera2.pipe.isTagKey
import androidx.camera.common.Metadata
import java.util.Objects.deepEquals
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

    @GuardedBy("lock") private var _capacity: Int = capacity

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

    override var capacity: Int
        get() = synchronized(lock) { _capacity }
        set(newCapacity) {
            require(newCapacity >= 0) { "Capacity cannot be negative" }

            val framesToClose: MutableList<Frame> = mutableListOf()
            synchronized(lock) {
                if (closed) {
                    return
                }

                val previousCapacity = _capacity

                if (newCapacity == previousCapacity) return

                _capacity = newCapacity

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

    /**
     * Returns true if a frame produced by [request] should be added to this buffer, based on the
     * tag bundles in [parameters]. For each tag bundle, [request] must carry a [TagBundle] for the
     * key in [Request.extras] that contains every tag in the tag bundle of this buffer.
     */
    private fun matchesTagBundles(request: Request): Boolean {
        for ((key, value) in parameters) {
            // Skip parameters that are not tag keys.
            if (!key.isTagKey()) continue
            // attach() makes sure every tag key has a TagBundle value.
            val bufferTags = value as TagBundle<*>
            // Missing and non-TagBundle keys in the request do not match.
            val requestTags =
                request.extras[key as Metadata.Key<*>] as? TagBundle<*> ?: return false
            // For TagBundle keys, make sure the request contains all values in the TagBundle.
            if (!requestTags.containsAll(bufferTags)) return false
        }
        return true
    }

    /**
     * Returns true if a frame produced by [request] should be added to this buffer, based on
     * [parameters]. Tag keys are checked with [matchesTagBundles]. For every other key in
     * [parameters], [request] must carry the key ([Request.parameters] for a [CaptureRequest.Key],
     * [Request.extras] for a [Metadata.Key]) with a value equal to the value of this buffer. Only
     * the [Request] values are compared, not values that CameraGraph default or required parameters
     * may override.
     */
    private fun matchesParameters(request: Request): Boolean {
        if (!matchesTagBundles(request)) return false
        for ((key, value) in parameters) {
            // Tag keys are checked by matchesTagBundles.
            if (key.isTagKey()) continue
            // A request that does not carry the key does not match.
            val requestValue =
                when (key) {
                    is CaptureRequest.Key<*> ->
                        if (key in request.parameters) request.parameters[key] else return false
                    is Metadata.Key<*> ->
                        if (key in request.extras) request.extras[key] else return false
                    // FrameGraphBuffers only accepts the two key types above.
                    else -> continue
                }
            // The request value must be equal to the value of this buffer.
            if (!deepEquals(requestValue, value)) return false
        }
        return true
    }

    override fun onFrameStarted(frameReference: FrameReference) {
        if (!frameReference.requestMetadata.streams.keys.containsAll(streams)) {
            return
        }

        // A frame whose request does not match the parameters of this buffer is ignored entirely.
        // It is not emitted, not acquired, and does not evict any frames that are already in the
        // buffer. Repeating and non-repeating requests are treated the same way: the repeating
        // request that FrameGraph builds carries the parameters of every attached buffer and the
        // union of their tag bundles, so it matches every buffer once it is running.
        if (!matchesParameters(frameReference.requestMetadata.request)) {
            return
        }

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
                if (frameQueue.size == _capacity) {
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

    override fun releaseFirst(predicate: ((FrameReference) -> Boolean)?): Boolean =
        release(predicate ?: { true }, reversed = false)

    override fun releaseLast(predicate: ((FrameReference) -> Boolean)?): Boolean =
        release(predicate ?: { true }, reversed = true)

    override fun releaseAll(predicate: ((FrameReference) -> Boolean)?): Boolean {
        val removedFrames = mutableListOf<BufferEntry>()
        synchronized(lock) {
            if (closed || frameQueue.isEmpty()) return false

            val iterator = frameQueue.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (predicate == null || predicate(entry.frameReference)) {
                    iterator.remove()
                    removedFrames.add(entry)
                }
            }
            _size.value = frameQueue.size
        }

        for (entry in removedFrames) {
            if (entry is BufferEntry.WithFrame) {
                entry.frame.close()
            }
        }

        return removedFrames.isNotEmpty()
    }

    override fun release(frameReference: FrameReference): Boolean = releaseFirst {
        it === frameReference
    }

    override fun peekFirstReference(predicate: ((FrameReference) -> Boolean)?): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            val entry =
                if (predicate == null) {
                    frameQueue.firstOrNull()
                } else {
                    frameQueue.firstOrNull { predicate(it.frameReference) }
                }
            entry?.frameReference
        }

    override fun peekLastReference(predicate: ((FrameReference) -> Boolean)?): FrameReference? =
        synchronized(lock) {
            if (closed) return null
            val entry =
                if (predicate == null) {
                    frameQueue.lastOrNull()
                } else {
                    frameQueue.lastOrNull { predicate(it.frameReference) }
                }
            entry?.frameReference
        }

    override fun peekAllReferences(
        predicate: ((FrameReference) -> Boolean)?
    ): List<FrameReference> =
        synchronized(lock) {
            if (closed) return emptyList()
            if (predicate == null) {
                frameQueue.map { it.frameReference }
            } else {
                frameQueue.mapNotNull { it.frameReference.takeIf(predicate) }
            }
        }

    fun trimFirst(): Boolean = releaseFirst { true }

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

    private fun release(
        predicate: (FrameReference) -> Boolean,
        reversed: Boolean = false,
    ): Boolean {
        var entryToClose: BufferEntry? = null
        synchronized(lock) {
            if (closed) return false

            val indexToRemove =
                if (reversed) {
                    frameQueue.indexOfLast { predicate(it.frameReference) }
                } else {
                    frameQueue.indexOfFirst { predicate(it.frameReference) }
                }

            if (indexToRemove != -1) {
                val bufferEntry = frameQueue.removeAt(indexToRemove)
                _size.value = frameQueue.size
                entryToClose = bufferEntry
            }
        }
        // Close entry outside of synchronized block to avoid deadlocks
        if (entryToClose is BufferEntry.WithFrame) {
            entryToClose.frame.close()
        }
        return entryToClose != null
    }

    private fun FrameReference.tryAcquireForInternalUse(): Frame? =
        (this as? FrameImpl)?.tryAcquireForInternalUse(streams)

    private fun BufferEntry.tryAcquireFrameForExternalUse(): Frame? =
        (this as? BufferEntry.WithFrame)?.frame?.also { (it as FrameImpl).isExternal = true }

    private companion object {
        const val FRAME_FLOW_EXTRA_BUFFER_CAPACITY = 4
    }
}
