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

package androidx.core.testing.util

import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An implementation of [Consumer] to capture values during a test and allows developers to perform
 * assertions on the values.
 *
 * @param T the type of the input to the operation
 */
public class TestConsumer<T> : Consumer<T> {

    private val lock = ReentrantLock()

    @GuardedBy("lock") private val values = mutableListOf<T>()

    /**
     * Records the value in the order it was received.
     *
     * @param t the input argument.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") /* Avoid breaking named parameter compat */
    override fun accept(t: T) {
        lock.withLock { values.add(t) }
    }

    /**
     * Asserts that the [values] match the received values. This method checks the order and the
     * elements.
     *
     * @param values expected to be in the [TestConsumer]
     * @throws AssertionError if the values do not match the current values.
     */
    public fun assertValues(values: List<T>) {
        lock.withLock {
            if (this.values != values) {
                throw AssertionError("Expected $values but received ${this.values}")
            }
        }
    }
}
