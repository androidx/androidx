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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.collections.emptyMap
import kotlin.test.Test
import kotlinx.serialization.Serializable

@IgnoreAndroidHostTestTarget
class UriDeepLinkParserTest {

    @Test
    fun testExtractPathArgs_singleParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("123")))
    }

    @Test
    fun testExtractPathArgs_singleParamWithMultiplePlaceholders() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{first}-{last}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/john-doe")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("first" to listOf("john"), "last" to listOf("doe")))
    }

    @Test
    fun testExtractPathArgs_wildcard() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/.*")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/anything/else")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(emptyMap<String, List<String>>())
    }

    @Test
    fun testExtractPathArgs_mixedLiteralAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/user_{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/user_123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("123")))
    }

    @Test
    fun testExtractPath_emptyString() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users//profile")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("")))
    }

    @Test
    fun testExtractPath_trailingEmptyString() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("")))
    }

    @Test
    fun testExtractPathArgs_colonParamDelimiter() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users;id={id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users;id=123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("123")))
    }

    @Test
    fun testExtractPathArgs_encodedCharacters() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{name}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/john%20doe")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("name" to listOf("john doe")))
    }

    @Test
    fun testExtractPathArgs_patternHasExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_requestHasExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_mismatchedLiteral() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/posts")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_missingLiteralInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_missingPlaceholderInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_misorderedLiteral() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/profile/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_misorderedLiteralAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/123/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_schemeMismatch() {
        val uriPattern = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_authorityMismatch() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://wrong.com/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_fallbackSchemeMatchesHttp() {
        val uriPattern = DeepLinkUri("$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("http://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(emptyMap<String, List<String>>())
    }

    @Test
    fun testExtractPathArgs_fallbackSchemeMatchesHttps() {
        val uriPattern = DeepLinkUri("$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(emptyMap<String, List<String>>())
    }

    @Test
    fun testExtractPathArgs_customSchemeMatches() {
        val uriPattern = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(emptyMap<String, List<String>>())
    }

    @Test
    fun testExtractPathArgs_customSchemeMismatch() {
        val uriPattern = DeepLinkUri("custom1://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("custom2://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_caseInsensitivity() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/Users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(emptyMap<String, List<String>>())
    }

    @Test
    fun testExtractPathArgs_placeholderCannotContainSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123/456/profile")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    @Test
    fun testExtractPathArgs_inputContainingEquals() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/id=123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("id=123")))
    }

    @Test
    fun testExtractPathArgs_queryValueContainingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri =
            DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123?redirect=fallback/path")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(mapOf("id" to listOf("123")))
    }

    @Test
    fun testExtractPathArgs_doubleSlashInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users//123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).equals(null)
    }

    companion object {
        private const val DEEP_LINK_BASE_PATH = "www.testUri.com"
    }
}

@Serializable private object TestKey
