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

package androidx.camera.camera2.pipe.core

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A wakelock is a thread-safe primitive that can invoke close after all tokens are released.
 *
 * This implementation has several defining characteristics:
 * 1. The timeout (if specified), does not start until at least 1 [Token] has been acquired.
 * 2. Acquiring a token a token at any time before the timeout completes will cancel the timeout.
 * 3. Acquiring a token is atomic: Either a token is acquired and the close method will not execute
 *    OR acquire will return a token and the close method will not execute until after the token is
 *    released.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class WakeLock(
    private val scope: CoroutineScope,
    private val timeout: Long = 0,
    private val callback: () -> Unit
) {
    private val lock = Any()

    @GuardedBy("lock")
    private var count = 0

    @GuardedBy("lock")
    private var timeoutJob: Job? = null

    @GuardedBy("lock")
    private var closed = false

    private inner class WakeLockToken : Token {
        private val closed = atomic(false)
        override fun release(): Boolean {
            if (closed.compareAndSet(expect = false, update = true)) {
                releaseToken()
                return true
            }
            return false
        }
    }

    fun acquire(): Token? {
        synchronized(lock) {
            if (closed) {
                return null
            }
            count += 1
            if (count == 1) {
                timeoutJob?.cancel()
                timeoutJob = null
            }
        }
        return WakeLockToken()
    }

    fun release(): Boolean {
        synchronized(lock) {
            if (closed) {
                return false
            }
            closed = true
            timeoutJob?.cancel()
            timeoutJob = null
        }

        scope.launch {
            // Execute the callback
            callback()
        }

        return true
    }

    internal fun releaseToken() {
        // This function is internal to avoid a synthetic accessor access from [WakeLockToken]
        synchronized(lock) {
            count -= 1
            if (count == 0 && !closed) {
                timeoutJob = scope.launch {
                    delay(timeout)

                    synchronized(lock) {
                        if (closed || count != 0) {
                            return@launch
                        }
                        timeoutJob = null
                        closed = true
                    }

                    // Execute the callback
                    callback()
                }
            }
        }
    }
}