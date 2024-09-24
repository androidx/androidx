/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.internal

import androidx.compose.runtime.makeSynchronizedObject
import androidx.compose.runtime.synchronized

/**
 * This is similar to a [ThreadLocal] but has lower overhead because it avoids a weak reference.
 * This should only be used when the writes are delimited by a try...finally call that will clean up
 * the reference such as [androidx.compose.runtime.snapshots.Snapshot.enter] else the reference
 * could get pinned by the thread local causing a leak.
 *
 * [ThreadLocal] can be used to implement the actual for platforms that do not exhibit the same
 * overhead for thread locals as the JVM and ART.
 */
internal class SnapshotThreadLocal<T> {
    private val map = AtomicReference(emptyThreadMap)
    private val writeMutex = makeSynchronizedObject()

    private var mainThreadValue: T? = null

    @Suppress("UNCHECKED_CAST")
    fun get(): T? {
        val threadId = currentThreadId()
        return if (threadId == MainThreadId) {
            mainThreadValue
        } else {
            map.get().get(threadId) as T?
        }
    }

    fun set(value: T?) {
        val key = currentThreadId()
        if (key == MainThreadId) {
            mainThreadValue = value
        } else {
            synchronized(writeMutex) {
                val current = map.get()
                if (current.trySet(key, value)) return
                map.set(current.newWith(key, value))
            }
        }
    }
}

internal class ThreadMap(
    private val size: Int,
    private val keys: LongArray,
    private val values: Array<Any?>
) {
    fun get(key: Long): Any? {
        val index = find(key)
        return if (index >= 0) values[index] else null
    }

    /**
     * Set the value if it is already in the map. Otherwise a new map must be allocated to contain
     * the new entry.
     */
    fun trySet(key: Long, value: Any?): Boolean {
        val index = find(key)
        if (index < 0) return false
        values[index] = value
        return true
    }

    fun newWith(key: Long, value: Any?): ThreadMap {
        val size = size
        val newSize = values.count { it != null } + 1
        val newKeys = LongArray(newSize)
        val newValues = arrayOfNulls<Any?>(newSize)
        if (newSize > 1) {
            var dest = 0
            var source = 0
            while (dest < newSize && source < size) {
                val oldKey = keys[source]
                val oldValue = values[source]
                if (oldKey > key) {
                    newKeys[dest] = key
                    newValues[dest] = value
                    dest++
                    // Continue with a loop without this check
                    break
                }
                if (oldValue != null) {
                    newKeys[dest] = oldKey
                    newValues[dest] = oldValue
                    dest++
                }
                source++
            }
            if (source == size) {
                // Appending a value to the end.
                newKeys[newSize - 1] = key
                newValues[newSize - 1] = value
            } else {
                while (dest < newSize) {
                    val oldKey = keys[source]
                    val oldValue = values[source]
                    if (oldValue != null) {
                        newKeys[dest] = oldKey
                        newValues[dest] = oldValue
                        dest++
                    }
                    source++
                }
            }
        } else {
            // The only element
            newKeys[0] = key
            newValues[0] = value
        }
        return ThreadMap(newSize, newKeys, newValues)
    }

    private fun find(key: Long): Int {
        var high = size - 1
        when (high) {
            -1 -> return -1
            0 -> return if (keys[0] == key) 0 else if (keys[0] > key) -2 else -1
        }
        var low = 0

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midVal = keys[mid]
            val comparison = midVal - key
            when {
                comparison < 0 -> low = mid + 1
                comparison > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -(low + 1)
    }
}

private val emptyThreadMap = ThreadMap(0, LongArray(0), emptyArray())
