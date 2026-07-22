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

package androidx.navigation3.runtime.samples.deeplink

import androidx.annotation.Sampled
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatchResult
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.StaticKeyDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.withBackStack
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable object UserListKey : NavKey

@Serializable object ImageKey : NavKey

@Serializable data class Article(val id: Int) : NavKey

@Serializable data class ArticleSection(val category: String) : NavKey

private const val SAMPLE_BASE_PATH = "www.nav3deeplinksample.com"

@Sampled
fun staticKeyDeepLinkMatcherSample() {
    // declare a matcher based on mime type
    val mimeType = "image/jpg"
    val mimeTypeFilter = DeepLinkMatcher.mimeTypeFilter(mimeType)
    val matcher = StaticKeyDeepLinkMatcher(ImageKey, listOf(mimeTypeFilter))

    // when handling a request
    val request =
        DeepLinkRequest(
            uri = DeepLinkUri("$SAMPLE_BASE_PATH/images/"),
            extras = DeepLinkRequest.mimeTypeExtra(mimeType),
        )
    // returns a valid result as long as mimeType matches
    val matchResult: DeepLinkMatcher.MatchResult<ImageKey>? = matcher.match(request)
}

@Sampled
fun deepLinkMatcherWithBackStackSample() {
    val matcher =
        UriDeepLinkMatcher(
                uriPattern = DeepLinkUri("https://www.nav3deeplinksample.com/userlist"),
                serializer = serializer<UserListKey>(),
            )
            .withBackStack { matchResult -> listOf(HomeKey, matchResult.key) }

    // handling a request
    val request = DeepLinkRequest(uri = DeepLinkUri("https://www.nav3deeplinksample.com/userlist/"))
    // returns a BackStackMatchResult containing the matched key and the constructed back stack
    val matchResult: BackStackMatchResult<UserListKey, NavKey>? = matcher.match(request)
    // listOf(HomeKey, UserListKey)
    val backStack: List<NavKey>? = matchResult?.backStack
    val key: UserListKey? = matchResult?.key
}

@Sampled
fun deepLinkMatcherWithConditionalBackStackSample() {
    val matcher =
        UriDeepLinkMatcher(
                uriPattern = DeepLinkUri("https://www.nav3deeplinksample.com/articles/{id}"),
                serializer = serializer<Article>(),
            )
            .withBackStack { matchResult ->
                val article = matchResult.key
                val parent =
                    if (article.id >= 10) ArticleSection("Shipping") else ArticleSection("Returns")
                listOf(parent, article)
            }

    // handling a request
    val request =
        DeepLinkRequest(uri = DeepLinkUri("https://www.nav3deeplinksample.com/articles/12"))

    // returns a BackStackMatchResult containing the Article key and parent key based on article id
    val matchResult: BackStackMatchResult<Article, NavKey>? = matcher.match(request)
    // listOf(parent, article)
    val backStack: List<NavKey>? = matchResult?.backStack
    val key: Article? = matchResult?.key
}
