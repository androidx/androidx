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
 * https://github.com/google/truth/blob/master/core/src/test/java/com/google/common/truth/StringSubjectTest.java
 *
 * Note: This does not include assertions against failure messages, as Kruth does not currently
 * implement the same fact system Truth has yet.
 */
class StringSubjectTest {

    @Test
    fun stringContains() {
        assertThat("abc").contains("c")
    }

    @Test
    fun stringContainsCharSeq() {
        val charSeq: CharSequence = StringBuilder("c")
        assertThat("abc").contains(charSeq)
    }

    @Test
    fun stringContainsFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").contains("d")
        }
    }

    @Test
    fun hasLength() {
        assertThat("kurt").hasLength(4)
    }

    @Test
    fun hasLengthZero() {
        assertThat("").hasLength(0)
    }

    @Test
    fun hasLengthFails() {
        assertFailsWith<AssertionError> {
            assertThat("kurt").hasLength(5)
        }
    }

    @Test
    fun hasLengthNegative() {
        assertFailsWith<AssertionError> {
            assertThat("kurt").hasLength(-1)
        }
    }

    @Test
    fun stringIsEmpty() {
        assertThat("").isEmpty()
    }

    @Test
    fun stringIsEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").isEmpty()
        }
    }

    @Test
    fun stringIsEmptyFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isEmpty()
        }
    }

    @Test
    fun stringIsNotEmpty() {
        assertThat("abc").isNotEmpty()
    }

    @Test
    fun stringIsNotEmptyFail() {
        assertFailsWith<AssertionError> {
            assertThat("").isNotEmpty()
        }
    }

    @Test
    fun stringIsNotEmptyFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).isNotEmpty()
        }
    }

    @Test
    fun stringDoesNotContain() {
        assertThat("abc").doesNotContain("d")
    }

    @Test
    fun stringDoesNotContainCharSequence() {
        val testString = "d"
        val charSeq: CharSequence = testString.subSequence(0, testString.length)
        assertThat("abc").doesNotContain(charSeq)
    }

    @Test
    fun stringDoesNotContainFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").doesNotContain("b")
        }
    }

    @Test
    fun stringStartsWith() {
        assertThat("abc").startsWith("ab")
    }

    @Test
    fun stringStartsWithFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").startsWith("bc")
        }
    }

    @Test
    fun stringEndsWith() {
        assertThat("abc").endsWith("bc")
    }

    @Test
    fun stringEndsWithFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").endsWith("ab")
        }
    }

    @Test
    fun emptyStringTests() {
        assertThat("").contains("")
        assertThat("").startsWith("")
        assertThat("").endsWith("")
        assertThat("a").contains("")
        assertThat("a").startsWith("")
        assertThat("a").endsWith("")
    }

    @Test
    fun stringEqualityIgnoringCase() {
        assertThat("café").ignoringCase().isEqualTo("CAFÉ")
    }

    @Test
    fun stringEqualityIgnoringCaseWithNullSubject() {
        assertThat(null as String?).ignoringCase().isEqualTo(null)
    }

    @Test
    fun stringEqualityIgnoringCaseFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").ignoringCase().isEqualTo("abd")
        }
    }

    @Test
    fun stringEqualityIgnoringCaseFailWithNullSubject() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).ignoringCase().isEqualTo("abc")
        }
    }

    @Test
    fun stringEqualityIgnoringCaseFailWithNullExpectedString() {
        assertFailsWith<AssertionError> {
            assertThat("abc").ignoringCase().isEqualTo(null)
        }
    }

    @Test
    fun stringInequalityIgnoringCase() {
        assertThat("café").ignoringCase().isNotEqualTo("AFÉ")
    }

    @Test
    fun stringInequalityIgnoringCaseWithNullSubject() {
        assertThat(null as String?).ignoringCase().isNotEqualTo("abc")
    }

    @Test
    fun stringInequalityIgnoringCaseWithNullExpectedString() {
        assertThat("abc").ignoringCase().isNotEqualTo(null)
    }

    @Test
    fun stringInequalityIgnoringCaseFail() {
        assertFailsWith<AssertionError> {
            assertThat("café").ignoringCase().isNotEqualTo("CAFÉ")
        }
    }

    @Test
    fun stringInequalityIgnoringCaseFailWithNullSubject() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).ignoringCase().isNotEqualTo(null)
        }
    }

    @Test
    fun stringContainsIgnoringCase() {
        assertThat("äbc").ignoringCase().contains("Ä")
    }

    @Test
    fun stringContainsIgnoringCaseEmptyString() {
        assertThat("abc").ignoringCase().contains("")
    }

    @Test
    fun stringContainsIgnoringCaseWithWord() {
        assertThat("abcdé").ignoringCase().contains("CdÉ")
    }

    @Test
    fun stringContainsIgnoringCaseWholeWord() {
        assertThat("abcde").ignoringCase().contains("ABCde")
    }

    @Test
    fun stringContainsIgnoringCaseCharSeq() {
        val charSeq: CharSequence = StringBuilder("C")
        assertThat("abc").ignoringCase().contains(charSeq)
    }

    @Test
    fun stringContainsIgnoringCaseFail() {
        assertFailsWith<AssertionError> {
            assertThat("abc").ignoringCase().contains("d")
        }
    }

    @Test
    fun stringContainsIgnoringCaseFailBecauseTooLarge() {
        assertFailsWith<AssertionError> {
            assertThat("abc").ignoringCase().contains("abcc")
        }
    }

    @Test
    fun stringContainsIgnoringCaseFailBecauseNullSubject() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).ignoringCase().contains("d")
        }
    }

    @Test
    fun stringDoesNotContainIgnoringCase() {
        assertThat("äbc").ignoringCase().doesNotContain("Äc")
    }

    @Test
    fun stringDoesNotContainIgnoringCaseCharSeq() {
        val charSeq: CharSequence = StringBuilder("cb")
        assertThat("abc").ignoringCase().doesNotContain(charSeq)
    }

    @Test
    fun stringDoesNotContainIgnoringCaseFail() {
        assertFailsWith<AssertionError> {
            assertThat("äbc").ignoringCase().doesNotContain("Äb")
        }
    }

    @Test
    fun stringDoesNotContainIgnoringCaseFailWithEmptyString() {
        assertFailsWith<AssertionError> {
            assertThat("abc").ignoringCase().doesNotContain("")
        }
    }

    @Test
    fun stringDoesNotContainIgnoringCaseFailBecauseNullSubject() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).ignoringCase().doesNotContain("d")
        }
    }
}
