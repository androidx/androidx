/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.coroutines

import androidx.room3.concurrent.FileLock
import androidx.room3.concurrent.SynchronizedObject
import androidx.room3.concurrent.synchronized
import kotlinx.coroutines.sync.Mutex

/**
 * An exclusive mutex for in-process and multi-process synchronization.
 *
 * The mutex is cooperative and only protects the critical region from other [ExclusiveMutex] users
 * with the same `filename`. The mutex is reentrant from within the same coroutine in the same
 * process.
 *
 * Locking is done via two levels:
 * 1. Coroutine locking within the same process is done via [withReentrantLock] on a [Mutex] keyed
 *    by the given `filename`.
 * 2. Multi-process locking is done via a [FileLock] whose lock file is based on the given
 *    `filename`.
 *
 * @param filename The path to the file to protect.
 * @param useFileLock Whether multi-process lock will be done or not.
 */
internal class ExclusiveMutex(filename: String, useFileLock: Boolean) {
    private val mutex: Mutex = getMutex(filename)
    private val fileLock: FileLock? = if (useFileLock) getFileLock(filename) else null

    /**
     * Attempts to acquire a lock, blocking if it is not available. Once a lock is acquired
     * [onLocked] will be invoked. If an error occurs during locking, then [onLockError] will be
     * invoked to give a chance for the caller make sense of the error.
     */
    suspend inline fun <T> withLock(
        crossinline onLocked: suspend () -> T,
        crossinline onLockError: (Throwable) -> Nothing,
    ): T {
        var locked = false
        return mutex.withReentrantLock {
            try {
                fileLock?.lock()
                try {
                    locked = true
                    onLocked()
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
            }
        }
    }

    companion object : SynchronizedObject() {
        private val mutexMap = mutableMapOf<String, Mutex>()

        private fun getMutex(key: String): Mutex =
            synchronized(this) {
                return mutexMap.getOrPut(key) { Mutex() }
            }

        private fun getFileLock(key: String) = FileLock(key)
    }
}
