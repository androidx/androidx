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

import kotlinx.coroutines.flow.StateFlow

public actual open class NavGraphNavigator actual constructor(
    private val navigatorProvider: NavigatorProvider
) : Navigator<NavGraph>(Companion.name) {

    /**
     * Gets the backstack of [NavBackStackEntry] associated with this Navigator
     */
    public actual val backStack: StateFlow<List<NavBackStackEntry>>
        get() = state.backStack

    /**
     * Creates a new [NavGraph] associated with this navigator.
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
        val args = entry.arguments
        val startRoute = destination.startDestinationRoute
        check(startRoute != null) {
            ("no start destination defined via app:startDestination for ${destination.displayName}")
        }
        val startDestination = destination.findNode(startRoute, false)
        requireNotNull(startDestination) {
            throw IllegalArgumentException(
                "navigation destination $startRoute is not a direct child of this NavGraph"
            )
        }

        val navigator = navigatorProvider.getNavigator<Navigator<NavDestination>>(
            startDestination.navigatorName
        )
        val startDestinationEntry = state.createBackStackEntry(
            startDestination,
            // could contain default args, restored args, args passed during setGraph,
            // and args from route
            startDestination.addInDefaultArgs(args)
        )
        navigator.navigate(listOf(startDestinationEntry), navOptions, navigatorExtras)
    }

    internal companion object {
        internal const val name = "navigation"
    }
}
