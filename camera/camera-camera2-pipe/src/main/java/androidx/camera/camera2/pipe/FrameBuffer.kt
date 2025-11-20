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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A buffer for frames, potentially circular and with auto-evicting characteristics.
 *
 * A FrameBuffer is typically created per use-case. Implementations might use reference counting for
 * re-use, meaning changes to frame data can be shared if frames are re-used.
 *
 * Closing a frame buffer should discard any frames currently held, and any pending frames should
 * also be discarded.
 *
 * Failing to close a FrameBuffer can result in leaking Frame resources.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameBuffer : AutoCloseable {

    /** The set of Stream IDs this FrameBuffer is associated with. */
    public val streams: Set<StreamId>

    /**
     * A map of parameters associated with this FrameBuffer. This can be used for custom settings or
     * metadata.
     */
    public val parameters: Map<Any, Any?>

    /** The maximum number of frame references this buffer can hold. */
    public val capacity: Int

    /** The current number of frame references held by the buffer. */
    public val size: StateFlow<Int>

    /** The flow of [FrameReference]s added to the buffer. */
    public val frameFlow: SharedFlow<FrameReference>

    /**
     * Removes the first frame reference in the buffer or null if the buffer is empty.
     *
     * The frame reference is removed from the buffer.
     */
    public fun removeFirstReference(): FrameReference?

    /**
     * Removes the last frame reference in the buffer or null if the buffer is empty.
     *
     * The frame reference is removed from the buffer.
     */
    public fun removeLastReference(): FrameReference?

    /**
     * Removes all the frame reference in the buffer or empty if the buffer is empty.
     *
     * The frame references are removed from the buffer.
     */
    public fun removeAllReferences(): List<FrameReference>

    /**
     * The first FrameReference in the buffer, or null if the buffer is empty. No frames or
     * references are removed by this call.
     */
    public fun peekFirstReference(): FrameReference?

    /**
     * The last FrameReference in the buffer, or null if the buffer is empty. No frame references
     * are removed by this call.
     */
    public fun peekLastReference(): FrameReference?

    /**
     * All the FrameReference(s) in the buffer, or empty if the buffer is empty. No frames
     * references are removed by this call.
     */
    public fun peekAllReferences(): List<FrameReference>

    /**
     * Closes this FrameBuffer and releases all the resources it holds. After this method has been
     * invoked, all incoming frames might be dropped, and no frames should be retrievable from the
     * FrameBuffer.
     *
     * Previously acquired Frame instances might still be available if externally referenced, and
     * previously acquired FrameReference objects may still attempt to resolve frames, potentially
     * failing if the frames have been fully released. This method is idempotent; calling it
     * multiple times has no further effect.
     */
    public override fun close()
}

/**
 * This object centralizes common extension operations for inspecting and manipulating a
 * [FrameBuffer], such as peeking at or removing frames.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object FrameBuffers {
    /**
     * Returns the first frame in the buffer without removing its reference, or null if the buffer
     * is empty. The acquired Frame must be closed by the caller.
     */
    @JvmStatic
    public fun FrameBuffer.tryPeekFirst(): Frame? = this.peekFirstReference()?.tryAcquire()

    /**
     * Returns the last frame in the buffer without removing its reference, or null if the buffer is
     * empty. The acquired Frame must be closed by the caller.
     */
    @JvmStatic public fun FrameBuffer.tryPeekLast(): Frame? = this.peekLastReference()?.tryAcquire()

    /**
     * Returns all frames in the buffer without removing their references, or an empty list if the
     * buffer is empty. Acquired Frames must be closed by the caller.
     */
    @JvmStatic
    public fun FrameBuffer.tryPeekAll(): List<Frame> =
        this.peekAllReferences().mapNotNull { it.tryAcquire() }

    /**
     * Removes the first frame reference in the buffer and returns the corresponding Frame, or null
     * if the buffer is empty.
     *
     * The frame is removed from the buffer.
     */
    @JvmStatic
    public fun FrameBuffer.tryRemoveFirst(): Frame? = this.removeFirstReference()?.tryAcquire()

    /**
     * Removes the last frame reference in the buffer and returns the corresponding Frame, or null
     * if the buffer is empty.
     *
     * The frame is removed from the buffer.
     */
    @JvmStatic
    public fun FrameBuffer.tryRemoveLast(): Frame? = this.removeLastReference()?.tryAcquire()

    /**
     * Removes all the frame references in the buffer and returns the corresponding Frame(s), or
     * empty if the buffer is empty.
     *
     * The frames are removed from the buffer.
     */
    @JvmStatic
    public fun FrameBuffer.tryRemoveAll(): List<Frame> =
        this.removeAllReferences().mapNotNull { it.tryAcquire() }
}
