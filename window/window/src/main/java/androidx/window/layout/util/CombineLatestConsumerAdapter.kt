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

/**
 * A utility class to combine two streams of values and notify a consumer with the combined value.
 * This class waits until both values are present before notifying the consumer for the first time.
 */
internal class CombineLatestConsumerAdapter<T, U, W>(
    private val transform: (T, U) -> W,
    private val consumer: Consumer<W>,
) {
    private val lock = ReentrantLock()
    private var lastT: T? = null
    private var lastU: U? = null

    /** Updates the value of T and notifies the consumer if both T and U are available. */
    fun updateT(newValue: T) {
        lock.withLock {
            lastT = newValue
            notifyConsumer()
        }
    }

    /** Updates the value of U and notifies the consumer if both T and U are available. */
    fun updateU(newValue: U) {
        lock.withLock {
            lastU = newValue
            notifyConsumer()
        }
    }

    private fun notifyConsumer() {
        val t = lastT ?: return
        val u = lastU ?: return
        consumer.accept(transform(t, u))
    }

    /** A [Consumer] for T. */
    val consumerT: Consumer<T> = Consumer { updateT(it) }

    /** A [Consumer] for U. */
    val consumerU: Consumer<U> = Consumer { updateU(it) }
}
