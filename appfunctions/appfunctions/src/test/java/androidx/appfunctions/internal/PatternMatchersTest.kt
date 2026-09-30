/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.internal

import android.os.PatternMatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class PatternMatchersTest {

    @Test
    fun create_validPattern_returnsMatcher() {
        val matcher = PatternMatchers.create("[0-9]+", PatternMatcher.PATTERN_ADVANCED_GLOB)

        assertThat(matcher.path).isEqualTo("[0-9]+")
        assertThat(matcher.type).isEqualTo(PatternMatcher.PATTERN_ADVANCED_GLOB)
    }

    @Test
    fun create_unknownType_returnsMatcher() {
        val matcher = PatternMatchers.create("x", 99)

        assertThat(matcher.path).isEqualTo("x")
        assertThat(matcher.type).isEqualTo(99)
    }

    @Test
    fun create_invalidAdvancedGlob_throws() {
        assertThrows(InvalidPatternMatcherException::class.java) {
            PatternMatchers.create("[", PatternMatcher.PATTERN_ADVANCED_GLOB)
        }
    }

    @Test
    fun createList_validPatterns_returnsMatchersInOrder() {
        val matchers =
            PatternMatchers.createList(
                "content:" to PatternMatcher.PATTERN_PREFIX,
                "x" to 99,
            )

        assertThat(matchers.map { it.path to it.type })
            .containsExactly("content:" to PatternMatcher.PATTERN_PREFIX, "x" to 99)
            .inOrder()
    }

    @Test
    fun createList_anyInvalidPattern_throws() {
        assertThrows(InvalidPatternMatcherException::class.java) {
            PatternMatchers.createList(
                "content:" to PatternMatcher.PATTERN_PREFIX,
                "[" to PatternMatcher.PATTERN_ADVANCED_GLOB,
            )
        }
    }
}
