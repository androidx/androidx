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

import com.google.common.base.Optional
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class GuavaOptionalSubjectTest {

    @Test
    fun isPresent() {
        assertThat(Optional.of("foo")).isPresent()
    }

    @Test
    fun isPresentFailing() {
        assertFailsWith<AssertionError> {
            assertThat(Optional.absent()).isPresent()
        }
    }

    @Test
    fun isPresentFailingNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as Optional<Any>?).isPresent()
        }
    }

    @Test
    fun isAbsent() {
        assertThat(Optional.absent()).isAbsent()
    }

    @Test
    fun isAbsentFailing() {
        assertFailsWith<AssertionError> {
            assertThat(Optional.of("foo")).isAbsent()
        }
    }

    @Test
    fun isAbsentFailingNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as Optional<Any>?).isAbsent()
        }
    }

    @Test
    fun hasValue() {
        assertThat(Optional.of("foo")).hasValue("foo")
    }

    @Test
    fun hasValue_failingWithAbsent() {
        assertFailsWith<AssertionError> {
            assertThat(Optional.absent()).hasValue("foo")
        }
    }

    @Test
    fun hasValue_npeWithNullParameter() {
        try {
            assertThat(Optional.of("foo")).hasValue(null)
            fail("Expected NPE")
        } catch (expected: NullPointerException) {
            assertThat(expected).hasMessageThat().contains("Optional")
        }
    }

    @Test
    fun hasValue_failingWithWrongValue() {
        assertFailsWith<AssertionError> {
            assertThat(Optional.of("foo")).hasValue("boo")
        }
    }
}
