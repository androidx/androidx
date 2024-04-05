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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.Navigator
import androidx.navigation.compose.ComposeNavigator.Destination
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 */
public expect class ComposeNavigator : Navigator<Destination> {

    /**
     * Get the map of transitions currently in progress from the [state].
     */
    internal val transitionsInProgress: StateFlow<Set<NavBackStackEntry>>

    /**
     * Get the back stack from the [state].
     */
    public val backStack: StateFlow<List<NavBackStackEntry>>

    internal val isPop: MutableState<Boolean>

    /**
     * Function to prepare the entry for transition.
     *
     * This should be called when the entry needs to move the [Lifecycle.State] in preparation for
     * a transition such as when using predictive back.
     */
    public fun prepareForTransition(entry: NavBackStackEntry)

    /**
     * Callback to mark a navigation in transition as complete.
     *
     * This should be called in conjunction with [navigate] and [popBackStack] as those
     * calls merely start a transition to the target destination, and requires manually marking
     * the transition as complete by calling this method.
     *
     * Failing to call this method could result in entries being prevented from reaching their
     * final [Lifecycle.State].
     */
    public fun onTransitionComplete(entry: NavBackStackEntry)

    /**
     * NavDestination specific to [ComposeNavigator]
     */
    public class Destination(
        navigator: ComposeNavigator,
        content:
            @Composable AnimatedContentScope.(@JvmSuppressWildcards NavBackStackEntry) -> Unit
    ) : NavDestination {
        internal val content:
            @Composable AnimatedContentScope.(@JvmSuppressWildcards NavBackStackEntry) -> Unit

        internal var enterTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)?

        internal var exitTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)?

        internal var popEnterTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)?

        internal var popExitTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)?

        internal var sizeTransform: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
    }

    internal companion object {
        internal val NAME: String
    }
}
