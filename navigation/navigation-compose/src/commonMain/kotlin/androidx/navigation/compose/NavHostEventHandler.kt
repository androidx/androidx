/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.Companion.EDGE_NONE
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
internal fun rememberNavHostEventHandler(
    navigator: ComposeNavigator,
    enabled: Boolean,
): NavHostEventHandler {
    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
            }
            .navigationEventDispatcher

    val handler = remember(navigator) { NavHostEventHandler(navigator) }

    LifecycleStartEffect(enabled, handler) {
        handler.isBackEnabled = enabled
        onStopOrDispose { handler.isBackEnabled = false }
    }

    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }

    return handler
}

/**
 * A [NavigationEventHandler] that bridges system predictive back gestures with a
 * [ComposeNavigator].
 *
 * This handler listens to the lifecycle of a predictive back gesture and exposes the ongoing
 * gesture metrics ([inPredictiveBack], [progress], [swipeEdge]) as observable Compose state. This
 * allows the surrounding UI to animate reactively as the user swipes.
 *
 * It acts as the coordinator between the system gesture and the [navigator], ensuring the navigator
 * is notified when a transition begins and popping the back stack when the gesture successfully
 * completes.
 *
 * @param navigator The [ComposeNavigator] managing the current back stack.
 */
internal class NavHostEventHandler(private val navigator: ComposeNavigator) :
    NavigationEventHandler<NavigationEventInfo>(
        initialInfo = NavigationEventInfo.None,
        isBackEnabled = true,
        isForwardEnabled = false,
    ) {

    /** The destination that is currently being swiped away. */
    private var currentBackStackEntry: NavBackStackEntry? = null

    /**
     * Observable state indicating whether a predictive back gesture is currently in progress. True
     * from the moment the user starts dragging until the gesture completes or cancels.
     */
    var inPredictiveBack by mutableStateOf(false)
        private set

    /** Observable state representing the progress of the current back gesture, from 0f to 1f. */
    var progress by mutableFloatStateOf(0f)
        private set

    /**
     * Observable state indicating which edge of the screen the swipe originated from (e.g.,
     * [NavigationEvent.EDGE_LEFT] or [NavigationEvent.EDGE_RIGHT]).
     */
    var swipeEdge by mutableIntStateOf(EDGE_NONE)
        private set

    override fun onBackStarted(event: NavigationEvent) {
        prepareTransition()
        inPredictiveBack = true
        progress = event.progress
        swipeEdge = event.swipeEdge
    }

    override fun onBackProgressed(event: NavigationEvent) {
        inPredictiveBack = true
        progress = event.progress
        swipeEdge = event.swipeEdge
    }

    override fun onBackCompleted() {
        if (!inPredictiveBack) {
            // Non-predictive back events do not emit a starting event, so the dispatcher skips
            // 'onBackStarted()' and routes straight here. We must manually prepare the transition
            // in these cases before popping the stack.
            prepareTransition()
        }
        currentBackStackEntry?.let { entry -> navigator.popBackStack(entry, false) }
        resetState()
    }

    override fun onBackCancelled() {
        resetState()
    }

    /**
     * Initializes the necessary references for the gesture and notifies the [navigator] that the
     * current and previous screens should prepare for a visual transition.
     */
    private fun prepareTransition() {
        val backStack = navigator.backStack.value

        // Safety check to ensure we have both a current and previous destination to animate.
        if (backStack.size < 2) return

        val currentEntry = backStack.last()
        val previousEntry = backStack[backStack.lastIndex - 1]

        navigator.prepareForTransition(currentEntry)
        navigator.prepareForTransition(previousEntry)

        currentBackStackEntry = currentEntry
    }

    /**
     * Resets the gesture metrics to their default idle values and clears held references to prevent
     * memory leaks after the transition finishes or cancels.
     */
    private fun resetState() {
        inPredictiveBack = false
        progress = 0f
        swipeEdge = EDGE_NONE
        currentBackStackEntry = null
    }
}
