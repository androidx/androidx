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

package androidx.kruth

import kotlin.test.Test

class KruthAsserterTest {

    private val asserter = KruthAsserter(formatMessage = { "$it." })

    @Test
    fun fail() {
        assertFailsWithMessage("Msg.") {
            asserter.fail(message = "Msg")
        }
    }

    @Test
    fun assertTrue_success() {
        asserter.assertTrue(actual = true)
    }

    @Test
    fun assertTrue_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertTrue(actual = false, message = "Msg")
        }
    }

    @Test
    fun assertFalse_success() {
        asserter.assertFalse(actual = false)
    }

    @Test
    fun assertFalse_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertFalse(actual = true, message = "Msg")
        }
    }

    @Test
    fun assertEquals_success() {
        asserter.assertEquals(expected = 0, actual = 0)
    }

    @Test
    fun assertEquals_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertEquals(expected = 0, actual = 1, message = "Msg")
        }
    }

    @Test
    fun assertNotEquals_success() {
        asserter.assertNotEquals(illegal = 0, actual = 1)
    }

    @Test
    fun assertNotEquals_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertNotEquals(illegal = 0, actual = 0, message = "Msg")
        }
    }

    @Test
    fun assertNull_success() {
        asserter.assertNull(actual = null)
    }

    @Test
    fun assertNull_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertNull(actual = 0, message = "Msg")
        }
    }

    @Test
    fun assertNotNull_success() {
        asserter.assertNotNull(actual = 0)
    }

    @Test
    fun assertNotNull_failsWithMessage() {
        assertFailsWithMessage("Msg.") {
            asserter.assertNotNull(actual = null, message = "Msg")
        }
    }
}
