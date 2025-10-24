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

package androidx.compose.ui.test.util

import org.junit.Assert.fail

/**
 * Asserts that [expect] calls occur in a specific order. Useful for coroutine dispatching tests.
 */
internal class TestCounter {
    private var count = 0

    fun expect(checkpoint: Int, message: String = "(no message)") {
        // `checkpoint` is the "expected", but keeping the name for API clarity
        val actual = count + 1
        if (checkpoint != actual) {
            fail("events out of order: expected=$checkpoint, actual=$actual, $message")
        }
        count = actual
    }
}
