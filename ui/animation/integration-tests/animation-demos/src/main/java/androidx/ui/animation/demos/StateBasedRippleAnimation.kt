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

import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import androidx.animation.FloatPropKey
import androidx.animation.InterruptionHandling
import androidx.animation.TransitionDefinition
import androidx.animation.TransitionState
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.ambientDensity
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.Container

class StateBasedRippleAnimation : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StateBasedRippleDemo() }
    }
}

@Composable
fun StateBasedRippleDemo() {
    CraneWrapper {
        Container(expanded = true) {
            RippleRect()
        }
    }
}

@Composable
fun RippleRect() {
    val radius = withDensity(+ambientDensity()) { TargetRadius.toPx() }
    val toState = +state { ButtonStatus.Initial }
    val rippleTransDef = +memo { createTransDef(radius.value) }
    val onPress: (PxPosition) -> Unit = { position ->
        down.x = position.x.value
        down.y = position.y.value
        toState.value = ButtonStatus.Pressed
    }

    val onRelease: () -> Unit = {
        toState.value = ButtonStatus.Released
    }
    PressGestureDetector(onPress = onPress, onRelease = onRelease) {
        Container(expanded = true) {
            Transition(definition = rippleTransDef, toState = toState.value) { state ->
                RippleRectFromState(state = state)
            }
        }
    }
}

@Composable
fun RippleRectFromState(state: TransitionState) {

    // TODO: file bug for when "down" is not a file level val, it's not memoized correctly
    val x = down.x
    val y = down.y

    val paint =
        Paint().apply {
            color = Color(
                alpha = (state[androidx.ui.animation.demos.alpha] * 255).toInt(),
                red = 0,
                green = 235,
                blue = 224
            )
        }

    val radius = state[radius]

    Draw { canvas, _ ->
        canvas.drawCircle(Offset(x, y), radius, paint)
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
