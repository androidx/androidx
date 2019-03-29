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
import androidx.ui.core.Constraints
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Layout
import androidx.ui.core.PxSize
import androidx.ui.core.adapter.Draw
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.StrokeCap
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.unaryPlus

/**
 * A Switch is a two state toggleable component that provides on/off like options
 *
 * if [onChecked] callback is represented, tapping this component will trigger callback with
 *  value opposite to what [checked] is.
 * If [onChecked] is not provided, this components will show
 * the current [checked] state and remains not clickable.
 *
 * @param checked whether or not this components is checked
 * @param onChecked callback to be invoked with opposite to [checked] value
 * when component is clicked
 * @param color optional active color for Switch,
 * by default [androidx.ui.material.MaterialColors.primary] will be used
 */
@Composable
fun Switch(
    checked: Boolean,
    onChecked: ((Boolean) -> Unit)? = null,
    color: Color? = null
) {
    val value = if (checked) ToggleableState.Checked else ToggleableState.Unchecked
    val onToggle = onChecked?.let { { it(!checked) } }
    <Toggleable value onToggle>
        <Layout layoutBlock = { measurables, constraints ->
            val height =
                MinHeight.toIntPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            val width =
                MinWidth.toIntPx().coerceIn(constraints.minWidth, constraints.maxWidth)
            val ps = measurables.map { m ->
                m.measure(Constraints.tightConstraints(width, height))
            }
            layout(width, height) { ps.forEach { it.place(0.ipx, 0.ipx) } }
        }>
            val colors = +ambient(Colors)
            <DrawSwitch checked color />
        </Layout>
    </Toggleable>
}

@Composable
private fun DrawSwitch(checked: Boolean, color: Color? = null) {
    val colors = +ambient(Colors)
    val transDef = +memo(color) {
        val activeColor = color ?: colors.primary
        generateTransitionDefinition(activeColor)
    }
    val trackColor = if (checked) color ?: colors.primary else UncheckedTrackColor
    <DrawTrack color=trackColor />
    <Transition definition=transDef toState=checked> state ->
        <DrawThumb
            color=state[ThumbColorProp]
            relativePosition=state[RelativeThumbTranslationProp] />
    </Transition>
}

@Composable
private fun DrawTrack(color: Color) {
    <Draw> canvas, parentSize ->
        drawTrack(canvas, parentSize, color)
    </Draw>
}

@Composable
private fun DrawThumb(relativePosition: Float, color: Color) {
    <Draw> canvas, parentSize ->
        drawThumb(canvas, parentSize, relativePosition, color)
    </Draw>
}

private fun DensityReceiver.drawTrack(
    canvas: Canvas,
    parentSize: PxSize,
    color: Color
) {
    val paint = Paint()

    paint.isAntiAlias = true
    paint.color = color
        .withAlpha(TrackAlpha)
    paint.strokeCap = StrokeCap.round
    paint.strokeWidth = TrackHeight.toPx().value

    // TODO(malkov): currently Switch gravity is always CENTER but we need to be flexible
    val centerHeight = parentSize.height.value / 2
    val centerWidth = parentSize.width.value / 2

    val startW = centerWidth - TrackWidth.toPx().value / 2
    val endW = centerWidth + TrackWidth.toPx().value / 2

    canvas.drawLine(Offset(startW, centerHeight), Offset(endW, centerHeight), paint)
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

    // currently I assume that layout gravity of Switch is always CENTER
    val centerHeight = parentSize.height.value / 2
    val centerWidth = parentSize.width.value / 2

    val thumbTrackWidth = TrackWidth + PointRadius
    val thumbStartPoint = centerWidth - thumbTrackWidth.toPx().value / 2

    val start = thumbStartPoint + thumbTrackWidth.toPx().value * relativePosition
    canvas.drawCircle(Offset(start, centerHeight), PointRadius.toPx().value, paint)
}

private val RelativeThumbTranslationProp = FloatPropKey()
private val ThumbColorProp = ColorPropKey()
private val SwitchAnimationDuration = 100

private fun generateTransitionDefinition(activeColor: Color) = transitionDefinition {
    fun <T> TransitionSpec.switchTween() = tween<T> {
        duration = SwitchAnimationDuration
    }

    state(false) {
        this[RelativeThumbTranslationProp] = 0f
        this[ThumbColorProp] = UncheckedThumbColor
    }
    state(true) {
        this[RelativeThumbTranslationProp] = 1f
        this[ThumbColorProp] = activeColor
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

// TODO(malkov): see how it goes and maybe move it to styles or cross-widget defaults
private val UncheckedThumbColor = Color(0xFFEBEBEB.toInt())
private val UncheckedTrackColor = Color(0xFF7D7D7D.toInt())
private val TrackAlpha = 75

// TODO(malkov): random numbers which produce the same switch as android.widget.Switch
private val MinWidth = 47.5.dp
private val MinHeight = 27.25.dp

private val TrackHeight = 14.dp
private val TrackWidth = 11.dp
private val PointRadius = 10.dp