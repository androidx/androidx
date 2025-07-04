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

package androidx.xr.compose.testapp.ui.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.xr.runtime.math.Vector3
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun initializePanelRotationData(): Pair<Float, Vector3> {
    // Spatial panel rotation
    val infiniteTransition = rememberInfiniteTransition()
    val singleRotationDurationMs = 7000
    val rotationValue by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 359f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(singleRotationDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
        )

    // Rotation axis
    val axisAngle by
        infiniteTransition.animateValue(
            initialValue = Vector3(0.1f, 0.0f, 0.0f),
            targetValue = Vector3(1.1f, 1.1f, 1.1f),
            typeConverter =
                TwoWayConverter(
                    {
                        val axisSingleValue =
                            (it.x.roundToInt()) + (2 * it.y.roundToInt()) + (4 * it.z.roundToInt())
                        AnimationVector1D(axisSingleValue.toFloat() / 7)
                    },
                    {
                        val scaledAnimationValue = (it.value * 7) + 1.0f
                        val x = floor(scaledAnimationValue % 2)
                        val y = floor((scaledAnimationValue / 2) % 2)
                        val z = floor((scaledAnimationValue / 4) % 2)

                        Vector3(x, y, z)
                    },
                ),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(singleRotationDurationMs * 7, easing = LinearEasing)
                ),
        )

    return Pair(rotationValue, axisAngle)
}
