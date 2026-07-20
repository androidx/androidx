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

package androidx.navigation3.runtime.deeplink

import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test

class WrappedMatchResultTest {

    @Test
    fun compareTo_nonWrapped() {
        val wrapped = WrappedTestMatchResult(MatchResult(KeyA, score = 5))
        val unwrappedLower = MatchResult(KeyB, score = 3)
        val unwrappedHigher = MatchResult(KeyB, score = 10)
        val unwrappedEqual = MatchResult(KeyB, score = 5)

        assertThat(wrapped.compareTo(unwrappedLower)).isGreaterThan(0)
        assertThat(wrapped.compareTo(unwrappedHigher)).isLessThan(0)
        assertThat(wrapped.compareTo(unwrappedEqual)).isEqualTo(0)
    }

    @Test
    fun compareTo_wrappedSameInnerType() {
        val wrappedHigher = WrappedTestMatchResult(MatchResult(KeyA, score = 10))
        val wrappedLower = WrappedTestMatchResult(MatchResult(KeyA, score = 5))
        val wrappedEqual = WrappedTestMatchResult(MatchResult(KeyA, score = 10))

        assertThat(wrappedHigher.compareTo(wrappedLower)).isGreaterThan(0)
        assertThat(wrappedLower.compareTo(wrappedHigher)).isLessThan(0)
        assertThat(wrappedHigher.compareTo(wrappedEqual)).isEqualTo(0)
    }

    @Test
    fun compareTo_wrappedDifferentInnerTypes() {
        val wrappedTypeA = WrappedTestMatchResult(MatchResult(KeyA, score = 10))
        val wrappedTypeB = WrappedTestMatchResult(MatchResultOther(KeyB, priority = 5))

        assertThat(wrappedTypeA.compareTo(wrappedTypeB)).isGreaterThan(0)
        assertThat(wrappedTypeB.compareTo(wrappedTypeA)).isLessThan(0)
    }

    @Test
    fun nonWrapped_compareTo() {
        val wrapped = WrappedTestMatchResult(MatchResult(KeyA, score = 5))
        val unwrappedLower = MatchResult(KeyB, score = 3)
        val unwrappedHigher = MatchResult(KeyB, score = 10)
        val unwrappedEqual = MatchResult(KeyB, score = 5)

        assertThat(unwrappedLower.compareTo(wrapped)).isLessThan(0)
        assertThat(unwrappedHigher.compareTo(wrapped)).isGreaterThan(0)
        assertThat(unwrappedEqual.compareTo(wrapped)).isEqualTo(0)
    }

    private class MatchResult(key: NavKey, val score: Int) :
        DeepLinkMatcher.MatchResult<NavKey>(key) {
        override fun compareTo(other: DeepLinkMatcher.MatchResult<NavKey>): Int {
            if (other is WrappedTestMatchResult && other.matchResult is MatchResult) {
                return score.compareTo(other.matchResult.score)
            }
            if (other is MatchResult) {
                return score.compareTo(other.score)
            }
            if (other is MatchResultOther) {
                return score.compareTo(other.priority)
            }
            return super.compareTo(other)
        }
    }

    private class MatchResultOther(key: NavKey, val priority: Int) :
        DeepLinkMatcher.MatchResult<NavKey>(key) {
        override fun compareTo(other: DeepLinkMatcher.MatchResult<NavKey>): Int {
            if (other is MatchResultOther) {
                return priority.compareTo(other.priority)
            }
            if (other is MatchResult) {
                return priority.compareTo(other.score)
            }
            return super.compareTo(other)
        }
    }

    private class WrappedTestMatchResult(matchResult: DeepLinkMatcher.MatchResult<NavKey>) :
        WrappedMatchResult<NavKey>(matchResult)

    private object KeyA : NavKey

    private object KeyB : NavKey
}
