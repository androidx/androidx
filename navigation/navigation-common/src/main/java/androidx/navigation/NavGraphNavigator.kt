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

import android.os.Bundle
import kotlinx.coroutines.flow.StateFlow

/**
 * A Navigator built specifically for [NavGraph] elements. Handles navigating to the correct
 * destination when the NavGraph is the target of navigation actions.
 *
 * Construct a Navigator capable of routing incoming navigation requests to the proper destination
 * within a [NavGraph].
 *
 * @param navigatorProvider NavigatorProvider used to retrieve the correct [Navigator] to navigate
 *   to the start destination
 */
@Navigator.Name("navigation")
public open class NavGraphNavigator(private val navigatorProvider: NavigatorProvider) :
    Navigator<NavGraph>() {

    /** Gets the backstack of [NavBackStackEntry] associated with this Navigator */
    public val backStack: StateFlow<List<NavBackStackEntry>>
        get() = state.backStack

    /**
     * Creates a new [NavGraph] associated with this navigator.
     *
     * @return The created [NavGraph].
     */
    override fun createDestination(): NavGraph {
        return NavGraph(this)
    }

    /**
     * @throws IllegalArgumentException if given destination is not a child of the current navgraph
     */
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        for (entry in entries) {
            navigate(entry, navOptions, navigatorExtras)
        }
    }

    private fun navigate(
        entry: NavBackStackEntry,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        val destination = entry.destination as NavGraph
        // contains restored args or args passed explicitly as startDestinationArgs
        var args = entry.arguments
        val startId = destination.startDestinationId
        val startRoute = destination.startDestinationRoute
        check(startId != 0 || startRoute != null) {
            ("no start destination defined via app:startDestination for ${destination.displayName}")
        }
        val startDestination =
            if (startRoute != null) {
                destination.findNode(startRoute, false)
            } else {
                destination.nodes[startId]
            }
        requireNotNull(startDestination) {
            val dest = destination.startDestDisplayName
            throw IllegalArgumentException(
                "navigation destination $dest is not a direct child of this NavGraph"
            )
        }
        if (startRoute != null) {
            // If startRoute contains only placeholders, we fallback to default arg values.
            // This is to maintain existing behavior of using default value for startDestination
            // while also adding support for args declared in startRoute.
            if (startRoute != startDestination.route) {
                val matchingArgs = startDestination.matchRoute(startRoute)?.matchingArgs
                if (matchingArgs != null && !matchingArgs.isEmpty) {
                    val bundle = Bundle()
                    // we need to add args from startRoute, but it should not override existing args
                    bundle.putAll(matchingArgs)
                    args?.let { bundle.putAll(args) }
                    args = bundle
                }
            }
            // by this point, the bundle should contain all arguments that don't have
            // default values (regardless of whether the actual default value is known or not).
            if (startDestination.arguments.isNotEmpty()) {
                val missingRequiredArgs =
                    startDestination.arguments.missingRequiredArguments { key ->
                        if (args == null) true else !args.containsKey(key)
                    }
                require(missingRequiredArgs.isEmpty()) {
                    "Cannot navigate to startDestination $startDestination. " +
                        "Missing required arguments [$missingRequiredArgs]"
                }
            }
        }

        val navigator =
            navigatorProvider.getNavigator<Navigator<NavDestination>>(
                startDestination.navigatorName
            )
        val startDestinationEntry =
            state.createBackStackEntry(
                startDestination,
                // could contain default args, restored args, args passed during setGraph,
                // and args from route
                startDestination.addInDefaultArgs(args)
            )
        navigator.navigate(listOf(startDestinationEntry), navOptions, navigatorExtras)
    }
}
