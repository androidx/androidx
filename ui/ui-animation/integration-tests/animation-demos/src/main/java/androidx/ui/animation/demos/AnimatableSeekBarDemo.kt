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

package androidx.ui.animation.demos

import androidx.animation.FastOutSlowInEasing
import androidx.animation.FloatPropKey
import androidx.animation.ManualAnimationClock
import androidx.animation.TweenBuilder
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.remember
import androidx.ui.animation.Transition
import androidx.ui.animation.animatedFloat
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.gesture.rawDragGestureFilter
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Text
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.sp

@Composable
fun AnimatableSeekBarDemo() {
    val clock = remember { ManualAnimationClock(0L) }
    Providers(AnimationClockAmbient provides clock) {
        Column {
            Text(
                "Drag to update AnimationClock",
                fontSize = 20.sp,
                modifier = Modifier.padding(40.dp)
            )

            Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 30.dp)) {
                MovingTargetExample(clock)
            }

            Transition(
                definition = transDef,
                initState = "start",
                toState = "end"
            ) { state ->
                Canvas(Modifier.preferredSize(600.dp, 400.dp)) {
                    val rectSize = size * 0.2f
                    drawRect(Color(1.0f, 0f, 0f, state[alphaKey]), size = rectSize)

                    drawRect(
                        Color(0f, 0f, 1f, state[alphaKey]),
                        topLeft = Offset(state[offset1] * size.width, 0.0f),
                        size = rectSize
                    )

                    drawRect(
                        Color(0f, 1f, 1f, state[alphaKey]),
                        topLeft = Offset(state[offset2] * size.width, 0.0f),
                        size = rectSize
                    )

                    drawRect(
                        Color(0f, 1f, 0f, state[alphaKey]),
                        topLeft = Offset(state[offset3] * size.width, 0.0f),
                        size = rectSize
                    )
                }
            }
        }
    }
}

@Composable
fun MovingTargetExample(clock: ManualAnimationClock) {
    val animValue = animatedFloat(0f)

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            animValue.snapTo(animValue.targetValue + dragDistance.x)
            return dragDistance
        }
    }

    val onPress: (PxPosition) -> Unit = { position ->
        animValue.animateTo(position.x,
            TweenBuilder<Float>().apply {
                duration = 400
            })
    }

    DrawSeekBar(
        Modifier
            .rawDragGestureFilter(dragObserver)
            .pressIndicatorGestureFilter(onStart = onPress),
        animValue.value,
        clock
    )
}

@Composable
fun DrawSeekBar(modifier: Modifier = Modifier, x: Float, clock: ManualAnimationClock) {
    Canvas(modifier.fillMaxWidth().preferredHeight(60.dp)) {
        val xConstraint = x.coerceIn(0f, size.width)
        clock.clockTimeMillis = (400 * (x / size.width)).toLong().coerceIn(0, 399)
        // draw bar
        val barHeight = 10.0f
        val offset = Offset(0.0f, center.dy - 5)
        drawRect(
            Color.Gray,
            topLeft = offset,
            size = Size(size.width, barHeight)
        )
        drawRect(
            Color.Magenta,
            topLeft = offset,
            size = Size(xConstraint, barHeight)
        )

        // draw ticker
        drawCircle(
            Color.Magenta,
            center = Offset(xConstraint, center.dy),
            radius = 40f
        )
    }
}

private val alphaKey = FloatPropKey()
private val offset1 = FloatPropKey()
private val offset2 = FloatPropKey()
private val offset3 = FloatPropKey()

private val transDef = transitionDefinition {

    state("start") {
        this[alphaKey] = 1f
        this[offset1] = 0f
        this[offset2] = 0f
        this[offset3] = 0f
    }

    state("end") {
        this[alphaKey] = 0.2f
        this[offset1] = 0.26f
        this[offset2] = 0.53f
        this[offset3] = 0.8f
    }

    transition {
        alphaKey using tween {
            easing = FastOutSlowInEasing
            duration = 400
        }
        offset1 using tween {
            easing = FastOutSlowInEasing
            duration = 400
        }
        offset2 using tween {
            easing = FastOutSlowInEasing
            duration = 400
        }
        offset3 using tween {
            easing = FastOutSlowInEasing
            duration = 400
        }
    }
}
