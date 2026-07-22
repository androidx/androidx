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

class DeepLinkMatcherTest {

    @Test
    fun match_emptyFilters() {
        val matcher = TestDeepLinkMatcher()
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterPasses() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter(MATCHER_STRING_FILTER_VALUE)))
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterFails() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter("wrongFilter")))
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun match_allFiltersPass() {
        val matcher =
            TestDeepLinkMatcher(
                filters =
                    listOf(
                        TestFilter(MATCHER_STRING_FILTER_VALUE),
                        TestFilter(MATCHER_STRING_FILTER_VALUE),
                    )
            )
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_someFiltersFail() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(MATCHER_STRING_FILTER_VALUE), TestFilter("wrongFilter"))
            )
        val request =
            DeepLinkRequest.withStringExtra("https://example.com", MATCHER_STRING_FILTER_VALUE)
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun match_defaultComparator() {
        val result1: DeepLinkMatcher.MatchResult<NavKey> = DeepLinkMatcher.MatchResult(First)
        val result2: DeepLinkMatcher.MatchResult<NavKey> = DeepLinkMatcher.MatchResult(Second)

        assertThat(result1.compareTo(result2)).isEqualTo(0)
        assertThat(result1.compareTo(result2)).isEqualTo(0)
    }

    @Test
    fun match_mimeTypeFilter() {
        val matcher =
            TestDeepLinkMatcher(filters = listOf(DeepLinkMatcher.mimeTypeFilter("image/test")))

        val request =
            DeepLinkRequest(
                uri = DeepLinkUri("https://example.com"),
                extras = DeepLinkRequest.mimeTypeExtra("image/TEST"),
            )
        val result = matcher.match(request)
        assertThat(result).isNotNull()

        val request2 =
            DeepLinkRequest(
                uri = DeepLinkUri("https://example.com"),
                extras = DeepLinkRequest.mimeTypeExtra("image/wrongMimeType"),
            )
        val result2 = matcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun match_actionFilter() {
        val matcher =
            TestDeepLinkMatcher(filters = listOf(DeepLinkMatcher.actionFilter("ACTION.TEST")))
        val request =
            DeepLinkRequest(
                uri = DeepLinkUri("https://example.com"),
                extras = DeepLinkRequest.actionExtra("ACTION.test"),
            )
        val result = matcher.match(request)
        assertThat(result).isNotNull()

        val request2 =
            DeepLinkRequest(
                uri = DeepLinkUri("https://example.com"),
                extras = DeepLinkRequest.actionExtra("wrongAction"),
            )
        val result2 = matcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun compare_matcherClassesTypedOnDerivedKeys() {
        val matcher1 =
            object :
                DeepLinkMatcher<
                    TestInterfaceImplA,
                    DeepLinkMatcher.MatchResult<TestInterfaceImplA>,
                >() {
                override fun matchRequest(
                    request: DeepLinkRequest
                ): MatchResult<TestInterfaceImplA> {
                    return MatchResult(TestInterfaceImplA)
                }
            }
        val matcher2 =
            object :
                DeepLinkMatcher<
                    TestInterfaceImplB,
                    DeepLinkMatcher.MatchResult<TestInterfaceImplB>,
                >() {
                override fun matchRequest(
                    request: DeepLinkRequest
                ): MatchResult<TestInterfaceImplB> {
                    return MatchResult(TestInterfaceImplB)
                }
            }
        val matchers:
            List<DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>> =
            listOf(matcher1, matcher2)

        val request = DeepLinkRequest(DeepLinkUri("www.testuri.com"))

        val results: List<DeepLinkMatcher.MatchResult<TestInterface>> = buildList {
            matchers.forEach { add(it.match(request)!!) }
        }
        assertThat(results.size).isEqualTo(2)
        assertThat(results.first().key).isEqualTo(TestInterfaceImplA)
        assertThat(results.last().key).isEqualTo(TestInterfaceImplB)
    }

    @Test
    fun compare_matcherClassesTypedOnBaseKeys() {
        val matcher1 =
            object : DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>() {
                override fun matchRequest(request: DeepLinkRequest): MatchResult<TestInterface> {
                    return MatchResult(TestInterfaceImplA)
                }
            }
        val matcher2 =
            object : DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>() {
                override fun matchRequest(request: DeepLinkRequest): MatchResult<TestInterface> {
                    return MatchResult(TestInterfaceImplB)
                }
            }
        val matchers:
            List<DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>> =
            listOf(matcher1, matcher2)

        val request = DeepLinkRequest(DeepLinkUri("https://www.testuri.com"))

        val results: List<DeepLinkMatcher.MatchResult<TestInterface>> = buildList {
            matchers.forEach { add(it.match(request)!!) }
        }
        assertThat(results.size).isEqualTo(2)
        assertThat(results.first().key).isEqualTo(TestInterfaceImplA)
        assertThat(results.last().key).isEqualTo(TestInterfaceImplB)
    }

    @Test
    fun compare_sameMatcherClassTypedOnBaseKey() {
        val matcher1 = TestHierarchicalDeepLinkMatcher(TestInterfaceImplA)
        val matcher2 = TestHierarchicalDeepLinkMatcher(TestInterfaceImplB)

        val matchers:
            List<DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>> =
            listOf(matcher1, matcher2)

        val request = DeepLinkRequest(DeepLinkUri("https://www.testuri.com"))

        val results: List<DeepLinkMatcher.MatchResult<TestInterface>> = buildList {
            matchers.forEach { add(it.match(request)!!) }
        }
        assertThat(results.size).isEqualTo(2)
        assertThat(results.first().key).isEqualTo(TestInterfaceImplA)
        assertThat(results.last().key).isEqualTo(TestInterfaceImplB)
    }

    private object First : NavKey

    private object Second : NavKey

    private class TestHierarchicalDeepLinkMatcher(private val key: TestInterface) :
        DeepLinkMatcher<TestInterface, DeepLinkMatcher.MatchResult<TestInterface>>() {
        override fun matchRequest(request: DeepLinkRequest): MatchResult<TestInterface> {
            return MatchResult(key)
        }
    }
}
