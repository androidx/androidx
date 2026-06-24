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
import kotlinx.serialization.Serializable

private const val filterString = "filterString"

private const val stringExtraKey = "StringExtraKey"

class DeepLinkMatcherTest {
    class TestDeepLinkMatcher(filters: List<Filter> = emptyList()) :
        DeepLinkMatcher<TestKey>(filters) {
        override fun matchRequest(request: DeepLinkRequest): MatchResult<TestKey>? {
            return MatchResult(TestKey)
        }
    }

    class TestFilter(val filter: String) : DeepLinkMatcher.Filter {
        override fun filterRequest(request: DeepLinkRequest): Boolean {
            return filter == request.extras[stringExtraKey]
        }
    }

    @Test
    fun match_emptyFilters() {
        val matcher = TestDeepLinkMatcher()
        val request = DeepLinkRequest.withStringExtra("https://example.com", filterString)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterPasses() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter(filterString)))
        val request = DeepLinkRequest.withStringExtra("https://example.com", filterString)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterFails() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter("wrongFilter")))
        val request = DeepLinkRequest.withStringExtra("https://example.com", filterString)
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun match_allFiltersPass() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(filterString), TestFilter(filterString))
            )
        val request = DeepLinkRequest.withStringExtra("https://example.com", filterString)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_someFiltersFail() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(filterString), TestFilter("wrongFilter"))
            )
        val request = DeepLinkRequest.withStringExtra("https://example.com", filterString)
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
    fun match_hierarchicalKeys() {
        val matcher1 =
            object : DeepLinkMatcher<DerivedKey1>() {
                override fun matchRequest(request: DeepLinkRequest): MatchResult<DerivedKey1> {
                    return MatchResult(DerivedKey1)
                }
            }
        val matcher2 =
            object : DeepLinkMatcher<DerivedKey2>() {
                override fun matchRequest(request: DeepLinkRequest): MatchResult<DerivedKey2> {
                    return MatchResult(DerivedKey2)
                }
            }
        val matchers: List<DeepLinkMatcher<BaseKey>> = listOf(matcher1, matcher2)

        val request = DeepLinkRequest(DeepLinkUri("www.testuri.com"))

        val results: List<DeepLinkMatcher.MatchResult<BaseKey>> = buildList {
            matchers.forEach { add(it.match(request)!!) }
        }
        assertThat(results.size).isEqualTo(2)
        assertThat(results.first().key).isEqualTo(DerivedKey1)
        assertThat(results.last().key).isEqualTo(DerivedKey2)
    }

    private object First : NavKey

    private object Second : NavKey

    private fun DeepLinkRequest.Companion.withStringExtra(uri: String, extra: String) =
        DeepLinkRequest(uri = DeepLinkUri(uri), extras = mapOf(stringExtraKey to extra))
}

interface BaseKey : NavKey

@Serializable object DerivedKey1 : BaseKey

@Serializable object DerivedKey2 : BaseKey
