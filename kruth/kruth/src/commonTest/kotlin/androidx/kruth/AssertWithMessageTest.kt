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
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class AssertWithMessageTest {

    private val assert = assertWithMessage("Msg1").withMessage("Msg2")

    @Test
    fun that_customObjects_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that(Some(value = 0)).isEqualTo(Some(value = 1))
        }
    }

    @Test
    fun that_comparable_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that(1).isLessThan(0)
        }
    }

    @Test
    fun that_throwable_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that(Exception("Msg")).hasMessageThat().contains("NonExistentString")
        }
    }

    @Test
    fun that_boolean_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that(false).isTrue()
        }
    }

    @Test
    fun that_string_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that("Str").contains("NonExistentString")
        }
    }

    @Test
    fun that_iterable_errorContainsMessage() {
        assertFailsWithMessage {
            assert.that(listOf(1, 2, 3)).contains(4)
        }
    }

    @Test
    fun fail_errorContainsMessage() {
        assertFailsWithMessage {
            assert.fail()
        }
    }

    private fun assertFailsWithMessage(block: () -> Unit) {
        try {
            block()
            fail("Expected to fail but didn't")
        } catch (e: AssertionError) {
            val msg = assertNotNull(e.message)
            assertTrue(msg.startsWith("Msg1"))
            assertContains(msg, "Msg2")
        }
    }

    private data class Some(val value: Int)
}
