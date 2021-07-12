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

package androidx.paging

import androidx.annotation.VisibleForTesting
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Helper class for thread-safe invalidation callback tracking + triggering on registration.
 */
internal class InvalidateCallbackTracker<T>(
    private val callbackInvoker: (T) -> Unit
) {
    private val lock = ReentrantLock()
    private val callbacks = mutableListOf<T>()
    internal var invalid = false
        private set

    @VisibleForTesting
    internal fun callbackCount() = callbacks.size

    internal fun registerInvalidatedCallback(callback: T) {
        if (invalid) {
            callbackInvoker(callback)
            return
        }

        var callImmediately = false
        lock.withLock {
            if (invalid) {
                callImmediately = true
            } else {
                callbacks.add(callback)
            }
        }

        if (callImmediately) {
            callbackInvoker(callback)
        }
    }

    internal fun unregisterInvalidatedCallback(callback: T) {
        lock.withLock {
            callbacks.remove(callback)
        }
    }

    internal fun invalidate() {
        if (invalid) return

        var callbacksToInvoke: List<T>?
        lock.withLock {
            if (invalid) return

            invalid = true
            callbacksToInvoke = callbacks.toList()
            callbacks.clear()
        }

        callbacksToInvoke?.forEach(callbackInvoker)
    }
}