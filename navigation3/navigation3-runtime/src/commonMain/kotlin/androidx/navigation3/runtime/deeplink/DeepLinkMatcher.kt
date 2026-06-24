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

import androidx.navigation3.runtime.fastForEachOrForEach

/**
 * Encompasses the logic to match a navigation key of type [T] against a [DeepLinkRequest].
 *
 * A navigation key can be associated with more than one [DeepLinkMatcher] if it supports different
 * forms of deep links.
 *
 * [T] The type of the navigation key associated with this deep link.
 *
 * @param filters an optional list of [Filter] to apply to the [DeepLinkRequest] during matching
 */
public abstract class DeepLinkMatcher<out T : Any>(
    private val filters: List<Filter> = emptyList()
) {
    /**
     * Matches a [DeepLinkRequest] to a [DeepLinkMatcher].
     *
     * The entry point to match a [DeepLinkMatcher] to a [DeepLinkRequest]. It iterates through any
     * [filters] and if all filters returns true, proceeds to call [matchRequest] to get the final
     * match result.
     *
     * Returns [MatchResult] if it is a match, and returns null otherwise.
     *
     * @param request the [DeepLinkRequest] to match against
     */
    public fun match(request: DeepLinkRequest): MatchResult<T>? {
        // check filters first to avoid creating and discarding a key that doesn't match filters\
        filters.fastForEachOrForEach { filter ->
            val isMatch = filter.filterRequest(request)
            if (!isMatch) return null
        }
        return matchRequest(request)
    }

    /**
     * Matches a [DeepLinkRequest] to a [DeepLinkMatcher].
     *
     * The core function that is called within the matching process after all [filters] are applied.
     * Subclasses should override this to implement matching logic beyond matching [filters].
     *
     * Returns [MatchResult] if it is a match, and returns null otherwise.
     *
     * @param request the [DeepLinkRequest] to match against
     */
    protected abstract fun matchRequest(request: DeepLinkRequest): MatchResult<T>?

    /**
     * A filter for a deep link, such as a mimeType.
     *
     * Filters declared in a [DeepLinkMatcher] must be present in a [DeepLinkRequest]. On the other
     * hand, a matching [DeepLinkRequest] may contain more filter info than is required by a
     * [DeepLinkMatcher]
     */
    public fun interface Filter {
        /**
         * Matches a [DeepLinkRequest] to this [Filter].
         *
         * Returns true if they are a match, false otherwise.
         *
         * @sample androidx.navigation3.runtime.samples.deeplink.staticKeyDeepLinkMatcherSample
         */
        public fun filterRequest(request: DeepLinkRequest): Boolean
    }

    // UnsafeVariance is safe here because the class and its function cannot mutate the key
    /**
     * The class that is returned when a [DeepLinkMatcher] matches with a [DeepLinkRequest]
     *
     * @param key the navigation key representing the deep link target
     */
    public open class MatchResult<out T>(public val key: T) :
        Comparable<MatchResult<@UnsafeVariance T>> {
        /**
         * Compares this [MatchResult] to [other] and returns an Int result.
         *
         * Returns zero if this result is equal to the other result, a negative number if it's less
         * than the other, or a positive number if it's greater than the other.
         */
        public override fun compareTo(other: MatchResult<@UnsafeVariance T>): Int = 0
    }

    public companion object {
        /**
         * Creates a [DeepLinkMatcher.Filter] that filters a [DeepLinkRequest] with the mimeType
         * defined on the [DeepLinkMatcher]. Matching is not case-sensitive.
         *
         * @param mimeType the action the filter by
         * @return true if the mimeType exactly matches the [DeepLinkMatcher]'s action or if the
         *   matcher did not define any actions, false otherwise.
         */
        public fun mimeTypeFilter(mimeType: String): Filter = Filter { request ->
            mimeType.equals(request.mimeType, true)
        }
    }
}
