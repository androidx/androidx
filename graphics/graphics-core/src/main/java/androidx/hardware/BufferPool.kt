/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.hardware

import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@RequiresApi(Build.VERSION_CODES.O)
internal class BufferPool<T : BufferPool.BufferProvider>(val maxPoolSize: Int) {

    private val mPool: ArrayList<Entry<T>> = ArrayList()
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private var mBuffersAvailable = 0
    private var mIsClosed = false

    init {
        if (maxPoolSize <= 0) {
            throw IllegalArgumentException("Pool size must be at least 1")
        }
    }

    fun add(buffer: T) {
        mPool.add(Entry(buffer))
    }

    inline fun obtain(factory: () -> T): T {
        mLock.withLock {
            return obtainFromPool() ?: factory.invoke().also { buffer -> insert(buffer) }
        }
    }

    private fun insert(buffer: T) {
        // Insert the buffer into the pool. If we are in single buffer mode mark this buffer
        // as being available to support front buffered rendering use cases of simultaneous
        // presentation and rendering
        val singleBuffered = maxPoolSize == 1
        if (singleBuffered) {
            mBuffersAvailable++
        }
        mPool.add(Entry(buffer, isAvailable = singleBuffered))
    }

    private fun obtainFromPool(): T? {
        if (mIsClosed) {
            throw IllegalStateException("Attempt to obtain frame buffer from FrameBufferPool " +
                "that has already been closed")
        }
        val singleBuffered = maxPoolSize == 1
        return if (singleBuffered) {
            val entry = mPool.firstOrNull()?.also { entry -> entry.isAvailable = true }
            entry?.bufferProvider
        } else {
            while (mBuffersAvailable == 0 && mPool.size >= maxPoolSize) {
                Log.w(
                    TAG,
                    "Waiting for buffer to become available, current allocation " +
                        "count: ${mPool.size}"
                )
                mCondition.await()
            }
            val entry = mPool.findEntryWith(
                isAvailable,
                signaledFence
            )?.also { entry ->
                mBuffersAvailable--
                with(entry) {
                    isAvailable = false
                    releaseFence?.let { fence ->
                        fence.awaitForever()
                        fence.close()
                    }
                    bufferProvider
                }
            }
            entry?.bufferProvider
        }
    }

    fun release(hardwareBuffer: HardwareBuffer, fence: SyncFenceCompat? = null) {
        mLock.withLock {
            val entry = mPool.find { entry -> entry.hardwareBuffer === hardwareBuffer }
            if (entry != null) {
                // Only mark the entry as available if it was previously allocated
                // This protects against the potential for the same buffer to be released
                // multiple times into the same pool
                if (!entry.isAvailable) {
                    if (!entry.hardwareBuffer.isClosed) {
                        entry.releaseFence = fence
                        entry.isAvailable = true
                        mBuffersAvailable++
                    } else {
                        // The consumer closed the buffer before releasing it to the pool.
                        // In this case remove the entry to allocate new buffers when requested.
                        // Because HardwareBuffer instances can be managed by applications, we
                        // should defend against this potential scenario.
                        mPool.remove(entry)
                    }
                }

                if (!mIsClosed) {
                    mCondition.signal()
                } else {
                    // If a buffer is attempted to be released after the pool is closed
                    // just remove it from the entries and release it
                    hardwareBuffer.close()
                    if (mBuffersAvailable == mPool.size) {
                        mPool.clear()
                    }
                }
            } else if (!hardwareBuffer.isClosed) {
                // If the FrameBuffer is not previously closed and we don't own this, flag this as
                // an error as most likely this buffer was attempted to be returned to the wrong
                // pool
                throw IllegalArgumentException("No entry associated with this framebuffer " +
                    "instance. Was this frame buffer created from a different FrameBufferPool?")
            }
        }
    }

    /**
     * Return the current pool allocation size. This will increase until the [maxPoolSize].
     * This count will decrease if a buffer is closed before it is returned to the pool as a
     * closed buffer is no longer re-usable
     */
    val allocationCount: Int
        get() = mPool.size

    val isClosed: Boolean
        get() = mLock.withLock { mIsClosed }

    /**
     * Invokes [BufferProvider.release] on all [Entry] instances currently available within the
     * pool and clears the pool. This method is thread safe.
     */
    fun close() {
        mLock.withLock {
            if (!mIsClosed) {
                for (entry in mPool) {
                    if (entry.isAvailable) {
                        entry.releaseFence?.let { fence ->
                            fence.awaitForever()
                            fence.close()
                        }
                        entry.bufferProvider.release()
                    }
                }
                if (mBuffersAvailable == mPool.size) {
                    mPool.clear()
                }
                mIsClosed = true
            }
        }
    }

    private data class Entry<T : BufferProvider> (
        val bufferProvider: T,
        var releaseFence: SyncFenceCompat? = null,
        var isAvailable: Boolean = true
    ) {
        val hardwareBuffer: HardwareBuffer
            get() = bufferProvider.hardwareBuffer
    }

    interface BufferProvider {
        val hardwareBuffer: HardwareBuffer
        fun release()
    }

    internal companion object {
        private const val TAG = "BufferPool"

        /**
         * Predicate used to search for the first entry within the pool that is either null
         * or has already signalled
         */
        private val signaledFence: (Entry<*>) -> Boolean = { entry ->
            val fence = entry.releaseFence
            fence == null || fence.getSignalTimeNanos() != SyncFenceCompat.SIGNAL_TIME_PENDING
        }

        private val isAvailable: (Entry<*>) -> Boolean = { entry -> entry.isAvailable }

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
