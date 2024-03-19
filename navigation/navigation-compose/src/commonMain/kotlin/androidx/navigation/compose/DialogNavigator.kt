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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.DialogNavigator.Destination
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator that navigates through [Composable]s that will be hosted within a
 * [Dialog]. Every destination using this Navigator must  set a valid [Composable] by setting it
 * directly on an instantiated [Destination] or calling [dialog].
 */
public expect class DialogNavigator : Navigator<Destination> {

    /**
     * Get the back stack from the [state].
     */
    internal val backStack: StateFlow<List<NavBackStackEntry>>

    /**
     * Get the transitioning dialogs from the [state].
     */
    internal val transitionInProgress: StateFlow<Set<NavBackStackEntry>>

    /**
     * Dismiss the dialog destination associated with the given [backStackEntry].
     */
    internal fun dismiss(backStackEntry: NavBackStackEntry)

    internal fun onTransitionComplete(entry: NavBackStackEntry)

    /**
     * NavDestination specific to [DialogNavigator]
     */
    public class Destination(
        navigator: DialogNavigator,
        dialogProperties: DialogProperties = DialogProperties(),
        content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination, FloatingWindow {
        internal val dialogProperties: DialogProperties
        internal val content: @Composable (NavBackStackEntry) -> Unit
    }

    internal companion object {
        internal val NAME: String
    }
}
