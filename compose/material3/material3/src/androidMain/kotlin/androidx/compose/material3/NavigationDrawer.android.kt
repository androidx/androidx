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

package androidx.compose.material3

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Registers a [PredictiveBackHandler] and provides animation values in [DrawerPredictiveBackState]
 * based on back progress.
 *
 * @param drawerState state of the drawer
 * @param content content of the rest of the UI
 */
@Composable
internal actual fun DrawerPredictiveBackHandler(
    drawerState: DrawerState,
    content: @Composable (DrawerPredictiveBackState) -> Unit
) {
    val drawerPredictiveBackState = remember { DrawerPredictiveBackState() }
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val maxScaleXDistanceGrow: Float
    val maxScaleXDistanceShrink: Float
    val maxScaleYDistance: Float
    with(LocalDensity.current) {
        maxScaleXDistanceGrow = PredictiveBackDrawerMaxScaleXDistanceGrow.toPx()
        maxScaleXDistanceShrink = PredictiveBackDrawerMaxScaleXDistanceShrink.toPx()
        maxScaleYDistance = PredictiveBackDrawerMaxScaleYDistance.toPx()
    }

    PredictiveBackHandler(enabled = drawerState.isOpen) { progress ->
        try {
            progress.collect { backEvent ->
                drawerPredictiveBackState.update(
                    PredictiveBack.transform(backEvent.progress),
                    backEvent.swipeEdge == BackEventCompat.EDGE_LEFT,
                    isRtl,
                    maxScaleXDistanceGrow,
                    maxScaleXDistanceShrink,
                    maxScaleYDistance
                )
            }
        } catch (e: CancellationException) {
            drawerPredictiveBackState.clear()
        } finally {
            if (drawerPredictiveBackState.swipeEdgeMatchesDrawer) {
                // If swipe edge matches drawer gravity and we've stretched the drawer horizontally,
                // un-stretch it smoothly so that it hides completely during the drawer close.
                scope.launch {
                    animate(
                        initialValue = drawerPredictiveBackState.scaleXDistance,
                        targetValue = 0f
                    ) { value, _ ->
                        drawerPredictiveBackState.scaleXDistance = value
                    }
                    drawerPredictiveBackState.clear()
                }
            }
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            drawerPredictiveBackState.clear()
        }
    }

    content(drawerPredictiveBackState)
}

internal val PredictiveBackDrawerMaxScaleXDistanceGrow = 12.dp
internal val PredictiveBackDrawerMaxScaleXDistanceShrink = 24.dp
internal val PredictiveBackDrawerMaxScaleYDistance = 48.dp
