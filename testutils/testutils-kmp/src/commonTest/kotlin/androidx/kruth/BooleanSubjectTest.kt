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
 * A mirror of existing tests in official Truth for parity.
 *
 * Partially migrated from Truth's source in Github:
 * https://github.com/google/truth/blob/master/core/src/test/java/com/google/common/truth/BooleanSubjectTest.java
 *
 * Note: This does not include assertions against failure messages, as Kruth does not currently
 * implement the same fact system Truth has yet.
 */
class BooleanSubjectTest {

    @Test
    fun isTrue() {
        assertThat(true).isTrue()
    }

    @Test
    fun nullIsTrueFailing() {
        assertFailsWith<AssertionError> {
            assertThat(null as Boolean?).isTrue()
        }
    }

    @Test
    fun nullIsFalseFailing() {
        assertFailsWith<AssertionError> {
            assertThat(null as Boolean?).isFalse()
        }
    }

    @Test
    fun isTrueFailing() {
        assertFailsWith<AssertionError> {
            assertThat(false).isTrue()
        }
    }

    @Test
    fun isFalse() {
        assertThat(false).isFalse()
    }

    @Test
    fun isFalseFailing() {
        assertFailsWith<AssertionError> {
            assertThat(true).isFalse()
        }
    }
}