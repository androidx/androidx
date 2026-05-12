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

import kotlin.text.orEmpty
import kotlinx.serialization.KSerializer

// catches placeholder pattern i.e. {argName}
private val FILL_IN_PATTERN = Regex("\\{(.+?)\\}")
private val PATH_REGEX = Regex("([^/]*?|)")

/**
 * Represents a deep link that can be deep linked into when matched with a [DeepLinkRequest]
 *
 * If the matcher constructed with this overload matches a [DeepLinkRequest], the provided
 * [serializer] will be used to instantiate the deep link target's navigation key.
 *
 * [T] The type of the navigation key associated with this deep link.
 *
 * **Supported Path patterns and extracted argument examples**
 * 1. Exact Path
 *     - Pattern: `www.example.com/users`
 *     - Arguments: `https://www.example.com/users`
 *     - Extracted values: Extracts no arguments.
 * 2. Single placeholder
 *     - Pattern: `www.example.com/users/{id}`
 *     - Arguments: Extracts argument `["id"]`.
 *     - Request: `https://www.example.com/users/123`
 *     - Extracted values: Maps "id" to ["123"].
 * 3. Multiple placeholders
 *     - Pattern: `www.example.com/users/{first}-{last}`
 *     - Arguments: Extracts arguments ["first", "last"].
 *     - Request: `https://www.example.com/users/john-doe`
 *     - Extracted values: Maps "first" to ["john"] and "last" to ["doe"].
 * 4. Wildcard
 *     - Pattern: `www.example.com/users/.*`
 *     - Arguments: Extracts no arguments.
 * 5. Mixed literal and placeholder
 *     - Pattern: `www.example.com/users/user_{id}`
 *     - Arguments: Extracts argument ["id"].
 *     - Request: `https://www.example.com/users/user_123`
 *     - Extracted values: Maps "id" to ["123"].
 * 6. Empty String
 *     - Pattern: `www.example.com/users/{id}/profile`
 *     - Arguments: Extracts argument ["id"].
 *     - Request: `www.example.com/users//profile`
 *     - Extracted values: Maps "id" to [""].
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

    // Pair of path pattern regex to list of extracted arg names. List is empty if path
    // has no args.
    private val parsedPath: Pair<Regex, List<String>> by lazy {
        UriPatternParser.parsePath(uriPattern)
    }

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
    override fun matchRequest(request: DeepLinkRequest): UriMatchResult<T>? =
        request.uri?.let { matchUri(it) }

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
        if (!uri.getScheme().equals(uriPattern.getScheme(), ignoreCase = true)) return null
        if (!uri.getAuthority().equals(uriPattern.getAuthority(), ignoreCase = true)) return null
        val hasTrailingWildcard = uriPattern.getPathSegments().lastOrNull() == ".*"
        if (!hasTrailingWildcard && uri.getPathSegments().size != uriPattern.getPathSegments().size)
            return null

        /**
         * This section mainly extracts arguments. Unless path regex mismatches, we won't know if
         * this is a true match or not until we actually attempt to instantiate an instance of T
         * later on.
         */
        // Request's path must match in number of path segments, string literals, and placeholders
        // in the same order as the pattern. Regex allows for any query or fragment if present.
        // Null if regex does not match, empty map if regex matches but no path args.
        val extractedPathArgs = UriRequestParser.extractPathArgs(parsedPath, uri) ?: return null
        return matchArguments(extractedPathArgs)
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

internal object UriPatternParser {
    /**
     * Parses the path of [uriPattern] into a REGEX and extracts path arguments. This includes
     * scheme, authority, and path segments.
     *
     * Iterates over path segments returned from [DeepLinkUri.getPathSegments] and extracts
     * placeholders (i.e., `{id}`) as argument names.
     *
     * @param uriPattern the uri pattern of a supported deep link
     * @return A list of extracted argument names in the order they appear in the path. Returns
     *   empty list if path does not contain arguments.
     * @see [UriDeepLinkMatcher] for supported path patterns and examples
     */
    internal fun parsePath(uriPattern: DeepLinkUri): Pair<Regex, List<String>> {
        val uriRegex = StringBuilder("^")
        // parse scheme
        uriPattern.getScheme()?.let { scheme ->
            // escape in case scheme contains any special regex characters e.g. "foo.bar://"
            uriRegex.append(Regex.escape(scheme)).append("://")
        } ?: uriRegex.append("http[s]?://")
        // parse authority
        uriPattern.getAuthority()?.let { uriRegex.append(Regex.escape(it)) }
        // parse args
        val args = mutableListOf<String>()
        uriPattern.getPathSegments().fastForEachOrForEach { segment ->
            uriRegex.append("/")
            buildRegex(segment, args, uriRegex)
        }
        // Match either the end of string if all params are optional or match the
        // question mark (or pound symbol) and 0 or more characters after it
        uriRegex.append("($|(\\?(.)*)|(#(.)*))")
        val regex = Regex(saveWildcardInRegex(uriRegex.toString()), RegexOption.IGNORE_CASE)
        return Pair(regex, args)
    }

    /**
     * Builds a regular expression for a uri segment by replacing placeholders with regex patterns.
     *
     * For example, given a segment "user_{id}_profile":
     * - It extracts the argument name "id" and adds it to [args].
     * - It builds the regex "user_([^/]*?|)_profile" and appends it to [uriRegex].
     *
     * @param segment The URI segment to parse i.e. path segment or fragment
     * @param args The list of extracted argument names
     * @param uriRegex The StringBuilder to build the regex
     */
    private fun buildRegex(segment: String, args: MutableList<String>, uriRegex: StringBuilder) {
        // if there are no placeholders, just append the string literal
        if (!segment.contains('{')) {
            uriRegex.append(Regex.escape(segment))
            return
        }
        // handles single or multi placeholders ({arg}, {arg1}-{arg2}), mixed literal & placeholder
        // (user_{id}), and invalid braces {{}}
        var result = FILL_IN_PATTERN.find(segment)
        var appendPos = 0
        // iterate through all possible placeholders in the segment
        while (result != null) {
            val argName = result.groups[1]!!.value
            args.add(argName)
            // string literal before the placeholder
            if (result.range.first > appendPos) {
                uriRegex.append(Regex.escape(segment.substring(appendPos, result.range.first)))
            }
            uriRegex.append(PATH_REGEX.pattern)
            appendPos = result.range.last + 1
            result = result.next()
        }
        // string literal after the placeholder
        if (appendPos < segment.length) {
            uriRegex.append(Regex.escape(segment.substring(appendPos)))
        }
    }

    /**
     * Converts an escaped wildcard back to its regex form. It is easier to escape first and revert
     * after than to avoid escaping wildcard when parsing the uri pattern.
     */
    private fun saveWildcardInRegex(regex: String): String =
        if (regex.contains("\\Q") && regex.contains("\\E")) regex.replace(".*", "\\E.*\\Q")
        else if (regex.contains("\\.\\*")) regex.replace("\\.\\*", ".*") else regex
}

internal object UriRequestParser {
    /**
     * Matches the path component of a requested URI against the parsed path pattern.
     *
     * @param parsedPath the supported path's regex pattern and extracted argument names
     * @param requestedUri The URI to match against.
     * @return A map of argument names to their extracted values if the path matches. Returns null
     *   if the requested path pattern does not match with the path pattern. Returns an empty map if
     *   the requested path matches but does not contain arguments.
     * @see [UriDeepLinkMatcher] for supported path patterns and examples
     */
    internal fun extractPathArgs(
        parsedPath: Pair<Regex, List<String>>,
        requestedUri: DeepLinkUri,
    ): Map<String, List<String>>? =
        parsedPath.first.matchEntire(requestedUri.toString())?.let { matchResult ->
            buildMap {
                parsedPath.second.fastMapIndexed { index, argName ->
                    val value =
                        matchResult.groups[index + 1]
                            ?.value
                            ?.let { DeepLinkUriUtils.decode(it) }
                            .orEmpty()
                    put(argName, listOf(value))
                }
            }
        }
}
