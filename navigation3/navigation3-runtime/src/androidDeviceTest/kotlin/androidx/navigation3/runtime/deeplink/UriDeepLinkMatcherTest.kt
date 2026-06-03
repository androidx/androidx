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
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class UriDeepLinkMatcherTest {
    @Test
    fun matchRequest_schemeMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_authorityMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.org/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_pathSegmentShorterMismatch() {
        val pattern = DeepLinkUri("http://example.com/path/subpath")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_pathSegmentLongerMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path/extra")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_schemeMatchCaseInsensitive() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("HTTP://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNotNull()
    }

    @Test
    fun matchRequest_authorityMatchCaseInsensitive() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://EXAMPLE.COM/path")

        val result = matcher.match(request)

        assertThat(result).isNotNull()
    }

    @Test
    fun matchRequest_pathSegmentSizeMatch() {
        val pattern = DeepLinkUri("http://example.com/path/subpath")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path/subpath")

        val result = matcher.match(request)

        assertThat(result).isNotNull()
    }

    @Test
    fun matchRequest_pathSegmentSizeMatch_trailingWildcard() {
        val pattern = DeepLinkUri("http://example.com/path/.*")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path/wildcard/segments")

        val result = matcher.match(request)

        assertThat(result).isNotNull()
    }

    @Test
    fun testCompareTo_exactPathWins() {
        val request = DeepLinkRequest.fromUriString("https://example.com/path")
        val result1 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path"), serializer<TestKey>())
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/.*"), serializer<TestKey>())
                .match(request) as UriMatchResult

        val bestMatch = maxOf(result1, result2)
        assertThat(bestMatch).isEqualTo(result1)
    }

    @Test
    fun testCompareTo_morePathArgsWin() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john/123/anything")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}/{id}/.*"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}/.*"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult

        val bestMatch = maxOf(result1, result2)
        assertThat(bestMatch).isEqualTo(result1)
    }

    @Test
    fun testCompareTo_moreQueryArgsWin() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john?q=abc")
        val result1 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/{name}"), serializer<TestArgKey>())
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}?q={q}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult

        val bestMatch = maxOf(result1, result2)
        assertThat(bestMatch).isEqualTo(result2)
    }

    @Test
    fun testCompareTo_exactPathWins_differentKeys() {
        val request = DeepLinkRequest.fromUriString("https://example.com/path")
        val result1 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path"), serializer<TestKey>())
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/{name}"), serializer<TestArgKey>())
                .match(request) as UriMatchResult

        @Suppress("UNCHECKED_CAST")
        val bestMatch = maxOf(result1 as UriMatchResult<Any>, result2 as UriMatchResult<Any>)
        assertThat(bestMatch).isEqualTo(result1)
    }

    @Test
    fun testCompareTo_morePathArgsWin_differentKeys() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john/123/anything")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}/.*"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}/{id}/.*"),
                    serializer<TestKey>(),
                )
                .match(request) as UriMatchResult

        @Suppress("UNCHECKED_CAST")
        val bestMatch = maxOf(result1 as UriMatchResult<Any>, result2 as UriMatchResult<Any>)
        assertThat(bestMatch).isEqualTo(result2)
    }

    @Test
    fun testCompareTo_moreQueryArgsWin_differentKeys() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john?q=abc")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}?q={q}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/{name}"), serializer<TestKey>())
                .match(request) as UriMatchResult

        @Suppress("UNCHECKED_CAST")
        val bestMatch = maxOf(result1 as UriMatchResult<Any>, result2 as UriMatchResult<Any>)
        assertThat(bestMatch).isEqualTo(result1)
    }

    @Test
    fun testCompareTo_moreFragmentArgsWin() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john#123")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}#{id}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/{name}"), serializer<TestArgKey>())
                .match(request) as UriMatchResult

        val bestMatch = maxOf(result1, result2)
        assertThat(bestMatch).isEqualTo(result1)
    }

    @Test
    fun testCompareTo_queryAndFragmentAreEqual() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john?q=abc#123")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}?q={q}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}#{id}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult

        assertThat(result1.compareTo(result2)).isEqualTo(0)
    }

    @Test
    fun testCompareTo_differentQueryNamesAreEqual() {
        val request = DeepLinkRequest.fromUriString("https://example.com/john?a=1&b=2")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}?a={a}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{name}?b={b}"),
                    serializer<TestArgKey>(),
                )
                .match(request) as UriMatchResult

        assertThat(result1.compareTo(result2)).isEqualTo(0)
    }

    @Test
    fun testCompareTo_differentPathNamesAreEqual() {
        val request = DeepLinkRequest.fromUriString("https://example.com/123/john")
        val result1 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{id}/{name}"),
                    serializer<TestDefaultArgKey>(),
                )
                .match(request) as UriMatchResult
        val result2 =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/{first}/{last}"),
                    serializer<TestDefaultArgKey>(),
                )
                .match(request) as UriMatchResult

        assertThat(result1.compareTo(result2)).isEqualTo(0)
    }
}

@Serializable private class TestKey

@Serializable private data class TestDefaultArgKey(val name: String = "test")

@Serializable private data class TestArgKey(val name: String)
