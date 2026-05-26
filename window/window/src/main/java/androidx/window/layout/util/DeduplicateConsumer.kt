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

package androidx.window.layout.util

import androidx.core.util.Consumer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** A [Consumer] that only notifies the [callback] if the value has changed. */
internal class DeduplicateConsumer<T>(private val callback: Consumer<T>) : Consumer<T> {
    private val lock = ReentrantLock()
    private var lastValue: T? = null

    override fun accept(value: T) {
        val shouldNotify =
            lock.withLock {
                if (lastValue != value) {
                    lastValue = value
                    true
                } else {
                    false
                }
            }
        if (shouldNotify) {
            callback.accept(value)
        }
    }
}
