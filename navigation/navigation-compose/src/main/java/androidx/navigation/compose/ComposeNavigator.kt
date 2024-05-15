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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.ComposeNavigator.Destination

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 */
@Navigator.Name("composable")
public class ComposeNavigator : Navigator<Destination>() {

    /**
     * Get the map of transitions currently in progress from the [state].
     */
    internal val transitionsInProgress get() = state.transitionsInProgress

    /**
     * Get the back stack from the [state].
     */
    public val backStack get() = state.backStack

    internal val isPop = mutableStateOf(false)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.pushWithTransition(entry)
        }
        isPop.value = false
    }

    override fun createDestination(): Destination {
        return Destination(this) { }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
        isPop.value = true
    }

    /**
     * Function to prepare the entry for transition.
     *
     * This should be called when the entry needs to move the [Lifecycle.State] in preparation for
     * a transition such as when using predictive back.
     */
    public fun prepareForTransition(entry: NavBackStackEntry) {
        state.prepareForTransition(entry)
    }

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
    public fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    /**
     * NavDestination specific to [ComposeNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: ComposeNavigator,
        internal val content:
            @Composable AnimatedContentScope.(@JvmSuppressWildcards NavBackStackEntry) -> Unit
    ) : NavDestination(navigator) {

        @Deprecated(
            message = "Deprecated in favor of Destination that supports AnimatedContent",
            level = DeprecationLevel.HIDDEN,
        )
        constructor(
            navigator: ComposeNavigator,
            content: @Composable (NavBackStackEntry) -> @JvmSuppressWildcards Unit
        ) : this(navigator, content = { entry -> content(entry) })

        internal var enterTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null

        internal var exitTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null

        internal var popEnterTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null

        internal var popExitTransition: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null

        internal var sizeTransform: (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? = null
    }

    internal companion object {
        internal const val NAME = "composable"
    }
}
