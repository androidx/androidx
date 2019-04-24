/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.material

import androidx.animation.ColorPropKey
import androidx.animation.FloatPropKey
import androidx.animation.TransitionSpec
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.baseui.selection.Toggleable
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.shrink
import androidx.ui.engine.geometry.withRadius
import androidx.ui.layout.Padding
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.StrokeCap
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.unaryPlus

// TODO(malkov): think about how to abstract it better
/**
 * Function to resolve parent checkbox state based on children checkboxes' state
 * Use it when you have a hierarchy relationship between checkboxes in different levels
 *
 * @param childrenStates states of children checkboxes this parent is responsible for
 */
fun parentCheckboxState(vararg childrenStates: ToggleableState): ToggleableState {
    return if (childrenStates.all { it == ToggleableState.Checked }) ToggleableState.Checked
    else if (childrenStates.all { it == ToggleableState.Unchecked }) ToggleableState.Unchecked
    else ToggleableState.Indeterminate
}

/**
 * A checkbox is a toggleable component that provides checked / unchecked / indeterminate options
 * A checkbox can only reach an indeterminate state when it has
 * child checkboxes with different state values.
 *
 * @see [parentCheckboxState] to create parent checkbox state based on children's state
 *
 * @param value whether checkbox is checked, unchecked or in indeterminate state
 * @param onToggle callback to be invoked when checkbox is being toggled
 * @param color custom color for checkbox. By default [MaterialColors.secondary] will be used
 * @param testTag temporary test tag for testing purposes
 */
@Composable
fun Checkbox(
    value: ToggleableState,
    onToggle: (() -> Unit)? = null,
    color: Color? = null
) {
    <Toggleable value onToggle>
        <Padding padding=CheckboxDefaultPadding>
            <Layout children=@Composable { <DrawCheckbox value color /> }> _, constraints ->
                val checkboxSizePx = CheckboxSize.toIntPx()
                val height = checkboxSizePx.coerceIn(constraints.minHeight, constraints.maxHeight)
                val width = checkboxSizePx.coerceIn(constraints.minWidth, constraints.maxWidth)
                layout(width, height) {}
            </Layout>
        </Padding>
    </Toggleable>
}

@Composable
private fun DrawCheckbox(value: ToggleableState, color: Color?) {
    val activeColor = +color.orFromTheme { secondary }
    val definition = +memo(activeColor) {
        generateTransitionDefinition(activeColor)
    }
    <Transition definition toState=value> state ->
        <DrawBox
            color=state[BoxColorProp]
            innerRadiusFraction=state[InnerRadiusFractionProp] />
        <DrawCheck
            checkFraction=state[CheckFractionProp]
            crossCenterGravitation=state[CenterGravitationForCheck] />
    </Transition>
}

@Composable
private fun DrawBox(color: Color, innerRadiusFraction: Float) {
    <Draw> canvas, _ ->
        val paint = Paint()
        paint.strokeWidth = StrokeWidth.toPx().value
        paint.isAntiAlias = true
        paint.color = color

        val checkboxSize = CheckboxSize.toPx().value

        val outer = RRect(
            0f,
            0f,
            checkboxSize,
            checkboxSize,
            Radius.circular(RadiusSize.toPx().value)
        )

        val shrinkTo = calcMiddleValue(
            paint.strokeWidth,
            outer.width / 2,
            innerRadiusFraction
        )
        val innerSquared = outer.shrink(shrinkTo)
        val squareMultiplier = innerRadiusFraction * innerRadiusFraction

        // TODO(malkov): this radius formula is not in material spec
        val inner = innerSquared
            .withRadius(Radius.circular(innerSquared.width * squareMultiplier))
        canvas.drawDRRect(outer, inner, paint)
    </Draw>
}

@Composable
private fun DrawCheck(
    checkFraction: Float,
    crossCenterGravitation: Float
) {
    <Draw> canvas, _ ->
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = PaintingStyle.stroke
        paint.strokeCap = StrokeCap.square
        paint.strokeWidth = StrokeWidth.toPx().value
        paint.color = CheckStrokeDefaultColor

        val width = CheckboxSize.toPx().value

        val checkCrossX = 0.4f
        val checkCrossY = 0.7f
        val leftX = 0.2f
        val leftY = 0.5f
        val rightX = 0.8f
        val rightY = 0.3f

        val gravitatedCrossX = calcMiddleValue(checkCrossX, 0.5f, crossCenterGravitation)
        val gravitatedCrossY = calcMiddleValue(checkCrossY, 0.5f, crossCenterGravitation)

        // gravitate only Y for end to achieve center line
        val gravitatedLeftY = calcMiddleValue(leftY, 0.5f, crossCenterGravitation)
        val gravitatedRightY = calcMiddleValue(rightY, 0.5f, crossCenterGravitation)

        val crossPoint = Offset(width * gravitatedCrossX, width * gravitatedCrossY)
        val rightBranch = Offset(
            width * calcMiddleValue(gravitatedCrossX, rightX, checkFraction),
            width * calcMiddleValue(gravitatedCrossY, gravitatedRightY, checkFraction)
        )
        val leftBranch = Offset(
            width * calcMiddleValue(gravitatedCrossX, leftX, checkFraction),
            width * calcMiddleValue(gravitatedCrossY, gravitatedLeftY, checkFraction)
        )
        canvas.drawLine(crossPoint, leftBranch, paint)
        canvas.drawLine(crossPoint, rightBranch, paint)
    </Draw>
}

private fun calcMiddleValue(start: Float, finish: Float, fraction: Float): Float {
    return start * (1 - fraction) + finish * fraction
}

// all float props are fraction now [0f .. 1f] as it seems convenient
private val InnerRadiusFractionProp = FloatPropKey()
private val CheckFractionProp = FloatPropKey()
private val CenterGravitationForCheck = FloatPropKey()
private val BoxColorProp = ColorPropKey()

private val BoxAnimationDuration = 100
private val CheckStrokeAnimationDuration = 100

private fun generateTransitionDefinition(color: Color) = transitionDefinition {
    state(ToggleableState.Checked) {
        this[CheckFractionProp] = 1f
        this[InnerRadiusFractionProp] = 1f
        this[CenterGravitationForCheck] = 0f
        this[BoxColorProp] = color
    }
    state(ToggleableState.Unchecked) {
        this[CheckFractionProp] = 0f
        this[InnerRadiusFractionProp] = 0f
        this[CenterGravitationForCheck] = 1f
        this[BoxColorProp] = UncheckedBoxColor
    }
    state(ToggleableState.Indeterminate) {
        this[CheckFractionProp] = 1f
        this[InnerRadiusFractionProp] = 1f
        this[CenterGravitationForCheck] = 1f
        this[BoxColorProp] = color
    }
    transition(fromState = ToggleableState.Unchecked, toState = ToggleableState.Checked) {
        boxTransitionFromUnchecked()
        CenterGravitationForCheck using tween {
            duration = 0
        }
    }
    transition(fromState = ToggleableState.Checked, toState = ToggleableState.Unchecked) {
        boxTransitionToUnchecked()
        CenterGravitationForCheck using tween {
            duration = CheckStrokeAnimationDuration
        }
    }
    transition(fromState = ToggleableState.Checked, toState = ToggleableState.Indeterminate) {
        CenterGravitationForCheck using tween {
            duration = CheckStrokeAnimationDuration
        }
    }
    transition(fromState = ToggleableState.Indeterminate, toState = ToggleableState.Checked) {
        CenterGravitationForCheck using tween {
            duration = CheckStrokeAnimationDuration
        }
    }
    transition(fromState = ToggleableState.Indeterminate, toState = ToggleableState.Unchecked) {
        boxTransitionToUnchecked()
    }
    transition(fromState = ToggleableState.Unchecked, toState = ToggleableState.Indeterminate) {
        boxTransitionFromUnchecked()
    }
}

private fun TransitionSpec.boxTransitionFromUnchecked() {
    BoxColorProp using tween {
        duration = 0
    }
    InnerRadiusFractionProp using tween {
        duration = BoxAnimationDuration
    }
    CheckFractionProp using tween {
        duration = CheckStrokeAnimationDuration
        delay = BoxAnimationDuration.toLong()
    }
}

private fun TransitionSpec.boxTransitionToUnchecked() {
    BoxColorProp using tween {
        duration = 0
    }
    InnerRadiusFractionProp using tween {
        duration = BoxAnimationDuration
        delay = CheckStrokeAnimationDuration.toLong()
    }
    CheckFractionProp using tween {
        duration = CheckStrokeAnimationDuration
    }
}

private val CheckboxDefaultPadding = 2.dp
private val CheckboxSize = 20.dp
private val StrokeWidth = 2.dp
private val RadiusSize = 2.dp

private val UncheckedBoxColor = Color(0xFF7D7D7D.toInt())
private val CheckStrokeDefaultColor = Color(0xFFFFFFFF.toInt())