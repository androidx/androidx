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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.makeMessage
import androidx.kruth.Fact.Companion.simpleFact
import kotlin.test.Test

class FactTest {
    @Test
    fun string() {
        assertThat(fact("foo", "bar").toString()).isEqualTo("foo: bar")
    }

    @Test
    fun stringWithoutValue() {
        assertThat(simpleFact("foo").toString()).isEqualTo("foo")
    }

    @Test
    fun oneFacts() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(fact("foo", "bar"))
            )
        )
            .isEqualTo("foo: bar")
    }

    @Test
    fun twoFacts() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(fact("foo", "bar"), fact("longer name", "other value"))
            )
        )
            .isEqualTo("foo        : bar\nlonger name: other value")
    }

    @Test
    fun oneFactWithoutValue() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(simpleFact("foo"))
            )
        )
            .isEqualTo("foo")
    }

    @Test
    fun twoFactsOneWithoutValue() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(fact("hello", "there"), simpleFact("foo"))
            )
        )
            .isEqualTo("hello: there\nfoo")
    }

    @Test
    fun newline() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(fact("foo", "bar\nbaz"))
            )
        )
            .isEqualTo("foo:\n    bar\n    baz")
    }

    @Test
    fun newlineWithoutValue() {
        assertThat(
            makeMessage(
                emptyList(),
                listOf(fact("hello", "there\neveryone"), simpleFact("xyz"))
            )
        )
            .isEqualTo("hello:\n    there\n    everyone\nxyz")
    }

    @Test
    fun withMessage() {
        assertThat(
            makeMessage(
                listOf("hello"),
                listOf(fact("foo", "bar"))
            )
        )
            .isEqualTo("hello\nfoo: bar")
    }

    @Test
    fun failWithActual_simpleFact() {
        val subject =
            object : Subject<Int>(
                actual = 0,
            ) {
                fun fail() {
                    failWithActual(simpleFact("Expected something else"))
                }
            }

        assertFailsWithMessage(
            """
                Expected something else
                but was: 0
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithActual_multipleFacts() {
        val subject =
            object : Subject<Int>(
                actual = 0,
            ) {
                fun fail() {
                    failWithActual(
                        simpleFact("Expected something else"),
                        fact("expected", "1"),
                    )
                }
            }

        assertFailsWithMessage(
            """
                Expected something else
                expected: 1
                but was : 0
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithoutActual_simpleFact() {
        val subject =
            object : Subject<Int>(
                actual = 0,
            ) {
                fun fail() {
                    failWithoutActual(simpleFact("Expected something else"))
                }
            }

        assertFailsWithMessage(
            """
                Expected something else
            """.trimIndent()
        ) { subject.fail() }
    }

    @Test
    fun failWithoutActual_multipleFacts() {
        val subject =
            object : Subject<Int>(
                actual = 0,
            ) {
                fun fail() {
                    failWithoutActual(
                        simpleFact("Expected something else"),
                        fact("Found", "$actual"),
                    )
                }
            }

        assertFailsWithMessage(
            """
                Expected something else
                Found: 0
            """.trimIndent()
        ) { subject.fail() }
    }
}
