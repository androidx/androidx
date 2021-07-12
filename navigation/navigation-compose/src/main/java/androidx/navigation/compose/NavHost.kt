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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.get

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * @sample androidx.navigation.compose.samples.NavScaffold
 *
 * @param navController the navController for this host
 * @param startDestination the route for the start destination
 * @param modifier The modifier to be applied to the layout.
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier
    )
}

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The graph passed into this method is [remember]ed. This means that for this NavHost, the graph
 * cannot be changed.
 *
 * @param navController the navController for this host
 * @param graph the graph for this host
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "NavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onBackPressedDispatcher = onBackPressedDispatcherOwner?.onBackPressedDispatcher
    val rememberedGraph = remember { graph }

    // on successful recompose we setup the navController with proper inputs
    // after the first time, this will only happen again if one of the inputs changes
    DisposableEffect(navController, lifecycleOwner, viewModelStoreOwner, onBackPressedDispatcher) {
        navController.setLifecycleOwner(lifecycleOwner)
        navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
        if (onBackPressedDispatcher != null) {
            navController.setOnBackPressedDispatcher(onBackPressedDispatcher)
        }

        onDispose { }
    }

    DisposableEffect(rememberedGraph) {
        navController.graph = rememberedGraph
        onDispose { }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    // Find the ComposeNavigator, returning early if it isn't found
    // (such as is the case when using TestNavHostController)
    val composeNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        ComposeNavigator.NAME
    ) as? ComposeNavigator ?: return
    val backStack by composeNavigator.backStack.collectAsState()
    val transitionsInProgress by composeNavigator.transitionsInProgress.collectAsState()

    val backStackEntry = transitionsInProgress.keys.lastOrNull { entry ->
        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    } ?: backStack.lastOrNull { entry ->
        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    SideEffect {
        // When we place the first entry on the backstack we won't get a call on onDispose since
        // the Crossfade will remain in the compose hierarchy. We need to move that entry to
        // RESUMED separately.
        if (backStack.size == 1 && transitionsInProgress.size == 1) {
            transitionsInProgress.forEach { entry ->
                entry.value.onTransitionComplete()
            }
        }
    }

    if (backStackEntry != null) {
        // while in the scope of the composable, we provide the navBackStackEntry as the
        // ViewModelStoreOwner and LifecycleOwner
        Crossfade(backStackEntry, modifier) { currentEntry ->
            currentEntry.LocalOwnersProvider(saveableStateHolder) {
                (currentEntry.destination as ComposeNavigator.Destination).content(currentEntry)
            }
            DisposableEffect(currentEntry) {
                onDispose {
                    transitionsInProgress.forEach { entry ->
                        entry.value.onTransitionComplete()
                    }
                }
            }
        }
    }

    val dialogNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        DialogNavigator.NAME
    ) as? DialogNavigator ?: return

    // Show any dialog destinations
    dialogNavigator.Dialogs()
}
