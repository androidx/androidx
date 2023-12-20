/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.updateAndGet

/**
 * A SharedReference manages a reference to a [value] and can be used to create wrapper objects that
 * have shared-pointer like semantics. When created, a SharedReferences begins with a reference
 * count of `1`. When the reference count reaches 0 the current [Finalizer] will be invoked and all
 * subsequent calls to [setFinalizer] and [acquireOrNull] will fail.
 *
 * This class is designed as a building block to make it easier to create reference counted wrapper
 * objects that may need to be closed and/or have a hook to process items that must be finalized
 * after all shared references to the underlying resource have been closed.
 *
 * This class is thread safe.
 */
internal class SharedReference<T>(private val value: T, defaultFinalizer: Finalizer<T>) {
    private val count = atomic(1)
    private var currentFinalizer = atomic<Finalizer<T>?>(defaultFinalizer)

    /**
     * Get the underlying value and atomically incrementing the reference count, or null if the
     * reference count is zero.
     */
    fun acquireOrNull(): T? {
        val current =
            count.updateAndGet { current ->
                if (current == 0) {
                    0
                } else {
                    current + 1
                }
            }
        if (current != 0) {
            return value
        }
        return null
    }

    /**
     * Decrement the reference count. This must be invoked exactly once per outstanding reference,
     * and must be guaranteed by classes using this object.
     *
     * If decrement causes the internal reference count to reach 0, it will invoke the currently set
     * finalizer.
     */
    fun decrement() {
        if (count.decrementAndGet() == 0) {
            // When the last reference is released, atomically ensure no additional finalizers can
            // be set by updating the reference to null, and invoking the finalizer on the
            // reference.
            val actual = currentFinalizer.getAndSet(null)

            // This is guaranteed to never be null, since this block can only be reached once.
            actual!!.finalize(value)
        }
    }

    /**
     * Replace the current finalizer with a new one, and invoke [Finalizer.finalize] with null on
     * the previously configured finalizer object. If the reference count of this object has reached
     * null, the [Finalizer.finalize] method will be immediately and synchronously invoked with
     * null.
     *
     * This can be used to gain access to the underlying object after all shared references have been
     * closed.
     */
    fun setFinalizer(value: Finalizer<T>) {
        // Update the finalizer to the new value, but only if the current finalizer is not null. If
        // the previous finalizer is null, do not update the value.
        val previous =
            currentFinalizer.getAndUpdate { previous ->
                if (previous == null) {
                    null
                } else {
                    value
                }
            }

        // If the previous finalizer is not null, then invoke the previous finalizer with null.
        // This indicates it has been replaced by a different finalizer instance and will not be
        // invoked. This is important for cases where a class may want exclusive access to an
        // object and may need to wait for access.
        if (previous != null) {
            previous.finalize(null)
        } else {
            // In this case, the object has already been finalized, and the most recently set
            // finalizer instance will never receive the real object. Invoke the Finalizer with
            // null so that the callback can handle this scenario.
            value.finalize(null)
        }
    }
}
