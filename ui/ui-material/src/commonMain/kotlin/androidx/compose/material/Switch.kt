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

package androidx.compose.material

import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.state
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.WithConstraints
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.indication
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.StackScope
import androidx.compose.foundation.layout.offsetPx
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.SwitchDefaults.disabledUncheckedThumbColor
import androidx.compose.material.SwitchDefaults.disabledUncheckedTrackColor
import androidx.compose.material.SwitchDefaults.makeDisabledCheckedThumbColor
import androidx.compose.material.SwitchDefaults.makeDisabledCheckedTrackColor
import androidx.compose.material.SwitchDefaults.uncheckedThumbColor
import androidx.compose.material.SwitchDefaults.uncheckedTrackColor
import androidx.compose.material.internal.stateDraggable
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.ui.unit.dp

/**
 * A Switch is a two state toggleable component that provides on/off like options
 *
 * @sample androidx.compose.material.samples.SwitchSample
 *
 * @param checked whether or not this components is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.
 * @param modifier Modifier to be applied to the switch layout
 * @param enabled whether or not components is enabled and can be clicked to request state change
 * @param color main color of the track and trumb when the Switch is checked
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colors.secondaryVariant
) {
    val minBound = 0f
    val maxBound = with(DensityAmbient.current) { ThumbPathLength.toPx() }
    val thumbPosition = state { if (checked) maxBound else minBound }
    val interactionState = remember { InteractionState() }
    WithConstraints {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        Stack(
            modifier
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    interactionState = interactionState,
                    indication = null
                )
                .stateDraggable(
                    state = checked,
                    onStateChange = onCheckedChange,
                    anchorsToState = listOf(minBound to false, maxBound to true),
                    animationSpec = AnimationSpec,
                    orientation = Orientation.Horizontal,
                    reverseDirection = isRtl,
                    minValue = minBound,
                    maxValue = maxBound,
                    enabled = enabled,
                    interactionState = interactionState,
                    onNewValue = { thumbPosition.value = it }
                )
                .padding(DefaultSwitchPadding)
        ) {
            SwitchImpl(
                checked = checked,
                enabled = enabled,
                checkedColor = color,
                thumbValue = thumbPosition,
                interactionState = interactionState
            )
        }
    }
}

@Composable
private fun StackScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    checkedColor: Color,
    thumbValue: State<Float>,
    interactionState: InteractionState
) {
    val hasInteraction =
        Interaction.Pressed in interactionState || Interaction.Dragged in interactionState
    val elevation =
        if (hasInteraction) {
            SwitchDefaults.ThumbPressedElevation
        } else {
            SwitchDefaults.ThumbDefaultElevation
        }
    val trackColor = SwitchDefaults.resolveTrackColor(checked, enabled, checkedColor)
    val thumbColor = SwitchDefaults.resolveThumbColor(checked, enabled, checkedColor)
    Canvas(Modifier.gravity(Alignment.Center).preferredSize(SwitchWidth, SwitchHeight)) {
        drawTrack(trackColor, TrackWidth.toPx(), TrackStrokeWidth.toPx())
    }
    Surface(
        shape = CircleShape,
        color = thumbColor,
        elevation = elevation,
        modifier = Modifier
            .gravity(Alignment.CenterStart)
            .offsetPx(x = thumbValue)
            .indication(
                interactionState = interactionState,
                indication = RippleIndication(radius = ThumbRippleRadius, bounded = false)
            )
            .preferredSize(ThumbDiameter)
    ) {}
}

private fun DrawScope.drawTrack(trackColor: Color, trackWidth: Float, strokeWidth: Float) {
    val strokeRadius = strokeWidth / 2
    drawLine(
        trackColor,
        Offset(strokeRadius, center.y),
        Offset(trackWidth - strokeRadius, center.y),
        strokeWidth,
        StrokeCap.round
    )
}

internal val TrackWidth = 34.dp
internal val TrackStrokeWidth = 14.dp
internal val ThumbDiameter = 20.dp

private val ThumbRippleRadius = 24.dp

private val DefaultSwitchPadding = 2.dp
private val SwitchWidth = TrackWidth
private val SwitchHeight = ThumbDiameter
private val ThumbPathLength = TrackWidth - ThumbDiameter

private val AnimationSpec = TweenSpec<Float>(durationMillis = 100)

internal object SwitchDefaults {

    const val CheckedTrackOpacity = 0.54f
    const val UncheckedTrackOpacity = 0.38f

    val ThumbDefaultElevation = 1.dp
    val ThumbPressedElevation = 6.dp

    @Composable
    private val uncheckedTrackColor
        get() = MaterialTheme.colors.onSurface

    @Composable
    private fun makeDisabledCheckedTrackColor(checkedColor: Color) = EmphasisAmbient.current
        .disabled
        .applyEmphasis(checkedColor)
        .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val disabledUncheckedTrackColor
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.onSurface)
            .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val uncheckedThumbColor
        get() = MaterialTheme.colors.surface

    @Composable
    private fun makeDisabledCheckedThumbColor(checkedColor: Color) = EmphasisAmbient.current
        .disabled
        .applyEmphasis(checkedColor)
        .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val disabledUncheckedThumbColor
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.surface)
            .compositeOver(MaterialTheme.colors.surface)

    @Composable
    internal fun resolveTrackColor(checked: Boolean, enabled: Boolean, checkedColor: Color): Color {
        return if (checked) {
            val color = if (enabled) checkedColor else makeDisabledCheckedTrackColor(checkedColor)
            color.copy(alpha = CheckedTrackOpacity)
        } else {
            val color = if (enabled) uncheckedTrackColor else disabledUncheckedTrackColor
            color.copy(alpha = UncheckedTrackOpacity)
        }
    }

    @Composable
    internal fun resolveThumbColor(checked: Boolean, enabled: Boolean, checkedColor: Color): Color {
        return if (checked) {
            if (enabled) checkedColor else makeDisabledCheckedThumbColor(checkedColor)
        } else {
            if (enabled) uncheckedThumbColor else disabledUncheckedThumbColor
        }
    }
}