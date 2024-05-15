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
import kotlin.test.assertFailsWith

class StringSubjectJvmTest {

    @Test
    fun stringMatchesPattern() {
        assertThat("abcaaadev").matches(".*aaa.*".toPattern())
    }

    @Test
    fun stringMatchesPatternWithFail() {
        assertFailsWith<AssertionError> {
            assertThat("abcaqadev").matches(".*aaa.*".toPattern())
        }
    }

    @Test
    fun stringMatchesPatternFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).matches(".*aaa.*".toPattern())
        }
    }

    @Test
    fun stringMatchesPatternLiteralFail() {
        assertFailsWith<AssertionError> {
            assertThat("\$abc").matches("\$abc".toPattern())
        }
    }

    @Test
    fun stringDoesNotMatchPattern() {
        assertThat("abcaqadev").doesNotMatch(".*aaa.*".toPattern())
    }

    @Test
    fun stringDoesNotMatchPatternWithFail() {
        assertFailsWith<AssertionError> {
            assertThat("abcaaadev").doesNotMatch(".*aaa.*".toPattern())
        }
    }

    @Test
    fun stringDoesNotMatchPatternFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).doesNotMatch(".*aaa.*".toPattern())
        }
    }

    @Test
    fun stringContainsMatchStringUsesFind() {
        assertThat("aba").containsMatch("[b]")
        assertThat("aba").containsMatch("[b]".toPattern())
    }

    @Test
    fun stringContainsMatchPattern() {
        assertThat("aba").containsMatch(".*b.*".toPattern())
        assertFailsWith<AssertionError> {
            assertThat("aaa").containsMatch(".*b.*".toPattern())
        }
    }

    @Test
    fun stringContainsMatchPatternFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).containsMatch(".*b.*".toPattern())
        }
    }

    @Test
    fun stringDoesNotContainMatchPattern() {
        assertThat("zzaaazz").doesNotContainMatch(".b.".toPattern())
        assertFailsWith<AssertionError> {
            assertThat("zzabazz").doesNotContainMatch(".b.".toPattern())
        }
    }

    @Test
    fun stringDoesNotContainMatchPatternFailNull() {
        assertFailsWith<AssertionError> {
            assertThat(null as String?).doesNotContainMatch(".b.".toPattern())
        }
    }
}
