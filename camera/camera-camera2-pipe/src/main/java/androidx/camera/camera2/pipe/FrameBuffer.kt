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

    /**
     * The maximum number of frame references this buffer can hold.
     *
     * If set a new capacity and the new capacity is less than the current number of buffered
     * frames, the oldest [FrameReference]s will be evicted until the buffer size matches new
     * capacity. Any [Frame] instances associated with the evicted [FrameReference]s will be closed
     * if the [FrameReference] is not hold by another [FrameBuffer]. Increasing the capacity does
     * not cause any frames to be evicted.
     */
    public var capacity: Int

    /** The current number of frame references held by the buffer. */
    public val size: StateFlow<Int>

    /** The flow of [FrameReference]s added to the buffer. */
    public val frameFlow: SharedFlow<FrameReference>

    /**
     * Removes the first entry in [FrameBuffer] that matches the optional [predicate] filter.
     *
     * If [predicate] is null, the first entry in the buffer is removed. If [predicate] is provided,
     * the first entry that matches the filter is removed. If a matching entry is found and removed,
     * its associated [Frame] is returned if valid; otherwise, null is returned.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the first entry is removed.
     * @return The [Frame] associated with the removed entry, or null if the buffer is empty, no
     *   entry matches the [predicate] filter, or the removed entry does not have a valid frame.
     */
    public fun removeFirst(predicate: ((FrameReference) -> Boolean)? = null): Frame?

    /**
     * Removes the last entry in [FrameBuffer] that matches the optional [predicate] filter.
     *
     * If [predicate] is null, the last entry in the buffer is removed. If [predicate] is provided,
     * the last entry that matches the filter is removed. If a matching entry is found and removed,
     * its associated [Frame] is returned if valid; otherwise, null is returned.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the last entry is removed.
     * @return The [Frame] associated with the removed entry, or null if the buffer is empty, no
     *   entry matches the [predicate] filter, or the removed entry does not have a valid frame.
     */
    public fun removeLast(predicate: ((FrameReference) -> Boolean)? = null): Frame?

    /**
     * Removes all entries in [FrameBuffer] that match the optional [predicate] filter.
     *
     * If [predicate] is null, all entries in the buffer are removed. If [predicate] is provided,
     * all entries that match the filter are removed. For each removed entry, its [Frame] is
     * returned if valid.
     *
     * @param predicate An optional filter function to apply. If null, all entries are matched and
     *   removed.
     * @return A list of [Frame]s associated with the removed entries.
     */
    public fun removeAll(predicate: ((FrameReference) -> Boolean)? = null): List<Frame>

    /**
     * Remove and close the first entry in [FrameBuffer] that matches the optional [predicate]
     * filter.
     *
     * If [predicate] is null, the first entry in the buffer is removed. If the entry contains an
     * unclosed underlying [Frame], the [Frame] will be closed. If [predicate] is provided, the
     * first entry that matches the filter is removed and closed. If a matching entry is found and
     * removed, true is returned; otherwise, false is returned.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the first entry is removed and closed.
     * @return true if a matching entry was found and removed, false otherwise.
     */
    public fun releaseFirst(predicate: ((FrameReference) -> Boolean)? = null): Boolean

    /**
     * Remove and close the last entry in [FrameBuffer] that matches the optional [predicate]
     * filter.
     *
     * If [predicate] is null, the last entry in the buffer is removed. If the entry contains an
     * unclosed underlying [Frame], the [Frame] will be closed. If [predicate] is provided, the last
     * entry that matches the filter is removed and closed. If a matching entry is found and
     * removed, true is returned; otherwise, false is returned.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the last entry is removed and closed.
     * @return true if a matching entry was found and removed, false otherwise.
     */
    public fun releaseLast(predicate: ((FrameReference) -> Boolean)? = null): Boolean

    /**
     * Remove and close all entries in [FrameBuffer] that match the optional [predicate] filter.
     *
     * If [predicate] is null, all entries in the buffer are removed. If any entry contains an
     * unclosed underlying [Frame], the [Frame] will be closed. If [predicate] is provided, all
     * entries that match the filter are removed and closed.
     *
     * @param predicate An optional filter function to apply. If null, all entries are matched and
     *   removed.
     * @return true if at least one matching entry was found and removed, false otherwise.
     */
    public fun releaseAll(predicate: ((FrameReference) -> Boolean)? = null): Boolean

    /**
     * Removes and closes the first entry in [FrameBuffer] that corresponds to the given
     * [FrameReference].
     *
     * If the [FrameBuffer] contains an entry for the given [FrameReference] that has not yet been
     * removed, that entry will be removed and its underlying [Frame] (if present and unclosed) will
     * be closed. If no matching entry is found, this method has no effect.
     *
     * This operation is idempotent: calling this method multiple times with the same
     * [FrameReference] will result in at most one frame being closed.
     *
     * @param frameReference The [FrameReference] whose corresponding entry should be removed and
     *   closed.
     * @return true if a matching entry was found and closed, false otherwise.
     */
    public fun release(frameReference: FrameReference): Boolean

    /**
     * The first FrameReference in the buffer, or null if the buffer is empty. No frames or
     * references are removed by this call.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the first entry is returned.
     * @return the first [FrameReference] in the buffer that matches the [predicate] filter, or null
     *   if the buffer is empty or no entry matches the filter.
     */
    public fun peekFirstReference(predicate: ((FrameReference) -> Boolean)? = null): FrameReference?

    /**
     * The last FrameReference in the buffer, or null if the buffer is empty. No frame references
     * are removed by this call.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the last entry is returned.
     * @return the last [FrameReference] in the buffer that matches the [predicate] filter, or null
     *   if the buffer is empty or no entry matches the filter.
     */
    public fun peekLastReference(predicate: ((FrameReference) -> Boolean)? = null): FrameReference?

    /**
     * All the FrameReference(s) in the buffer, or empty if the buffer is empty. No frames
     * references are removed by this call.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   all entries are returned.
     *     @return A list of [FrameReference]s that match the [predicate] filter, or an empty list
     *       if the buffer is empty or no entry matches the filter.
     */
    public fun peekAllReferences(
        predicate: ((FrameReference) -> Boolean)? = null
    ): List<FrameReference>

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
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the first entry is returned.
     * @return The [Frame] associated with the first entry that matches the [predicate] filter, or
     *   the [Frame] associated with the first entry if the predicate is null. Returns null if the
     *   buffer is empty or no entry matches the filter or the frame cannot be acquired.
     */
    @JvmOverloads
    @JvmStatic
    public fun FrameBuffer.tryPeekFirst(predicate: ((FrameReference) -> Boolean)? = null): Frame? =
        this.peekFirstReference(predicate)?.tryAcquire()

    /**
     * Returns the last frame in the buffer without removing its reference, or null if the buffer is
     * empty. The acquired Frame must be closed by the caller.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   the last entry is returned.
     * @return The [Frame] associated with the last entry that matches the [predicate] filter, or
     *   the [Frame] associated with the last entry if the predicate is null. Returns null if the
     *   buffer is empty or no entry matches the filter or the frame cannot be acquired.
     */
    @JvmOverloads
    @JvmStatic
    public fun FrameBuffer.tryPeekLast(predicate: ((FrameReference) -> Boolean)? = null): Frame? =
        this.peekLastReference(predicate)?.tryAcquire()

    /**
     * Returns all frames in the buffer without removing their references, or an empty list if the
     * buffer is empty. Acquired Frames must be closed by the caller.
     *
     * @param predicate An optional filter function to apply. If null, no filtering is performed and
     *   all entries are returned.
     * @return A list of [Frame]s obtainable that match the [predicate] filter, or an empty list if
     *   the buffer is empty or no entry matches the filter.
     */
    @JvmOverloads
    @JvmStatic
    public fun FrameBuffer.tryPeekAll(
        predicate: ((FrameReference) -> Boolean)? = null
    ): List<Frame> = this.peekAllReferences(predicate).mapNotNull { it.tryAcquire() }
}
