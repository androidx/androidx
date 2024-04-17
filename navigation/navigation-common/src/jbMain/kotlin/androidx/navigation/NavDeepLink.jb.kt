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
import kotlin.jvm.JvmStatic

public actual class NavDeepLink internal actual constructor(
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

    public actual var isExactDeepLink: Boolean = false
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get
        internal set

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

        // TODO: Extract arguments from query and fragment part

        // Check that all required arguments are present in bundle
        val missingRequiredArguments = arguments.missingRequiredArguments { argName ->
            !bundle.containsKey(argName)
        }
        if (missingRequiredArguments.isNotEmpty()) return null

        return bundle
    }

    private fun getMatchingPathArguments(
        result: MatchResult,
        bundle: Bundle,
        arguments: Map<String, NavArgument?>
    ): Boolean {
        this.pathArgs.mapIndexed { index, argumentName ->
            // TODO: Decode Uri
            val value = result.groups[index + 1]!!.value
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
        Regex("(\\?|\\#|$)").find(uriPattern)?.let {
            buildRegex(uriPattern.substring(0, it.range.first), pathArgs, uriRegex)
            isExactDeepLink = !uriRegex.contains(".*") && !uriRegex.contains("([^/]+?)")
            // Match either the end of string if all params are optional or match the
            // question mark (or pound symbol) and 0 or more characters after it
            uriRegex.append("($|(\\?(.)*)|(\\#(.)*))")
        }
        // we need to specifically escape any .* instances to ensure
        // they are still treated as wildcards in our final regex
        pathRegex = uriRegex.toString().replace(".*", "\\E.*\\Q")
    }

    init {
        parsePath()
    }
}
