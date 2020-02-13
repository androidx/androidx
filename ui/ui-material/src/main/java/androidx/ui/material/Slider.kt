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

package androidx.ui.material

import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationEndReason
import androidx.animation.DefaultAnimationClock
import androidx.animation.TargetAnimation
import androidx.animation.TweenBuilder
import androidx.annotation.IntRange
import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.AnimatedFloatModel
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Draw
import androidx.ui.core.Modifier
import androidx.ui.core.WithConstraints
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.foundation.Box
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.animation.fling
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.PointMode
import androidx.ui.graphics.StrokeCap
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Spacer
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.accessibilityValue
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.px
import androidx.ui.unit.toRect
import androidx.ui.util.lerp
import kotlin.math.abs

/**
 * State for Slider that represents the Slider value, its bounds and optional amount of steps
 * evenly distributed across the Slider range.
 *
 * @param initial initial value for the Slider when created. If outside of range provided,
 * initial position will be coerced to this range
 * @param valueRange range of values that Slider value can take
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified
 */
class SliderPosition(
    initial: Float = 0f,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    // TODO: remove this default
    animatedClock: AnimationClockObservable = DefaultAnimationClock()
) {

    internal val startValue: Float = valueRange.start
    internal val endValue: Float = valueRange.endInclusive

    init {
        require(steps >= 0) {
            "steps should be >= 0"
        }
    }

    /**
     * Current Slider value. If set outside of range provided, value will be coerced to this range
     */
    var value: Float
        get() = scale(startPx, endPx, holder.value, startValue, endValue)
        set(value) {
            holder.snapTo(scale(startValue, endValue, value, startPx, endPx))
        }

    private var endPx = Float.MAX_VALUE
    private var startPx = Float.MIN_VALUE

    internal fun setBounds(min: Float, max: Float) {
        if (startPx == min && endPx == max) return
        val newValue = scale(startPx, endPx, holder.value, min, max)
        startPx = min
        endPx = max
        holder.setBounds(min, max)
        anchorsPx = tickFractions.map {
            lerp(
                startPx,
                endPx,
                it
            )
        }
        holder.snapTo(newValue)
    }

    internal val holder =
        AnimatedFloatModel(scale(startValue, endValue, initial, startPx, endPx), animatedClock)

    internal val tickFractions: List<Float> =
        if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }

    internal var anchorsPx: List<Float> = emptyList()
        private set
}

/**
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 *
 * Use continuous sliders allow users to make meaningful selections that don’t
 * require a specific value:
 *
 * @sample androidx.ui.material.samples.SliderSample
 *
 * You can allow the user to choose only between predefined set of values by providing
 * discrete values in [SliderPosition].
 *
 * You can do it by specifying the amount of steps between min and max values:
 *
 * @sample androidx.ui.material.samples.StepsSliderSample
 *
 * @param position [SliderPosition] object to represent value of the Slider
 * @param onValueChange lambda in which value should be updated
 * @param modifier modifiers for the Slider layout
 * @param onValueChangeEnd lambda to be invoked when value change has ended. This callback
 * shouldn't be used to update the slider value (use [onValueChange] for that), but rather to
 * know when the user has completed selecting a new value by ending a drag or a click.
 * @param color color of the Slider
 */
@Composable
fun Slider(
    position: SliderPosition,
    onValueChange: (Float) -> Unit = { position.value = it },
    modifier: Modifier = Modifier.None,
    onValueChangeEnd: () -> Unit = {},
    color: Color = MaterialTheme.colors().primary
) {
    Semantics(container = true, mergeAllDescendants = true) {
        Container(modifier = modifier) {
            WithConstraints { constraints ->
                val maxPx = constraints.maxWidth.value.toFloat()
                val minPx = 0f
                position.setBounds(minPx, maxPx)

                fun Float.toSliderPosition(): Float =
                    scale(minPx, maxPx, this, position.startValue, position.endValue)

                val flingConfig =
                    if (position.anchorsPx.isNotEmpty()) {
                        SliderFlingConfig(position, position.anchorsPx) { endValue ->
                            onValueChange(endValue.toSliderPosition())
                            onValueChangeEnd()
                        }
                    } else {
                        null
                    }
                val gestureEndAction = { velocity: Float ->
                    if (flingConfig != null) {
                        position.holder.fling(flingConfig, velocity)
                    } else {
                        onValueChangeEnd()
                    }
                }
                val pressed = state { false }
                PressGestureDetector(
                    onPress = { pos ->
                        onValueChange(pos.x.value.toSliderPosition())
                        pressed.value = true
                    },
                    onRelease = {
                        pressed.value = false
                        gestureEndAction(0f)
                    }) {
                    Draggable(
                        dragDirection = DragDirection.Horizontal,
                        dragValue = position.holder,
                        onDragStarted = { pressed.value = true },
                        onDragValueChangeRequested = { onValueChange(it.toSliderPosition()) },
                        onDragStopped = { velocity ->
                            pressed.value = false
                            gestureEndAction(velocity)
                        },
                        children = { SliderImpl(position, color, maxPx, pressed.value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderImpl(position: SliderPosition, color: Color, width: Float, pressed: Boolean) {
    val widthDp = with(DensityAmbient.current) {
        width.px.toDp()
    }
    Semantics(container = true, properties = { accessibilityValue = "${position.value}" }) {
        Container(
            expanded = true,
            constraints = DefaultSliderConstraints,
            alignment = Alignment.CenterLeft
        ) {
            val thumbSize = ThumbRadius * 2
            val fraction = with(position) { calcFraction(startValue, endValue, this.value) }
            val offset = (widthDp - thumbSize) * fraction
            DrawTrack(color, position)
            Box(LayoutPadding(left = offset)) {
                Ripple(bounded = false) {
                    Surface(
                        shape = CircleShape,
                        color = color,
                        elevation = if (pressed) 6.dp else 1.dp
                    ) {
                        Spacer(LayoutSize(thumbSize, thumbSize))
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawTrack(
    color: Color,
    position: SliderPosition
) {
    val activeTickColor = MaterialTheme.colors().onPrimary.copy(alpha = TickColorAlpha)
    val inactiveTickColor = color.copy(alpha = TickColorAlpha)
    val paint = remember {
        Paint().apply {
            this.isAntiAlias = true
            this.strokeCap = StrokeCap.round
            this.style = PaintingStyle.stroke
        }
    }
    Draw { canvas: Canvas, parentSize: PxSize ->
        paint.strokeWidth = TrackHeight.toPx().value
        val fraction = with(position) { calcFraction(startValue, endValue, this.value) }
        val parentRect = parentSize.toRect()
        val thumbPx = ThumbRadius.toPx().value
        val centerHeight = parentSize.height.value / 2
        val sliderStart = Offset(parentRect.left + thumbPx, centerHeight)
        val sliderMax = Offset(parentRect.right - thumbPx, centerHeight)
        paint.color = color.copy(alpha = InactiveTrackColorAlpha)
        canvas.drawLine(sliderStart, sliderMax, paint)
        val sliderValue = Offset(
            sliderStart.dx + (sliderMax.dx - sliderStart.dx) * fraction,
            centerHeight
        )
        paint.color = color
        canvas.drawLine(sliderStart, sliderValue, paint)
        position.tickFractions.groupBy { it > fraction }.forEach { (afterFraction, list) ->
            paint.color = if (afterFraction) inactiveTickColor else activeTickColor
            val points = list.map {
                Offset(Offset.lerp(sliderStart, sliderMax, it).dx, centerHeight)
            }
            canvas.drawPoints(PointMode.points, points, paint)
        }
    }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun SliderFlingConfig(
    value: SliderPosition,
    anchors: List<Float>,
    onSuccessfulEnd: (Float) -> Unit
): FlingConfig {
    val adjustTarget: (Float) -> TargetAnimation? = { _ ->
        val now = value.holder.value
        val point = anchors.minBy { abs(it - now) }
        val adjusted = point ?: now
        TargetAnimation(adjusted, SliderToTickAnimation)
    }
    return FlingConfig(
        adjustTarget = adjustTarget,
        onAnimationEnd = { reason, endValue, _ ->
            if (reason != AnimationEndReason.Interrupted) {
                onSuccessfulEnd(endValue)
            }
        }
    )
}

private val ThumbRadius = 10.dp
private val TrackHeight = 4.dp
private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp // TODO: clarify min width
private val DefaultSliderConstraints =
    DpConstraints(minWidth = SliderMinWidth, maxHeight = SliderHeight)
private val InactiveTrackColorAlpha = 0.24f
private val TickColorAlpha = 0.54f
private val SliderToTickAnimation = TweenBuilder<Float>().apply { duration = 100 }