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

package androidx.graphics.lowlatency

import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EglSpec
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Allocation pool used for the creation and reuse of [RenderBuffer] instances.
 * This class is thread safe.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class RenderBufferPool(
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
     * another [RenderBuffer] is released. Must be greater than 0.
     */
    private val maxPoolSize: Int
) {

    private val mPool = ArrayList<RenderBuffer>()
    private var mNumAllocated = 0
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()

    init {
        if (maxPoolSize <= 0) {
            throw IllegalArgumentException("Pool size must be at least 1")
        }
    }

    /**
     * Obtains a [RenderBuffer] instance. This will either return a [RenderBuffer] if one is
     * available within the pool, or creates a new [RenderBuffer] instance if the number of
     * outstanding [RenderBuffer] instances is less than [maxPoolSize]
     */
    fun obtain(eglSpec: EglSpec): RenderBuffer {
        mLock.withLock {
            while (mPool.isEmpty() && mNumAllocated >= maxPoolSize) {
                Log.w(
                    TAG,
                    "Waiting for RenderBuffer to become available, current allocation " +
                        "count: $mNumAllocated"
                )
                mCondition.await()
            }
            return if (mPool.isNotEmpty()) {
                val renderBuffer = mPool[mPool.size - 1]
                mPool.removeAt(mPool.size - 1)
                renderBuffer
            } else {
                mNumAllocated++
                RenderBuffer(
                    eglSpec,
                    HardwareBuffer.create(
                        width,
                        height,
                        format,
                        1,
                        usage
                    )
                )
            }
        }
    }

    /**
     * Releases the given [RenderBuffer] back to the pool and signals all
     * consumers that are currently waiting for a buffer to become available
     * via [RenderBufferPool.obtain]
     * This method is thread safe.
     */
    fun release(renderBuffer: RenderBuffer) {
        mLock.withLock {
            mPool.add(renderBuffer)
            mCondition.signal()
        }
    }

    /**
     * Invokes [RenderBuffer.close] on all [RenderBuffer] instances currently available within
     * the pool and clears the pool.
     * This method is thread safe.
     */
    fun close() {
        mLock.withLock {
            for (renderBuffer in mPool) {
                renderBuffer.close()
            }
            mPool.clear()
            mNumAllocated = 0
        }
    }

    private companion object {
        private const val TAG = "RenderBufferPool"
    }
}