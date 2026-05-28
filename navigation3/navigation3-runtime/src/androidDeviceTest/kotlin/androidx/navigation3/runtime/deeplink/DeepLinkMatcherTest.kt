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

class DeepLinkMatcherTest {

    object TestKey

    class TestDeepLinkMatcher(filters: List<Filter<*>> = emptyList()) :
        DeepLinkMatcher<TestKey>(filters) {
        override fun matchRequest(request: DeepLinkRequest): MatchResult<TestKey>? {
            return MatchResult(TestKey)
        }
    }

    class TestFilter(val matches: Boolean) : DeepLinkMatcher.Filter<String>("test") {
        override fun filterRequest(request: DeepLinkRequest): Boolean = matches
    }

    @Test
    fun match_emptyFilters() {
        val matcher = TestDeepLinkMatcher()
        val request = DeepLinkRequest.fromUriString("https://example.com")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterPasses() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter(matches = true)))
        val request = DeepLinkRequest.fromUriString("https://example.com")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_filterFails() {
        val matcher = TestDeepLinkMatcher(filters = listOf(TestFilter(matches = false)))
        val request = DeepLinkRequest.fromUriString("https://example.com")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun match_allFiltersPass() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(matches = true), TestFilter(matches = true))
            )
        val request = DeepLinkRequest.fromUriString("https://example.com")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key).isEqualTo(TestKey)
    }

    @Test
    fun match_someFiltersFail() {
        val matcher =
            TestDeepLinkMatcher(
                filters = listOf(TestFilter(matches = true), TestFilter(matches = false))
            )
        val request = DeepLinkRequest.fromUriString("https://example.com")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }
}
