/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace

/**
 * A helper utility that can queue actions to be executed when the target value is available.
 *
 * If the target is available the actions will be executed immediately in the same thread; however,
 * if the target is null the actions will be queued to be executed later once [value] is set. If
 * [value] becomes invalidated, execution will stop and actions will be queued until a new, valid
 * [value] is set.
 *
 * This class is not thread-safe and should only be used from a single thread.
 *
 * @param initialValue the initial value of the queue. If a valid value is initially provided then
 *   calls to [executeWhenAvailable] will trigger immediately in the same thread.
 * @property isValid a predicate function that indicates whether the current value is valid. This
 *   will be checked before and after executing any action on the value. If the value is invalid,
 *   execution of the queue will be paused and the value will be set to null. Execution will not
 *   proceed until a new, valid value is set.
 */
internal class ActionQueue<T>(
    initialValue: T? = null,
    private val isValid: (T) -> Boolean = { true },
) {
    private val queue = ArrayDeque<(T) -> Any?>()

    var value: T? = initialValue
        get() = field?.takeIf(isValid)
        set(nextValue) {
            field = nextValue
            while (value != null && !queue.isEmpty()) {
                field?.let(queue.removeFirst())
            }
        }

    /**
     * Execute the given action immediately for the current target if the target is available (not
     * null); otherwise, enqueue the action to be run on the next target when the target is set with
     * [value].
     */
    fun <U> executeWhenAvailable(action: (T) -> U): U? {
        if (value == null) {
            queue.add(action)
            return null
        }
        return value?.let(action)
    }

    fun clear() {
        queue.clear()
    }
}
