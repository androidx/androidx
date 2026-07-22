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
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

class UriDeepLinkMatcherTest {
    @Test
    fun matchRequest_schemeMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("ftp://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_schemeHttp() {
        val pattern1 = DeepLinkUri("http://example.com/path")
        val matcher1 = UriDeepLinkMatcher(pattern1, serializer<TestDefaultArgKey>())
        val request1 = DeepLinkRequest.fromUriString("https://example.com/path")
        assertThat(matcher1.match(request1)).isNotNull()

        val pattern2 = DeepLinkUri("http://example.com/path")
        val matcher2 = UriDeepLinkMatcher(pattern2, serializer<TestDefaultArgKey>())
        val request2 = DeepLinkRequest.fromUriString("http://example.com/path")
        assertThat(matcher2.match(request2)).isNotNull()
    }

    @Test
    fun matchRequest_schemeHttps() {
        val pattern1 = DeepLinkUri("https://example.com/path")
        val matcher1 = UriDeepLinkMatcher(pattern1, serializer<TestDefaultArgKey>())
        val request1 = DeepLinkRequest.fromUriString("https://example.com/path")
        assertThat(matcher1.match(request1)).isNotNull()

        // strictly enforce https
        val pattern2 = DeepLinkUri("https://example.com/path")
        val matcher2 = UriDeepLinkMatcher(pattern2, serializer<TestDefaultArgKey>())
        val request2 = DeepLinkRequest.fromUriString("http://example.com/path")
        assertThat(matcher2.match(request2)).isNull()
    }

    @Test
    fun matchRequest_defaultSchemeHttpHttpsMatch() {
        val pattern1 =
            DeepLinkUri("//example.com/path") // network-path references can start with //
        val matcher1 = UriDeepLinkMatcher(pattern1, serializer<TestDefaultArgKey>())
        val request1 = DeepLinkRequest.fromUriString("https://example.com/path")
        assertThat(matcher1.match(request1)).isNotNull()

        val pattern2 = DeepLinkUri("//example.com/path")
        val matcher2 = UriDeepLinkMatcher(pattern2, serializer<TestDefaultArgKey>())
        val request2 = DeepLinkRequest.fromUriString("http://example.com/path")
        assertThat(matcher2.match(request2)).isNotNull()
    }

    @Test
    fun matchRequest_defaultSchemeHttpHttpsMatch_noAuthority() {
        // no scheme and not starting with "//" is treated as no authority
        val pattern1 = DeepLinkUri("www.example.com/path")
        val matcher1 = UriDeepLinkMatcher(pattern1, serializer<TestDefaultArgKey>())
        val request1 = DeepLinkRequest.fromUriString("https://www.example.com/path")
        assertThat(matcher1.match(request1)).isNotNull()

        val pattern2 = DeepLinkUri("example.com/path")
        val matcher2 = UriDeepLinkMatcher(pattern2, serializer<TestDefaultArgKey>())
        val request2 = DeepLinkRequest.fromUriString("http://example.com/path")
        assertThat(matcher2.match(request2)).isNotNull()
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
    fun matchRequest_authorityMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestDefaultArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.org/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
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

    @Test
    fun testCompareTo_nonUriMatchResult() {
        val request = DeepLinkRequest.fromUriString("https://example.com/path")
        val uriResult =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path"), serializer<TestKey>())
                .match(request) as UriMatchResult
        val nonUriResult = DeepLinkMatcher.MatchResult(TestKey)

        assertThat(uriResult.compareTo(nonUriResult)).isEqualTo(1)
    }

    @Test
    fun testCompareTo_wrappedSameInnerType() {
        val request = DeepLinkRequest.fromUriString("https://example.com/path")

        val uriResultExact =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path"), serializer<TestKey>())
                .match(request) as UriMatchResult
        val uriResultWildcard =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/.*"), serializer<TestKey>())
                .match(request) as UriMatchResult

        val wrappedWildcard = object : WrappedMatchResult<TestKey>(uriResultWildcard) {}
        val wrappedExact = object : WrappedMatchResult<TestKey>(uriResultExact) {}

        assertThat(uriResultExact.compareTo(wrappedWildcard)).isEqualTo(1)
        assertThat(uriResultWildcard.compareTo(wrappedExact)).isEqualTo(-1)
        assertThat(uriResultExact.compareTo(wrappedExact)).isEqualTo(0)
    }

    @Test
    fun testCompareTo_wrappedDifferentInnerType() {
        val request = DeepLinkRequest.fromUriString("https://example.com/path")
        val uriResult =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path"), serializer<TestKey>())
                .match(request) as UriMatchResult
        val nonUriResult = DeepLinkMatcher.MatchResult(TestKey)
        val wrappedNonUri = object : WrappedMatchResult<TestKey>(nonUriResult) {}

        assertThat(uriResult.compareTo(wrappedNonUri)).isEqualTo(1)
    }

    @Test
    fun matchRequest_booleanArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{bool}"),
                serializer<BooleanKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/true")
        val result = matcher.match(request)
        assertThat(result?.key?.bool).isEqualTo(true)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{bool}"),
                serializer<BooleanKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notABoolean")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_byteArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{byte}"),
                serializer<ByteKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/1")
        val result = matcher.match(request)
        assertThat(result?.key?.byte).isEqualTo(1.toByte())

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{byte}"),
                serializer<ByteKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notAByte")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_shortArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{short}"),
                serializer<ShortKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/2")
        val result = matcher.match(request)
        assertThat(result?.key?.short).isEqualTo(2.toShort())

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{short}"),
                serializer<ShortKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notAShort")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_longArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{long}"),
                serializer<LongKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/4")
        val result = matcher.match(request)
        assertThat(result?.key?.long).isEqualTo(4L)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{long}"),
                serializer<LongKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notALong")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_floatArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{float}"),
                serializer<FloatKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/5.0")
        val result = matcher.match(request)
        assertThat(result?.key?.float).isEqualTo(5.0f)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{float}"),
                serializer<FloatKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notAFloat")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_doubleArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{double}"),
                serializer<DoubleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/6.0")
        val result = matcher.match(request)
        assertThat(result?.key?.double).isEqualTo(6.0)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{double}"),
                serializer<DoubleKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notADouble")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_charArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{char}"),
                serializer<CharKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path/a")
        val result = matcher.match(request)
        assertThat(result?.key?.char).isEqualTo('a')

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path/{char}"),
                serializer<CharKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_intArgument() {
        val matcher =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path/{int}"), serializer<IntKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path/1")
        val result = matcher.match(request)
        assertThat(result?.key?.int).isEqualTo(1)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path/{int}"), serializer<IntKey>())
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path/notAnInt")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_intListArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?list={list}"),
                serializer<ListKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?list=1&list=2")
        val result = matcher.match(request)
        assertThat(result?.key?.list).containsExactly(1, 2).inOrder()

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?list={list}"),
                serializer<ListKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/path?list=a&list=b")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_multiplePlaceholdersSingleSegment() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user_user_{name}_{age}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user_user_john_30")
        val result = matcher.match(request)
        assertThat(result?.key?.name).isEqualTo("john")
        assertThat(result!!.key.age).isEqualTo(30)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user_user_{name}_{age}"),
                serializer<SimpleKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/user_user_john_notAnAge")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_defaultValueFallback() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}"),
                serializer<DefaultKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john")
        val result = matcher.match(request)
        assertThat(result?.key?.name).isEqualTo("john")
        assertThat(result!!.key.age).isEqualTo(0)

        val fallbackMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}"),
                serializer<DefaultKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/user")
        val result2 = fallbackMatcher.match(request2)
        assertThat(result2?.key?.name).isEqualTo("default")
        assertThat(result2!!.key.age).isEqualTo(0)
    }

    @Test
    fun matchRequest_pathEmptyStringArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/{name}/{age}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com//30")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key?.name).isEqualTo("")
        assertThat(result!!.key.age).isEqualTo(30)
    }

    @Test
    fun matchRequest_pathEmptyNullableIntWithNullFallback() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/{name}/{age}"),
                serializer<NullableDefaultKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/john/")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key?.name).isEqualTo("john")
        assertThat(result!!.key.age).isNull()
    }

    @Test
    fun matchRequest_pathEmptyNullableIntNoFallbackFails() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/{name}/{age}"),
                serializer<NullableKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/john/")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_pathEmptyNonNullableIntFails() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/{name}/{age}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/john/")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_queryEmptyStringArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}&age={age}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=&age=30")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key?.name).isEqualTo("")
        assertThat(result!!.key.age).isEqualTo(30)
    }

    @Test
    fun matchRequest_queryEmptyNullableIntWithNullFallback() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}&age={age}"),
                serializer<NullableDefaultKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john&age=")
        val result = matcher.match(request)
        assertThat(result).isNotNull()
        assertThat(result?.key?.name).isEqualTo("john")
        assertThat(result!!.key.age).isNull()
    }

    @Test
    fun matchRequest_queryEmptyNullableIntNoFallbackFails() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}&age={age}"),
                serializer<NullableKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john&age=")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_queryEmptyNonNullableIntFails() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}&age={age}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john&age=")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_missingRequiredArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user?name={name}"),
                serializer<SimpleKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john")
        val result = matcher.match(request)
        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_nestedObjects() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/user/{name}/{age}/{flag}"),
                serializer<NestedKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/user/john/30/true")
        val result = matcher.match(request)
        assertThat(result?.key?.user?.name).isEqualTo("john")
        assertThat(result!!.key.user.age).isEqualTo(30)
        assertThat(result.key.flag).isEqualTo(true)
    }

    @Test
    fun matchRequest_enumArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/direction/{direction}"),
                serializer<EnumKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/direction/NORTH")
        val result = matcher.match(request)
        assertThat(result?.key?.direction).isEqualTo(DirectionEnum.NORTH)

        val wrongTypeMatcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/direction/{direction}"),
                serializer<EnumKey>(),
            )
        val request2 = DeepLinkRequest.fromUriString("https://example.com/direction/WEST")
        val result2 = wrongTypeMatcher.match(request2)
        assertThat(result2).isNull()
    }

    @Test
    fun matchRequest_fragmentPlaceholder() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path#{char}"),
                serializer<CharKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path#a")
        val result = matcher.match(request)
        assertThat(result?.key?.char).isEqualTo('a')
    }

    @Test
    fun matchRequest_fragmentWildcard() {
        val matcher =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path#.*"), serializer<TestKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path#123")
        val result = matcher.match(request)
        assertThat(result?.key).isEqualTo(TestKey)
        assertThat((result as UriMatchResult<TestKey>).arguments).isEmpty()
    }

    @Test
    fun matchRequest_fragmentPartialWildcard() {
        val matcher =
            UriDeepLinkMatcher(DeepLinkUri("https://example.com/path#a_.*"), serializer<TestKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path#a_123")
        val result = matcher.match(request)
        assertThat(result?.key).isEqualTo(TestKey)
        assertThat((result as UriMatchResult<TestKey>).arguments).isEmpty()
    }

    @Test
    fun matchRequest_nonPrimitiveList_throws() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?list={list}"),
                serializer<NonPrimitiveListKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?list=1&list=2")
        kotlin.test.assertFailsWith<kotlinx.serialization.SerializationException> {
            matcher.match(request)
        }
    }

    @Test
    fun matchRequest_mapKey_throws() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?map={map}"),
                serializer<MapKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?map=1")
        kotlin.test.assertFailsWith<IllegalArgumentException> { matcher.match(request) }
    }

    @Test
    fun compare_matchersTypedOnDerivedKeys() {
        val matcher1 =
            object :
                UriDeepLinkMatcher<TestInterfaceImplA>(
                    DeepLinkUri("www.testuri.com"),
                    serializer<TestInterfaceImplA>(),
                ) {}
        val matcher2 =
            object :
                UriDeepLinkMatcher<TestInterfaceImplB>(
                    DeepLinkUri("www.testuri.com"),
                    serializer<TestInterfaceImplB>(),
                ) {}
        val matchers: List<UriDeepLinkMatcher<TestInterface>> = listOf(matcher1, matcher2)

        val request = DeepLinkRequest.fromUriString("https://www.testuri.com")
        val results: List<DeepLinkMatcher.MatchResult<TestInterface>> = buildList {
            matchers.forEach { add(it.match(request)!!) }
        }
        assertThat(results.size).isEqualTo(2)
        assertThat(results.first().key).isEqualTo(TestInterfaceImplA)
        assertThat(results.last().key).isEqualTo(TestInterfaceImplB)
    }

    @Test
    fun matchRequest_matchResultTypePreserved() {
        val matcher =
            UriDeepLinkMatcher(DeepLinkUri("http://example.com/path"), serializer<TestKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path")
        val result = matcher.match(request)
        // allows direct access to UriMatchResult fields without casting
        assertThat(result!!.arguments).isEmpty()
    }

    @Test
    fun matchRequest_withBackStack() {
        val matcher =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/user?name={name}&age={age}"),
                    serializer<SimpleKey>(),
                )
                .withBackStack { listOf(TestKey, it.key) }
        val request = DeepLinkRequest.fromUriString("https://example.com/user?name=john&age=30")
        val result = matcher.match(request)
        assertThat(result?.backStack).containsExactly(TestKey, SimpleKey("john", 30)).inOrder()
    }

    @Test
    fun matchRequest_withBackStack_interfaceKeys() {
        val matcher =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/any/"),
                    serializer<TestInterfaceImplB>(),
                )
                .withBackStack { listOf(TestInterfaceImplA, TestInterfaceImplB) }
        val request = DeepLinkRequest.fromUriString("https://example.com/any/")
        val result: BackStackMatchResult<TestInterfaceImplB, TestInterface>? =
            matcher.match(request)

        val backStack: List<TestInterface> = result!!.backStack
        val key: TestInterface = result.key
        assertThat(backStack).containsExactly(TestInterfaceImplA, TestInterfaceImplB).inOrder()
        assertThat(key).isEqualTo(TestInterfaceImplB)
    }

    @Test
    fun matchRequest_withBackStack_sealedKeys() {
        val matcher =
            UriDeepLinkMatcher(
                    DeepLinkUri("https://example.com/any/"),
                    serializer<TestSealedClass.Key2>(),
                )
                .withBackStack { listOf(TestSealedClass.Key1, TestSealedClass.Key2) }
        val request = DeepLinkRequest.fromUriString("https://example.com/any/")
        val result = matcher.match(request)

        val backStack: List<TestSealedClass> = result!!.backStack
        val key: TestSealedClass = result.key
        assertThat(backStack).containsExactly(TestSealedClass.Key1, TestSealedClass.Key2).inOrder()
        assertThat(key).isEqualTo(TestSealedClass.Key2)
    }

    @Test
    fun matchRequest_polymorphicConcreteArgument() {
        val name = "theName"
        val owner = "theOwner"
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?owner={owner}&name={name}"),
                serializer<SerializableConcreteArgKey>(),
            )
        val request =
            DeepLinkRequest.fromUriString("https://example.com/path?owner=$owner&name=$name")
        val result = matcher.match(request) as? UriMatchResult
        assertThat(result).isNotNull()
        assertThat(result!!.key.derived.owner).isEqualTo(owner)
        assertThat(result.key.derived.name).isEqualTo(name)
    }

    @Test
    fun matchRequest_polymorphicAbstractArgument() {
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?derived={derived}"),
                serializer<SerializableAbstractArgKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?derived=anything")
        val exception = assertFailsWith<SerializationException> { matcher.match(request) }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot decode field [derived] for type with serial " +
                    "name kotlinx.serialization.Polymorphic<SerializableAbstract>. " +
                    "Unsupported structure kind: OPEN"
            )
    }

    @Test
    fun matchRequest_polymorphicOpenClassKey() {
        val name = "theName"
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?name={name}"),
                serializer<SerializableOpenKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?name=$name")
        val result = matcher.match(request) as? UriMatchResult
        assertThat(result).isNotNull()
        assertThat(result!!.key.name).isEqualTo(name)
    }

    @Test
    fun matchRequest_deepLinkSerializer_interfaceImplArgument() {
        val arg = 13
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?arg={arg}"),
                serializer<NonSerializableInterfaceImplArgKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?arg=$arg")
        val result = matcher.match(request) as? UriMatchResult
        assertThat(result).isNotNull()
        assertThat(result!!.key.arg.id).isEqualTo(arg)
    }

    @Test
    fun matchRequest_deepLinkSerializer_abstractImplArgument() {
        val arg = "name"
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?arg={arg}"),
                serializer<NonSerializableAbstractImplArgKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?arg=$arg")
        val result = matcher.match(request) as? UriMatchResult
        assertThat(result).isNotNull()
        assertThat(result!!.key.arg.name).isEqualTo(arg)
    }

    @Test
    fun matchRequest_deepLinkSerializer_openImplArgument() {
        val arg = "name"
        val matcher =
            UriDeepLinkMatcher(
                DeepLinkUri("https://example.com/path?arg={arg}"),
                serializer<NonSerializableOpenImplArgKey>(),
            )
        val request = DeepLinkRequest.fromUriString("https://example.com/path?arg=$arg")
        val result = matcher.match(request) as? UriMatchResult
        assertThat(result).isNotNull()
        assertThat(result!!.key.arg.name).isEqualTo(arg)
    }

    @Serializable class SerializableAbstractArgKey(val derived: SerializableAbstract) : NavKey

    class NonSerializableInterfaceImpl(val id: Int) : NavKey

    @Serializable
    class NonSerializableInterfaceImplArgKey(
        @Serializable(InterfaceImplSerializer::class) val arg: NonSerializableInterfaceImpl
    )

    object InterfaceImplSerializer : DeepLinkSerializer<NonSerializableInterfaceImpl>() {
        override val serialName: String
            get() = "NonSerializableInterfaceImpl"

        override fun deserialize(value: String): NonSerializableInterfaceImpl {
            return NonSerializableInterfaceImpl(value.toInt())
        }

        override fun serialize(value: NonSerializableInterfaceImpl): String {
            return value.id.toString()
        }
    }

    class NonSerializableAbstractImpl(override val name: String) : SerializableAbstract()

    @Serializable
    class NonSerializableAbstractImplArgKey(
        @Serializable(AbstractImplSerializer::class) val arg: NonSerializableAbstractImpl
    )

    object AbstractImplSerializer : DeepLinkSerializer<NonSerializableAbstractImpl>() {
        override val serialName: String
            get() = "NonSerializableAbstractImpl"

        override fun deserialize(value: String): NonSerializableAbstractImpl {
            return NonSerializableAbstractImpl(value)
        }

        override fun serialize(value: NonSerializableAbstractImpl): String {
            return value.name
        }
    }

    @Serializable open class SerializableOpenKey(val name: String)

    class NonSerializableOpenImpl(name: String) : SerializableOpenKey(name)

    @Serializable
    class NonSerializableOpenImplArgKey(
        @Serializable(OpenImplSerializer::class) val arg: NonSerializableOpenImpl
    )

    object OpenImplSerializer : DeepLinkSerializer<NonSerializableOpenImpl>() {
        override val serialName: String
            get() = "OpenImpl"

        override fun deserialize(value: String): NonSerializableOpenImpl {
            return NonSerializableOpenImpl(value)
        }

        override fun serialize(value: NonSerializableOpenImpl): String {
            return value.name
        }
    }

    object Key1 : NavKey

    object Key2 : NavKey

    private fun DeepLinkRequest.Companion.fromUriString(uri: String) =
        DeepLinkRequest(uri = DeepLinkUri(uri))
}
