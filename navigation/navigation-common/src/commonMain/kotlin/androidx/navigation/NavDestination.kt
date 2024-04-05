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

/**
 * NavDestination represents one node within an overall navigation graph.
 *
 * Each destination is associated with a [Navigator] which knows how to navigate to this
 * particular destination.
 *
 * Each destination has a set of [arguments][arguments] that will
 * be applied when [navigating][NavController.navigate] to that destination.
 * Any default values for those arguments can be overridden at the time of navigation.
 *
 * NavDestinations should be created via [Navigator.createDestination].
 */
public expect open class NavDestination(
    navigatorName: String
) {
    /**
     * The name associated with this destination's [Navigator].
     */
    public val navigatorName: String

    /**
     * Gets the [NavGraph] that contains this destination. This will be set when a
     * destination is added to a NavGraph via [NavGraph.addDestination].
     */
    public var parent: NavGraph?
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public set

    /**
     * The descriptive label of this destination.
     */
    public var label: CharSequence?

    /**
     * The arguments supported by this destination. Returns a read-only map of argument names
     * to [NavArgument] objects that can be used to check the type, default value
     * and nullability of the argument.
     *
     * To add and remove arguments for this NavDestination
     * use [addArgument] and [removeArgument].
     * @return Read-only map of argument names to arguments.
     */
    public val arguments: Map<String, NavArgument>

    /**
     * NavDestinations should be created via [Navigator.createDestination].
     */
    public constructor(navigator: Navigator<out NavDestination>)

    /**
     * The destination's unique route.
     *
     * @return this destination's route, or null if no route is set
     *
     * @throws IllegalArgumentException is the given route is empty
     */
    public var route: String?

    public open val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get

    /**
     * Add a deep link to this destination. Matching Uris sent to
     * [NavController.handleDeepLink] or [NavController.navigate] will
     * trigger navigating to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * - Uris without a scheme are assumed as http and https. For example,
     * `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     * - Placeholders in the form of `{placeholder_name}` matches 1 or more
     * characters. The parsed value of the placeholder will be available in the arguments
     * [Bundle] with a key of the same name. For example,
     * `http://www.example.com/users/{id}` will match
     * `http://www.example.com/users/4`.
     * - The `.*` wildcard can be used to match 0 or more characters.
     *
     * These Uris can be declared in your navigation XML files by adding one or more
     * `<deepLink app:uri="uriPattern" />` elements as
     * a child to your destination.
     *
     * Deep links added in navigation XML files will automatically replace instances of
     * `${applicationId}` with the applicationId of your app.
     * Programmatically added deep links should use [Context.getPackageName] directly
     * when constructing the uriPattern.
     * @param uriPattern The uri pattern to add as a deep link
     * @see NavController.handleDeepLink
     * @see NavController.navigate
     * @see NavDestination.addDeepLink
     */
    public fun addDeepLink(uriPattern: String)

    /**
     * Add a deep link to this destination. Uris that match the given [NavDeepLink] uri
     * sent to [NavController.handleDeepLink] or
     * [NavController.navigate] will trigger navigating to this
     * destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * Uris without a scheme are assumed as http and https. For example,
     * `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     * Placeholders in the form of `{placeholder_name}` matches 1 or more
     * characters. The String value of the placeholder will be available in the arguments
     * [Bundle] with a key of the same name. For example,
     * `http://www.example.com/users/{id}` will match
     * `http://www.example.com/users/4`.
     * The `.*` wildcard can be used to match 0 or more characters.
     *
     * These Uris can be declared in your navigation XML files by adding one or more
     * `<deepLink app:uri="uriPattern" />` elements as
     * a child to your destination.
     *
     * Custom actions and mimetypes are also supported by [NavDeepLink] and can be declared
     * in your navigation XML files by adding
     * `<app:action="android.intent.action.SOME_ACTION" />` or
     * `<app:mimetype="type/subtype" />` as part of your deepLink declaration.
     *
     * Deep link Uris, actions, and mimetypes added in navigation XML files will automatically
     * replace instances of `${applicationId}` with the applicationId of your app.
     * Programmatically added deep links should use [Context.getPackageName] directly
     * when constructing the uriPattern.
     *
     * When matching deep links for calls to [NavController.handleDeepLink] or
     * [NavController.navigate] the order of precedence is as follows:
     * the deep link with the most matching arguments will be chosen, followed by the deep link
     * with a matching action, followed by the best matching mimeType (e.i. when matching
     * mimeType image/jpg: image/ * > *\/jpg > *\/ *).
     * @param navDeepLink The NavDeepLink to add as a deep link
     * @see NavController.handleDeepLink
     * @see NavController.navigate
     */
    public fun addDeepLink(navDeepLink: NavDeepLink)

    /**
     * Returns true if the [NavBackStackEntry.destination] contains the route.
     *
     * The route may be either:
     * 1. an exact route without arguments
     * 2. a route containing arguments where no arguments are filled in
     * 3. a route containing arguments where some or all arguments are filled in
     * 4. a partial route
     *
     * In the case of 3., it will only match if the entry arguments
     * match exactly with the arguments that were filled in inside the route.
     *
     * @param [route] The route to match with the route of this destination
     *
     * @param [arguments] The [NavBackStackEntry.arguments] that was used to navigate
     * to this destination
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun hasRoute(route: String, arguments: Bundle?): Boolean

    /**
     * Sets an argument type for an argument name
     *
     * @param argumentName argument object to associate with destination
     * @param argument argument object to associate with destination
     */
    public fun addArgument(argumentName: String, argument: NavArgument)

    /**
     * Unsets the argument type for an argument name.
     *
     * @param argumentName argument to remove
     */
    public fun removeArgument(argumentName: String)

    /**
     * Combines the default arguments for this destination with the arguments provided
     * to construct the final set of arguments that should be used to navigate
     * to this destination.
     */
    @Suppress("NullableCollection") // Needed for nullable bundle
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun addInDefaultArgs(args: Bundle?): Bundle?

    public companion object {
        /**
         * Provides a sequence of the NavDestination's hierarchy. The hierarchy starts with this
         * destination itself and is then followed by this destination's [NavDestination.parent], then that
         * graph's parent, and up the hierarchy until you've reached the root navigation graph.
         */
        @JvmStatic
        public val NavDestination.hierarchy: Sequence<NavDestination>
    }
}
