/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.integration.demos.common

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

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
        this
            .onRotaryScrollEvent {
                channel.trySend(TimestampedDelta(it.uptimeMillis, it.verticalScrollPixels))
                rsbScrollInProgress = true
                true
            }
            .let {
                if (focusRequester != null) {
                    it
                        .focusRequester(focusRequester)
                        .focusable()
                } else it
            }
    }
}

internal data class TimestampedDelta(val time: Long, val delta: Float)
