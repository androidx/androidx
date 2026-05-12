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
 * A [DeepLinkMatcher] that matches based on a list of [Filter] and if all filters match, returns
 * the input [key] in the [MatchResult].
 *
 * [T] The Type of the navigation key associated with this deep link matcher.
 *
 * @param key the navigation key associated with this deep link matcher
 * @param filters the list of [Filter] to match with the [DeepLinkRequest]
 */
public class StaticKeyDeepLinkMatcher<T : Any>(public val key: T, filters: List<Filter<Any>>) :
    DeepLinkMatcher<T>(filters) {

    /**
     * Returns a [MatchResult] containing the [key] if all [filters] match the [DeepLinkRequest].
     */
    override fun matchRequest(request: DeepLinkRequest): MatchResult<T> = MatchResult(key)
}
