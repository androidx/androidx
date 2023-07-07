/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.testutils

import java.io.IOException
import org.junit.Test

class AssertionsTest {
    @Test
    fun testNoFailureThrowsAssertionError() {
        try {
            assertThrows(IOException::class.java) {
                // No Exception thrown
            }
        } catch (e: AssertionError) {
            return // expected
        }

        fail("expected assertion error for no failure")
    }

    @Test
    fun testIncorrectFailureThrowsAssertionError() {
        try {
            assertThrows(IOException::class.java) {
                throw IllegalStateException()
            }
        } catch (e: IllegalStateException) {
            return // expected
        }

        fail("expected IllegalStateException to propagate")
    }

    @Test
    fun testCorrectFailureTypeIsCaughtAndReturnsAsThrowableSubject() {
        assertThrows(IOException::class.java) {
            throw IOException("test123")
        }.hasMessageThat().contains("test123")
    }
}
