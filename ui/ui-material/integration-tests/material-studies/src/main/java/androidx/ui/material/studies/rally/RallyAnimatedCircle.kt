/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.studies.rally

import androidx.animation.CubicBezierEasing
import androidx.animation.FloatPropKey
import androidx.animation.LinearOutSlowInEasing
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.foundation.Canvas
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.unit.dp
import androidx.ui.unit.minDimension

private const val DividerLengthInDegrees = 1.8f
private val AngleOffset = FloatPropKey()
private val Shift = FloatPropKey()

private val CircularTransition = transitionDefinition {
    state(0) {
        this[AngleOffset] = 0f
        this[Shift] = 0f
    }
    state(1) {
        this[AngleOffset] = 360f
        this[Shift] = 30f
    }
    transition(fromState = 0, toState = 1) {
        AngleOffset using tween {
            delay = 500
            duration = 900
            easing = CubicBezierEasing(0f, 0.75f, 0.35f, 0.85f)
        }
        Shift using tween {
            delay = 500
            duration = 900
            easing = LinearOutSlowInEasing
        }
    }
}

/** when calculating a proportion of N elements, the sum of elements has to be (1 - N * 0.005)
 * because there will be N dividers of size 1.8 degrees */
@Composable
fun AnimatedCircle(
    modifier: Modifier = Modifier.None,
    proportions: List<Float>,
    colors: List<Color>
) {
    val strokeWidthDp = 5.dp
    val paint = remember { Paint() }
    Transition(definition = CircularTransition, initState = 0, toState = 1) { state ->
        Canvas(modifier) {
            val strokeWidth = strokeWidthDp.toPx().value
            paint.style = PaintingStyle.stroke
            paint.strokeWidth = strokeWidth
            paint.isAntiAlias = true

            val innerRadius = (size.minDimension.value - strokeWidth) / 2
            val parentHalfWidth = size.width.value / 2
            val parentHalfHeight = size.height.value / 2
            val rect = Rect(
                parentHalfWidth - innerRadius,
                parentHalfHeight - innerRadius,
                parentHalfWidth + innerRadius,
                parentHalfHeight + innerRadius
            )
            var startAngle = state[Shift] - 90f
            proportions.forEachIndexed { index, proportion ->
                paint.color = colors[index]
                val sweep = proportion * state[AngleOffset]
                drawArc(rect, startAngle, sweep, false, paint = paint)
                startAngle += sweep + DividerLengthInDegrees
            }
        }
    }
}