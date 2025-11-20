/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Test scene with nested Animated content so that the scene handles the transitions internally
 *
 * The scene always returns the same scene key, so NavDisplay is expected to only recompose the new
 * scene but *not* animate the transition.
 */
class CardStackSceneStrategy<T : Any>(val duration: Int = 300) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val cardEntries = entries.takeLastWhile { entry -> entry.metadata.contains(CARD_KEY) }
        return if (cardEntries.isNotEmpty()) {
            CardStackScene(
                cardEntries.toMutableStateList(),
                entries.dropLast(1),
                duration,
                onBack = onBack,
            )
        } else {
            null
        }
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a stack of cards.
         */
        fun card(): Map<String, Any> = mapOf(CARD_KEY to true)

        internal const val CARD_KEY = "card"
    }
}

class CardStackScene<T : Any>(
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
    val duration: Int,
    val onBack: () -> Unit,
) : Scene<T> {
    // Stack all card entries in the same Scene
    override val key = Unit

    override val content: @Composable () -> Unit = {
        // Keep track of all of the currently displayed entries
        // this may include entries that have been popped, but are
        // animating out
        val currentEntries = remember { SnapshotStateSet<NavEntry<T>>() }
        LaunchedEffect(entries) {
            // Every time the entries change, add all of the new entries
            // to the current entries list
            currentEntries.addAll(entries)
        }
        // Handle Predictive Back
        val navigationEventState =
            rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = entries.size > 1,
            onBackCompleted = onBack,
        )
        val transitionState = navigationEventState.transitionState
        CardStack(modifier = Modifier.fillMaxSize()) {
            currentEntries.forEachIndexed { index, entry ->
                AnimatedCard(
                    entry,
                    visible = entry in entries,
                    // Only pass the active transitionState to the last entry
                    transitionState =
                        if (index == currentEntries.size - 1) {
                            transitionState
                        } else {
                            NavigationEventTransitionState.Idle
                        },
                    onNoLongerVisible = { currentEntries.remove(entry) },
                    duration = duration,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Composable
    private fun AnimatedCard(
        entry: NavEntry<T>,
        visible: Boolean,
        transitionState: NavigationEventTransitionState,
        onNoLongerVisible: () -> Unit,
        duration: Int,
        modifier: Modifier = Modifier,
    ) {
        // Set up the Transition
        val seekableTransitionState = remember { SeekableTransitionState(visible) }
        val transition = rememberTransition(seekableTransitionState, label = "cardStackScene")

        var lastEdge by remember { mutableIntStateOf(NavigationEvent.EDGE_RIGHT) }
        if (transitionState is NavigationEventTransitionState.InProgress) {
            // Predictive Back is happening
            lastEdge = transitionState.latestEvent.swipeEdge
            val progress = transitionState.latestEvent.progress
            LaunchedEffect(progress) { seekableTransitionState.seekTo(progress, false) }
        } else {
            LaunchedEffect(visible) {
                seekableTransitionState.animateTo(visible)
                if (!visible) {
                    onNoLongerVisible()
                }
            }
        }

        transition.AnimatedVisibility(
            visible = { it },
            modifier = modifier,
            exit =
                slideOut(tween(duration)) { fullSize ->
                    val xOffset =
                        if (lastEdge == NavigationEvent.EDGE_RIGHT) -fullSize.width
                        else fullSize.width
                    IntOffset(xOffset, -(fullSize.height / 8))
                },
        ) {
            ElevatedCard { entry.Content() }
        }
    }

    @Composable
    private fun CardStack(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val paddingInPixels = CARD_PADDING.dp.toPx().toInt()
            val placeables =
                measurables.mapIndexed { index, measurable -> measurable.measure(constraints) }

            val height =
                if (placeables.isNotEmpty()) {
                    placeables.first().height + (paddingInPixels * placeables.size)
                } else {
                    0
                }

            val width =
                if (placeables.isNotEmpty()) {
                    placeables.first().width
                } else {
                    0
                }

            layout(width = width, height = height) {
                placeables.mapIndexed { index, placeable ->
                    placeable.place(x = 0, y = paddingInPixels * index)
                }
            }
        }
    }

    companion object {
        const val CARD_PADDING = 48
    }
}
