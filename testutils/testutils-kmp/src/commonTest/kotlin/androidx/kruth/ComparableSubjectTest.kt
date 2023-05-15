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
 * https://github.com/google/truth/blob/master/core/src/test/java/com/google/common/truth/ComparableSubjectTest.java
 *
 * Note: This does not include assertions against failure messages, as Kruth does not currently
 * implement the same fact system Truth has yet.
 */
class ComparableSubjectTest {

    @Test
    fun testNulls() {
        assertFailsWith<NullPointerException> {
            assertThat(6).isLessThan(null)
        }
        assertFailsWith<NullPointerException> {
            assertThat(6).isAtLeast(null)
        }
    }

    @Test
    fun isLessThan_failsEqual() {
        assertThat(4).isLessThan(5)
        assertFailsWith<AssertionError> {
            assertThat(4).isLessThan(4)
        }
    }

    @Test
    fun isLessThan_failsGreater() {
        assertFailsWith<AssertionError> {
            assertThat(4).isLessThan(3)
        }
    }

    @Test
    fun isAtLeast() {
        assertThat(4).isAtLeast(3)
        assertThat(4).isAtLeast(4)
        assertFailsWith<AssertionError> {
            assertThat(4).isAtLeast(5)
        }
    }

    // Brief tests with other comparable types (no negative test cases)

    @Test
    fun longs() {
        assertThat(4L).isLessThan(5L)

        assertThat(4L).isAtLeast(4L)
        assertThat(4L).isAtLeast(3L)
    }

    @Test
    fun strings() {
        assertThat("gak").isLessThan("kak")

        assertThat("kak").isAtLeast("kak")
        assertThat("kak").isAtLeast("gak")
    }

    @Test
    fun comparableType() {
        assertThat(ComparableType(3)).isLessThan(ComparableType(4))
    }

    @Test
    fun rawComparableType() {
        assertThat(RawComparableType(3)).isLessThan(RawComparableType(4))
    }
}

private class ComparableType(val wrapped: Int) : Comparable<ComparableType> {

    override fun compareTo(other: ComparableType): Int {
        return wrapped - other.wrapped
    }
}

private class RawComparableType(val wrapped: Int) : Comparable<RawComparableType> {

    override fun compareTo(other: RawComparableType): Int {
        return wrapped - other.wrapped
    }

    override fun toString(): String {
        return wrapped.toString()
    }
}