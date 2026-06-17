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

private const val filterString = "filterString"

class DeepLinkMatcherTest {
    class TestDeepLinkMatcher(filters: List<Filter> = emptyList()) :
        DeepLinkMatcher<TestKey>(filters) {
        override fun matchRequest(request: DeepLinkRequest): MatchResult<TestKey>? {
            return MatchResult(TestKey)
        }
    }

    class TestFilter(val filter: String) : DeepLinkMatcher.Filter {
        override fun filterRequest(request: DeepLinkRequest): Boolean {
            return filter == request.mimeType
        }
    }

    @Test
    fun match_emptyFilters() {
        val matcher = TestDeepLinkMatcher()
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = filterString)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterPasses() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter(filterString)))
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = filterString)
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterFails() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter("wrongFilter")))
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = filterString)
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun match_allFiltersPass() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(filterString), TestFilter(filterString))
            )
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = filterString)
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
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = filterString)
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
        val request = DeepLinkRequest.fromUriString("https://example.com", mimeType = "image/TEST")
        val result = matcher.match(request)
        assertThat(result).isNotNull()

        val request2 =
            DeepLinkRequest.fromUriString("https://example.com", mimeType = "wrongMimeType")
        val result2 = matcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun match_actionFilter() {
        val matcher =
            TestDeepLinkMatcher(filters = listOf(DeepLinkMatcher.actionFilter("ACTION.TEST")))
        val request = DeepLinkRequest.fromUriString("https://example.com", action = "ACTION.test")
        val result = matcher.match(request)
        assertThat(result).isNotNull()

        val request2 = DeepLinkRequest.fromUriString("https://example.com", action = "wrongAction")
        val result2 = matcher.match(request2)
        assertThat(result2).isNull()
    }

    private object First : NavKey

    private object Second : NavKey
}
