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
import androidx.core.os.bundleOf
import androidx.navigation.serialization.generateRoutePattern
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 *
 * This should be added to a [NavDestination] using [NavDestination.addDeepLink].
 */
public class NavDeepLink
internal constructor(
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
    private val fragArgs by
        lazy(LazyThreadSafetyMode.NONE) { fragArgsAndRegex?.first ?: mutableListOf() }
    private val fragRegex by lazy(LazyThreadSafetyMode.NONE) { fragArgsAndRegex?.second }
    private val fragPattern by lazy {
        fragRegex?.let { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }
    }

    // mime
    private var mimeTypeRegex: String? = null
    private val mimeTypePattern by lazy { mimeTypeRegex?.let { Pattern.compile(it) } }

    /** Arguments present in the deep link, including both path and query arguments. */
    internal val argumentsNames: List<String>
        get() = pathArgs + queryArgsMap.values.flatMap { it.arguments } + fragArgs

    public var isExactDeepLink: Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get
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
            // allows for empty path arguments
            uriRegex.append("([^/]*?|)")
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
        } else MimeType(this.mimeType).compareTo(MimeType(mimeType))
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "NullableCollection")
    /**
     * Pattern.compile has no nullability for the regex parameter
     *
     * May return null if any of the following:
     * 1. missing required arguments that don't have default values
     * 2. wrong value type (i.e. null for non-nullable arg)
     * 3. other exceptions from parsing an argument value
     *
     * May return empty bundle if any of the following:
     * 1. deeplink has no arguments
     * 2. deeplink contains arguments with unknown default values (i.e. deeplink from safe args with
     *    unknown default values)
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMatchingArguments(deepLink: Uri, arguments: Map<String, NavArgument?>): Bundle? {
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
        val missingRequiredArguments =
            arguments.missingRequiredArguments { argName -> !bundle.containsKey(argName) }
        if (missingRequiredArguments.isNotEmpty()) return null

        return bundle
    }

    /**
     * Returns a bundle containing matching path and query arguments with the requested uri. It
     * returns empty bundle if this Deeplink's path pattern does not match with the uri.
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
                parseArgument(bundle, argumentName, value, argument)
            } catch (e: IllegalArgumentException) {
                // parse failed, quick return
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
                parseArgument(bundle, argumentName, value, argument)
            } catch (e: IllegalArgumentException) {
                // Failed to parse means this isn't a valid deep link
                // for the given URI - i.e., the URI contains a non-integer
                // value for an integer argument
                return false
            }
        }
        // parse success
        return true
    }

    private fun getMatchingQueryArguments(
        deepLink: Uri,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        // key is queryParameterName (argName could be different), value is NavDeepLink.ParamQuery
        queryArgsMap.forEach { entry ->
            val paramName = entry.key
            val storedParam = entry.value

            // a list of the arg values under this queryParameterName
            // collection types (i.e. list, array) would potentially have listOf(arg1, arg2, arg3,
            // etc..)
            // non-collection types would usually have listOf(theArgValue)
            var inputParams = deepLink.getQueryParameters(paramName)
            if (isSingleQueryParamValueOnly) {
                // If the deep link contains a single query param with no value,
                // we will treat everything after the '?' as the input parameter
                val argValue = deepLink.query
                if (argValue != null && argValue != deepLink.toString()) {
                    inputParams = listOf(argValue)
                }
            }
            val parseSuccess = parseInputParams(inputParams, storedParam, bundle, arguments)
            if (!parseSuccess) return false
        }
        // parse success
        return true
    }

    /**
     * @param inputParams list of arg values under the same Uri.queryParameterName. For example:
     * 1. sample route "...?myArg=1&myArg=2" inputParams = listOf("1", "2")
     * 2. sample route "...?myArg=John_Doe" inputParams = listOf("John_Doe")
     *
     * @param storedParam the [ParamQuery] for a single Uri.queryParameter
     */
    private fun parseInputParams(
        inputParams: List<String>,
        storedParam: ParamQuery,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>,
    ): Boolean {
        val tempBundle = bundleOf()
        // try to start off by adding an empty bundle if there is no default value.
        storedParam.arguments.forEach { argName ->
            val argument = arguments[argName]
            val navType = argument?.type
            // for CollectionNavType, only fallback to empty collection if there isn't a default
            // value
            if (navType is CollectionNavType && !argument.isDefaultValuePresent) {
                navType.put(tempBundle, argName, navType.emptyCollection())
            }
        }
        inputParams.forEach { inputParam ->
            val argMatcher =
                storedParam.paramRegex?.let {
                    Pattern.compile(it, Pattern.DOTALL).matcher(inputParam)
                }
            // check if this particular arg value matches the expected regex.
            // for example, if the query was list of Int like "...?intId=1&intId=2&intId=abc",
            // this would return false when matching "abc".
            if (argMatcher == null || !argMatcher.matches()) {
                return false
            }
            // iterate over each argName under the same queryParameterName
            storedParam.arguments.mapIndexed { index, argName ->
                // make sure we get the correct value for this particular argName
                // i.e. if route is "...?myArg={firstName}_{lastName}"
                // and the inputParam is "John_Doe"
                // we need to map values to argName like this:
                // [firstName to "John", lastName to "Doe"]
                val value = argMatcher.group(index + 1) ?: ""
                val argument = arguments[argName]

                try {
                    if (!tempBundle.containsKey(argName)) {
                        // Passing in a value the exact same as the placeholder will be treated the
                        // as if no value was passed (unless value is based on String),
                        // being replaced if it is optional or throwing an error if it is required.
                        parseArgument(tempBundle, argName, value, argument)
                    } else {
                        parseArgumentForRepeatedParam(tempBundle, argName, value, argument)
                    }
                } catch (e: IllegalArgumentException) {
                    // Failed to parse means that at least one of the arguments that
                    // were supposed to fill in the query parameter was not valid.
                    // We will need to handle it here. Values that are not handled
                    // here will just be excluded from the argument bundle.
                }
            }
        }
        bundle.putAll(tempBundle)
        // parse success
        return true
    }

    internal fun calculateMatchingPathSegments(requestedLink: Uri?): Int {
        if (requestedLink == null || uriPattern == null) return 0

        val requestedPathSegments = requestedLink.pathSegments
        val uriPathSegments = Uri.parse(uriPattern).pathSegments

        val matches = requestedPathSegments.intersect(uriPathSegments)
        return matches.size
    }

    /**
     * Parses [value] based on the NavArgument's NavType and stores the result inside the [bundle].
     * Throws if parse fails.
     */
    private fun parseArgument(bundle: Bundle, name: String, value: String, argument: NavArgument?) {
        if (argument != null) {
            val type = argument.type
            type.parseAndPut(bundle, name, value)
        } else {
            bundle.putString(name, value)
        }
    }

    /**
     * Parses subsequent arg values under the same queryParameterName
     *
     * For example with route "...?myArg=one&myArg=two&myArg=three", [bundle] is expected to already
     * contain bundleOf([name] to "one"), and this function will parse & put values "two" and
     * "three" into the bundle under the same [name].
     */
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

    /** Used to maintain query parameters and the mArguments they match with. */
    private class ParamQuery {
        var paramRegex: String? = null
        // list of arg names under the same queryParamName, i.e. "...?name={first}_{last}"
        // queryParamName = "name", arguments = ["first", "last"]
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
            val typeAndSubType = mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }
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

    /** A builder for constructing [NavDeepLink] instances. */
    public class Builder {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public constructor()

        private var uriPattern: String? = null
        private var action: String? = null
        private var mimeType: String? = null

        /**
         * Set the uri pattern for the [NavDeepLink].
         *
         * @param uriPattern The uri pattern to add to the NavDeepLink
         * @return This builder.
         */
        public fun setUriPattern(uriPattern: String): Builder {
            this.uriPattern = uriPattern
            return this
        }

        /**
         * Set the uri pattern for the [NavDeepLink].
         *
         * Arguments extracted from destination [T] will be automatically appended to the base path
         * provided in [basePath].
         *
         * Arguments are appended based on property name and in the same order as their declaration
         * order in [T]. They are appended as query parameters if the argument has either:
         * 1. a default value
         * 2. a [NavType] of [CollectionNavType]
         *
         * Otherwise, the argument will be appended as path parameters. The final uriPattern is
         * generated by concatenating `uriPattern + path parameters + query parameters`.
         *
         * For example, the `name` property in this class does not meet either conditions and will
         * be appended as a path param.
         *
         * ```
         * @Serializable
         * class MyClass(val name: String)
         * ```
         *
         * Given a uriPattern of "www.example.com", the generated final uriPattern will be
         * `www.example.com/{name}`.
         *
         * The `name` property in this class has a default value and will be appended as a query.
         *
         * ```
         * @Serializable
         * class MyClass(val name: String = "default")
         * ```
         *
         * Given a uriPattern of "www.example.com", the final generated uriPattern will be
         * `www.example.com?name={name}`
         *
         * The append order is based on their declaration order in [T]
         *
         * ```
         * @Serializable
         * class MyClass(val name: String = "default", val id: Int, val code: Int)
         * ```
         *
         * Given a uriPattern of "www.example.com", the final generated uriPattern will be
         * `www.example.com/{id}/{code}?name={name}`. In this example, `name` is appended first as a
         * query param, then `id` and `code` respectively as path params. The final pattern is then
         * concatenated with `uriPattern + path + query`.
         *
         * @param T The destination's route from KClass
         * @param basePath The base uri path to append arguments onto
         * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
         *   [NavType]. May be empty if [T] does not use custom NavTypes.
         * @return This builder.
         */
        public inline fun <reified T : Any> setUriPattern(
            basePath: String,
            typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
        ): Builder = setUriPattern(basePath, T::class, typeMap)

        @OptIn(InternalSerializationApi::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // need to be public for reified delegation
        public fun <T : Any> setUriPattern(
            basePath: String,
            route: KClass<T>,
            typeMap: Map<KType, NavType<*>> = emptyMap(),
        ): Builder {
            this.uriPattern = route.serializer().generateRoutePattern(typeMap, basePath)
            return this
        }

        /**
         * Set the action for the [NavDeepLink].
         *
         * @param action the intent action for the NavDeepLink
         * @return This builder.
         * @throws IllegalArgumentException if the action is empty.
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
             * Creates a [NavDeepLink.Builder] with a set uri pattern.
             *
             * Arguments extracted from destination [T] will be automatically appended to the base
             * path provided in [basePath]
             *
             * @param T The destination's route from KClass
             * @param basePath The base uri path to append arguments onto
             * @param typeMap map of destination arguments' kotlin type [KType] to its respective
             *   custom [NavType]. May be empty if [T] does not use custom NavTypes.
             * @return a [Builder] instance
             */
            @JvmStatic
            inline fun <reified T : Any> fromUriPattern(
                basePath: String,
                typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
            ): Builder {
                val builder = Builder()
                builder.setUriPattern(basePath, T::class, typeMap)
                return builder
            }

            /**
             * Creates a [NavDeepLink.Builder] with a set action.
             *
             * @param action the intent action for the NavDeepLink
             * @return a [Builder] instance
             * @throws IllegalArgumentException if the action is empty.
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
            // example of singleQueryParamValueOnly "www.example.com?{arg}"
            val queryParam =
                queryParams.firstOrNull() ?: paramName.apply { isSingleQueryParamValueOnly = true }
            val matcher = FILL_IN_PATTERN.matcher(queryParam)
            var appendPos = 0
            val param = ParamQuery()
            // Build the regex for each query param
            while (matcher.find()) {
                // matcher.group(1) as String = "tab" (the extracted param arg from {tab})
                param.addArgumentName(matcher.group(1) as String)
                argRegex.append(Pattern.quote(queryParam.substring(appendPos, matcher.start())))
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
        val splitMimeType = MimeType(mimeType)

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
