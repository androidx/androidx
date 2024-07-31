/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.sqlite.util

import android.util.Log
import androidx.annotation.RestrictTo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Utility class for in-process and multi-process key-based lock mechanism for safely doing
 * synchronized operations.
 *
 * Acquiring the lock will be quick if no other thread or process has a lock with the same key. But
 * if the lock is already held then acquiring it will block, until the other thread or process
 * releases the lock. Note that the key and lock directory must be the same to achieve
 * synchronization.
 *
 * Locking is done via two levels:
 * 1. Thread locking within the same JVM process is done via a map of String key to ReentrantLock
 *    objects.
 * 2. Multi-process locking is done via a lock file whose name contains the key and FileLock
 *    objects.
 *
 * Creates a lock with `name` and using `lockDir` as the directory for the lock files.
 *
 * @param name the name of this lock.
 * @param lockDir the directory where the lock files will be located.
 * @param processLock whether to use file for process level locking or not by default. The behaviour
 *   can be overridden via the [lock] method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProcessLock(name: String, lockDir: File?, private val processLock: Boolean) {
    private val lockFile: File? = lockDir?.let { File(it, "$name.lck") }
    private val threadLock: Lock = getThreadLock(name)
    private var lockChannel: FileChannel? = null

    /**
     * Attempts to grab the lock, blocking if already held by another thread or process.
     *
     * @param [processLock] whether to use file for process level locking or not.
     */
    public fun lock(processLock: Boolean = this.processLock) {
        threadLock.lock()
        if (processLock) {
            try {
                if (lockFile == null) {
                    throw IOException("No lock directory was provided.")
                }
                // Verify parent dir
                val parentDir = lockFile.parentFile
                parentDir?.mkdirs()
                lockChannel = FileOutputStream(lockFile).channel.apply { lock() }
            } catch (e: IOException) {
                lockChannel = null
                Log.w(TAG, "Unable to grab file lock.", e)
            }
        }
    }

    /** Releases the lock. */
    public fun unlock() {
        try {
            lockChannel?.close()
        } catch (ignored: IOException) {}
        threadLock.unlock()
    }

    private companion object {
        private const val TAG = "SupportSQLiteLock"
        // in-process lock map
        private val threadLocksMap: MutableMap<String, Lock> = HashMap()

        private fun getThreadLock(key: String): Lock =
            synchronized(threadLocksMap) {
                return threadLocksMap.getOrPut(key) { ReentrantLock() }
            }
    }
}
