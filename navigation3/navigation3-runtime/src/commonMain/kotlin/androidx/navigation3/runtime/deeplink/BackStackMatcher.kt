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

/**
 * A [DeepLinkMatcher] that builds a back stack when match succeeds.
 *
 * Must be created by calling [withBackStack] on a [DeepLinkMatcher]. The receiving
 * [DeepLinkMatcher] will become the nested [matcher].
 *
 * Wraps the provided [matcher] and matches it with a [DeepLinkRequest]. When a match succeeds,
 * returns a [BackStackMatchResult] containing the original [DeepLinkMatcher.MatchResult] and the
 * back stack built with the back stack builder.
 *
 * [T] the type of the navigation key associated with this matcher, must be of type [K]
 *
 * [K] the element type of the back stack [List]
 *
 * @sample androidx.navigation3.runtime.samples.deeplink.deepLinkMatcherWithBackStackSample
 * @sample androidx.navigation3.runtime.samples.deeplink.deepLinkMatcherWithConditionalBackStackSample
 */
public class BackStackMatcher<out T : K, K : Any>
/**
 * @param matcher the [DeepLinkMatcher] to wrap
 * @param backStackBuilder lambda that provides the [DeepLinkMatcher.MatchResult] from [matcher] and
 *   returns a back stack of `List<K>`
 */
internal constructor(
    private val matcher: DeepLinkMatcher<T, MatchResult<T>>,
    private val backStackBuilder: (matchResult: MatchResult<T>) -> List<K>,
) : DeepLinkMatcher<T, BackStackMatchResult<T, K>>() {

    override fun matchRequest(request: DeepLinkRequest): BackStackMatchResult<T, K>? {
        val result = matcher.match(request) ?: return null
        return BackStackMatchResult(result, backStackBuilder)
    }
}

/**
 * Result returned when a [BackStackMatcher] successfully matches a [DeepLinkRequest].
 *
 * Contains the original [DeepLinkMatcher.MatchResult] and the [backStack] constructed for the
 * match.
 *
 * [T] the type of the navigation key associated with this result, must be of type [K]
 *
 * [K] the element type of the back stack [List]
 */
public class BackStackMatchResult<out T : K, K : Any>
/**
 * @param matchResult the [DeepLinkMatcher.MatchResult] to wrap
 * @param backStackBuilder lambda used to construct the [backStack] from [matchResult]
 */
internal constructor(
    matchResult: DeepLinkMatcher.MatchResult<T>,
    private val backStackBuilder: (matchResult: DeepLinkMatcher.MatchResult<T>) -> List<K>,
) : WrappedMatchResult<T>(matchResult) {

    /**
     * The back stack of navigation keys constructed for this match result.
     *
     * The back stack is evaluated on initial access and caches the result.
     */
    public val backStack: List<K> by lazy { backStackBuilder(matchResult) }
}

/**
 * Returns a [BackStackMatcher] which wraps this [DeepLinkMatcher] to build a back stack upon
 * matching.
 *
 * When this matcher matches a [DeepLinkRequest], [backStackBuilder] is invoked with the
 * [DeepLinkMatcher.MatchResult] to construct the back stack.
 *
 * [T] the type of the navigation key associated with this matcher, must be of type [K] [K] the
 * element type of the back stack [List]
 *
 * @param backStackBuilder lambda used to construct the back stack from the
 *   [DeepLinkMatcher.MatchResult]
 * @return a [BackStackMatcher] wrapping this matcher
 * @sample androidx.navigation3.runtime.samples.deeplink.deepLinkMatcherWithBackStackSample
 * @sample androidx.navigation3.runtime.samples.deeplink.deepLinkMatcherWithConditionalBackStackSample
 */
public fun <T : K, K : Any> DeepLinkMatcher<T, DeepLinkMatcher.MatchResult<T>>.withBackStack(
    backStackBuilder: (matchResult: DeepLinkMatcher.MatchResult<T>) -> List<K>
): BackStackMatcher<T, K> = BackStackMatcher(this, backStackBuilder)
