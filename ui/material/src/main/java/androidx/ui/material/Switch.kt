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
import androidx.animation.FastOutSlowInEasing
import androidx.animation.FloatPropKey
import androidx.animation.TransitionSpec
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.Transition
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Draw
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.Padding
import androidx.ui.layout.Wrap
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.StrokeCap

/**
 * A Switch is a two state toggleable component that provides on/off like options
 *
 * @param checked whether or not this components is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.
 * if [null], Switch appears in [checked] state and remains disabled
 * @param color optional active color for Switch,
 * by default [MaterialColors.secondaryVariant] will be used
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    color: Color = +themeColor { secondaryVariant }
) {
    val value = if (checked) ToggleableState.Checked else ToggleableState.Unchecked
    Wrap {
        Ripple(bounded = false) {
            Toggleable(value = value, onToggle = onCheckedChange?.let { { it(!checked) } }) {
                Padding(padding = DefaultSwitchPadding) {
                    Container(width = SwitchWidth, height = SwitchHeight) {
                        DrawSwitch(checked = checked, checkedThumbColor = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawSwitch(checked: Boolean, checkedThumbColor: Color) {
    val uncheckedThumbColor = +themeColor { surface }
    val transDef = +memo(checkedThumbColor, uncheckedThumbColor) {
        generateTransitionDefinition(checkedThumbColor, uncheckedThumbColor)
    }
    val trackColor = if (checked) {
        checkedThumbColor.copy(alpha = CheckedTrackOpacity)
    } else {
        (+themeColor { onSurface }).copy(alpha = UncheckedTrackOpacity)
    }
    DrawTrack(color = trackColor)
    Transition(definition = transDef, toState = checked) { state ->
        DrawThumb(
            color = state[ThumbColorProp],
            relativePosition = state[RelativeThumbTranslationProp]
        )
    }
}

@Composable
private fun DrawTrack(color: Color) {
    Draw { canvas, parentSize ->
        drawTrack(canvas, parentSize, color)
    }
}

@Composable
private fun DrawThumb(relativePosition: Float, color: Color) {
    Draw { canvas, parentSize ->
        drawThumb(canvas, parentSize, relativePosition, color)
    }
}

private fun DensityReceiver.drawTrack(
    canvas: Canvas,
    parentSize: PxSize,
    color: Color
) {
    val paint = Paint()
    paint.isAntiAlias = true
    paint.color = color
    paint.strokeCap = StrokeCap.round
    paint.strokeWidth = TrackStrokeWidth.toPx().value

    val strokeRadius = TrackStrokeWidth / 2
    val centerHeight = parentSize.height / 2

    canvas.drawLine(
        Offset(strokeRadius.toPx().value, centerHeight.value),
        Offset((TrackWidth - strokeRadius).toPx().value, centerHeight.value),
        paint
    )
}

private fun DensityReceiver.drawThumb(
    canvas: Canvas,
    parentSize: PxSize,
    relativePosition: Float,
    color: Color
) {
    val paint = Paint()
    paint.isAntiAlias = true
    paint.color = color

    val centerHeight = parentSize.height / 2
    val thumbRadius = ThumbDiameter / 2
    val thumbPathWidth = TrackWidth - ThumbDiameter

    val start = thumbRadius + thumbPathWidth * relativePosition
    canvas.drawCircle(
        Offset(start.toPx().value, centerHeight.value),
        // TODO(malkov): wierd +1 but necessary in order to properly cover track. Investigate
        thumbRadius.toPx().value + 1,
        paint
    )
}

private val RelativeThumbTranslationProp = FloatPropKey()
private val ThumbColorProp = ColorPropKey()
private const val SwitchAnimationDuration = 100

private fun generateTransitionDefinition(checkedColor: Color, uncheckedColor: Color) =
    transitionDefinition {
        fun <T> TransitionSpec<Boolean>.switchTween() = tween<T> {
            duration = SwitchAnimationDuration
            easing = FastOutSlowInEasing
        }

        state(false) {
            this[RelativeThumbTranslationProp] = 0f
            this[ThumbColorProp] = uncheckedColor
        }
        state(true) {
            this[RelativeThumbTranslationProp] = 1f
            this[ThumbColorProp] = checkedColor
        }
        transition(fromState = false, toState = true) {
            RelativeThumbTranslationProp using switchTween()
            ThumbColorProp using switchTween()
        }
        transition(fromState = true, toState = false) {
            RelativeThumbTranslationProp using switchTween()
            ThumbColorProp using switchTween()
        }
    }

private val CheckedTrackOpacity = 0.54f
private val UncheckedTrackOpacity = 0.38f

private val TrackWidth = 34.dp
private val TrackStrokeWidth = 14.dp

private val ThumbDiameter = 20.dp

// TODO(malkov): clarify this padding for Switch
private val DefaultSwitchPadding = 2.dp
private val SwitchWidth = TrackWidth
private val SwitchHeight = ThumbDiameter