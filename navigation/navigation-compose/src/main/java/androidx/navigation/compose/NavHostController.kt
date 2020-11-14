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

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.ui.platform.AmbientContext
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import androidx.navigation.navigation

/**
 * The route linked to the current destination.
 */
const val KEY_ROUTE = "android-support-nav:controller:route"

/**
 * Gets the current navigation back stack entry as a [MutableState]. When the given navController
 * changes the back stack due to a [NavController.navigate] or [NavController.popBackStack] this
 * will trigger a recompose and return the top entry on the back stack.
 *
 * @return a mutable state of the current back stack entry
 */
@Composable
public fun NavController.currentBackStackEntryAsState(): State<NavBackStackEntry?> {
    val currentNavBackStackEntry = remember { mutableStateOf(currentBackStackEntry) }
    // setup the onDestinationChangedListener responsible for detecting when the
    // current back stack entry changes
    onCommit(this) {
        val callback = NavController.OnDestinationChangedListener { controller, _, _ ->
            currentNavBackStackEntry.value = controller.currentBackStackEntry
        }
        addOnDestinationChangedListener(callback)
        // remove the navController on dispose (i.e. when the composable is destroyed)
        onDispose {
            removeOnDestinationChangedListener(callback)
        }
    }
    return currentNavBackStackEntry
}

/**
 * Creates a NavHostController that handles the adding of the [ComposeNavigator].
 *
 * @see NavHost
 */
@Composable
public fun rememberNavController(): NavHostController {
    val context = AmbientContext.current
    return rememberSavedInstanceState(saver = NavControllerSaver(context)) {
        createNavController(context)
    }
}

private fun createNavController(context: Context) =
    NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
    }

/**
 * Saver to save and restore the NavController across config change and process death.
 */
private fun NavControllerSaver(
    context: Context
): Saver<NavHostController, *> = Saver<NavHostController, Bundle>(
    save = { it.saveState() },
    restore = { createNavController(context).apply { restoreState(it) } }
)

/**
 * Navigate to a route in the current NavGraph.
 *
 * @param route route for the destination
 * @param builder DSL for constructing a new [NavOptions]
 */
public fun NavController.navigate(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(
        NavDeepLinkRequest.Builder.fromUri(createRoute(route).toUri()).build(),
        navOptions(builder)
    )
}

/**
 * Construct a new [NavGraph]
 *
 * @param route the route for the graph
 * @param startDestination the route for the start destination
 * @param builder the builder used to construct the graph
 */
fun NavController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(
    if (route != null) createRoute(route).hashCode() else 0,
    createRoute(startDestination).hashCode(),
    builder
)

internal fun createRoute(route: String) = "android-app://androidx.navigation.compose/$route"
