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

import androidx.annotation.RestrictTo
import androidx.navigation.serialization.generateRoutePattern
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSuppressWildcards
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
    public val mimeType: String?,
) {
    // path
    private val pathArgs = mutableListOf<String>()
    private var pathRegex: String? = null
    private val pathPattern by lazy { pathRegex?.let { Regex(it, RegexOption.IGNORE_CASE) } }

    // query
    private val isParameterizedQuery by lazy {
        uriPattern != null && QUERY_PATTERN.matches(uriPattern)
    }
    private val queryArgsMap by lazy(LazyThreadSafetyMode.NONE) { parseQuery() }
    private var isSingleQueryParamValueOnly = false

    // fragment
    private val fragArgsAndRegex: Pair<MutableList<String>, String>? by
        lazy(LazyThreadSafetyMode.NONE) { parseFragment() }
    private val fragArgs by
        lazy(LazyThreadSafetyMode.NONE) { fragArgsAndRegex?.first ?: mutableListOf() }
    private val fragRegex by lazy(LazyThreadSafetyMode.NONE) { fragArgsAndRegex?.second }
    private val fragPattern by lazy { fragRegex?.let { Regex(it, RegexOption.IGNORE_CASE) } }

    // mime
    private var mimeTypeRegex: String? = null
    private val mimeTypePattern by lazy { mimeTypeRegex?.let { Regex(it) } }

    /** Arguments present in the deep link, including both path and query arguments. */
    internal val argumentsNames: List<String>
        get() = pathArgs + queryArgsMap.values.flatMap { it.arguments } + fragArgs

    public var isExactDeepLink: Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get
        internal set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(uri: String) : this(uri, null, null)

    private fun buildRegex(uri: String, args: MutableList<String>, uriRegex: StringBuilder) {
        var result = FILL_IN_PATTERN.find(uri)
        var appendPos = 0
        while (result != null) {
            val argName = result.groups[1]!!.value
            args.add(argName)
            // Use Regex.escape() to treat the input string as a literal
            if (result.range.first > appendPos) {
                uriRegex.append(Regex.escape(uri.substring(appendPos, result.range.first)))
            }
            uriRegex.append(PATH_REGEX.pattern)
            appendPos = result.range.last + 1
            result = result.next()
        }
        if (appendPos < uri.length) {
            // Use Regex.escape() to treat the input string as a literal
            uriRegex.append(Regex.escape(uri.substring(appendPos)))
        }
    }

    internal fun matches(uri: NavUri): Boolean {
        return matches(NavDeepLinkRequest(uri, null, null))
    }

    /**
     * Checks for matches on uri, action, and mimType between a deepLink and a deepLinkRequest.
     *
     * Returns false if either
     * 1. the deepLink's field is non-null while requested link's field is null
     * 2. both fields are non-null but don't match
     *
     * Returns true otherwise (including when both fields are null).
     */
    internal fun matches(deepLinkRequest: NavDeepLinkRequest): Boolean =
        matchUri(deepLinkRequest.uri) &&
            matchAction(deepLinkRequest.action) &&
            matchMimeType(deepLinkRequest.mimeType)

    private fun matchUri(uri: NavUri?): Boolean =
        when {
            pathPattern == null -> true
            uri == null -> false
            else -> pathPattern!!.matches(uri.toString())
        }

    private fun matchAction(action: String?): Boolean =
        when {
            this.action == null -> true
            action == null -> false
            else -> this.action == action
        }

    private fun matchMimeType(mimeType: String?): Boolean =
        when {
            this.mimeType == null -> true
            mimeType == null -> false
            else -> mimeTypePattern!!.matches(mimeType)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMimeTypeMatchRating(mimeType: String): Int {
        return if (this.mimeType == null || !mimeTypePattern!!.matches(mimeType)) {
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
     * May return empty SavedState if any of the following:
     * 1. deeplink has no arguments
     * 2. deeplink contains arguments with unknown default values (i.e. deeplink from safe args with
     *    unknown default values)
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMatchingArguments(
        deepLink: NavUri,
        arguments: Map<String, NavArgument?>,
    ): SavedState? {
        // first check overall uri pattern for quick return if general pattern does not match
        val result = pathPattern?.matchEntire(deepLink.toString()) ?: return null

        // get matching path and query arguments and store in bundle
        val savedState = savedState()
        if (!getMatchingPathArguments(result, savedState, arguments)) return null
        if (isParameterizedQuery && !getMatchingQueryArguments(deepLink, savedState, arguments)) {
            return null
        }
        // no match on optional fragment should not prevent a link from matching otherwise
        getMatchingUriFragment(deepLink.getFragment(), savedState, arguments)

        // Check that all required arguments are present in bundle
        val missingRequiredArguments =
            arguments.missingRequiredArguments { argName -> !savedState.read { contains(argName) } }
        if (missingRequiredArguments.isNotEmpty()) return null

        return savedState
    }

    /**
     * Returns a SavedState containing matching path and query arguments with the requested uri. It
     * returns empty SavedState if this Deeplink's path pattern does not match with the uri.
     */
    internal fun getMatchingPathAndQueryArgs(
        deepLink: NavUri?,
        arguments: Map<String, NavArgument?>,
    ): SavedState {
        val savedState = savedState()
        if (deepLink == null) return savedState
        val result = pathPattern?.matchEntire(deepLink.toString()) ?: return savedState

        getMatchingPathArguments(result, savedState, arguments)
        if (isParameterizedQuery) getMatchingQueryArguments(deepLink, savedState, arguments)
        return savedState
    }

    private fun getMatchingUriFragment(
        fragment: String?,
        savedState: SavedState,
        arguments: Map<String, NavArgument?>,
    ) {
        // Base condition of a matching fragment is a complete match on regex pattern. If a
        // required fragment arg is present while regex does not match, this will be caught later
        // on as a non-match when we check for presence of required args in the bundle.
        val result = fragPattern?.matchEntire(fragment.toString()) ?: return

        this.fragArgs.mapIndexed { index, argumentName ->
            val value = result.groups[index + 1]?.value?.let { NavUriUtils.decode(it) }.orEmpty()
            val argument = arguments[argumentName]
            try {
                parseArgument(savedState, argumentName, value, argument)
            } catch (e: IllegalArgumentException) {
                // parse failed, quick return
                return
            }
        }
    }

    private fun getMatchingPathArguments(
        result: MatchResult,
        savedState: SavedState,
        arguments: Map<String, NavArgument?>,
    ): Boolean {
        this.pathArgs.mapIndexed { index, argumentName ->
            val value = result.groups[index + 1]?.value?.let { NavUriUtils.decode(it) }.orEmpty()
            val argument = arguments[argumentName]
            try {
                parseArgument(savedState, argumentName, value, argument)
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
        deepLink: NavUri,
        savedState: SavedState,
        arguments: Map<String, NavArgument?>,
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
                val argValue = deepLink.getQuery()
                if (argValue != null && argValue != deepLink.toString()) {
                    inputParams = listOf(argValue)
                }
            }
            val parseSuccess = parseInputParams(inputParams, storedParam, savedState, arguments)
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
        savedState: SavedState,
        arguments: Map<String, NavArgument?>,
    ): Boolean {
        val tempSavedState = savedState()
        // try to start off by adding an empty bundle if there is no default value.
        storedParam.arguments.forEach { argName ->
            val argument = arguments[argName]
            val navType = argument?.type
            // for CollectionNavType, only fallback to empty collection if there isn't a default
            // value
            if (navType is CollectionNavType && !argument.isDefaultValuePresent) {
                navType.put(tempSavedState, argName, navType.emptyCollection())
            }
        }
        inputParams.forEach { inputParam ->
            val argMatchResult = storedParam.paramRegex?.let { Regex(it).matchEntire(inputParam) }
            // check if this particular arg value matches the expected regex.
            // for example, if the query was list of Int like "...?intId=1&intId=2&intId=abc",
            // this would return false when matching "abc".
            if (argMatchResult == null) {
                return false
            }
            // iterate over each argName under the same queryParameterName
            storedParam.arguments.mapIndexed { index, argName ->
                // make sure we get the correct value for this particular argName
                // i.e. if route is "...?myArg={firstName}_{lastName}"
                // and the inputParam is "John_Doe"
                // we need to map values to argName like this:
                // [firstName to "John", lastName to "Doe"]

                val value = argMatchResult.groups[index + 1]?.value.orEmpty()
                val argument = arguments[argName]

                try {
                    if (!tempSavedState.read { contains(argName) }) {
                        // Passing in a value the exact same as the placeholder will be treated the
                        // as if no value was passed (unless value is based on String),
                        // being replaced if it is optional or throwing an error if it is required.
                        parseArgument(tempSavedState, argName, value, argument)
                    } else {
                        parseArgumentForRepeatedParam(tempSavedState, argName, value, argument)
                    }
                } catch (e: IllegalArgumentException) {
                    // Failed to parse means that at least one of the arguments that
                    // were supposed to fill in the query parameter was not valid.
                    // We will need to handle it here. Values that are not handled
                    // here will just be excluded from the argument bundle.
                }
            }
        }
        savedState.write { putAll(tempSavedState) }
        // parse success
        return true
    }

    internal fun calculateMatchingPathSegments(requestedLink: NavUri?): Int {
        if (requestedLink == null || uriPattern == null) return 0

        val requestedPathSegments = requestedLink.getPathSegments()
        val uriPathSegments = NavUriUtils.parse(uriPattern).getPathSegments()

        val matches = requestedPathSegments.intersect(uriPathSegments)
        return matches.size
    }

    /**
     * Parses [value] based on the NavArgument's NavType and stores the result inside the
     * [savedState]. Throws if parse fails.
     */
    private fun parseArgument(
        savedState: SavedState,
        name: String,
        value: String,
        argument: NavArgument?,
    ) {
        if (argument != null) {
            val type = argument.type
            type.parseAndPut(savedState, name, value)
        } else {
            savedState.write { putString(name, value) }
        }
    }

    /**
     * Parses subsequent arg values under the same queryParameterName
     *
     * For example with route "...?myArg=one&myArg=two&myArg=three", [savedState] is expected to
     * already contain bundleOf([name] to "one"), and this function will parse & put values "two"
     * and "three" into the SavedState under the same [name].
     */
    private fun parseArgumentForRepeatedParam(
        savedState: SavedState,
        name: String,
        value: String?,
        argument: NavArgument?,
    ): Boolean {
        if (!savedState.read { contains(name) }) {
            return true
        }
        if (argument != null) {
            val type = argument.type
            val previousValue = type[savedState, name]
            type.parseAndPut(savedState, name, value, previousValue)
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
        ): Builder = setUriPattern(T::class, basePath, typeMap)

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
         * @param route The destination's route from KClass
         * @param basePath The base uri path to append arguments onto
         * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
         *   [NavType]. May be empty if [T] does not use custom NavTypes.
         * @return This builder.
         */
        @OptIn(InternalSerializationApi::class)
        @JvmOverloads
        public fun <T : Any> setUriPattern(
            route: KClass<T>,
            basePath: String,
            typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
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
                builder.setUriPattern(T::class, basePath, typeMap)
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
        private val SCHEME_PATTERN = Regex("^[a-zA-Z]+[+\\w\\-.]*:")
        private val FILL_IN_PATTERN = Regex("\\{(.+?)\\}")
        private val SCHEME_REGEX = Regex("http[s]?://")
        private val WILDCARD_REGEX = Regex(".*")
        // allows for empty path arguments i.e. empty strings ""
        private val PATH_REGEX = Regex("([^/]*?|)")
        private val QUERY_PATTERN = Regex("^[^?#]+\\?([^#]*).*")

        // TODO: Use [RegexOption.DOT_MATCHES_ALL] once available in common
        //  https://youtrack.jetbrains.com/issue/KT-67574
        private const val ANY_SYMBOLS_IN_THE_TAIL = "([\\s\\S]+?)?"
    }

    private fun parsePath() {
        if (uriPattern == null) return

        val uriRegex = StringBuilder("^")
        // append scheme pattern
        if (!SCHEME_PATTERN.containsMatchIn(uriPattern)) {
            uriRegex.append(SCHEME_REGEX.pattern)
        }
        // extract beginning of uriPattern until it hits either a query(?), a framgment(#), or
        // end of uriPattern
        Regex("(\\?|#|$)").find(uriPattern)?.let {
            buildRegex(uriPattern.substring(0, it.range.first), pathArgs, uriRegex)
            isExactDeepLink = !uriRegex.contains(WILDCARD_REGEX) && !uriRegex.contains(PATH_REGEX)
            // Match either the end of string if all params are optional or match the
            // question mark (or pound symbol) and 0 or more characters after it
            uriRegex.append("($|(\\?(.)*)|(#(.)*))")
        }
        // we need to specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        pathRegex = uriRegex.toString().saveWildcardInRegex()
    }

    private fun parseQuery(): MutableMap<String, ParamQuery> {
        val paramArgMap = mutableMapOf<String, ParamQuery>()
        if (!isParameterizedQuery) return paramArgMap
        val uri = NavUriUtils.parse(uriPattern!!)

        for (paramName in uri.getQueryParameterNames()) {
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
            var result = FILL_IN_PATTERN.find(queryParam)
            var appendPos = 0
            val param = ParamQuery()
            // Build the regex for each query param
            while (result != null) {
                // matcher.group(1) as String = "tab" (the extracted param arg from {tab})
                param.addArgumentName(result.groups[1]!!.value)
                if (result.range.first > appendPos) {
                    val inputLiteral = queryParam.substring(appendPos, result.range.first)
                    argRegex.append(Regex.escape(inputLiteral))
                }
                argRegex.append(ANY_SYMBOLS_IN_THE_TAIL)
                appendPos = result.range.last + 1
                result = result.next()
            }
            if (appendPos < queryParam.length) {
                argRegex.append(Regex.escape(queryParam.substring(appendPos)))
            }
            argRegex.append("$")

            // Save the regex with wildcards unquoted, and add the param to the map with its
            // name as the key
            param.paramRegex = argRegex.toString().saveWildcardInRegex()
            paramArgMap[paramName] = param
        }
        return paramArgMap
    }

    private fun parseFragment(): Pair<MutableList<String>, String>? {
        if (uriPattern == null || NavUriUtils.parse(uriPattern).getFragment() == null) return null

        val fragArgs = mutableListOf<String>()
        val fragment = NavUriUtils.parse(uriPattern).getFragment()
        val fragRegex = StringBuilder()
        buildRegex(fragment!!, fragArgs, fragRegex)
        return fragArgs to fragRegex.toString()
    }

    private fun parseMime() {
        if (mimeType == null) return

        val mimeTypePattern = Regex("^[\\s\\S]+/[\\s\\S]+$")
        require(mimeTypePattern.matches(mimeType)) {
            "The given mimeType $mimeType does not match to required \"type/subtype\" format"
        }

        // get the type and subtype of the mimeType
        val splitMimeType = MimeType(mimeType)

        // the matching pattern can have the exact name or it can be wildcard literal (*)
        val regex = "^(${splitMimeType.type}|[*]+)/(${splitMimeType.subType}|[*]+)$"

        // if the deep link type or subtype is wildcard, allow anything
        mimeTypeRegex = regex.replace("*|[*]", "[\\s\\S]")
    }

    // for more info see #Regexp.escape platform actuals
    private fun String.saveWildcardInRegex(): String =
        // non-js regex escaping
        if (this.contains("\\Q") && this.contains("\\E")) replace(".*", "\\E.*\\Q")
        // js regex escaping
        else if (this.contains("\\.\\*")) replace("\\.\\*", ".*")
        // fallback
        else this

    init {
        parsePath()
        parseMime()
    }
}
