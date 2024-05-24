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

package androidx.camera.viewfinder.compose.internal

import android.util.Log
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop

/**
 * A thread-safe, lock-free class for managing the lifecycle of an object using ref-counting.
 *
 * The object that is ref-counted can be initialized late using [initialize].
 *
 * @param[debugRefCounts] whether to print debug log statements.
 * @param[onRelease] a block that will be invoked once when the ref-count reaches 0.
 */
class RefCounted<T : Any>(
    private val debugRefCounts: Boolean = false,
    private val onRelease: (T) -> Unit
) {
    private val refCounted = atomic(uninitialized<T>())

    /**
     * Initializes the ref-count managed object with the object being managed.
     *
     * This also initializes the implicit ref-count to 1.
     *
     * All calls to this function must be paired with [release] to ensure the initial implicit ref
     * count is decremented and the `onRelease` callback can be called.
     */
    fun initialize(newValue: T) {
        val initialVal = Pair(newValue, 1)
        check(refCounted.compareAndSet(uninitialized(), initialVal)) {
            "Ref-count managed object has already been initialized."
        }

        if (debugRefCounts) {
            Log.d(
                TAG,
                "RefCounted@${"%x".format(hashCode())}<${newValue::class.simpleName}> " +
                    "initialized: [refCount: 1, value: $newValue]",
                Throwable()
            )
        }
    }

    /**
     * Retrieves the underlying managed object, increasing the ref-count by 1.
     *
     * This increases the ref-count if the object has not already been released. If the object has
     * been released, `null` is returned.
     *
     * All calls to this function must be paired with [release], unless `null` is returned.
     */
    fun acquire(): T? {
        check(refCounted.value != uninitialized<T>()) {
            "Ref-count managed object has not yet been initialized. Unable to acquire."
        }

        refCounted.loop { old ->
            if (old == released<T>()) {
                if (debugRefCounts) {
                    Log.d(
                        TAG,
                        "RefCounted@${"%x".format(hashCode())}.acquire() failure: " +
                            "[refCount: 0]",
                        Throwable()
                    )
                }
                return null
            }

            val (value, oldCount) = old
            val new = Pair(value, oldCount + 1)
            if (refCounted.compareAndSet(old, new)) {
                if (debugRefCounts) {
                    Log.d(
                        TAG,
                        "RefCounted@${"%x".format(hashCode())}<${value::class.simpleName}>" +
                            ".acquire() success: [refCount: ${oldCount + 1}, value: $value]",
                        Throwable()
                    )
                }
                return value
            }
        }
    }

    /**
     * Decrements the ref-count by 1 after a call to [acquire] or [initialize].
     *
     * This should always be called once for each [initialize] call and once for each [acquire] call
     * that does not return `null`.
     */
    fun release() {
        check(refCounted.value != uninitialized<T>()) {
            "Ref-count managed object has not yet been initialized. Unable to release."
        }

        refCounted.loop { old ->
            check(old != released<T>()) { "Release called more times than initialize + acquire." }

            val (value, oldCount) = old
            val new = if (oldCount == 1) released() else Pair(value, oldCount - 1)
            if (refCounted.compareAndSet(old, new)) {
                if (new == released<T>()) {
                    if (debugRefCounts) {
                        Log.d(
                            TAG,
                            "RefCounted@${"%x".format(hashCode())}<${value::class.simpleName}>" +
                                ".release() (last ref): [refCount: 0, value: $value]",
                            Throwable()
                        )
                    }
                    onRelease(value)
                } else {
                    if (debugRefCounts) {
                        Log.d(
                            TAG,
                            "RefCounted@${"%x".format(hashCode())}<${value::class.simpleName}>" +
                                ".release(): [refCount: ${oldCount - 1}, value: $value]",
                            Throwable()
                        )
                    }
                }
                return
            }
        }
    }

    companion object {
        private const val TAG = "RefCounted"
        private val UNINITIALIZED = Pair(Unit, -1)
        private val RELEASED = Pair(Unit, 0)

        @Suppress("UNCHECKED_CAST")
        private fun <T> uninitialized(): Pair<T, Int> {
            return UNINITIALIZED as Pair<T, Int>
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> released(): Pair<T, Int> {
            return RELEASED as Pair<T, Int>
        }
    }
}
