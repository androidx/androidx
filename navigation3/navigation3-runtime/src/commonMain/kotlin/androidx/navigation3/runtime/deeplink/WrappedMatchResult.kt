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
 * A [DeepLinkMatcher.MatchResult] implementation that wraps another MatchResult.
 *
 * This class can be used to create a MatchResult layered on top of another MatchResult without
 * making changes to the wrapped one.
 *
 * [T] The type of the navigation key associated with this result.
 *
 * @param matchResult the [DeepLinkMatcher.MatchResult] to wrap
 */
public abstract class WrappedMatchResult<out T : Any>(
    public val matchResult: DeepLinkMatcher.MatchResult<T>
) : DeepLinkMatcher.MatchResult<T>(matchResult.key) {

    /**
     * Compares this [WrappedMatchResult] to [other] and returns an Int result.
     *
     * If [other] is also a WrappedMatchResult, compares this [matchResult] with other.matchResult.
     * Otherwise, compares this [matchResult] with [other] directly.
     *
     * Returns zero if this result is equal to the other result, a negative number if it's less, and
     * a positive number if it's greater.
     *
     * @param other the [DeepLinkMatcher.MatchResult] to compare with
     */
    override fun compareTo(other: DeepLinkMatcher.MatchResult<@UnsafeVariance T>): Int {
        val otherResult = if (other is WrappedMatchResult<T>) other.matchResult else other
        return matchResult.compareTo(otherResult)
    }
}
