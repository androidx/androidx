/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 */
@Navigator.Name("wear-navigator")
public class WearNavigator : Navigator<WearNavigator.Destination>() {
    /**
     * Get the map of transitions currently in progress from the [state].
     */
    internal val transitionsInProgress get() = state.transitionsInProgress

    /**
     * Get the back stack from the [state].
     */
    internal val backStack get() = state.backStack

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.push(entry)
        }
    }

    override fun createDestination() = Destination(this) {}

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }

    /**
     * Callback that removes the given [NavBackStackEntry] from the [map of the transitions in
     * progress][transitionsInProgress]. This should be called in conjunction with [navigate] and
     * [popBackStack] as those call are responsible for adding entries to [transitionsInProgress].
     *
     * Failing to call this method could result in entries being prevented from reaching their
     * final Lifecycle.State.
     */
    internal fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    /**
     * NavDestination specific to [WearNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: WearNavigator,
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator)

    internal companion object {
        internal const val NAME = "wear-navigator"
    }
}
