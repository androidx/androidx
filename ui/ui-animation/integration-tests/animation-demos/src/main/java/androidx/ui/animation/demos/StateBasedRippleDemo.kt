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

import android.graphics.PointF
import androidx.animation.FloatPropKey
import androidx.animation.InterruptionHandling
import androidx.animation.TransitionDefinition
import androidx.animation.TransitionState
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

@Composable
fun StateBasedRippleDemo() {
    Box(Modifier.fillMaxSize()) {
        RippleRect()
    }
}

@Composable
private fun RippleRect() {
    val radius = with(DensityAmbient.current) { TargetRadius.toPx() }
    val toState = state { ButtonStatus.Initial }
    val rippleTransDef = remember { createTransDef(radius) }
    val onPress: (PxPosition) -> Unit = { position ->
        down.x = position.x
        down.y = position.y
        toState.value = ButtonStatus.Pressed
    }

    val onRelease: () -> Unit = {
        toState.value = ButtonStatus.Released
    }
    Transition(definition = rippleTransDef, toState = toState.value) { state ->
        RippleRectFromState(
            Modifier.pressIndicatorGestureFilter(onStart = onPress, onStop = onRelease), state =
            state
        )
    }
}

@Composable
private fun RippleRectFromState(modifier: Modifier = Modifier, state: TransitionState) {
    Canvas(modifier.fillMaxSize()) {
        // TODO: file bug for when "down" is not a file level val, it's not memoized correctly
        drawCircle(
            Color(
                alpha = (state[alpha] * 255).toInt(),
                red = 0,
                green = 235,
                blue = 224
            ),
            center = Offset(down.x, down.y),
            radius = state[radius]
        )
    }
}

private enum class ButtonStatus {
    Initial,
    Pressed,
    Released
}

private val TargetRadius = 200.dp

private val down = PointF(0f, 0f)

private val alpha = FloatPropKey()
private val radius = FloatPropKey()

private fun createTransDef(targetRadius: Float): TransitionDefinition<ButtonStatus> {
    return transitionDefinition {
        state(ButtonStatus.Initial) {
            this[alpha] = 0f
            this[radius] = targetRadius * 0.3f
        }
        state(ButtonStatus.Pressed) {
            this[alpha] = 0.2f
            this[radius] = targetRadius + 15f
        }
        state(ButtonStatus.Released) {
            this[alpha] = 0f
            this[radius] = targetRadius + 15f
        }

        // Grow the ripple
        transition(ButtonStatus.Initial to ButtonStatus.Pressed) {
            alpha using keyframes {
                duration = 225
                0f at 0 // optional
                0.2f at 75
                0.2f at 225 // optional
            }
            radius using tween {
                duration = 225
            }
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }

        // Fade out the ripple
        transition(ButtonStatus.Pressed to ButtonStatus.Released) {
            alpha using tween {
                duration = 200
            }
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
            // switch back to Initial to prepare for the next ripple cycle
            nextState = ButtonStatus.Initial
        }

        // State switch without animation
        snapTransition(ButtonStatus.Released to ButtonStatus.Initial)
    }
}
