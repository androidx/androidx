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
import androidx.paging.internal.ReentrantLock
import androidx.paging.internal.withLock

/**
 * Helper class for thread-safe invalidation callback tracking + triggering on registration.
 */
internal class InvalidateCallbackTracker<T>(
    private val callbackInvoker: (T) -> Unit,
    /**
     * User-provided override of DataSource.isInvalid
     */
    private val invalidGetter: (() -> Boolean)? = null,
) {
    private val lock = ReentrantLock()
    private val callbacks = mutableListOf<T>()
    internal var invalid = false
        private set

    @VisibleForTesting
    internal fun callbackCount() = callbacks.size

    internal fun registerInvalidatedCallback(callback: T) {
        // This isn't sufficient, but is the best we can do in cases where DataSource.isInvalid
        // is overridden, since we have no way of knowing when the result gets flipped if user
        // never calls .invalidate().
        if (invalidGetter?.invoke() == true) {
            invalidate()
        }

        if (invalid) {
            callbackInvoker(callback)
            return
        }

        val callImmediately = lock.withLock {
            if (invalid) {
                true // call immediately
            } else {
                callbacks.add(callback)
                false // don't call, not invalid yet.
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

    internal fun invalidate(): Boolean {
        if (invalid) return false

        val callbacksToInvoke = lock.withLock {
            if (invalid) return false

            invalid = true
            callbacks.toList().also {
                callbacks.clear()
            }
        }

        callbacksToInvoke.forEach(callbackInvoker)
        return true
    }
}
