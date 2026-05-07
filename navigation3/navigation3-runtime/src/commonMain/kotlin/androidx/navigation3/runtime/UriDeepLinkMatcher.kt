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

import kotlinx.serialization.KSerializer

/**
 * Represents a deep link that can be deep linked into when matched with a [DeepLinkRequest]
 *
 * If the matcher constructed with this overload matches a [DeepLinkRequest], the provided
 * [serializer] will be used to instantiate the deep link target's navigation key.
 *
 * [T] The type of the navigation key associated with this deep link.
 *
 * @param uriPattern The [DeepLinkUri] containing the uri pattern that this matcher supports.
 * @param serializer The serializer to instantiate an instance of [T]
 * @param filters an optional list of filters to filter a [DeepLinkRequest] when matching
 */
public open class UriDeepLinkMatcher<T : Any>(
    private val uriPattern: DeepLinkUri,
    private val serializer: KSerializer<T>,
    filters: List<Filter<Any>> = emptyList(),
) : DeepLinkMatcher<T>(filters) {

    /**
     * Matches this matcher against a [DeepLinkRequest].
     *
     * Match succeeds if:
     * - request passes all the provided [filters]
     * - can successfully instantiate a [T] instance with the [serializer] based on the provided
     *   arguments extracted from the request
     *
     * @param request The deep link request to match.
     * @return A [UriMatchResult] containing the navigation key [T] and extracted arguments if
     *   matched, null otherwise.
     */
    override fun matchRequest(request: DeepLinkRequest): UriMatchResult<T>? {
        TODO("Not yet implemented")
    }

    /**
     * Matches this [UriDeepLinkMatcher] against a [DeepLinkRequest.uri].
     *
     * It checks for scheme, authority, and path segment count. If they match, proceeds to extract
     * all argument values from the [DeepLinkRequest.uri].
     *
     * @param uri The requested [DeepLinkUri] to match with.
     * @return A [UriMatchResult] containing the navigation key [T] and extracted arguments if
     *   matched, null otherwise.
     */
    protected open fun matchUri(uri: DeepLinkUri): UriMatchResult<T>? {
        TODO()
    }

    /**
     * Matches the argument values extracted from [DeepLinkRequest.uri] and instantiates a [T]
     * instance with the provided [serializer] and extracted values.
     *
     * @param pathArgs The extracted path arguments. Empty if no values were extracted.
     * @param queryArgs The extracted query arguments. Empty if the uri contains no query or no
     *   values were extracted.
     * @param fragmentArgs The extracted fragment arguments. Empty if the uri contains no fragment
     *   or no values were extracted.
     * @return A [UriMatchResult] containing the navigation key [T] and extracted arguments if
     *   matched, null otherwise.
     */
    protected open fun matchArguments(
        pathArgs: Map<String, List<String>> = emptyMap(),
        queryArgs: Map<String, List<String>> = emptyMap(),
        fragmentArgs: Map<String, List<String>> = emptyMap(),
    ): UriMatchResult<T>? {
        TODO()
    }
}

/**
 * The class that is returned when a [UriDeepLinkMatcher] matches with a [DeepLinkRequest]
 *
 * @param key the navigation key representing the deep link target
 * @param arguments the map of arguments extracted from the [DeepLinkRequest]
 */
public class UriMatchResult<T : Any>(key: T, arguments: Map<String, List<String>>) :
    DeepLinkMatcher.MatchResult<T>(key)
