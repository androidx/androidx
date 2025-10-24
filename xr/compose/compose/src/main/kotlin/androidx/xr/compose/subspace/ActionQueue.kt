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

import androidx.compose.ui.util.fastForEach

/**
 * A helper utility that can queue actions to be executed when the target value is available.
 *
 * If the target is available the actions will be executed immediately in the same thread; however,
 * if the target is null the actions will be queued to be executed later once [value] is set.
 *
 * This class is not thread-safe and should only be used from a single thread.
 */
internal class ActionQueue<T>(initialValue: T? = null) {
    private val queue = mutableListOf<T.() -> Any?>()

    var value: T? = initialValue
        set(value) {
            field = value
            if (value != null) {
                queue.fastForEach { action -> value.action() }
                clear()
            }
        }

    /**
     * Execute the given action immediately for the current target if the target is available (not
     * null); otherwise, enqueue the action to be run on the next target when the target is set with
     * [value].
     */
    fun <U> executeWhenAvailable(action: T.() -> U): U? {
        return value?.run(action)
            ?: run {
                queue.add(action)
                null
            }
    }

    fun clear() {
        queue.clear()
    }
}
