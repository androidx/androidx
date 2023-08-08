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

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe class responsible for maintaining a list of objects as well as an index
 * that keeps track of the last consumed object within the queue.
 * This ensures that the collection as well as the current index parameter are updated atomically
 * within the same lock.
 */
internal class ParamQueue<T> {

    private val mLock = ReentrantLock()
    private var mParams = ArrayList<T>()
    private var mIndex = 0

    /**
     * Clears the parameter queue and resets the index position to 0
     */
    fun clear() {
        mLock.withLock {
            mIndex = 0
            mParams.clear()
        }
    }

    /**
     * Returns the current queue and resets the index position to 0.
     * This collection is no longer owned by [ParamQueue] and a new queue is maintained after
     * this call is made. This allows callers to manipulate the returned collection as they
     * see fit without impacting the integrity of the [ParamQueue] after this method is invoked.
     */
    fun release(): MutableCollection<T> {
        mLock.withLock {
            val result = mParams
            mParams = ArrayList<T>()
            mIndex = 0
            return result
        }
    }

    /**
     * Returns the next parameter at the index position and increments the index position.
     * This parameter is provided as a callback. If the index position is out of range, this
     * method acts as a no-op.
     * This does not actually remove the parameter from the collection. Consumers must either
     * call [clear], or clear the collection returned in [release] to ensure contents do not
     * grow unbounded.
     */
    inline fun next(block: (T) -> Unit) {
        mLock.withLock {
            if (mIndex < mParams.size) {
                val param = mParams[mIndex++]
                block(param)
            }
        }
    }

    /**
     * Adds a new entry into the parameter queue. This leaves the current index position unchanged.
     */
    fun add(param: T) {
        mLock.withLock {
            mParams.add(param)
        }
    }

    fun count(): Int = mLock.withLock { mParams.size }

    fun isEmpty() = count() == 0
}
