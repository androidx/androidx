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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.get
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissBoxDefaults
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur,
 * with backwards navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * Content is displayed within a [SwipeToDismissBox], showing the current navigation level.
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in
 * the background.
 *
 * @param navController The navController for this host
 * @param startDestination The route for the start destination
 * @param modifier The modifier to be applied to the layout
 * @param route The route for the graph
 * @param builder The builder used to construct the graph
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) =
    SwipeDismissableNavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier
    )

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur,
 * with backwards navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * Content is displayed within a [SwipeToDismissBox], showing the current navigation level.
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in
 * the background.
 *
 * @param navController [NavHostController] for this host
 * @param graph Graph for this host
 * @param modifier [Modifier] to be applied to the layout.
 *
 * @throws IllegalArgumentException if no WearNavigation.Destination is on the navigation backstack.
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "SwipeDismissableNavHost requires a ViewModelStoreOwner to be provided " +
            "via LocalViewModelStoreOwner"
    }

    // Setup the navController with proper owners
    navController.setLifecycleOwner(lifecycleOwner)
    navController.setViewModelStore(viewModelStoreOwner.viewModelStore)

    // Then set the graph
    navController.graph = graph

    val stateHolder = rememberSaveableStateHolder()

    // Find the WearNavigator, returning early if it isn't found
    // (such as is the case when using TestNavHostController).
    val wearNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        WearNavigator.NAME
    ) as? WearNavigator ?: return
    val backStack by wearNavigator.backStack.collectAsState()
    val transitionsInProgress by wearNavigator.transitionsInProgress.collectAsState()
    val initialContent = remember { mutableStateOf(true) }

    val previous = if (backStack.size <= 1) null else backStack[backStack.lastIndex - 1]
    val current = if (backStack.isNotEmpty()) backStack.last() else throw IllegalArgumentException(
        "No WearNavigation.Destination has been added to the WearNavigator in this NavGraph. " +
            "For convenience, build NavGraph using androidx.wear.compose.navigation.composable."
    )

    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        // This effect operates when the swipe gesture is complete:
        // 1) Resets the screen offset (otherwise, the next destination is draw off-screen)
        // 2) Pops the navigation back stack to return to the previous level
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            state.snapTo(SwipeDismissTarget.Original)
            navController.popBackStack()
        }
    }
    LaunchedEffect(state.isAnimationRunning) {
        // This effect marks the transitions completed when swipe animations finish,
        // so that the navigation backstack entries can go to Lifecycle.State.RESUMED.
        if (state.isAnimationRunning == false) {
                transitionsInProgress.forEach { entry ->
                    wearNavigator.onTransitionComplete(entry)
                }
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = Modifier,
        hasBackground = previous != null,
        backgroundKey = previous?.id ?: SwipeToDismissBoxDefaults.BackgroundKey,
        contentKey = current.id,
        content = { isBackground ->
            BoxedStackEntryContent(if (isBackground) previous else current, stateHolder, modifier)
        }
    )

    if (initialContent.value) {
        // There are no animations for showing the initial content, so mark transitions complete,
        // allowing the navigation backstack entry to go to Lifecycle.State.RESUMED.
        transitionsInProgress.forEach { entry ->
            wearNavigator.onTransitionComplete(entry)
        }
        initialContent.value = false
    }
}

@Composable
private fun BoxedStackEntryContent(
    entry: NavBackStackEntry?,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier = Modifier,
) {
    if (entry != null) {
        Box(modifier, propagateMinConstraints = true) {
            val destination = entry.destination as WearNavigator.Destination
            entry.LocalOwnersProvider(saveableStateHolder) {
                destination.content(entry)
            }
        }
    }
}
