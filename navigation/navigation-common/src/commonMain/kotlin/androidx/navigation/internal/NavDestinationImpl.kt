/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation.internal

import androidx.navigation.NavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.DeepLinkMatch
import androidx.navigation.NavUri
import androidx.navigation.missingRequiredArguments
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.collections.iterator
import kotlin.collections.set

internal class NavDestinationImpl(val destination: NavDestination) {

    internal var idName: String? = null

    internal val deepLinks = mutableListOf<NavDeepLink>()

    internal var arguments: MutableMap<String, NavArgument> = mutableMapOf()

    internal var id: Int = 0
        set(id) {
            field = id
            idName = null
        }

    internal var route: String? = null
        set(route) {
            if (route == null) {
                id = 0
            } else {
                require(route.isNotBlank()) { "Cannot have an empty route" }

                // make sure the route contains all required arguments
                val tempRoute = createRoute(route)
                val tempDeepLink = NavDeepLink.Builder().setUriPattern(tempRoute).build()
                val missingRequiredArguments =
                    arguments.missingRequiredArguments { key ->
                        key !in tempDeepLink.argumentsNames
                    }
                require(missingRequiredArguments.isEmpty()) {
                    "Cannot set route \"$route\" for destination $destination. " +
                        "Following required arguments are missing: $missingRequiredArguments"
                }

                routeDeepLink = lazy { NavDeepLink.Builder().setUriPattern(tempRoute).build() }
                id = tempRoute.hashCode()
            }
            field = route
        }

    /**
     * This destination's unique route as a NavDeepLink.
     *
     * This deeplink must be kept private and segregated from the explicitly added public deeplinks
     * to ensure that external users cannot deeplink into this destination with this routeDeepLink.
     *
     * This value is reassigned a new lazy value every time [route] is updated to ensure that any
     * initialized lazy value is overwritten with the latest value.
     */
    private var routeDeepLink: Lazy<NavDeepLink>? = null

    internal fun addDeepLink(navDeepLink: NavDeepLink) {
        val missingRequiredArguments =
            arguments.missingRequiredArguments { key -> key !in navDeepLink.argumentsNames }
        require(missingRequiredArguments.isEmpty()) {
            "Deep link ${navDeepLink.uriPattern} can't be used to open destination $destination.\n" +
                "Following required arguments are missing: $missingRequiredArguments"
        }

        deepLinks.add(navDeepLink)
    }

    internal fun matchRoute(route: String): DeepLinkMatch? {
        val routeDeepLink = this.routeDeepLink?.value ?: return null

        val uri = NavUri(createRoute(route))

        // includes matching args for path, query, and fragment
        val matchingArguments = routeDeepLink.getMatchingArguments(uri, arguments) ?: return null
        val matchingPathSegments = routeDeepLink.calculateMatchingPathSegments(uri)
        return DeepLinkMatch(
            destination,
            matchingArguments,
            routeDeepLink.isExactDeepLink,
            matchingPathSegments,
            false,
            -1,
        )
    }

    internal fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        if (deepLinks.isEmpty()) {
            return null
        }
        var bestMatch: DeepLinkMatch? = null
        for (deepLink in deepLinks) {
            val uri = navDeepLinkRequest.uri
            // first filter out invalid matches
            if (!deepLink.matches(navDeepLinkRequest)) continue
            // then look for positive matches
            val matchingArguments =
                // includes matching args for path, query, and fragment
                if (uri != null) deepLink.getMatchingArguments(uri, arguments) else null
            val matchingPathSegments = deepLink.calculateMatchingPathSegments(uri)
            val requestAction = navDeepLinkRequest.action
            val matchingAction = requestAction != null && requestAction == deepLink.action
            val mimeType = navDeepLinkRequest.mimeType
            val mimeTypeMatchLevel =
                if (mimeType != null) deepLink.getMimeTypeMatchRating(mimeType) else -1
            if (
                matchingArguments != null ||
                    ((matchingAction || mimeTypeMatchLevel > -1) &&
                        hasRequiredArguments(deepLink, uri, arguments))
            ) {
                val newMatch =
                    DeepLinkMatch(
                        destination,
                        matchingArguments,
                        deepLink.isExactDeepLink,
                        matchingPathSegments,
                        matchingAction,
                        mimeTypeMatchLevel,
                    )
                if (bestMatch == null || newMatch > bestMatch) {
                    bestMatch = newMatch
                }
            }
        }
        return bestMatch
    }

    private fun hasRequiredArguments(
        deepLink: NavDeepLink,
        uri: NavUri?,
        arguments: Map<String, NavArgument>,
    ): Boolean {
        val matchingArgs = deepLink.getMatchingPathAndQueryArgs(uri, arguments)
        val missingRequiredArguments =
            arguments.missingRequiredArguments { key -> !matchingArgs.read { contains(key) } }
        return missingRequiredArguments.isEmpty()
    }

    internal fun hasRoute(route: String, arguments: SavedState?): Boolean {
        // this matches based on routePattern
        if (this.route == route) return true

        // if no match based on routePattern, this means route contains filled in args or query
        // params
        val matchingDeepLink = matchRoute(route)

        // if no matchingDeepLink or mismatching destination, return false directly
        if (destination != matchingDeepLink?.destination) return false

        // Any args (partially or completely filled in) must exactly match between
        // the route and entry's route.
        return matchingDeepLink.hasMatchingArgs(arguments)
    }

    internal fun addArgument(argumentName: String, argument: NavArgument) {
        arguments[argumentName] = argument
    }

    internal fun removeArgument(argumentName: String) {
        arguments.remove(argumentName)
    }

    internal fun addInDefaultArgs(args: SavedState?): SavedState? {
        if (args == null && arguments.isEmpty()) {
            return null
        }
        val defaultArgs = savedState()
        for ((key, value) in arguments) {
            value.putDefaultValue(key, defaultArgs)
        }
        if (args != null) {
            defaultArgs.write { putAll(args) }
            // Don't verify unknown default values - these default values are only available
            // during deserialization for safe args.
            for ((key, value) in arguments) {
                if (!value.isDefaultValueUnknown) {
                    require(value.verify(key, defaultArgs)) {
                        "Wrong argument type for '$key' in argument savedState. ${value.type.name} " +
                            "expected."
                    }
                }
            }
        }
        return defaultArgs
    }
}
