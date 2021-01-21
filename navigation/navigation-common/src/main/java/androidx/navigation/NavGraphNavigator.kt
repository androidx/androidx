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
import java.lang.IllegalArgumentException

/**
 * A Navigator built specifically for [NavGraph] elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 */
@Navigator.Name("navigation")
public open class NavGraphNavigator
/**
 * Construct a Navigator capable of routing incoming navigation requests to the proper
 * destination within a [NavGraph].
 *
 * @param navigatorProvider NavigatorProvider used to retrieve the correct
 * [Navigator] to navigate to the start destination
 */
constructor(private val navigatorProvider: NavigatorProvider) : Navigator<NavGraph>() {
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
        destination: NavGraph,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        val startId = destination.startDestination
        check(startId != 0) {
            ("no start destination defined via app:startDestination for ${destination.displayName}")
        }
        val startDestination = destination.findNode(startId, false)
        requireNotNull(startDestination) {
            val dest = destination.startDestDisplayName
            throw IllegalArgumentException(
                "navigation destination $dest is not a direct child of this NavGraph"
            )
        }
        val navigator = navigatorProvider.getNavigator<Navigator<NavDestination>>(
            startDestination.navigatorName
        )
        return navigator.navigate(
            startDestination,
            startDestination.addInDefaultArgs(args),
            navOptions,
            navigatorExtras
        )
    }

    override fun popBackStack(): Boolean {
        return true
    }
}