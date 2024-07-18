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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.SwipeToDismissBox
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissKeys
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.integration.demos.common.ActivityDemo
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Demo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.integration.demos.common.DemoParameters
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DemoApp(
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
    scrollStates: MutableList<ScalingLazyListState>,
) {
    val swipeToDismissState = swipeDismissStateWithNavigation(onNavigateBack)
    DisplayDemo(
        swipeToDismissState, currentDemo, parentDemo, onNavigateTo, onNavigateBack, scrollStates)
}

@Composable
private fun DisplayDemo(
    state: SwipeToDismissBoxState,
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
    scrollStates: MutableList<ScalingLazyListState>,
) {
    SwipeToDismissBox(
        state = state,
        userSwipeEnabled = parentDemo != null,
        backgroundKey = parentDemo?.title ?: SwipeToDismissKeys.Background,
        contentKey = currentDemo.title,
    ) { isBackground ->
        BoxDemo(
            state,
            if (isBackground) parentDemo else currentDemo,
            onNavigateTo,
            onNavigateBack,
            scrollStates.lastIndex - (if (isBackground) 1 else 0),
            scrollStates,
        )
    }
}

@Composable
private fun BoxScope.BoxDemo(
    state: SwipeToDismissBoxState,
    demo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
    scrollStateIndex: Int,
    scrollStates: MutableList<ScalingLazyListState>,
) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }

        is ComposableDemo -> {
            demo.content(DemoParameters(onNavigateBack, state))
        }

        is DemoCategory -> {
            DisplayDemoList(demo, onNavigateTo, scrollStateIndex, scrollStates)
        }

        else -> {
        }
    }
}

@Composable
internal fun BoxScope.DisplayDemoList(
    category: DemoCategory,
    onNavigateTo: (Demo) -> Unit,
    scrollStateIndex: Int,
    scrollStates: MutableList<ScalingLazyListState>,
) {
    val state = rememberScalingLazyListState()

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DemoListTag),
        state = scrollStates[scrollStateIndex],
        snap = false,
        autoCentering = AutoCenteringParams(itemIndex = if (category.demos.size >= 2) 2 else 1),
    ) {
        item {
            ListHeader {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()

                )
            }
        }
        category.demos.forEach { demo ->
            item {
                Chip(
                    onClick = {
                        scrollStates.add(state)
                        onNavigateTo(demo)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    label = {
                        Text(
                            text = demo.title,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            demo.description?.let { description ->
                item {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.caption3
                    ) {
                        Text(
                            text = description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun swipeDismissStateWithNavigation(
    onNavigateBack: () -> Unit
): SwipeToDismissBoxState {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissValue.Dismissed) {
            state.snapTo(SwipeToDismissValue.Default)
            onNavigateBack()
        }
    }
    return state
}

internal data class TimestampedDelta(val time: Long, val delta: Float)

@Suppress("ComposableModifierFactory")
@Composable
fun Modifier.rsbScroll(
    scrollableState: ScrollableState,
    flingBehavior: FlingBehavior,
    focusRequester: FocusRequester? = null
): Modifier {
    val channel = remember {
        Channel<TimestampedDelta>(
            capacity = 10,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    var lastTimeMillis = remember { 0L }
    var smoothSpeed = remember { 0f }
    val speedWindowMillis = 200L
    val timeoutToFling = 100L

    return composed {
        var rsbScrollInProgress by remember { mutableStateOf(false) }
        LaunchedEffect(rsbScrollInProgress) {
            if (rsbScrollInProgress) {
                scrollableState.scroll(MutatePriority.UserInput) {
                    channel.receiveAsFlow().collectLatest {
                        val toScroll = if (lastTimeMillis > 0L && it.time > lastTimeMillis) {
                            val timeSinceLastEventMillis = it.time - lastTimeMillis

                            // Speed is in pixels per second.
                            val speed = it.delta * 1000 / timeSinceLastEventMillis
                            val cappedElapsedTimeMillis =
                                timeSinceLastEventMillis.coerceAtMost(speedWindowMillis)
                            smoothSpeed = ((speedWindowMillis - cappedElapsedTimeMillis) * speed +
                                cappedElapsedTimeMillis * smoothSpeed) / speedWindowMillis
                            smoothSpeed * cappedElapsedTimeMillis / 1000
                        } else {
                            0f
                        }
                        lastTimeMillis = it.time
                        scrollBy(toScroll)

                        // If more than the given time pass, start a fling.
                        delay(timeoutToFling)

                        lastTimeMillis = 0L

                        if (smoothSpeed != 0f) {
                            val launchSpeed = smoothSpeed
                            smoothSpeed = 0f
                            with(flingBehavior) {
                                performFling(launchSpeed)
                            }
                            rsbScrollInProgress = false
                        }
                    }
                }
            }
        }
        this.onRotaryScrollEvent {
            channel.trySend(TimestampedDelta(it.uptimeMillis, it.verticalScrollPixels))
            rsbScrollInProgress = true
            true
        }.let {
            if (focusRequester != null) {
                it
                    .focusRequester(focusRequester)
                    .focusable()
            } else it
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun ScalingLazyColumnWithRSB(
    modifier: Modifier = Modifier,
    state: ScalingLazyListState = rememberScalingLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp),
    scalingParams: ScalingParams = ScalingLazyColumnDefaults.scalingParams(),
    reverseLayout: Boolean = false,
    snap: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(
        space = 4.dp,
        alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom
    ),
    autoCentering: AutoCenteringParams? = AutoCenteringParams(),
    content: ScalingLazyListScope.() -> Unit
) {
    val flingBehavior = if (snap) ScalingLazyColumnDefaults.snapFlingBehavior(
        state = state
    ) else ScrollableDefaults.flingBehavior()
    val focusRequester = rememberActiveFocusRequester()
    ScalingLazyColumn(
        modifier = modifier.rsbScroll(
            scrollableState = state,
            flingBehavior = flingBehavior,
            focusRequester = focusRequester
        ),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        scalingParams = scalingParams,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        autoCentering = autoCentering,
        content = content
    )
}
