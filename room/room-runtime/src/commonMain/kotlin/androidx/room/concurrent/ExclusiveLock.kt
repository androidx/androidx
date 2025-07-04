/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.concurrent

/**
 * An exclusive lock for in-process and multi-process synchronization.
 *
 * The lock is cooperative and only protects the critical region from other [ExclusiveLock] users
 * with the same `filename`. The lock is reentrant from within the same thread in the same process.
 *
 * Locking is done via two levels:
 * 1. Thread locking within the same process is done via a [ReentrantLock] keyed by the given
 *    `filename`.
 * 2. Multi-process locking is done via a [FileLock] whose lock file is based on the given
 *    `filename`.
 *
 * @param filename The path to the file to protect.
 * @param useFileLock Whether multi-process lock will be done or not.
 */
internal class ExclusiveLock(filename: String, useFileLock: Boolean) {
    private val threadLock: ReentrantLock = getThreadLock(filename)
    private val fileLock: FileLock? = if (useFileLock) getFileLock(filename) else null

    /**
     * Attempts to acquire a lock, blocking if it is not available. Once a lock is acquired
     * [onLocked] will be invoked. If an error occurs during locking, then [onLockError] will be
     * invoked to give a chance for the caller make sense of the error.
     */
    fun <T> withLock(onLocked: () -> T, onLockError: (Throwable) -> Nothing): T {
        var locked = false
        threadLock.lock()
        try {
            fileLock?.lock()
            try {
                locked = true
                return onLocked()
            } finally {
                fileLock?.unlock()
            }
        } catch (t: Throwable) {
            if (locked) {
                // Lock was acquired so error comes from critical region, simply re-throw.
                throw t
            } else {
                onLockError(t)
            }
        } finally {
            threadLock.unlock()
        }
    }

    companion object : SynchronizedObject() {
        private val threadLocksMap = mutableMapOf<String, ReentrantLock>()

        private fun getThreadLock(key: String): ReentrantLock =
            synchronized(this) {
                return threadLocksMap.getOrPut(key) { ReentrantLock() }
            }

        private fun getFileLock(key: String) = FileLock(key)
    }
}
