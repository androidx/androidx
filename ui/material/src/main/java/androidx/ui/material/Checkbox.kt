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
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.Transition
import androidx.ui.core.Draw
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.shrink
import androidx.ui.engine.geometry.withRadius
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.Padding
import androidx.ui.layout.Wrap
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.StrokeCap

/**
 * A TriStateCheckbox is a toggleable component that provides
 * checked / unchecked / indeterminate options.
 * <p>
 * A TriStateCheckbox should be used when there are
 * dependent checkboxes associated to this component and those can have different values.
 *
 * Example:
 *     Column {
 *         // define dependent checkboxes states
 *         val (state, onStateChange) = +state { true }
 *         val (state2, onStateChange2) = +state { true }
 *
 *         // TriStateCheckbox state reflects state of dependent checkboxes
 *         val parentState = +memo(state, state2) {
 *             if (state && state2) ToggleableState.Checked
 *             else if (!state && !state2) ToggleableState.Unchecked
 *             else ToggleableState.Indeterminate
 *         }
 *         // click on TriStateCheckbox can set state for dependent checkboxes
 *         val onParentClick = {
 *             val s = parentState != Checked
 *             onStateChange(s)
 *             onStateChange2(s)
 *         }
 *
 *         TriStateCheckbox(value = parentState, onClick = onParentClick)
 *         Padding(left = 10.dp) {
 *             Checkbox(state, onStateChange)
 *             Checkbox(state2, onStateChange2)
 *         }
 *     }
 *
 * @see [Checkbox] if you want a simple component that represents Boolean state
 *
 * @param value whether TriStateCheckbox is checked, unchecked or in indeterminate state
 * @param onClick callback to be invoked when checkbox is being clicked,
 * therefore the change of ToggleableState state is requested.
 * If [null], TriStateCheckbox appears in the [value] state and remains disabled
 * @param color custom color for checkbox. By default [MaterialColors.secondary] will be used
 */
@Composable
fun TriStateCheckbox(
    value: ToggleableState,
    onClick: (() -> Unit)?,
    color: Color = +themeColor { secondary }
) {
    Wrap {
        Ripple(bounded = false) {
            Toggleable(value = value, onToggle = onClick) {
                Padding(padding = CheckboxDefaultPadding) {
                    Container(width = CheckboxSize, height = CheckboxSize) {
                        DrawCheckbox(value = value, activeColor = color)
                    }
                }
            }
        }
    }
}

/**
 * A component that represents only two states (checked / unchecked).
 *
 * Example:
 *     val checkedState = +state { true }
 *     Checkbox(
 *         checked = checkedState.value,
 *         onCheckedChange = { checkedState.value = it },
 *         color = customColor
 *     )
 *
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 *
 * @param checked whether Checkbox is checked or unchecked
 * @param onCheckedChange callback to be invoked when checkbox is being clicked,
 * therefore the change of checked state in requested.
 * If [null], Checkbox will appears in the [checked] state and remains disabled
 * @param color custom color for checkbox. By default [MaterialColors.secondary] will be used
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    color: Color = +themeColor { secondary }
) {
    TriStateCheckbox(
        value = ToggleableState(checked),
        onClick = onCheckedChange?.let { { it(!checked) } },
        color = color
    )
}

@Composable
private fun DrawCheckbox(value: ToggleableState, activeColor: Color) {
    val unselectedColor = (+themeColor { onSurface }).copy(alpha = UncheckedBoxOppacity)
    val definition = +memo(activeColor, unselectedColor) {
        generateTransitionDefinition(activeColor, unselectedColor)
    }
    Transition(definition = definition, toState = value) { state ->
        DrawBox(
            color = state[BoxColorProp],
            innerRadiusFraction = state[InnerRadiusFractionProp]
        )
        DrawCheck(
            checkFraction = state[CheckFractionProp],
            crossCenterGravitation = state[CenterGravitationForCheck]
        )
    }
}

@Composable
private fun DrawBox(color: Color, innerRadiusFraction: Float) {
    Draw { canvas, parentSize ->
        val paint = Paint()
        paint.strokeWidth = StrokeWidth.toPx().value
        paint.isAntiAlias = true
        paint.color = color

        val checkboxSize = parentSize.width.value

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
    }
}

@Composable
private fun DrawCheck(
    checkFraction: Float,
    crossCenterGravitation: Float
) {
    Draw { canvas, parentSize ->
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = PaintingStyle.stroke
        paint.strokeCap = StrokeCap.square
        paint.strokeWidth = StrokeWidth.toPx().value
        paint.color = CheckStrokeDefaultColor

        val width = parentSize.width.value

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
    }
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

private fun generateTransitionDefinition(color: Color, unselectedColor: Color) =
    transitionDefinition {
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
            this[BoxColorProp] = unselectedColor
        }
        state(ToggleableState.Indeterminate) {
            this[CheckFractionProp] = 1f
            this[InnerRadiusFractionProp] = 1f
            this[CenterGravitationForCheck] = 1f
            this[BoxColorProp] = color
        }
        transition(fromState = ToggleableState.Unchecked, toState = ToggleableState.Checked) {
            boxTransitionFromUnchecked()
            CenterGravitationForCheck using snap()
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

private fun TransitionSpec<ToggleableState>.boxTransitionFromUnchecked() {
    BoxColorProp using snap()
    InnerRadiusFractionProp using tween {
        duration = BoxAnimationDuration
    }
    CheckFractionProp using tween {
        duration = CheckStrokeAnimationDuration
        delay = BoxAnimationDuration
    }
}

private fun TransitionSpec<ToggleableState>.boxTransitionToUnchecked() {
    BoxColorProp using snap()
    InnerRadiusFractionProp using tween {
        duration = BoxAnimationDuration
        delay = CheckStrokeAnimationDuration
    }
    CheckFractionProp using tween {
        duration = CheckStrokeAnimationDuration
    }
}

private val CheckboxDefaultPadding = 2.dp
private val CheckboxSize = 20.dp
private val StrokeWidth = 2.dp
private val RadiusSize = 2.dp

private val UncheckedBoxOppacity = 0.6f
private val CheckStrokeDefaultColor = Color(0xFFFFFFFF.toInt())