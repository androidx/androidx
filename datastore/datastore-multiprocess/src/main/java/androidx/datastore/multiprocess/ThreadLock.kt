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

package androidx.datastore.multiprocess

import java.io.Closeable
import java.io.InterruptedIOException
import java.util.concurrent.Semaphore

internal class ThreadLock(
    private val semaphore: Semaphore?
) : Closeable {
    fun acquired(): Boolean {
        return semaphore != null
    }

    override fun close() {
        if (acquired()) {
            semaphore!!.release()
        }
    }

    internal companion object {
        suspend fun tryAcquire(semaphore: Semaphore): ThreadLock {
            val acquired = semaphore.tryAcquire()
            return if (acquired) ThreadLock(semaphore) else ThreadLock(null)
        }

        suspend fun acquire(semaphore: Semaphore): ThreadLock {
            try {
                semaphore.acquire()
            } catch (ex: InterruptedException) {
                throw InterruptedIOException("semaphore not acquired: $ex")
            }
            return ThreadLock(semaphore)
        }
    }
}
