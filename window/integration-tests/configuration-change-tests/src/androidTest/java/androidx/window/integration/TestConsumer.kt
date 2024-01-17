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
package androidx.window.integration

import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.junit.Assert.assertTrue

/**
 * Data structure to hold values in a mutable list.
 */
internal class TestConsumer<T>(count: Int) : Consumer<T> {
    private val valueLock = ReentrantLock()
    @GuardedBy("valueLock")
    private val values = mutableListOf<T>()

    private val countDownLock = ReentrantLock()
    @GuardedBy("countDownLock")
    private var valueLatch = CountDownLatch(count)

    private val waitTimeSeconds: Long = 3L

    /**
     * Appends the new value at the end of the mutable list values.
     */
    override fun accept(value: T) {
        valueLock.withLock {
            values.add(value)
        }
        countDownLock.withLock {
            valueLatch.countDown()
        }
    }

    /**
     * Returns the current number of values stored.
     */
    private fun size(): Int {
        valueLock.withLock {
            return values.size
        }
    }

    /**
     * Waits for the mutable list's length to be at a certain number (count).
     * The method will wait waitTimeSeconds for the count before asserting false.
     */
    fun waitForValueCount() {
        assertTrue(
            // Wait a total of waitTimeSeconds for the count before throwing an assertion error.
            try {
                valueLatch.await(waitTimeSeconds, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                false
            }
        )
    }

    /**
     * Returns {@code true} if there are no stored values, {@code false} otherwise.
     */
    fun isEmpty(): Boolean {
        return size() == 0
    }

    /**
     * Returns the object in the mutable list at the requested index.
     */
    fun get(valueIndex: Int): T {
        valueLock.withLock {
            return values[valueIndex]
        }
    }
}
