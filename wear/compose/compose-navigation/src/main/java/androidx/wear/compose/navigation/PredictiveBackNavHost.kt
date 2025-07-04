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

import android.os.Build
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.get
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun PredictiveBackNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    userSwipeEnabled: Boolean = true,
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

    DisposableEffect(lifecycleOwner) {
        // Setup the navController with proper owners
        navController.setLifecycleOwner(lifecycleOwner)
        onDispose {}
    }

    val stateHolder = rememberSaveableStateHolder()

    val previous = backStack.getOrNull(backStack.lastIndex - 1)
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

    val scrimColor = LocalSwipeToDismissBackgroundScrimColor.current
    val isRoundDevice = isRoundDevice()

    // Use PredictiveBackHandler instead of BackHandler on API >= 35
    var progress by remember { mutableFloatStateOf(0f) }
    var inPredictiveBack by remember { mutableStateOf(false) }
    val transitionState = remember { SeekableTransitionState(current) }
    PredictiveBackHandler(userSwipeEnabled && backStack.size > 1) { backEvent ->
        inPredictiveBack = true
        progress = 0f
        try {
            backEvent.collect { progress = it.progress }
            Animatable(progress).animateTo(1f, TRANSITION_ANIMATION_SPEC) { progress = value }
            inPredictiveBack = false
            navigateBack()
        } catch (e: CancellationException) {
            inPredictiveBack = false
        }
    }

    val zIndices = remember { mutableObjectFloatMapOf<String>() }
    val transition = rememberTransition(transitionState, label = "entry")

    if (inPredictiveBack && previous != null) {
        LaunchedEffect(progress) { transitionState.seekTo(progress, previous) }
    } else {
        LaunchedEffect(current) {
            if (transitionState.currentState != current) {
                transitionState.animateTo(current)
            } else {
                animate(transitionState.fraction, 0f, animationSpec = TRANSITION_ANIMATION_SPEC) {
                    value,
                    _ ->
                    this@LaunchedEffect.launch {
                        if (value > 0) {
                            // Seek the original transition back to the currentState
                            transitionState.seekTo(value)
                        }
                        if (value == 0f) {
                            // Once we animate to the start, we need to snap to the right state.
                            transitionState.snapTo(current)
                        }
                    }
                }
            }
        }
    }

    transition.AnimatedContent(
        modifier,
        transitionSpec = {
            val initialZIndex = zIndices.getOrPut(initialState.id) { 0f }
            val targetZIndex =
                when {
                    targetState.id == initialState.id -> initialZIndex
                    wearNavigator.isPop.value || inPredictiveBack ->
                        initialZIndex - 1f // Going to the previous page, so zIndex - 1
                    else -> initialZIndex + 1f // Going to the next page, so zIndex + 1
                }.also { zIndices[targetState.id] = it }

            ContentTransform(
                targetContentEnter =
                    if (wearNavigator.isPop.value || inPredictiveBack) POP_ENTER_TRANSITION
                    else ENTER_TRANSITION,
                initialContentExit =
                    if (wearNavigator.isPop.value || inPredictiveBack) POP_EXIT_TRANSITION
                    else EXIT_TRANSITION,
                targetContentZIndex = targetZIndex,
                sizeTransform = null,
            )
        },
        contentAlignment = Alignment.Center,
        contentKey = { it.id },
    ) {
        // In some specific cases, such as popping your back stack or changing your
        // start destination, AnimatedContent can contain an entry that is no longer
        // part of visible entries since it was cleared from the back stack and is not
        // animating.
        val currentEntry =
            if (wearNavigator.isPop.value || inPredictiveBack) {
                // We have to do this because the previous entry might not show up in backStack
                it
            } else {
                backStack.lastOrNull { entry -> it == entry }
            }

        if (currentEntry != null) {
            val parentScreenActive = LocalScreenIsActive.current
            CompositionLocalProvider(
                LocalScreenIsActive provides (currentEntry == current && parentScreenActive)
            ) {
                Box(
                    modifier =
                        Modifier.background(
                                scrimColor,
                                if (isRoundDevice) CircleShape else RectangleShape,
                            )
                            .fillMaxSize()
                            .hierarchicalFocusGroup(currentEntry == current)
                ) {
                    // while in the scope of the composable, we provide the navBackStackEntry as the
                    // ViewModelStoreOwner and LifecycleOwner
                    if (currentEntry.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                        currentEntry.LocalOwnersProvider(stateHolder) {
                            DestinationContent(backStackEntry = currentEntry)
                        }
                    }
                    if (currentEntry != current) {
                        Box(
                            modifier =
                                Modifier.clickable(
                                        enabled = false,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) {
                                        // Ignore taps on previous backstack entries
                                    }
                                    .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(transition.currentState, transition.targetState) {
        if (transition.currentState == transition.targetState) {
            backStack.forEach { entry -> wearNavigator.onTransitionComplete(entry) }
            zIndices.forEach { key, _ ->
                if (key != transition.targetState.id) zIndices.remove(key)
            }
        }
    }
}

// Using this @Composable function instead of an inline lambda in `NavGraphBuilder.composable` helps
// prevent unnecessary continuous recomposition of the lambda block during predictive back swipe
// animations. Once strong skipping is enabled in the Compose compiler, inline composable lambdas
// are expected to be automatically memoized, providing similar behavior to this explicit function.
// This change ensures the optimization is in place regardless of the current compiler
// configuration. This approach may be reverted once strong skipping becomes a standard feature.
@Composable
private fun DestinationContent(backStackEntry: NavBackStackEntry) {
    (backStackEntry.destination as WearNavigator.Destination).content(backStackEntry)
}

private val ENTER_TRANSITION =
    slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = spring(0.8f, 300f)) +
        scaleIn(initialScale = 0.8f, animationSpec = spring(1f, 500f)) +
        fadeIn(animationSpec = spring(1f, 1500f))
private val EXIT_TRANSITION =
    scaleOut(targetScale = 0.85f, animationSpec = spring(1f, 150f)) +
        slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = spring(0.8f, 200f)) +
        fadeOut(targetAlpha = 0.6f, animationSpec = spring(1f, 1400f))
private val POP_ENTER_TRANSITION =
    scaleIn(initialScale = 0.8f, animationSpec = tween(easing = LinearEasing)) +
        slideInHorizontally(
            initialOffsetX = { -it / 2 },
            animationSpec = tween(easing = LinearEasing),
        ) +
        fadeIn(initialAlpha = 0.5f, animationSpec = tween(easing = LinearEasing))
private val POP_EXIT_TRANSITION =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(easing = LinearEasing)) +
        scaleOut(targetScale = 0.8f, animationSpec = tween(easing = LinearEasing))

private val TRANSITION_ANIMATION_SPEC =
    spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
