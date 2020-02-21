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

import androidx.animation.AnimatedFloat
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.ui.core.DensityAmbient
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.CanvasScope
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.StrokeCap
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Wrap
import androidx.ui.material.internal.StateDraggable
import androidx.ui.material.ripple.Ripple
import androidx.ui.semantics.Semantics
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * A Switch is a two state toggleable component that provides on/off like options
 *
 * @sample androidx.ui.material.samples.SwitchSample
 *
 * @param checked whether or not this components is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.
 * if `null`, Switch appears in [checked] state and remains disabled
 * @param color active color for Switch,
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    color: Color = MaterialTheme.colors().secondaryVariant
) {
    Semantics(container = true, mergeAllDescendants = true) {
        Wrap {
            Ripple(bounded = false) {
                Toggleable(value = checked, onValueChange = onCheckedChange) {
                    Box(LayoutPadding(DefaultSwitchPadding)) {
                        SwitchImpl(checked, onCheckedChange, color)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchImpl(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, color: Color) {
    val minBound = 0f
    val maxBound = with(DensityAmbient.current) { ThumbPathLength.toPx().value }
    StateDraggable(
        state = checked,
        onStateChange = onCheckedChange ?: {},
        anchorsToState = listOf(minBound to false, maxBound to true),
        animationBuilder = AnimationBuilder,
        dragDirection = DragDirection.Horizontal,
        minValue = minBound,
        maxValue = maxBound
    ) { model ->
        DrawSwitch(
            checked = checked,
            checkedThumbColor = color,
            thumbValue = model
        )
    }
}

@Composable
private fun DrawSwitch(checked: Boolean, checkedThumbColor: Color, thumbValue: AnimatedFloat) {
    val thumbColor = if (checked) checkedThumbColor else MaterialTheme.colors().surface
    val trackColor = if (checked) {
        checkedThumbColor.copy(alpha = CheckedTrackOpacity)
    } else {
        MaterialTheme.colors().onSurface.copy(alpha = UncheckedTrackOpacity)
    }
    Canvas(LayoutSize(SwitchWidth, SwitchHeight)) {
        drawTrack(trackColor)
        drawThumb(thumbValue.value, thumbColor)
    }
}

private fun CanvasScope.drawTrack(trackColor: Color) {
    val paint = Paint().apply {
        isAntiAlias = true
        color = trackColor
        strokeCap = StrokeCap.round
        strokeWidth = TrackStrokeWidth.toPx().value
    }

    val strokeRadius = TrackStrokeWidth / 2
    val centerHeight = size.height / 2

    drawLine(
        Offset(strokeRadius.toPx().value, centerHeight.value),
        Offset((TrackWidth - strokeRadius).toPx().value, centerHeight.value),
        paint
    )
}

private fun CanvasScope.drawThumb(position: Float, thumbColor: Color) {
    val paint = Paint().apply {
        isAntiAlias = true
        color = thumbColor
    }
    val centerHeight = size.height / 2
    val thumbRadius = (ThumbDiameter / 2).toPx().value
    val x = position.px.value + thumbRadius

    drawCircle(Offset(x, centerHeight.value), thumbRadius, paint)
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
private val ThumbPathLength = TrackWidth - ThumbDiameter

private val AnimationBuilder = TweenBuilder<Float>().apply { duration = 100 }
