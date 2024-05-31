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

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationSpecApi::class)
@Sampled
fun KeyframesBuilderForOffsetWithSplines() {
    keyframesWithSpline {
        durationMillis = 200
        Offset(0f, 0f) at 0
        Offset(500f, 100f) at 100
        Offset(400f, 50f) at 150
    }
}

@OptIn(ExperimentalAnimationSpecApi::class)
@Sampled
fun KeyframesBuilderForIntOffsetWithSplines() {
    keyframesWithSpline {
        durationMillis = 200
        IntOffset(0, 0) at 0
        IntOffset(500, 100) at 100
        IntOffset(400, 50) at 150
    }
}

@OptIn(ExperimentalAnimationSpecApi::class)
@Sampled
fun KeyframesBuilderForDpOffsetWithSplines() {
    keyframesWithSpline {
        durationMillis = 200
        DpOffset(0.dp, 0.dp) at 0
        DpOffset(500.dp, 100.dp) at 100
        DpOffset(400.dp, 50.dp) at 150
    }
}

@OptIn(ExperimentalAnimationSpecApi::class)
@Composable
@Sampled
fun PeriodicKeyframesWithSplines() {
    var alpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    // With a periodicBias of 0.5f it creates a similar animation to a sinusoidal
                    // curve
                    // so the transition as the animation repeats is completely seamless
                    animation =
                        keyframesWithSpline(periodicBias = 0.5f) {
                            durationMillis = 2000

                            1f at 1000 using LinearEasing
                        },
                    repeatMode = RepeatMode.Restart
                )
        ) { value, _ ->
            alpha = value
        }
    }
    Image(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        modifier = Modifier.size(150.dp).graphicsLayer { this.alpha = alpha },
        colorFilter = ColorFilter.tint(Color.Red)
    )
}
