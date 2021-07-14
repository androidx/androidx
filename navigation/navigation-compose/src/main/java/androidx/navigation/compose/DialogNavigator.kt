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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import androidx.navigation.compose.DialogNavigator.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator that navigates through [Composable]s that will be hosted within a
 * [Dialog]. Every destination using this Navigator must  set a valid [Composable] by setting it
 * directly on an instantiated [Destination] or calling [dialog].
 */
@Navigator.Name("dialog")
public class DialogNavigator : Navigator<Destination>() {
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

    /**
     * Dismiss the dialog destination associated with the given [backStackEntry].
     */
    internal fun dismiss(backStackEntry: NavBackStackEntry) {
        check(attached) {
            "The DialogNavigator must be attached to a NavController to call dismiss"
        }
        state.pop(backStackEntry, false)
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
            state.push(entry)
        }
    }

    override fun createDestination(): Destination {
        return Destination(this) { }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }

    /**
     * NavDestination specific to [DialogNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: DialogNavigator,
        internal val dialogProperties: DialogProperties = DialogProperties(),
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator), FloatingWindow

    internal companion object {
        internal const val NAME = "dialog"
    }
}
