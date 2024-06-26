/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.core.bundle.Bundle
import androidx.navigation.internal.Uri
import androidx.navigation.serialization.generateRoutePattern
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual class NavDeepLink
internal actual constructor(
    public actual val uriPattern: String?,
    public actual val action: String?,
    public actual val mimeType: String?
) {
    // path
    private val pathArgs = mutableListOf<String>()
    private var pathRegex: String? = null
    private val pathPattern by lazy {
        pathRegex?.let { Regex(it, RegexOption.IGNORE_CASE) }
    }

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
    private val fragPattern by lazy {
        fragRegex?.let { Regex(it, RegexOption.IGNORE_CASE) }
    }

    /** Arguments present in the deep link, including both path and query arguments. */
    internal val argumentsNames: List<String>
        get() = pathArgs + queryArgsMap.values.flatMap { it.arguments } + fragArgs

    public actual var isExactDeepLink: Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get
        internal set

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual constructor(uri: String) : this(uri, null, null)

    private fun buildRegex(
        uri: String,
        args: MutableList<String>,
        uriRegex: StringBuilder,
    ) {
        var result = FILL_IN_PATTERN.find(uri)
        var appendPos = 0
        while (result != null) {
            val argName = result.groups[1]!!.value
            args.add(argName)
            // Use Regex.escape() to treat the input string as a literal
            if (result.range.first > appendPos) {
                uriRegex.append(Regex.escape(uri.substring(appendPos, result.range.first)))
            }
            uriRegex.append("([^/]+?)")
            appendPos = result.range.last + 1
            result = result.next()
        }
        if (appendPos < uri.length) {
            // Use Regex.escape() to treat the input string as a literal
            uriRegex.append(Regex.escape(uri.substring(appendPos)))
        }
    }

    internal fun matches(uri: String): Boolean {
        return matchUri(uri)
        // TODO: matchAction + matchMimeType
    }

    private fun matchUri(uri: String?): Boolean {
        // If the null status of both are not the same return false.
        return if (uri == null == (pathPattern != null)) {
            false
        } else uri == null || pathPattern!!.matches(uri)
        // If both are null return true, otherwise see if they match
    }

    /**
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
    public fun getMatchingArguments(
        deepLink: String,
        arguments: Map<String, NavArgument?>
    ): Bundle? {
        // first check overall uri pattern for quick return if general pattern does not match
        val result = pathPattern?.find(deepLink) ?: return null

        // get matching path and query arguments and store in bundle
        val bundle = Bundle()
        if (!getMatchingPathArguments(result, bundle, arguments)) return null
        if (isParameterizedQuery && !getMatchingQueryArguments(deepLink, bundle, arguments)) {
            return null
        }
        // no match on optional fragment should not prevent a link from matching otherwise
        getMatchingUriFragment(deepLink, bundle, arguments)

        // Check that all required arguments are present in bundle
        val missingRequiredArguments =
            arguments.missingRequiredArguments { argName -> !bundle.containsKey(argName) }
        if (missingRequiredArguments.isNotEmpty()) return null

        return bundle
    }

    private fun getMatchingUriFragment(
        deepLink: String,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ) {
        val fragment = getFragment(deepLink) ?: return
        // Base condition of a matching fragment is a complete match on regex pattern. If a
        // required fragment arg is present while regex does not match, this will be caught later
        // on as a non-match when we check for presence of required args in the bundle.
        val result = fragPattern?.find(fragment) ?: return

        this.fragArgs.mapIndexed { index, argumentName ->
            val value = result.groups[index + 1]?.value?.let { Uri.decode(it) } ?: ""
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
        result: MatchResult,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        this.pathArgs.mapIndexed { index, argumentName ->
            val value = result.groups[index + 1]?.value?.let { Uri.decode(it) } ?: ""
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
        deepLink: String,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        val queryParameters = parseQueryParameters(deepLink)
        // key is queryParameterName (argName could be different), value is NavDeepLink.ParamQuery
        queryArgsMap.forEach { entry ->
            val paramName = entry.key
            val storedParam = entry.value

            // a list of the arg values under this queryParameterName
            // collection types (i.e. list, array) would potentially have listOf(arg1, arg2, arg3,
            // etc..)
            // non-collection types would usually have listOf(theArgValue)
            var inputParams = queryParameters[paramName]
            if (isSingleQueryParamValueOnly) {
                // If the deep link contains a single query param with no value,
                // we will treat everything after the '?' as the input parameter
                val argValue = getQuery(deepLink)
                if (argValue != null && argValue != deepLink) {
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
        inputParams: List<String>?,
        storedParam: ParamQuery,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>,
    ): Boolean {
        val tempBundle = Bundle()
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
        inputParams?.forEach { inputParam ->
            val argMatchResult =
                storedParam.paramRegex?.let {
                    // TODO: Use [RegexOption.DOT_MATCHES_ALL] once available in common
                    //  https://youtrack.jetbrains.com/issue/KT-67574
                    Regex(it).find(inputParam)
                }
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
                val value = argMatchResult.groups[index + 1]?.value ?: ""
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

    public actual class Builder {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual constructor()

        private var uriPattern: String? = null
        private var action: String? = null
        private var mimeType: String? = null

        public actual fun setUriPattern(uriPattern: String): Builder {
            this.uriPattern = uriPattern
            return this
        }

        public actual inline fun <reified T : Any> setUriPattern(
            basePath: String,
            typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        ): Builder = setUriPattern(basePath, T::class, typeMap)

        @OptIn(InternalSerializationApi::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // need to be public for reified delegation
        public actual fun <T : Any> setUriPattern(
            basePath: String,
            route: KClass<T>,
            typeMap: Map<KType, NavType<*>>,
        ): Builder {
            this.uriPattern = route.serializer().generateRoutePattern(typeMap, basePath)
            return this
        }

        public actual fun setAction(action: String): Builder {
            // if the action given at runtime is empty we should throw
            require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
            this.action = action
            return this
        }

        public actual fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }

        public actual fun build(): NavDeepLink {
            return NavDeepLink(uriPattern, action, mimeType)
        }

        internal actual companion object {
            @JvmStatic
            actual fun fromUriPattern(uriPattern: String): Builder {
                val builder = Builder()
                builder.setUriPattern(uriPattern)
                return builder
            }

            @JvmStatic
            actual inline fun <reified T : Any> fromUriPattern(
                basePath: String,
                typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
            ): Builder {
                val builder = Builder()
                builder.setUriPattern(basePath, T::class, typeMap)
                return builder
            }

            @JvmStatic
            actual fun fromAction(action: String): Builder {
                // if the action given at runtime is empty we should throw
                require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }

            @JvmStatic
            actual fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }

    private companion object {
        private val SCHEME_PATTERN = Regex("^[a-zA-Z]+[+\\w\\-.]*:")
        private val FILL_IN_PATTERN = Regex("\\{(.+?)\\}")
        private val QUERY_PATTERN = Regex("^[^?#]+\\?([^#]*).*")
        private val FRAGMENT_PATTERN = Regex("#(.+)")
    }

    private fun parsePath() {
        if (uriPattern == null) return

        val uriRegex = StringBuilder("^")
        // append scheme pattern
        if (!SCHEME_PATTERN.containsMatchIn(uriPattern)) {
            uriRegex.append("http[s]?://")
        }
        // extract beginning of uriPattern until it hits either a query(?), a framgment(#), or
        // end of uriPattern
        Regex("(\\?|#|$)").find(uriPattern)?.let {
            buildRegex(uriPattern.substring(0, it.range.first), pathArgs, uriRegex)
            isExactDeepLink = !uriRegex.contains(".*") && !uriRegex.contains("([^/]+?)")
            // Match either the end of string if all params are optional or match the
            // question mark (or pound symbol) and 0 or more characters after it
            uriRegex.append("($|(\\?(.)*)|(#(.)*))")
        }
        // we need to specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        pathRegex = uriRegex.toString().replace(".*", "\\E.*\\Q")
    }

    private fun parseQuery(): MutableMap<String, ParamQuery> {
        val paramArgMap = mutableMapOf<String, ParamQuery>()
        if (uriPattern == null || !isParameterizedQuery) return paramArgMap
        val queryParameters = parseQueryParameters(uriPattern)

        for ((paramName, queryParams) in queryParameters) {
            val argRegex = StringBuilder("^")
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
                // TODO: Revert to "(.+?)?" when [RegexOption.DOT_MATCHES_ALL] will be available
                //  https://youtrack.jetbrains.com/issue/KT-67574
                argRegex.append("([\\s\\S]+?)?")
                appendPos = result.range.last + 1
                result = result.next()
            }
            if (appendPos < queryParam.length) {
                argRegex.append(Regex.escape(queryParam.substring(appendPos)))
            }
            argRegex.append("$")

            // Save the regex with wildcards unquoted, and add the param to the map with its
            // name as the key
            param.paramRegex = argRegex.toString().replace(".*", "\\E.*\\Q")
            paramArgMap[paramName] = param
        }
        return paramArgMap
    }

    private fun getQuery(uri: String) = QUERY_PATTERN.find(uri)?.groups?.get(1)?.value

    private fun parseQueryParameters(uri: String): Map<String, List<String>> {
        val query = getQuery(uri) ?: return emptyMap()
        return query.split("&")
            .map { it.split("=") }
            .groupBy(
                keySelector = { it[0] },
                valueTransform = { split ->
                    split.getOrNull(1)?.let { Uri.decode(it) }
                }
            )
            .mapValues { it.value.filterNotNull() }
    }

    private fun getFragment(uri: String) = FRAGMENT_PATTERN.find(uri)?.groups?.get(1)?.value

    private fun parseFragment(): Pair<MutableList<String>, String>? {
        if (uriPattern == null) return null

        val fragArgs = mutableListOf<String>()
        val fragment = getFragment(uriPattern) ?: return null
        val fragRegex = StringBuilder("^")
        buildRegex(fragment, fragArgs, fragRegex)
        fragRegex.append("$")
        return fragArgs to fragRegex.toString()
    }

    init {
        parsePath()
    }
}
