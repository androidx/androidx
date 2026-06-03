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
import androidx.navigation3.runtime.IgnoreAndroidHostTestTarget
import kotlin.test.Test
import kotlin.test.assertFailsWith

@IgnoreAndroidHostTestTarget
class UriDeepLinkParserTest {

    @Test
    fun testExtractPathArgs_singleParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly(("123"))
    }

    @Test
    fun testExtractPathArgs_singleParamWithMultiplePlaceholders() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{first}-{last}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/john-doe")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("first")).containsExactly(("john"))
        assertThat(result?.get("last")).containsExactly(("doe"))
    }

    @Test
    fun testExtractPathArgs_wildcard() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/.*")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/anything/else")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_mixedLiteralAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/user_{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/user_123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly(("123"))
    }

    @Test
    fun testExtractPath_emptyString() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users//profile")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly((""))
    }

    @Test
    fun testExtractPath_trailingEmptyString() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly((""))
    }

    @Test
    fun testExtractPathArgs_colonParamDelimiter() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users;id={id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users;id=123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly(("123"))
    }

    @Test
    fun testExtractPathArgs_encodedCharacters() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{name}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/john%20doe")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("name")).containsExactly(("john doe"))
    }

    @Test
    fun testExtractPathArgs_authorityOnly() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_authorityOnlyTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_authorityOnlyPatternExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_authorityOnlyRequestExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_patternHasExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_requestHasExtraTrailingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_mismatchedLiteral() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/posts")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_missingLiteralInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_extraLiteralInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/user/extra")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_missingPlaceholderInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_misorderedLiteral() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/profile/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_misorderedLiteralAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/123/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_schemeMismatch() {
        val uriPattern = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_authorityMismatch() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://wrong.com/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_fallbackSchemeMatchesHttp() {
        val uriPattern = DeepLinkUri("$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("http://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_fallbackSchemeMatchesHttps() {
        val uriPattern = DeepLinkUri("$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_customSchemeMatches() {
        val uriPattern = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("custom://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_customSchemeMismatch() {
        val uriPattern = DeepLinkUri("custom1://$DEEP_LINK_BASE_PATH/users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("custom2://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_caseInsensitivity() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/Users")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractPathArgs_placeholderCannotContainSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}/profile")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123/456/profile")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractPathArgs_inputContainingEquals() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/id=123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly(("id=123"))
    }

    @Test
    fun testExtractPathArgs_queryValueContainingSlash() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri =
            DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/123?redirect=fallback/path")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result?.get("id")).containsExactly(("123"))
    }

    @Test
    fun testExtractPathArgs_doubleSlashInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users/{id}")
        val parsedPath = UriPatternParser.parsePath(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/users//123")
        val result = UriRequestParser.extractPathArgs(parsedPath, requestedUri)
        assertThat(result).isNull()
    }

    @Test
    fun testExtractQueryArgs_namedParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={name}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name=john")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["name"]).containsExactly(("john"))
    }

    @Test
    fun testExtractQueryArgs_namedParamsMultiple() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={name}&age={age}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name=john&age=30")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["name"]).containsExactly(("john"))
        assertThat(result["age"]).containsExactly(("30"))
    }

    @Test
    fun testExtractQueryArgs_differentOrder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={name}&age={age}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?age=30&name=john")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["name"]).containsExactly(("john"))
        assertThat(result["age"]).containsExactly(("30"))
    }

    @Test
    fun testExtractQueryArgs_unnamedParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?{rawQuery}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?anything&else")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["rawQuery"]).containsExactly("anything", "else")
    }

    @Test
    fun testExtractQueryArgs_unnamedParamEncoded() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?{rawQuery}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?hello%20world")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["rawQuery"]).containsExactly("hello world")
    }

    @Test
    fun testExtractQueryArgs_mixedNamedAndUnnamedParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?user={name}&{other}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?user=john&anything&else")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["name"]).containsExactly(("john"))
        assertThat(result["other"]).containsExactly("anything", "else")
    }

    @Test
    fun testExtractQueryArgs_mixedNamedUnnamedAndExtraParams() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?user={name}&{other}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri =
            DeepLinkUri("https://$DEEP_LINK_BASE_PATH?something&user=john&else&extra=stuff")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        // extra=stuff is ignored
        assertThat(result["name"]).containsExactly(("john"))
        assertThat(result["other"]).containsExactly("something", "else")
        assertThat(result["extra"]).isNull()
    }

    @Test
    fun testExtractQueryArgs_repeatedParams() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?list={val}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?list=10&list=20")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["val"]).containsExactly("10", "20")
    }

    @Test
    fun testExtractQueryArgs_repeatedParamsMixedMatching() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_.*")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_admin&type=guest")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["type"]).containsExactly("admin")
    }

    @Test
    fun testExtractQueryArgs_wildcard() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=.*")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_admin")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["type"]).containsExactly(("user_admin"))
    }

    @Test
    fun testExtractQueryArgs_wildcardSuffix() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_.*")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_admin")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["type"]).containsExactly(("admin"))
    }

    @Test
    fun testExtractQueryArgs_wildcardPrefix() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=.*_user")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=admin_user")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["type"]).containsExactly(("admin"))
    }

    @Test
    fun testExtractQueryArgs_wildcardMatchesEmptyString() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_.*")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["type"]).containsExactly("")
    }

    @Test
    fun testExtractQueryArgs_wildcardAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_{id}_.*")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_123_admin")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["id"]).containsExactly(("123"))
        assertThat(result["type"]).isNull()
    }

    @Test
    fun testExtractQueryArgs_partialLiteralMatching() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_{id}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?type=user_123")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["id"]).containsExactly(("123"))
    }

    @Test
    fun testExtractQueryArgs_multiplePlaceholdersInOneParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={first}_{last}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name=john_doe")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["first"]).containsExactly(("john"))
        assertThat(result["last"]).containsExactly(("doe"))
    }

    @Test
    fun testExtractQuery_noQueryArgsInPattern() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractQueryArgs_emptyValueForNamedParam() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={name}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name=")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["name"]).containsExactly((""))
    }

    @Test
    fun testExtractQueryArgs_encodedCharacters() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?query={query}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?query=hello%20world")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["query"]).containsExactly(("hello world"))
    }

    @Test
    fun testExtractQueryArgs_dashBetweenPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?user={first}-{last}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?user=john-doe")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["first"]).containsExactly(("john"))
        assertThat(result["last"]).containsExactly(("doe"))
    }

    @Test
    fun testExtractQueryArgs_commaSeparatedExtractedAsSingleValue() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?colors={colorList}")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?colors=purple,red,green")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["colorList"]).containsExactly("purple,red,green")
    }

    @Test
    fun testExtractQuery_queryArgsFlag() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?debug")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?debug")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["debug"]).containsExactly(("debug"))
    }

    @Test
    fun testExtractQueryArgs_flagWithValueInRequestMismatch() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?debug")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?debug=true")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result["debug"]).isNull()
    }

    @Test
    fun testExtractQueryArgs_trailingQuestionMark() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a?")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractQueryArgs_noParameters() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a")
        val parsedQuery = UriPatternParser.parseQuery(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a?=")
        val result = UriRequestParser.extractQueryArgs(parsedQuery, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractQueryArgs_multipleUnnamedParamsThrows() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?{rawQuery1}&{rawQuery2}")
        assertFailsWith<IllegalArgumentException> { UriPatternParser.parseQuery(uriPattern) }
    }

    @Test
    fun testExtractQueryArgs_duplicateParamNameThrows() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH?name={first}&name={last}")
        assertFailsWith<IllegalArgumentException> { UriPatternParser.parseQuery(uriPattern) }
    }

    @Test
    fun testExtractFragmentArgs_staticFragment() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractFragmentArgs_placeholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#{id}")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#123")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result["id"]).containsExactly(("123"))
    }

    @Test
    fun testExtractFragmentArgs_mixedLiteralAndPlaceholder() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section_{id}")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section_123")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result["id"]).containsExactly(("123"))
    }

    @Test
    fun testExtractFragmentArgs_missingLiteralFragmentInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractFragmentArgs_missingFragmentArgInRequest() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#{id}")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractFragmentArgs_mismatchedFragment() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section1")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#section2")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result).isEmpty()
    }

    @Test
    fun testExtractFragmentArgs_extraFragmentInPattern() {
        val uriPattern = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a")
        val parsedFragment = UriPatternParser.parseFragment(uriPattern)
        val requestedUri = DeepLinkUri("https://$DEEP_LINK_BASE_PATH/a#123")
        val result = UriRequestParser.extractFragmentArgs(parsedFragment, requestedUri)
        assertThat(result).isEmpty()
    }

    companion object {
        private const val DEEP_LINK_BASE_PATH = "www.testUri.com"
    }
}
