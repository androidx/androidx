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

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur, with backwards
 * navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in the
 * background. BackgroundScrimColor and ContentScrimColor of it are taken from
 * [LocalSwipeToDismissBackgroundScrimColor] and [LocalSwipeToDismissContentScrimColor].
 *
 * Below API level 36, content of the current navigation level is displayed within a
 * [BasicSwipeToDismissBox] to detect swipe back gestures.
 *
 * API level 36 onwards, [SwipeDismissableNavHost] listens to platform predictive back events for
 * navigation, and [BasicSwipeToDismissBox] is not used for swipe gesture detection.
 *
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 *
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 *
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 * @param navController The navController for this host
 * @param startDestination The route for the start destination
 * @param modifier The modifier to be applied to the layout
 * @param userSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 * @param state State containing information about ongoing swipe and animation. This parameter is
 *   unused API level 36 onwards, because the platform supports predictive back and
 *   [SwipeDismissableNavHost] uses platform gestures to detect the back gestures.
 * @param route The route for the graph
 * @param builder The builder used to construct the graph
 */
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    userSwipeEnabled: Boolean = true,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit,
): Unit =
    SwipeDismissableNavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier,
        userSwipeEnabled,
        state = state,
    )

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur, with backwards
 * navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in the
 * background. BackgroundScrimColor and ContentScrimColor of it are taken from
 * [LocalSwipeToDismissBackgroundScrimColor] and [LocalSwipeToDismissContentScrimColor].
 *
 * Below API level 36, content of the current navigation level is displayed within a
 * [BasicSwipeToDismissBox] to detect swipe back gestures.
 *
 * API level 36 onwards, [SwipeDismissableNavHost] listens to platform predictive back events for
 * navigation, and [BasicSwipeToDismissBox] is not used for swipe gesture detection. Therefore,
 * [Modifier.edgeSwipeToDismiss] is not compatible with [SwipeDismissableNavHost] for API level 36
 * onwards.
 *
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 *
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 *
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 * @param navController [NavHostController] for this host
 * @param graph Graph for this host
 * @param modifier [Modifier] to be applied to the layout
 * @param userSwipeEnabled [Boolean] Whether swipe-to-dismiss gesture is enabled.
 * @param state State containing information about ongoing swipe and animation. This parameter is
 *   unused API level 36 onwards, because the platform supports predictive back and
 *   [SwipeDismissableNavHost] uses platform gestures to detect the back gestures.
 * @throws IllegalArgumentException if no WearNavigation.Destination is on the navigation backstack.
 */
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    userSwipeEnabled: Boolean = true,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        PredictiveBackNavHost(
            navController = navController,
            graph = graph,
            modifier = modifier,
            userSwipeEnabled = userSwipeEnabled,
        )
    } else {
        BasicSwipeToDismissBoxNavHost(
            navController = navController,
            graph = graph,
            modifier = modifier,
            userSwipeEnabled = userSwipeEnabled,
            state = state,
        )
    }
}

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur, with backwards
 * navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * Content is displayed within a [BasicSwipeToDismissBox], showing the current navigation level.
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in the
 * background. BackgroundScrimColor and ContentScrimColor of it are taken from
 * [LocalSwipeToDismissBackgroundScrimColor] and [LocalSwipeToDismissContentScrimColor].
 *
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 *
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 *
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 * @param navController The navController for this host
 * @param startDestination The route for the start destination
 * @param modifier The modifier to be applied to the layout
 * @param state State containing information about ongoing swipe and animation.
 * @param route The route for the graph
 * @param builder The builder used to construct the graph
 */
@Deprecated(
    "This overload is provided for backwards compatibility. " +
        "A newer overload is available with an additional userSwipeEnabled param.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit,
): Unit =
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        userSwipeEnabled = true,
        state = state,
        route = route,
        builder = builder,
    )

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur, with backwards
 * navigation provided by a swipe gesture.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * Content is displayed within a [BasicSwipeToDismissBox], showing the current navigation level.
 * During a swipe-to-dismiss gesture, the previous navigation level (if any) is shown in the
 * background. BackgroundScrimColor and ContentScrimColor of it are taken from
 * [LocalSwipeToDismissBackgroundScrimColor] and [LocalSwipeToDismissContentScrimColor].
 *
 * Example of a [SwipeDismissableNavHost] alternating between 2 screens:
 *
 * @sample androidx.wear.compose.navigation.samples.SimpleNavHost
 *
 * Example of a [SwipeDismissableNavHost] for which a destination has a named argument:
 *
 * @sample androidx.wear.compose.navigation.samples.NavHostWithNamedArgument
 * @param navController [NavHostController] for this host
 * @param graph Graph for this host
 * @param modifier [Modifier] to be applied to the layout
 * @param state State containing information about ongoing swipe and animation.
 * @throws IllegalArgumentException if no WearNavigation.Destination is on the navigation backstack.
 */
@Deprecated(
    "This overload is provided for backwards compatibility. " +
        "A newer overload is available with an additional userSwipeEnabled param.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun SwipeDismissableNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
): Unit =
    SwipeDismissableNavHost(
        navController = navController,
        graph = graph,
        modifier = modifier,
        userSwipeEnabled = true,
        state = state,
    )

/**
 * State for [SwipeDismissableNavHost]
 *
 * @param swipeToDismissBoxState State for [BasicSwipeToDismissBox], which is used to support the
 *   swipe-to-dismiss gesture in [SwipeDismissableNavHost] and can also be used to support
 *   edge-swiping, using [edgeSwipeToDismiss].
 */
public class SwipeDismissableNavHostState(
    internal val swipeToDismissBoxState: SwipeToDismissBoxState
) {
    @Suppress("DEPRECATION")
    @Deprecated(
        "This overload is provided for backward compatibility. " +
            "A newer overload is available which uses SwipeToDismissBoxState " +
            "from androidx.wear.compose.foundation package."
    )
    public constructor(
        swipeToDismissBoxState: androidx.wear.compose.material.SwipeToDismissBoxState
    ) : this(swipeToDismissBoxState.foundationState)
}

/**
 * Create a [SwipeToDismissBoxState] and remember it.
 *
 * @param swipeToDismissBoxState State for [BasicSwipeToDismissBox], which is used to support the
 *   swipe-to-dismiss gesture in [SwipeDismissableNavHost] and can also be used to support
 *   edge-swiping, using [edgeSwipeToDismiss]. This parameter is unused after API 36, because the
 *   platform supports edge-swiping via predictive back gesture, and [SwipeDismissableNavHost] drops
 *   the use of [BasicSwipeToDismissBox] in favour of predictive back based navigation.
 */
@Composable
public fun rememberSwipeDismissableNavHostState(
    swipeToDismissBoxState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState()
): SwipeDismissableNavHostState {
    return remember(swipeToDismissBoxState) { SwipeDismissableNavHostState(swipeToDismissBoxState) }
}

@Suppress("DEPRECATION")
@Deprecated(
    "This overload is provided for backward compatibility. A newer overload is available " +
        "which uses SwipeToDismissBoxState from androidx.wear.compose.foundation package.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun rememberSwipeDismissableNavHostState(
    swipeToDismissBoxState: androidx.wear.compose.material.SwipeToDismissBoxState =
        androidx.wear.compose.material.rememberSwipeToDismissBoxState()
): SwipeDismissableNavHostState {
    return remember(swipeToDismissBoxState) { SwipeDismissableNavHostState(swipeToDismissBoxState) }
}

@Composable
internal fun isRoundDevice(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) { configuration.isScreenRound }
}

internal const val TAG = "SwipeDismissableNavHost"
