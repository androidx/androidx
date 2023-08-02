/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.opengl

import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLSpec
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Allocation pool used for the creation and reuse of [FrameBuffer] instances.
 * This class is thread safe.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class FrameBufferPool(
    /**
     * Width of the [HardwareBuffer] objects to allocate if none are available in the pool
     */
    private val width: Int,

    /**
     * Height of the [HardwareBuffer] objects to allocate if none are available in the pool
     */
    private val height: Int,

    /**
     * Format of the [HardwareBuffer] objects to allocate if none are available in the pool
     */
    private val format: Int,

    /**
     * Usage hint flag of the [HardwareBuffer] objects to allocate if none are available in the pool
     */
    private val usage: Long,

    /**
     * Maximum size that the pool before additional requests to allocate buffers blocks until
     * another [FrameBuffer] is released. Must be greater than 0.
     */
    private val maxPoolSize: Int
) {

    private data class FrameBufferEntry(
        var frameBuffer: FrameBuffer,
        var fence: SyncFenceCompat?,
        var isAvailable: Boolean
    )

    private val mPool = ArrayList<FrameBufferEntry>()
    private var mBuffersAvailable = 0
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private var mIsClosed = false

    init {
        if (maxPoolSize <= 0) {
            throw IllegalArgumentException("Pool size must be at least 1")
        }
    }

    /**
     * Return the current pool allocation size. This will increase until the [maxPoolSize].
     * This count will decrease if a buffer is closed before it is returned to the pool as a
     * closed buffer is no longer re-usable
     */
    val allocationCount: Int
        get() = mPool.size

    /**
     * Obtains a [FrameBuffer] instance. This will either return a [FrameBuffer] if one is
     * available within the pool, or creates a new [FrameBuffer] instance if the number of
     * outstanding [FrameBuffer] instances is less than [maxPoolSize]
     */
    fun obtain(eglSpec: EGLSpec): FrameBuffer {
        mLock.withLock {
            if (mIsClosed) {
                throw IllegalStateException("Attempt to obtain frame buffer from FrameBufferPool " +
                    "that has already been closed")
            }
            while (mBuffersAvailable == 0 && mPool.size >= maxPoolSize) {
                Log.w(
                    TAG,
                    "Waiting for FrameBuffer to become available, current allocation " +
                        "count: ${mPool.size}"
                )
                mCondition.await()
            }
            val entry = mPool.findEntryWith(isAvailable, signaledFence)
            return if (entry != null) {
                mBuffersAvailable--
                entry.isAvailable = false
                entry.fence?.let { fence ->
                    fence.awaitForever()
                    fence.close()
                }
                entry.frameBuffer
            } else {
                FrameBuffer(
                    eglSpec,
                    HardwareBuffer.create(
                        width,
                        height,
                        format,
                        1,
                        usage
                    )
                ).also {
                    mPool.add(FrameBufferEntry(it, null, false))
                }
            }
        }
    }

    /**
     * Releases the given [FrameBuffer] back to the pool and signals all
     * consumers that are currently waiting for a buffer to become available
     * via [FrameBufferPool.obtain]
     * This method is thread safe.
     */
    fun release(frameBuffer: FrameBuffer, fence: SyncFenceCompat? = null) {
        mLock.withLock {
            val entry = mPool.find { entry -> entry.frameBuffer === frameBuffer }
            if (entry != null) {
                // Only mark the entry as available if it was previously allocated
                // This protects against the potential for the same buffer to be released
                // multiple times into the same pool
                if (!entry.isAvailable) {
                    if (!entry.frameBuffer.isClosed) {
                        entry.fence = fence
                        entry.isAvailable = true
                        mBuffersAvailable++
                    } else {
                        // The consumer closed the buffer before releasing it to the pool.
                        // In this case remove the entry to allocate new buffers when requested.
                        // Because framebuffer instances can be managed by applications, we should
                        // defend against this potential scenario.
                        mPool.remove(entry)
                    }
                }

                if (!mIsClosed) {
                    mCondition.signal()
                } else {
                    // If a buffer is attempted to be released after the pool is closed
                    // just remove it from the entries and release it
                    frameBuffer.close()
                    if (mBuffersAvailable == mPool.size) {
                        mPool.clear()
                    }
                }
            } else if (!frameBuffer.isClosed) {
                // If the FrameBuffer is not previously closed and we don't own this, flag this as
                // an error as most likely this buffer was attempted to be returned to the wrong
                // pool
                throw IllegalArgumentException("No entry associated with this framebuffer " +
                    "instance. Was this frame buffer created from a different FrameBufferPool?")
            }
        }
    }

    val isClosed: Boolean
        get() = mLock.withLock { mIsClosed }

    /**
     * Invokes [FrameBuffer.close] on all [FrameBuffer] instances currently available within
     * the pool and clears the pool.
     * This method is thread safe.
     */
    fun close() {
        mLock.withLock {
            if (!mIsClosed) {
                for (entry in mPool) {
                    val frameBuffer = entry.frameBuffer
                    if (entry.isAvailable) {
                        entry.fence?.let { fence ->
                            fence.awaitForever()
                            fence.close()
                        }
                        frameBuffer.close()
                    }
                }
                if (mBuffersAvailable == mPool.size) {
                    mPool.clear()
                }
                mIsClosed = true
            }
        }
    }

    internal companion object {
        private const val TAG = "FrameBufferPool"

        /**
         * Predicate used to search for the first entry within the pool that is either null
         * or has already signalled
         */
        private val signaledFence: (FrameBufferEntry) -> Boolean = { entry ->
            val fence = entry.fence
            fence == null || fence.getSignalTimeNanos() != SyncFenceCompat.SIGNAL_TIME_PENDING
        }

        private val isAvailable: (FrameBufferEntry) -> Boolean = { entry -> entry.isAvailable }

        /**
         * Finds the first element within the ArrayList that satisfies both primary and
         * secondary conditions. If no entries satisfy the secondary condition, this returns
         * the first entry that satisfies the primary condition or null if no entries do.
         */
        internal fun <T> ArrayList<T>.findEntryWith(
            primaryCondition: ((T) -> Boolean),
            secondaryCondition: ((T) -> Boolean)
        ): T? {
            var fallback: T? = null
            for (entry in this) {
                if (primaryCondition(entry)) {
                    if (fallback == null) {
                        fallback = entry
                    }
                    if (secondaryCondition(entry)) {
                        return entry
                    }
                }
            }
            // No elements satisfy the condition, return the entry that satisfies the primary
            // condition if available
            return fallback
        }
    }
}
