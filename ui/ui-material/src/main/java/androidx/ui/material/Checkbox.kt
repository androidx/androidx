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

import androidx.animation.FloatPropKey
import androidx.animation.TransitionSpec
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.TriStateToggleable
import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Radius
import androidx.ui.geometry.Size
import androidx.ui.geometry.outerRect
import androidx.ui.geometry.shrink
import androidx.ui.graphics.ClipOp
import androidx.ui.graphics.Color
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.material.ripple.ripple
import androidx.ui.semantics.Semantics
import androidx.ui.unit.dp

/**
 * A component that represents two states (checked / unchecked).
 *
 * @sample androidx.ui.material.samples.CheckboxSample
 *
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 *
 * @param checked whether Checkbox is checked or unchecked
 * @param onCheckedChange callback to be invoked when checkbox is being clicked,
 * therefore the change of checked state in requested.
 * @param enabled enabled whether or not this [Checkbox] will handle input events and appear
 * enabled for semantics purposes
 * @param modifier Modifier to be applied to the layout of the checkbox
 * @param color custom color for checkbox
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.secondary
) {
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        color = color,
        modifier = modifier
    )
}

/**
 * A TriStateCheckbox is a toggleable component that provides
 * checked / unchecked / indeterminate options.
 * <p>
 * A TriStateCheckbox should be used when there are
 * dependent checkboxes associated to this component and those can have different values.
 *
 * @sample androidx.ui.material.samples.TriStateCheckboxSample
 *
 * @see [Checkbox] if you want a simple component that represents Boolean state
 *
 * @param state whether TriStateCheckbox is checked, unchecked or in indeterminate state
 * @param onClick callback to be invoked when checkbox is being clicked,
 * therefore the change of ToggleableState state is requested.
 * @param enabled enabled whether or not this [TriStateCheckbox] will handle input events and
 * appear enabled for semantics purposes
 * @param modifier Modifier to be applied to the layout of the checkbox
 * @param color custom color for checkbox
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.secondary
) {
    Semantics(container = true, mergeAllDescendants = true) {
        Box(modifier, gravity = ContentGravity.Center) {
            TriStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.ripple(bounded = false, enabled = enabled)
            ) {
                DrawCheckbox(
                    value = state,
                    activeColor = color,
                    modifier = CheckboxDefaultPadding
                )
            }
        }
    }
}

@Composable
private fun DrawCheckbox(value: ToggleableState, activeColor: Color, modifier: Modifier) {
    val unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = UncheckedBoxOpacity)
    val definition = remember(activeColor, unselectedColor) {
        generateTransitionDefinition(activeColor, unselectedColor)
    }
    Transition(definition = definition, toState = value) { state ->
        Canvas(modifier.preferredSize(CheckboxSize)) {
            val strokeWidthPx = StrokeWidth.toPx()
            drawBox(
                color = state[BoxColorProp],
                innerRadiusFraction = state[InnerRadiusFractionProp],
                radius = RadiusSize.toPx(),
                strokeWidth = strokeWidthPx
            )
            drawCheck(
                checkFraction = state[CheckFractionProp],
                crossCenterGravitation = state[CenterGravitationForCheck],
                strokeWidthPx = strokeWidthPx
            )
        }
    }
}

private fun DrawScope.drawBox(
    color: Color,
    innerRadiusFraction: Float,
    radius: Float,
    strokeWidth: Float
) {
    val halfStrokeWidth = strokeWidth / 2.0f
    val stroke = Stroke(strokeWidth)
    val checkboxSize = size.width

    val outer = RRect(
        halfStrokeWidth,
        halfStrokeWidth,
        checkboxSize - halfStrokeWidth,
        checkboxSize - halfStrokeWidth,
        Radius.circular(radius)
    )

    // Determine whether or not we need to offset the inset by a pixel
    // to ensure that there is no gap between the outer stroked round rect
    // and the inner rect.
    val offset = (halfStrokeWidth - halfStrokeWidth.toInt()) + 0.5f

    // TODO(malkov): this radius formula is not in material spec

    // If the inner region is to be filled such that it is larger than the outer stroke size
    // then create a difference clip to draw the stroke outside of the rectangular region
    // to be drawn within the interior rectangle. This is done to ensure that pixels do
    // not overlap which might cause unexpected blending if the target color has some
    // opacity. If the inner region is not to be drawn or will occupy a smaller width than
    // the outer stroke then just draw the outer stroke
    val innerStrokeWidth = innerRadiusFraction * checkboxSize / 2
    if (innerStrokeWidth > strokeWidth) {
        val clipRect = outer.shrink(strokeWidth / 2 - offset).outerRect()
        clipRect(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom, ClipOp.difference) {
            drawRoundRect(
                color,
                Offset(outer.left, outer.top),
                Size(outer.width, outer.height),
                radius,
                style = stroke
            )
        }

        clipRect(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom) {
            val innerHalfStrokeWidth = innerStrokeWidth / 2
            val rect = outer.shrink(innerHalfStrokeWidth - offset).outerRect()
            drawRect(
                color = color,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(innerStrokeWidth)
            )
        }
    } else {
        drawRoundRect(
            color,
            topLeft = Offset(outer.left, outer.top),
            size = Size(outer.width, outer.height),
            radiusX = radius,
            radiusY = radius,
            style = stroke
        )
    }
}

private fun DrawScope.drawCheck(
    checkFraction: Float,
    crossCenterGravitation: Float,
    strokeWidthPx: Float
) {
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.square)
    val width = size.width
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
    drawLine(CheckStrokeDefaultColor, crossPoint, leftBranch, stroke)
    drawLine(CheckStrokeDefaultColor, crossPoint, rightBranch, stroke)
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
        state(ToggleableState.On) {
            this[CheckFractionProp] = 1f
            this[InnerRadiusFractionProp] = 1f
            this[CenterGravitationForCheck] = 0f
            this[BoxColorProp] = color
        }
        state(ToggleableState.Off) {
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
        transition(fromState = ToggleableState.Off, toState = ToggleableState.On) {
            boxTransitionFromUnchecked()
            CenterGravitationForCheck using snap()
        }
        transition(fromState = ToggleableState.On, toState = ToggleableState.Off) {
            boxTransitionToUnchecked()
            CenterGravitationForCheck using tween {
                duration = CheckStrokeAnimationDuration
            }
        }
        transition(
            ToggleableState.On to ToggleableState.Indeterminate,
            ToggleableState.Indeterminate to ToggleableState.On
        ) {
            CenterGravitationForCheck using tween {
                duration = CheckStrokeAnimationDuration
            }
        }
        transition(fromState = ToggleableState.Indeterminate, toState = ToggleableState.Off) {
            boxTransitionToUnchecked()
        }
        transition(fromState = ToggleableState.Off, toState = ToggleableState.Indeterminate) {
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

private val CheckboxDefaultPadding = Modifier.padding(2.dp)
private val CheckboxSize = 20.dp
private val StrokeWidth = 2.dp
private val RadiusSize = 2.dp

private val UncheckedBoxOpacity = 0.6f
private val CheckStrokeDefaultColor = Color.White