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
import kotlin.test.Test

class WithBackStackTest {

    @Test
    fun withBackStack() {
        val matcher = TestDeepLinkMatcher().withBackStack { listOf(ParentKeyA, it.key) }
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)

        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
        assertThat(result?.backStack).containsExactly(ParentKeyA, TestKey).inOrder()
    }

    @Test
    fun withBackStack_wrongNestedFilter() {
        var builderCalled = false
        val matcher =
            TestDeepLinkMatcher(filters = listOf(TestFilter("wrongFilter"))).withBackStack {
                builderCalled = true
                listOf(it.key)
            }
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)

        assertThat(result).isNull()
        assertThat(builderCalled).isFalse()
    }

    @Test
    fun withBackStack_emptyBackStack() {
        val matcher = TestDeepLinkMatcher().withBackStack { emptyList<Any>() }
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)

        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
        assertThat(result?.backStack).isEmpty()
    }

    @Test
    fun withBackStack_backStackAndKeyTypePreserved() {
        val matcher1 =
            StaticKeyDeepLinkMatcher(TestInterfaceImplA, emptyList()).withBackStack {
                listOf(TestInterfaceImplC, it.key)
            }
        val matcher2 =
            StaticKeyDeepLinkMatcher(TestInterfaceImplB, emptyList()).withBackStack {
                listOf(TestInterfaceImplC, it.key)
            }

        val matchers: List<BackStackMatcher<TestInterface, TestInterface>> =
            listOf(matcher1, matcher2)
        val request = DeepLinkRequest("https://www.test.com")
        val results: List<BackStackMatchResult<TestInterface, TestInterface>?> =
            matchers.map { it.match(request) }

        val firstResult = results.first()!!
        val backStack: List<TestInterface> = firstResult.backStack
        val key: TestInterface = firstResult.key
        assertThat(backStack).containsExactly(TestInterfaceImplC, TestInterfaceImplA).inOrder()
        assertThat(key).isEqualTo(TestInterfaceImplA)
    }

    @Test
    fun compareTo_basedOnNestedMatchResult() {
        val higherInner = ScoredMatchResult(TestKey, score = 10)
        val lowerInner = ScoredMatchResult(TestKey, score = 5)

        val resultHigher = BackStackMatchResult(higherInner) { listOf(TestKey) }
        val resultLower = BackStackMatchResult(lowerInner) { listOf(TestKey) }
        val resultEqual = BackStackMatchResult(higherInner) { listOf(TestKey) }

        assertThat(resultHigher.compareTo(resultLower)).isGreaterThan(0)
        assertThat(resultLower.compareTo(resultHigher)).isLessThan(0)
        assertThat(resultEqual.compareTo(resultHigher)).isEqualTo(0)
    }

    private class ScoredMatchResult(key: Any, val score: Int) :
        DeepLinkMatcher.MatchResult<Any>(key) {
        override fun compareTo(other: DeepLinkMatcher.MatchResult<Any>): Int {
            val otherResult = if (other is WrappedMatchResult<*>) other.matchResult else other
            if (otherResult is ScoredMatchResult) {
                return score.compareTo(otherResult.score)
            }
            return super.compareTo(other)
        }
    }
}

private object ChildKey

private object ParentKeyA

private object ParentKeyB
