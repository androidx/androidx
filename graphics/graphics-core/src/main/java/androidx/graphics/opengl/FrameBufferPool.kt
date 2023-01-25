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

    private val mPool = ArrayList<FrameBuffer>()
    private var mNumAllocated = 0
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()

    init {
        if (maxPoolSize <= 0) {
            throw IllegalArgumentException("Pool size must be at least 1")
        }
    }

    /**
     * Obtains a [FrameBuffer] instance. This will either return a [FrameBuffer] if one is
     * available within the pool, or creates a new [FrameBuffer] instance if the number of
     * outstanding [FrameBuffer] instances is less than [maxPoolSize]
     */
    fun obtain(eglSpec: EGLSpec): FrameBuffer {
        mLock.withLock {
            while (mPool.isEmpty() && mNumAllocated >= maxPoolSize) {
                Log.w(
                    TAG,
                    "Waiting for FrameBuffer to become available, current allocation " +
                        "count: $mNumAllocated"
                )
                mCondition.await()
            }
            return if (mPool.isNotEmpty()) {
                val frameBuffer = mPool[mPool.size - 1]
                mPool.removeAt(mPool.size - 1)
                frameBuffer
            } else {
                mNumAllocated++
                FrameBuffer(
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
     * Releases the given [FrameBuffer] back to the pool and signals all
     * consumers that are currently waiting for a buffer to become available
     * via [FrameBufferPool.obtain]
     * This method is thread safe.
     */
    fun release(frameBuffer: FrameBuffer) {
        mLock.withLock {
            mPool.add(frameBuffer)
            mCondition.signal()
        }
    }

    /**
     * Invokes [FrameBuffer.close] on all [FrameBuffer] instances currently available within
     * the pool and clears the pool.
     * This method is thread safe.
     */
    fun close() {
        mLock.withLock {
            for (frameBuffer in mPool) {
                frameBuffer.close()
            }
            mPool.clear()
            mNumAllocated = 0
        }
    }

    private companion object {
        private const val TAG = "FrameBufferPool"
    }
}