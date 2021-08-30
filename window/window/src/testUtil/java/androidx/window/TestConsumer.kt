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

package androidx.window

import androidx.core.util.Consumer
import org.junit.Assert.assertEquals

/**
 * A test [Consumer] to hold all the values and make assertions based on the values.
 */
public class TestConsumer<T> : Consumer<T> {

    private val values: MutableList<T> = mutableListOf()

    /**
     * Add the value to the list of seen values.
     */
    override fun accept(t: T) {
        values.add(t)
    }

    /**
     * Assert that there have been a fixed number of values
     */
    public fun assertValueCount(count: Int) {
        assertEquals(count, values.size)
    }

    /**
     * Assert that there has been exactly one value.
     */
    public fun assertValue(t: T) {
        assertValueCount(1)
        assertEquals(t, values[0])
    }

    public fun assertValues(vararg expected: T) {
        assertValueCount(expected.size)
        assertEquals(expected.toList(), values.toList())
    }
}
