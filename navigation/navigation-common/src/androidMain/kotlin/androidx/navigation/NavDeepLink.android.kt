/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.navigation

import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 *
 * This should be added to a [NavDestination] using
 * [NavDestination.addDeepLink].
 */
public class NavDeepLink internal constructor(
    /**
     * The uri pattern from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.uri
     */
    public val uriPattern: String?,
    /**
     * The action from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.action
     */
    public val action: String?,
    /**
     * The mimeType from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.mimeType
     */
    public val mimeType: String?
) {
    // path
    private val pathArgs = mutableListOf<String>()
    private var pathRegex: String? = null
    private val pathPattern by lazy {
        pathRegex?.let { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }
    }

    // query
    private val isParameterizedQuery by lazy {
        uriPattern != null && Uri.parse(uriPattern).query != null
    }
    private val queryArgsMap by lazy(LazyThreadSafetyMode.NONE) { parseQuery() }
    private var isSingleQueryParamValueOnly = false

    // fragment
    private val fragArgsAndRegex: Pair<MutableList<String>, String>? by
        lazy(LazyThreadSafetyMode.NONE) { parseFragment() }
    private val fragArgs by lazy(LazyThreadSafetyMode.NONE) {
        fragArgsAndRegex?.first ?: mutableListOf()
    }
    private val fragRegex by lazy(LazyThreadSafetyMode.NONE) {
        fragArgsAndRegex?.second
    }
    private val fragPattern by lazy {
        fragRegex?.let { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }
    }

    // mime
    private var mimeTypeRegex: String? = null
    private val mimeTypePattern by lazy {
        mimeTypeRegex?.let { Pattern.compile(it) }
    }

    /** Arguments present in the deep link, including both path and query arguments. */
    internal val argumentsNames: List<String>
        get() = pathArgs + queryArgsMap.values.flatMap { it.arguments } + fragArgs

    public var isExactDeepLink: Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get
        internal set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(uri: String) : this(uri, null, null)

    private fun buildRegex(
        uri: String,
        args: MutableList<String>,
        uriRegex: StringBuilder,
    ) {
        val matcher = FILL_IN_PATTERN.matcher(uri)
        var appendPos = 0
        while (matcher.find()) {
            val argName = matcher.group(1) as String
            args.add(argName)
            // Use Pattern.quote() to treat the input string as a literal
            if (matcher.start() > appendPos) {
                uriRegex.append(Pattern.quote(uri.substring(appendPos, matcher.start())))
            }
            uriRegex.append("([^/]+?)")
            appendPos = matcher.end()
        }
        if (appendPos < uri.length) {
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos)))
        }
    }

    internal fun matches(uri: Uri): Boolean {
        return matches(NavDeepLinkRequest(uri, null, null))
    }

    internal fun matches(deepLinkRequest: NavDeepLinkRequest): Boolean {
        if (!matchUri(deepLinkRequest.uri)) {
            return false
        }
        return if (!matchAction(deepLinkRequest.action)) {
            false
        } else matchMimeType(deepLinkRequest.mimeType)
    }

    private fun matchUri(uri: Uri?): Boolean {
        // If the null status of both are not the same return false.
        return if (uri == null == (pathPattern != null)) {
            false
        } else uri == null || pathPattern!!.matcher(uri.toString()).matches()
        // If both are null return true, otherwise see if they match
    }

    private fun matchAction(action: String?): Boolean {
        // If the null status of both are not the same return false.
        return if (action == null == (this.action != null)) {
            false
        } else action == null || this.action == action
        // If both are null return true, otherwise see if they match
    }

    private fun matchMimeType(mimeType: String?): Boolean {
        // If the null status of both are not the same return false.
        return if (mimeType == null == (this.mimeType != null)) {
            false
        } else mimeType == null || mimeTypePattern!!.matcher(mimeType).matches()

        // If both are null return true, otherwise see if they match
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMimeTypeMatchRating(mimeType: String): Int {
        return if (this.mimeType == null || !mimeTypePattern!!.matcher(mimeType).matches()) {
            -1
        } else MimeType(this.mimeType)
            .compareTo(MimeType(mimeType))
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "NullableCollection")
    /** Pattern.compile has no nullability for the regex parameter
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMatchingArguments(
        deepLink: Uri,
        arguments: Map<String, NavArgument?>
    ): Bundle? {
        // first check overall uri pattern for quick return if general pattern does not match
        val matcher = pathPattern?.matcher(deepLink.toString()) ?: return null
        if (!matcher.matches()) {
            return null
        }
        // get matching path and query arguments and store in bundle
        val bundle = Bundle()
        if (!getMatchingPathArguments(matcher, bundle, arguments)) return null
        if (isParameterizedQuery && !getMatchingQueryArguments(deepLink, bundle, arguments)) {
            return null
        }
        // no match on optional fragment should not prevent a link from matching otherwise
        getMatchingUriFragment(deepLink.fragment, bundle, arguments)

        // Check that all required arguments are present in bundle
        val missingRequiredArguments = arguments.missingRequiredArguments { argName ->
            !bundle.containsKey(argName)
        }
        if (missingRequiredArguments.isNotEmpty()) return null

        return bundle
    }

    /**
     * Returns a bundle containing matching path and query arguments with the requested uri.
     * It returns empty bundle if this Deeplink's path pattern does not match with the uri.
     */
    internal fun getMatchingPathAndQueryArgs(
        deepLink: Uri?,
        arguments: Map<String, NavArgument?>
    ): Bundle {
        val bundle = Bundle()
        if (deepLink == null) return bundle
        val matcher = pathPattern?.matcher(deepLink.toString()) ?: return bundle
        if (!matcher.matches()) {
            return bundle
        }
        getMatchingPathArguments(matcher, bundle, arguments)
        if (isParameterizedQuery) getMatchingQueryArguments(deepLink, bundle, arguments)
        return bundle
    }

    private fun getMatchingUriFragment(
        fragment: String?,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ) {
        // Base condition of a matching fragment is a complete match on regex pattern. If a
        // required fragment arg is present while regex does not match, this will be caught later
        // on as a non-match when we check for presence of required args in the bundle.
        val matcher = fragPattern?.matcher(fragment.toString()) ?: return
        if (!matcher.matches()) return

        this.fragArgs.mapIndexed { index, argumentName ->
            val value = Uri.decode(matcher.group(index + 1))
            val argument = arguments[argumentName]
            try {
                if (parseArgument(bundle, argumentName, value, argument)) {
                    return
                }
            } catch (e: IllegalArgumentException) {
                return
            }
        }
    }

    private fun getMatchingPathArguments(
        matcher: Matcher,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        this.pathArgs.mapIndexed { index, argumentName ->
            val value = Uri.decode(matcher.group(index + 1))
            val argument = arguments[argumentName]
            try {
                if (parseArgument(bundle, argumentName, value, argument)) {
                    return false
                }
            } catch (e: IllegalArgumentException) {
                // Failed to parse means this isn't a valid deep link
                // for the given URI - i.e., the URI contains a non-integer
                // value for an integer argument
                return false
            }
        }
        return true
    }

    private fun getMatchingQueryArguments(
        deepLink: Uri,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        queryArgsMap.forEach { entry ->
            val paramName = entry.key
            val storedParam = entry.value

            var inputParams = deepLink.getQueryParameters(paramName)
            if (isSingleQueryParamValueOnly) {
                // If the deep link contains a single query param with no value,
                // we will treat everything after the '?' as the input parameter
                val argValue = deepLink.query
                if (argValue != null && argValue != deepLink.toString()) {
                    inputParams = listOf(argValue)
                }
            }
            if (!parseInputParams(inputParams, storedParam, bundle, arguments)) {
                return false
            }
        }
        return true
    }

    private fun parseInputParams(
        inputParams: List<String>?,
        storedParam: ParamQuery,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>,
    ): Boolean {
        inputParams?.forEach { inputParam ->
            val argMatcher = storedParam.paramRegex?.let {
                Pattern.compile(
                    it, Pattern.DOTALL
                ).matcher(inputParam)
            }
            if (argMatcher == null || !argMatcher.matches()) {
                return false
            }

            val queryParamBundle = Bundle()
            try {
                storedParam.arguments.mapIndexed { index, argName ->
                    val value = argMatcher.group(index + 1) ?: ""
                    val argument = arguments[argName]
                    if (parseArgumentForRepeatedParam(bundle, argName, value, argument)) {
                        // Passing in a value the exact same as the placeholder will be treated the
                        // as if no value was passed (unless value is based on String),
                        // being replaced if it is optional or throwing an error if it is required.
                        if (parseArgument(queryParamBundle, argName, value, argument)) {
                            return false
                        }
                    }
                }
                bundle.putAll(queryParamBundle)
            } catch (e: IllegalArgumentException) {
                // Failed to parse means that at least one of the arguments that were supposed
                // to fill in the query parameter was not valid and therefore, we will exclude
                // that particular parameter from the argument bundle.
            }
        }
        return true
    }

    internal fun calculateMatchingPathSegments(requestedLink: Uri?): Int {
        if (requestedLink == null || uriPattern == null) return 0

        val requestedPathSegments = requestedLink.pathSegments
        val uriPathSegments = Uri.parse(uriPattern).pathSegments

        val matches = requestedPathSegments.intersect(uriPathSegments)
        return matches.size
    }

    private fun parseArgument(
        bundle: Bundle,
        name: String,
        value: String,
        argument: NavArgument?
    ): Boolean {
        if (argument != null) {
            val type = argument.type
            type.parseAndPut(bundle, name, value)
        } else {
            bundle.putString(name, value)
        }
        return false
    }

    private fun parseArgumentForRepeatedParam(
        bundle: Bundle,
        name: String,
        value: String?,
        argument: NavArgument?
    ): Boolean {
        if (!bundle.containsKey(name)) {
            return true
        }
        if (argument != null) {
            val type = argument.type
            val previousValue = type[bundle, name]
            type.parseAndPut(bundle, name, value, previousValue)
        }
        return false
    }

    /**
     * Used to maintain query parameters and the mArguments they match with.
     */
    private class ParamQuery {
        var paramRegex: String? = null
        val arguments = mutableListOf<String>()

        fun addArgumentName(name: String) {
            arguments.add(name)
        }

        fun getArgumentName(index: Int): String {
            return arguments[index]
        }

        fun size(): Int {
            return arguments.size
        }
    }

    private class MimeType(mimeType: String) : Comparable<MimeType> {
        var type: String
        var subType: String
        override fun compareTo(other: MimeType): Int {
            var result = 0
            // matching just subtypes is 1
            // matching just types is 2
            // matching both is 3
            if (type == other.type) {
                result += 2
            }
            if (subType == other.subType) {
                result++
            }
            return result
        }

        init {
            val typeAndSubType =
                mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            type = typeAndSubType[0]
            subType = typeAndSubType[1]
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NavDeepLink) return false
        return uriPattern == other.uriPattern &&
            action == other.action &&
            mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + uriPattern.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }

    /**
     * A builder for constructing [NavDeepLink] instances.
     */
    public class Builder {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor()

        private var uriPattern: String? = null
        private var action: String? = null
        private var mimeType: String? = null

        /**
         * Set the uri pattern for the [NavDeepLink].
         *
         * @param uriPattern The uri pattern to add to the NavDeepLink
         *
         * @return This builder.
         */
        public fun setUriPattern(uriPattern: String): Builder {
            this.uriPattern = uriPattern
            return this
        }

        /**
         * Set the action for the [NavDeepLink].
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLink
         *
         * @return This builder.
         */
        public fun setAction(action: String): Builder {
            // if the action given at runtime is empty we should throw
            require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
            this.action = action
            return this
        }

        /**
         * Set the mimeType for the [NavDeepLink].
         *
         * @param mimeType the mimeType for the NavDeepLink
         *
         * @return This builder.
         */
        public fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }

        /**
         * Build the [NavDeepLink] specified by this builder.
         *
         * @return the newly constructed NavDeepLink.
         */
        public fun build(): NavDeepLink {
            return NavDeepLink(uriPattern, action, mimeType)
        }

        internal companion object {
            /**
             * Creates a [NavDeepLink.Builder] with a set uri pattern.
             *
             * @param uriPattern The uri pattern to add to the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromUriPattern(uriPattern: String): Builder {
                val builder = Builder()
                builder.setUriPattern(uriPattern)
                return builder
            }

            /**
             * Creates a [NavDeepLink.Builder] with a set action.
             *
             * @throws IllegalArgumentException if the action is empty.
             *
             * @param action the intent action for the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromAction(action: String): Builder {
                // if the action given at runtime is empty we should throw
                require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }

            /**
             * Creates a [NavDeepLink.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLink
             * @return a [Builder] instance
             */
            @JvmStatic
            fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }

    private companion object {
        private val SCHEME_PATTERN = Pattern.compile("^[a-zA-Z]+[+\\w\\-.]*:")
        private val FILL_IN_PATTERN = Pattern.compile("\\{(.+?)\\}")
    }

    private fun parsePath() {
        if (uriPattern == null) return

        val uriRegex = StringBuilder("^")
        // append scheme pattern
        if (!SCHEME_PATTERN.matcher(uriPattern).find()) {
            uriRegex.append("http[s]?://")
        }
        // extract beginning of uriPattern until it hits either a query(?), a framgment(#), or
        // end of uriPattern
        var matcher = Pattern.compile("(\\?|\\#|$)").matcher(uriPattern)
        matcher.find().let {
            buildRegex(uriPattern.substring(0, matcher.start()), pathArgs, uriRegex)
            isExactDeepLink = !uriRegex.contains(".*") && !uriRegex.contains("([^/]+?)")
            // Match either the end of string if all params are optional or match the
            // question mark (or pound symbol) and 0 or more characters after it
            uriRegex.append("($|(\\?(.)*)|(\\#(.)*))")
        }
        // we need to specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        pathRegex = uriRegex.toString().replace(".*", "\\E.*\\Q")
    }

    private fun parseQuery(): MutableMap<String, ParamQuery> {
        val paramArgMap = mutableMapOf<String, ParamQuery>()
        if (!isParameterizedQuery) return paramArgMap
        val uri = Uri.parse(uriPattern)

        for (paramName in uri.queryParameterNames) {
            val argRegex = StringBuilder()
            val queryParams = uri.getQueryParameters(paramName)
            require(queryParams.size <= 1) {
                "Query parameter $paramName must only be present once in $uriPattern. " +
                    "To support repeated query parameters, use an array type for your " +
                    "argument and the pattern provided in your URI will be used to " +
                    "parse each query parameter instance."
            }
            val queryParam = queryParams.firstOrNull()
                ?: paramName.apply { isSingleQueryParamValueOnly = true }
            val matcher = FILL_IN_PATTERN.matcher(queryParam)
            var appendPos = 0
            val param = ParamQuery()
            // Build the regex for each query param
            while (matcher.find()) {
                // matcher.group(1) as String = "tab" (the extracted param arg from {tab})
                param.addArgumentName(matcher.group(1) as String)
                argRegex.append(
                    Pattern.quote(
                        queryParam.substring(
                            appendPos,
                            matcher.start()
                        )
                    )
                )
                argRegex.append("(.+?)?")
                appendPos = matcher.end()
            }
            if (appendPos < queryParam.length) {
                argRegex.append(Pattern.quote(queryParam.substring(appendPos)))
            }

            // Save the regex with wildcards unquoted, and add the param to the map with its
            // name as the key
            param.paramRegex = argRegex.toString().replace(".*", "\\E.*\\Q")
            paramArgMap[paramName] = param
        }
        return paramArgMap
    }

    private fun parseFragment(): Pair<MutableList<String>, String>? {
        if (uriPattern == null || Uri.parse(uriPattern).fragment == null) return null

        val fragArgs = mutableListOf<String>()
        val fragment = Uri.parse(uriPattern).fragment
        val fragRegex = StringBuilder()
        buildRegex(fragment!!, fragArgs, fragRegex)
        return fragArgs to fragRegex.toString()
    }

    private fun parseMime() {
        if (mimeType == null) return

        val mimeTypePattern = Pattern.compile("^[\\s\\S]+/[\\s\\S]+$")
        val mimeTypeMatcher = mimeTypePattern.matcher(mimeType)
        require(mimeTypeMatcher.matches()) {
            "The given mimeType $mimeType does not match to required \"type/subtype\" format"
        }

        // get the type and subtype of the mimeType
        val splitMimeType = MimeType(
            mimeType
        )

        // the matching pattern can have the exact name or it can be wildcard literal (*)
        val regex = "^(${splitMimeType.type}|[*]+)/(${splitMimeType.subType}|[*]+)$"

        // if the deep link type or subtype is wildcard, allow anything
        mimeTypeRegex = regex.replace("*|[*]", "[\\s\\S]")
    }

    init {
        parsePath()
        parseMime()
    }
}
