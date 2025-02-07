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

package androidx.wear.compose.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.get
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.SwipeToDismissKeys

@Composable
internal fun BasicSwipeToDismissBoxNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    userSwipeEnabled: Boolean = true,
    state: SwipeDismissableNavHostState = rememberSwipeDismissableNavHostState(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "SwipeDismissableNavHost requires a ViewModelStoreOwner to be provided " +
                "via LocalViewModelStoreOwner"
        }

    navController.setViewModelStore(viewModelStoreOwner.viewModelStore)

    // Then set the graph
    navController.graph = graph

    // Find the WearNavigator, returning early if it isn't found
    // (such as is the case when using TestNavHostController).
    val wearNavigator =
        navController.navigatorProvider.get<Navigator<out NavDestination>>(WearNavigator.NAME)
            as? WearNavigator ?: return

    val backStack by wearNavigator.backStack.collectAsState()

    val navigateBack: () -> Unit = { navController.popBackStack() }

    val transitionsInProgress by wearNavigator.transitionsInProgress.collectAsState()

    DisposableEffect(lifecycleOwner) {
        // Setup the navController with proper owners
        navController.setLifecycleOwner(lifecycleOwner)
        onDispose {}
    }

    val stateHolder = rememberSaveableStateHolder()

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
        // There's nothing to draw here, so we can return early to make sure "current" is always
        // available below this line
        return
    }

    val isRoundDevice = isRoundDevice()
    val reduceMotionEnabled = LocalReduceMotion.current

    var initialContent by remember { mutableStateOf(true) }
    val swipeState = state.swipeToDismissBoxState

    LaunchedEffect(swipeState.isAnimationRunning) {
        // This effect marks the transitions completed when swipe animations finish,
        // so that the navigation backstack entries can go to Lifecycle.State.RESUMED.
        if (!swipeState.isAnimationRunning) {
            transitionsInProgress.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
        }
    }

    val animationProgress =
        remember(current) {
            if (!wearNavigator.isPop.value) {
                Animatable(0f)
            } else {
                Animatable(1f)
            }
        }

    LaunchedEffect(animationProgress.isRunning) {
        if (!animationProgress.isRunning) {
            transitionsInProgress.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
        }
    }

    BackHandler(enabled = backStack.size > 1, onBack = navigateBack)

    BasicSwipeToDismissBox(
        onDismissed = navigateBack,
        state = swipeState,
        modifier = Modifier,
        userSwipeEnabled = userSwipeEnabled && previous != null,
        backgroundKey = previous?.id ?: SwipeToDismissKeys.Background,
        contentKey = current.id,
    ) { isBackground ->
        BoxedStackEntryContent(
            entry = if (isBackground) previous else current,
            saveableStateHolder = stateHolder,
            modifier =
                if (isBackground) {
                    modifier
                } else {
                    modifier.graphicsLayer {
                        val scaleProgression =
                            NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(
                                (animationProgress.value / 0.75f)
                            )
                        val alphaProgression =
                            NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(
                                (animationProgress.value / 0.25f)
                            )
                        val scale = lerp(0.75f, 1f, scaleProgression).coerceAtMost(1f)
                        val alpha = lerp(0.1f, 1f, alphaProgression).coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        clip = true
                        shape = if (isRoundDevice) CircleShape else RectangleShape
                    }
                },
            layerColor =
                if (isBackground || wearNavigator.isPop.value) {
                    Color.Unspecified
                } else FLASH_COLOR,
            animatable = animationProgress
        )
    }

    LaunchedEffect(current) {
        if (!wearNavigator.isPop.value) {
            if (reduceMotionEnabled) {
                animationProgress.snapTo(1f)
            } else {
                animationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis =
                                NAV_HOST_ENTER_TRANSITION_DURATION_MEDIUM +
                                    NAV_HOST_ENTER_TRANSITION_DURATION_SHORT,
                            easing = LinearEasing
                        )
                )
            }
        }
    }

    DisposableEffect(previous, current) {
        if (initialContent) {
            // There are no animations for showing the initial content, so mark transitions
            // complete, allowing the navigation backstack entry to go to
            // Lifecycle.State.RESUMED.
            transitionsInProgress.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
            initialContent = false
        }
        onDispose {
            transitionsInProgress.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
        }
    }
}

@Composable
private fun BoxedStackEntryContent(
    entry: NavBackStackEntry?,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier = Modifier,
    layerColor: Color,
    animatable: Animatable<Float, AnimationVector1D>
) {
    if (entry != null) {
        val isRoundDevice = isRoundDevice()
        var lifecycleState by remember { mutableStateOf(entry.lifecycle.currentState) }
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event -> lifecycleState = event.targetState }
            entry.lifecycle.addObserver(observer)
            onDispose { entry.lifecycle.removeObserver(observer) }
        }
        if (lifecycleState.isAtLeast(Lifecycle.State.CREATED)) {
            Box(modifier, propagateMinConstraints = true) {
                val destination = entry.destination as WearNavigator.Destination
                entry.LocalOwnersProvider(saveableStateHolder) { destination.content(entry) }
                // Adding a flash effect when a new screen opens
                if (layerColor != Color.Unspecified) {
                    Canvas(Modifier.fillMaxSize()) {
                        val absoluteProgression =
                            ((animatable.value - 0.25f) / 0.75f).coerceIn(0f, 1f)
                        val easedProgression =
                            NAV_HOST_ENTER_TRANSITION_EASING_STANDARD.transform(absoluteProgression)
                        val alpha = lerp(0.07f, 0f, easedProgression).coerceIn(0f, 1f)
                        if (isRoundDevice) {
                            drawCircle(color = layerColor.copy(alpha))
                        } else {
                            drawRect(color = layerColor.copy(alpha))
                        }
                    }
                }
            }
        }
    }
}

private const val NAV_HOST_ENTER_TRANSITION_DURATION_SHORT = 100
private const val NAV_HOST_ENTER_TRANSITION_DURATION_MEDIUM = 300
private val NAV_HOST_ENTER_TRANSITION_EASING_STANDARD = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val FLASH_COLOR = Color.White
