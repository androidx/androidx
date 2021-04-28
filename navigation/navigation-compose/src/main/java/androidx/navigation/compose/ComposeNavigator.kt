/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import androidx.navigation.compose.ComposeNavigator.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 */
@Navigator.Name("composable")
public class ComposeNavigator : Navigator<Destination>() {
    private var attached by mutableStateOf(false)

    /**
     * Get the back stack from the [state]. NavHost will compose at least
     * once (due to the use of [androidx.compose.runtime.DisposableEffect]) before
     * the Navigator is attached, so we specifically return an empty flow if we
     * aren't attached yet.
     */
    internal val backStack: StateFlow<List<NavBackStackEntry>> get() = if (attached) {
        state.backStack
    } else {
        MutableStateFlow(emptyList())
    }

    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        attached = true
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.add(entry)
        }
    }

    override fun createDestination(): Destination {
        return Destination(this) { }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }

    /**
     * NavDestination specific to [ComposeNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: ComposeNavigator,
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator)

    internal companion object {
        internal const val NAME = "composable"
    }
}
