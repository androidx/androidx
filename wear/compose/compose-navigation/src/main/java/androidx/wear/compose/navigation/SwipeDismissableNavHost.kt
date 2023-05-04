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

import android.util.Log
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.wear.compose.material.SwipeToDismissValue
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.SwipeToDismissKeys
import androidx.wear.compose.material.edgeSwipeToDismiss
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
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 *
 * @param navController The navController for this host
 * @param startDestination The route for the start destination
 * @param modifier The modifier to be applied to the layout
 * @param state State containing information about ongoing swipe and animation.
 * @param route The route for the graph
 * @param builder The builder used to construct the graph
 */
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) =
    SwipeDismissableNavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier,
        state = state,
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
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 *
 * @param navController [NavHostController] for this host
 * @param graph Graph for this host
 * @param modifier [Modifier] to be applied to the layout
 * @param state State containing information about ongoing swipe and animation.
 *
 * @throws IllegalArgumentException if no WearNavigation.Destination is on the navigation backstack.
 */
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "SwipeDismissableNavHost requires a ViewModelStoreOwner to be provided " +
            "via LocalViewModelStoreOwner"
    }
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onBackPressedDispatcher = onBackPressedDispatcherOwner?.onBackPressedDispatcher

    // Setup the navController with proper owners
    navController.setLifecycleOwner(lifecycleOwner)
    navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
    if (onBackPressedDispatcher != null) {
        navController.setOnBackPressedDispatcher(onBackPressedDispatcher)
    }
    // Ensure that the NavController only receives back events while
    // the NavHost is in composition
    DisposableEffect(navController) {
        navController.enableOnBackPressed(true)
        onDispose {
            navController.enableOnBackPressed(false)
        }
    }

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
    var initialContent by remember { mutableStateOf(true) }

    val previous = if (backStack.size <= 1) null else backStack[backStack.lastIndex - 1]
    // Get the current navigation backstack entry. If the backstack is empty, it could be because
    // no WearNavigator.Destinations were added to the navigation backstack (be sure to build
    // the NavGraph using androidx.wear.compose.navigation.composable) or because the last entry
    // was popped prior to navigating (instead, use navigate with popUpTo).
    // If the activity is using FLAG_ACTIVITY_NEW_TASK then it also needs to set
    // FLAG_ACTIVITY_CLEAR_TASK, otherwise the activity will be created twice,
    // the first of these with an empty backstack.
    val current = backStack.lastOrNull()

    if (current == null) {
        val warningText =
            "Current backstack entry is empty. Please ensure: \n" +
                "1. The current WearNavigator navigation backstack is not empty (e.g. by using " +
                "androidx.wear.compose.navigation.composable to build your nav graph). \n" +
                "2. The last entry is not popped prior to navigation " +
                "(instead, use navigate with popUpTo). \n" +
                "3. If the activity uses FLAG_ACTIVITY_NEW_TASK you should also set " +
                "FLAG_ACTIVITY_CLEAR_TASK to maintain the backstack consistency."

        Log.w(TAG, warningText)
    }

    val swipeState = state.swipeToDismissBoxState
    LaunchedEffect(swipeState.currentValue) {
        // This effect operates when the swipe gesture is complete:
        // 1) Resets the screen offset (otherwise, the next destination is draw off-screen)
        // 2) Pops the navigation back stack to return to the previous level
        if (swipeState.currentValue == SwipeToDismissValue.Dismissed) {
            swipeState.snapTo(SwipeToDismissValue.Default)
            navController.popBackStack()
        }
    }
    LaunchedEffect(swipeState.isAnimationRunning) {
        // This effect marks the transitions completed when swipe animations finish,
        // so that the navigation backstack entries can go to Lifecycle.State.RESUMED.
        if (swipeState.isAnimationRunning == false) {
            transitionsInProgress.forEach { entry ->
                wearNavigator.onTransitionComplete(entry)
            }
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = Modifier,
        hasBackground = previous != null,
        backgroundKey = previous?.id ?: SwipeToDismissKeys.Background,
        contentKey = current?.id ?: SwipeToDismissKeys.Content,
        content = { isBackground ->
            BoxedStackEntryContent(if (isBackground) previous else current, stateHolder, modifier)
        }
    )

    DisposableEffect(previous, current) {
        if (initialContent) {
            // There are no animations for showing the initial content, so mark transitions complete,
            // allowing the navigation backstack entry to go to Lifecycle.State.RESUMED.
            transitionsInProgress.forEach { entry ->
                wearNavigator.onTransitionComplete(entry)
            }
            initialContent = false
        }
        onDispose {
            transitionsInProgress.forEach { entry ->
                wearNavigator.onTransitionComplete(entry)
            }
        }
    }
}

/**
 * State for [SwipeDismissableNavHost]
 *
 * @param swipeToDismissBoxState State for [SwipeToDismissBox], which is used to support the
 * swipe-to-dismiss gesture in [SwipeDismissableNavHost] and can also be used to support
 * edge-swiping, using [edgeSwipeToDismiss].
 */
public class SwipeDismissableNavHostState(
    internal val swipeToDismissBoxState: SwipeToDismissBoxState
)

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param swipeToDismissBoxState State for [SwipeToDismissBox], which is used to support the
 * swipe-to-dismiss gesture in [SwipeDismissableNavHost] and can also be used to support
 * edge-swiping, using [edgeSwipeToDismiss].
 */
@Composable
public fun rememberSwipeDismissableNavHostState(
    swipeToDismissBoxState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
): SwipeDismissableNavHostState {
    return remember(swipeToDismissBoxState) {
        SwipeDismissableNavHostState(swipeToDismissBoxState)
    }
}

@Composable
private fun BoxedStackEntryContent(
    entry: NavBackStackEntry?,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier = Modifier,
) {
    if (entry != null) {
        var lifecycleState by remember {
            mutableStateOf(entry.lifecycle.currentState)
        }
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                lifecycleState = event.targetState
            }
            entry.lifecycle.addObserver(observer)
            onDispose {
                entry.lifecycle.removeObserver(observer)
            }
        }
        if (lifecycleState.isAtLeast(Lifecycle.State.CREATED)) {
            Box(modifier, propagateMinConstraints = true) {
                val destination = entry.destination as WearNavigator.Destination
                entry.LocalOwnersProvider(saveableStateHolder) {
                    destination.content(entry)
                }
            }
        }
    }
}

private const val TAG = "SwipeDismissableNavHost"