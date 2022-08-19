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

package androidx.kruth

import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for [Throwable] subjects.
 *
 * A mirror of existing tests in official Truth for parity.
 *
 * Partially migrated from Truth's source in Github:
 * https://github.com/google/truth/blob/master/core/src/test/java/com/google/common/truth/ThrowableSubjectTest.java
 *
 * Note: This does not include assertions against failure messages, as Kruth does not currently
 * implement the same fact system Truth has yet.
 */
class ThrowableSubjectTest {

    @Test
    fun hasMessageThat() {
        val npe = NullPointerException("message")
        assertThat(npe).hasMessageThat().isEqualTo("message")
    }

    @Test
    fun hasMessageThat_null() {
        assertThat(NullPointerException()).hasMessageThat().isNull()
        assertThat(NullPointerException(null)).hasMessageThat().isNull()
    }

    @Test
    fun hasMessageThat_failure() {
        val actual = NullPointerException("message")
        assertFailsWith<AssertionError> {
            assertThat(actual).hasMessageThat().isEqualTo("foobar")
        }
    }

    @Test
    fun hasMessageThat_MessageHasNullMessage_failure() {
        assertFailsWith<AssertionError> {
            assertThat(NullPointerException("message")).hasMessageThat().isNull()
        }
    }

    @Test
    fun hasMessageThat_NullMessageHasMessage_failure() {
        val npe = NullPointerException(null)
        assertFailsWith<AssertionError> {
            assertThat(npe).hasMessageThat().isEqualTo("message")
        }
    }
}
