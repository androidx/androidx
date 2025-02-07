/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.internal.IncreaseHorizontalSemanticsBounds
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.awaitHorizontalPointerSlopOrCancellation
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.pointerSlop
import androidx.compose.material3.tokens.SliderTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design slider</a>.
 *
 * Sliders allow users to make selections from a range of values.
 *
 * It uses [SliderDefaults.Thumb] and [SliderDefaults.Track] as the thumb and track.
 *
 * Sliders reflect a range of values along a horizontal bar, from which users may select a single
 * value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * ![Sliders
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqe2zb2b-1.png?alt=media)
 *
 * Use continuous sliders to allow users to make meaningful selections that don’t require a specific
 * value:
 *
 * @sample androidx.compose.material3.samples.SliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepsSliderSample
 * @param value current value of the slider. If outside of [valueRange] provided, value will be
 *   coerced to this range.
 * @param onValueChange callback in which value should be updated
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param valueRange range of values that this slider can take. The passed [value] will be coerced
 *   to this range.
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param onValueChangeFinished called when value change has ended. This should not be used to
 *   update the slider value (use [onValueChange] instead), but rather to know when the user has
 *   completed selecting a new value by ending a drag or a click.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [SliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = colors,
                enabled = enabled
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(colors = colors, enabled = enabled, sliderState = sliderState)
        },
        valueRange = valueRange
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design slider</a>.
 *
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a horizontal bar, from which users may select a single
 * value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * ![Sliders
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqe2zb2b-1.png?alt=media)
 *
 * Use continuous sliders to allow users to make meaningful selections that don’t require a specific
 * value:
 *
 * @sample androidx.compose.material3.samples.SliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepsSliderSample
 *
 * Slider using a custom thumb:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomThumbSample
 *
 * Slider using custom track and thumb:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomTrackAndThumbSample
 *
 * Slider using track icons:
 *
 * @sample androidx.compose.material3.samples.SliderWithTrackIconsSample
 * @param value current value of the slider. If outside of [valueRange] provided, value will be
 *   coerced to this range.
 * @param onValueChange callback in which value should be updated
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onValueChangeFinished called when value change has ended. This should not be used to
 *   update the slider value (use [onValueChange] instead), but rather to know when the user has
 *   completed selecting a new value by ending a drag or a click.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [SliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param valueRange range of values that this slider can take. The passed [value] will be coerced
 *   to this range.
 */
@Composable
@ExperimentalMaterial3Api
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(colors = colors, enabled = enabled, sliderState = sliderState)
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val state =
        remember(steps, valueRange) { SliderState(value, steps, onValueChangeFinished, valueRange) }
    state.onValueChangeFinished = onValueChangeFinished
    state.onValueChange = onValueChange
    state.value = value

    Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design slider</a>.
 *
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a horizontal bar, from which users may select a single
 * value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * ![Sliders
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqe2zb2b-1.png?alt=media)
 *
 * Use continuous sliders to allow users to make meaningful selections that don’t require a specific
 * value:
 *
 * @sample androidx.compose.material3.samples.SliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepsSliderSample
 *
 * Slider using a custom thumb:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomThumbSample
 *
 * Slider using custom track and thumb:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomTrackAndThumbSample
 *
 * Slider using track icons:
 *
 * @sample androidx.compose.material3.samples.SliderWithTrackIconsSample
 * @param state [SliderState] which contains the slider's current value.
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [SliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 */
@Composable
@ExperimentalMaterial3Api
fun Slider(
    state: SliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(colors = colors, enabled = enabled, sliderState = sliderState)
    }
) {
    require(state.steps >= 0) { "steps should be >= 0" }

    SliderImpl(
        state = state,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design slider</a>.
 *
 * Vertical Sliders allow users to make selections from a range of values.
 *
 * Vertical Sliders reflect a range of values along a vertical bar, from which users may select a
 * single value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * ![Sliders
 * image](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fgoogle-material-3%2Fimages%2Flqe2zb2b-1.png?alt=media)
 *
 * Vertical Slider:
 *
 * @sample androidx.compose.material3.samples.VerticalSliderSample
 * @param state [SliderState] which contains the slider's current value.
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param reverseDirection controls the direction of this slider. Default is top to bottom.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [SliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun VerticalSlider(
    state: SliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            sliderState = sliderState,
            colors = colors,
            enabled = enabled,
            thumbSize = VerticalThumbSize
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState,
            trackCornerSize = sliderState.trackWidth.dp / 2
        )
    }
) {
    require(state.steps >= 0) { "steps should be >= 0" }

    state.orientation = Vertical
    state.reverseVerticalDirection = reverseDirection

    SliderImpl(
        state = state,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design Range slider</a>.
 *
 * Range Sliders expand upon [Slider] using the same concepts but allow the user to select 2 values.
 *
 * The two values are still bounded by the value range but they also cannot cross each other.
 *
 * Use continuous Range Sliders to allow users to make meaningful selections that don’t require a
 * specific values:
 *
 * @sample androidx.compose.material3.samples.RangeSliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepRangeSliderSample
 * @param value current values of the RangeSlider. If either value is outside of [valueRange]
 *   provided, it will be coerced to this range.
 * @param onValueChange lambda in which values should be updated
 * @param modifier modifiers for the Range Slider layout
 * @param enabled whether or not component is enabled and can we interacted with or not
 * @param valueRange range of values that Range Slider values can take. Passed [value] will be
 *   coerced to this range
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param onValueChangeFinished lambda to be invoked when value change has ended. This callback
 *   shouldn't be used to update the range slider values (use [onValueChange] for that), but rather
 *   to know when the user has completed selecting a new value by ending a drag or a click.
 * @param colors [SliderColors] that will be used to determine the color of the Range Slider parts
 *   in different state. See [SliderDefaults.colors] to customize.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = {
            SliderDefaults.Thumb(
                interactionSource = startInteractionSource,
                colors = colors,
                enabled = enabled
            )
        },
        endThumb = {
            SliderDefaults.Thumb(
                interactionSource = endInteractionSource,
                colors = colors,
                enabled = enabled
            )
        },
        track = { rangeSliderState ->
            SliderDefaults.Track(
                colors = colors,
                enabled = enabled,
                rangeSliderState = rangeSliderState
            )
        }
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design Range slider</a>.
 *
 * Range Sliders expand upon [Slider] using the same concepts but allow the user to select 2 values.
 *
 * The two values are still bounded by the value range but they also cannot cross each other.
 *
 * It uses the provided startThumb for the slider's start thumb and endThumb for the slider's end
 * thumb. It also uses the provided track for the slider's track. If nothing is passed for these
 * parameters, it will use [SliderDefaults.Thumb] and [SliderDefaults.Track] for the thumbs and
 * track.
 *
 * Use continuous Range Sliders to allow users to make meaningful selections that don’t require a
 * specific values:
 *
 * @sample androidx.compose.material3.samples.RangeSliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepRangeSliderSample
 *
 * A custom start/end thumb and track can be provided:
 *
 * @sample androidx.compose.material3.samples.RangeSliderWithCustomComponents
 * @param value current values of the RangeSlider. If either value is outside of [valueRange]
 *   provided, it will be coerced to this range.
 * @param onValueChange lambda in which values should be updated
 * @param modifier modifiers for the Range Slider layout
 * @param enabled whether or not component is enabled and can we interacted with or not
 * @param onValueChangeFinished lambda to be invoked when value change has ended. This callback
 *   shouldn't be used to update the range slider values (use [onValueChange] for that), but rather
 *   to know when the user has completed selecting a new value by ending a drag or a click.
 * @param colors [SliderColors] that will be used to determine the color of the Range Slider parts
 *   in different state. See [SliderDefaults.colors] to customize.
 * @param startInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the start thumb. You can create and pass in your own `remember`ed instance
 *   to observe.
 * @param endInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the end thumb. You can create and pass in your own `remember`ed instance to
 *   observe.
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param startThumb the start thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param endThumb the end thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the range slider, it is placed underneath the thumb.
 *   The lambda receives a [RangeSliderState] which is used to obtain the current active track.
 * @param valueRange range of values that Range Slider values can take. Passed [value] will be
 *   coerced to this range.
 */
@Composable
@ExperimentalMaterial3Api
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = startInteractionSource,
            colors = colors,
            enabled = enabled
        )
    },
    endThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = endInteractionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (RangeSliderState) -> Unit = { rangeSliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    },
    @IntRange(from = 0) steps: Int = 0
) {
    val state =
        remember(steps, valueRange) {
            RangeSliderState(
                value.start,
                value.endInclusive,
                steps,
                onValueChangeFinished,
                valueRange
            )
        }

    state.onValueChangeFinished = onValueChangeFinished
    state.onValueChange = { onValueChange(it.start..it.endInclusive) }
    state.activeRangeStart = value.start
    state.activeRangeEnd = value.endInclusive

    RangeSlider(
        modifier = modifier,
        state = state,
        enabled = enabled,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = startThumb,
        endThumb = endThumb,
        track = track
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external"
 * target="_blank">Material Design Range slider</a>.
 *
 * Range Sliders expand upon [Slider] using the same concepts but allow the user to select 2 values.
 *
 * The two values are still bounded by the value range but they also cannot cross each other.
 *
 * It uses the provided startThumb for the slider's start thumb and endThumb for the slider's end
 * thumb. It also uses the provided track for the slider's track. If nothing is passed for these
 * parameters, it will use [SliderDefaults.Thumb] and [SliderDefaults.Track] for the thumbs and
 * track.
 *
 * Use continuous Range Sliders to allow users to make meaningful selections that don’t require a
 * specific values:
 *
 * @sample androidx.compose.material3.samples.RangeSliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material3.samples.StepRangeSliderSample
 *
 * A custom start/end thumb and track can be provided:
 *
 * @sample androidx.compose.material3.samples.RangeSliderWithCustomComponents
 * @param state [RangeSliderState] which contains the current values of the RangeSlider.
 * @param modifier modifiers for the Range Slider layout
 * @param enabled whether or not component is enabled and can we interacted with or not
 * @param colors [SliderColors] that will be used to determine the color of the Range Slider parts
 *   in different state. See [SliderDefaults.colors] to customize.
 * @param startInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the start thumb. You can create and pass in your own `remember`ed instance
 *   to observe.
 * @param endInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the end thumb. You can create and pass in your own `remember`ed instance to
 *   observe.
 * @param startThumb the start thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param endThumb the end thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the range slider, it is placed underneath the thumb.
 *   The lambda receives a [RangeSliderState] which is used to obtain the current active track.
 */
@Composable
@ExperimentalMaterial3Api
fun RangeSlider(
    state: RangeSliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    startInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = startInteractionSource,
            colors = colors,
            enabled = enabled
        )
    },
    endThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = endInteractionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (RangeSliderState) -> Unit = { rangeSliderState ->
        SliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState
        )
    }
) {
    require(state.steps >= 0) { "steps should be >= 0" }

    RangeSliderImpl(
        modifier = modifier,
        state = state,
        enabled = enabled,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = startThumb,
        endThumb = endThumb,
        track = track
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderImpl(
    modifier: Modifier,
    state: SliderState,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    thumb: @Composable (SliderState) -> Unit,
    track: @Composable (SliderState) -> Unit
) {
    state.isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reverseDirection =
        (state.orientation == Horizontal && state.isRtl) ||
            (state.orientation == Vertical && state.reverseVerticalDirection)
    val press = Modifier.sliderTapModifier(state, interactionSource, enabled)
    val drag =
        Modifier.draggable(
            orientation = state.orientation,
            reverseDirection = reverseDirection,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { state.gestureEndAction() },
            startDragImmediately = state.isDragging,
            state = state
        )
    val thumbModifier =
        if (state.orientation == Vertical) {
            Modifier.layoutId(SliderComponents.THUMB).wrapContentHeight()
        } else {
            Modifier.layoutId(SliderComponents.THUMB).wrapContentWidth()
        }

    Layout(
        {
            Box(
                modifier =
                    thumbModifier.onSizeChanged {
                        state.thumbWidth = it.width
                        state.thumbHeight = it.height
                    }
            ) {
                thumb(state)
            }
            Box(modifier = Modifier.layoutId(SliderComponents.TRACK)) { track(state) }
        },
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .requiredSizeIn(
                    minWidth = if (state.orientation == Vertical) TrackHeight else ThumbWidth,
                    minHeight = if (state.orientation == Vertical) ThumbWidth else TrackHeight,
                )
                .sliderSemantics(state, enabled)
                .focusable(enabled, interactionSource)
                .slideOnKeyEvents(
                    enabled,
                    state.steps,
                    state.valueRange,
                    state.value,
                    reverseDirection,
                    state.onValueChange,
                    state.onValueChangeFinished
                )
                .then(press)
                .then(drag)
    ) { measurables, constraints ->
        val thumbPlaceable =
            measurables.fastFirst { it.layoutId == SliderComponents.THUMB }.measure(constraints)

        val trackMeasurable = measurables.fastFirst { it.layoutId == SliderComponents.TRACK }
        val trackPlaceable =
            if (state.orientation == Vertical) {
                trackMeasurable.measure(
                    constraints.offset(vertical = -thumbPlaceable.height).copy(minWidth = 0)
                )
            } else {
                trackMeasurable.measure(
                    constraints.offset(horizontal = -thumbPlaceable.width).copy(minHeight = 0)
                )
            }

        val sliderWidth: Int
        val sliderHeight: Int
        val trackOffsetX: Int
        val trackOffsetY: Int
        val thumbOffsetX: Int
        var thumbOffsetY: Int
        if (state.orientation == Vertical) {
            sliderWidth = max(trackPlaceable.width, thumbPlaceable.width)
            sliderHeight = thumbPlaceable.height + trackPlaceable.height
            trackOffsetX = (sliderWidth - trackPlaceable.width) / 2
            trackOffsetY = thumbPlaceable.height / 2
            thumbOffsetX = (sliderWidth - thumbPlaceable.width) / 2
            thumbOffsetY = (trackPlaceable.height * state.coercedValueAsFraction).roundToInt()
            if (state.reverseVerticalDirection) {
                thumbOffsetY = trackPlaceable.height - thumbOffsetY
            }
        } else {
            sliderWidth = thumbPlaceable.width + trackPlaceable.width
            sliderHeight = max(trackPlaceable.height, thumbPlaceable.height)
            trackOffsetX = thumbPlaceable.width / 2
            trackOffsetY = (sliderHeight - trackPlaceable.height) / 2
            thumbOffsetX = (trackPlaceable.width * state.coercedValueAsFraction).roundToInt()
            thumbOffsetY = (sliderHeight - thumbPlaceable.height) / 2
        }

        state.updateDimensions(
            trackPlaceable.width,
            trackPlaceable.height,
            sliderWidth,
            sliderHeight
        )

        layout(sliderWidth, sliderHeight) {
            trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)
            thumbPlaceable.placeRelative(thumbOffsetX, thumbOffsetY)
        }
    }
}

private fun Modifier.slideOnKeyEvents(
    enabled: Boolean,
    steps: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    reverseDirection: Boolean,
    onValueChangeState: ((Float) -> Unit)?,
    onValueChangeFinishedState: (() -> Unit)?
): Modifier {
    require(steps >= 0) { "steps should be >= 0" }
    return this.onKeyEvent {
        if (!enabled) return@onKeyEvent false
        if (onValueChangeState == null) return@onKeyEvent false
        when (it.type) {
            KeyEventType.KeyDown -> {
                val rangeLength = abs(valueRange.endInclusive - valueRange.start)
                // When steps == 0, it means that a user is not limited by a step length (delta)
                // when using touch or mouse. But it is not possible to adjust the value
                // continuously when using keyboard buttons - the delta has to be discrete.
                // In this case, 1% of the valueRange seems to make sense.
                val actualSteps = if (steps > 0) steps + 1 else 100
                val delta = rangeLength / actualSteps
                val sign = if (reverseDirection) -1 else 1
                when (it.key) {
                    Key.DirectionUp -> {
                        onValueChangeState((value + sign * delta).coerceIn(valueRange))
                        true
                    }
                    Key.DirectionDown -> {
                        onValueChangeState((value - sign * delta).coerceIn(valueRange))
                        true
                    }
                    Key.DirectionRight -> {
                        onValueChangeState((value + sign * delta).coerceIn(valueRange))
                        true
                    }
                    Key.DirectionLeft -> {
                        onValueChangeState((value - sign * delta).coerceIn(valueRange))
                        true
                    }
                    Key.MoveHome -> {
                        onValueChangeState(valueRange.start)
                        true
                    }
                    Key.MoveEnd -> {
                        onValueChangeState(valueRange.endInclusive)
                        true
                    }
                    Key.PageUp -> {
                        val page = (actualSteps / 10).coerceIn(1, 10)
                        onValueChangeState((value - page * delta).coerceIn(valueRange))
                        true
                    }
                    Key.PageDown -> {
                        val page = (actualSteps / 10).coerceIn(1, 10)
                        onValueChangeState((value + page * delta).coerceIn(valueRange))
                        true
                    }
                    else -> false
                }
            }
            KeyEventType.KeyUp -> {
                when (it.key) {
                    Key.DirectionUp,
                    Key.DirectionDown,
                    Key.DirectionRight,
                    Key.DirectionLeft,
                    Key.MoveHome,
                    Key.MoveEnd,
                    Key.PageUp,
                    Key.PageDown -> {
                        onValueChangeFinishedState?.invoke()
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSliderImpl(
    modifier: Modifier,
    state: RangeSliderState,
    enabled: Boolean,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    startThumb: @Composable ((RangeSliderState) -> Unit),
    endThumb: @Composable ((RangeSliderState) -> Unit),
    track: @Composable ((RangeSliderState) -> Unit)
) {
    state.isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val pressDrag =
        Modifier.rangeSliderPressDragModifier(
            state,
            startInteractionSource,
            endInteractionSource,
            enabled
        )

    val startContentDescription = getString(Strings.SliderRangeStart)
    val endContentDescription = getString(Strings.SliderRangeEnd)

    Layout(
        {
            Box(
                modifier =
                    Modifier.layoutId(RangeSliderComponents.STARTTHUMB)
                        .wrapContentWidth()
                        .onSizeChanged {
                            state.startThumbWidth = it.width.toFloat()
                            state.startThumbHeight = it.height.toFloat()
                        }
                        .rangeSliderStartThumbSemantics(state, enabled)
                        .semantics(mergeDescendants = true) {
                            contentDescription = startContentDescription
                        }
                        .focusable(enabled, startInteractionSource)
            ) {
                startThumb(state)
            }
            Box(
                modifier =
                    Modifier.layoutId(RangeSliderComponents.ENDTHUMB)
                        .wrapContentWidth()
                        .onSizeChanged {
                            state.endThumbWidth = it.width.toFloat()
                            state.endThumbHeight = it.height.toFloat()
                        }
                        .rangeSliderEndThumbSemantics(state, enabled)
                        .semantics(mergeDescendants = true) {
                            contentDescription = endContentDescription
                        }
                        .focusable(enabled, endInteractionSource)
            ) {
                endThumb(state)
            }
            Box(modifier = Modifier.layoutId(RangeSliderComponents.TRACK)) { track(state) }
        },
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .requiredSizeIn(minWidth = ThumbWidth, minHeight = TrackHeight)
                .then(pressDrag)
    ) { measurables, constraints ->
        val startThumbPlaceable =
            measurables
                .fastFirst { it.layoutId == RangeSliderComponents.STARTTHUMB }
                .measure(constraints)

        val endThumbPlaceable =
            measurables
                .fastFirst { it.layoutId == RangeSliderComponents.ENDTHUMB }
                .measure(constraints)

        val trackPlaceable =
            measurables
                .fastFirst { it.layoutId == RangeSliderComponents.TRACK }
                .measure(
                    constraints
                        .offset(
                            horizontal = -(startThumbPlaceable.width + endThumbPlaceable.width) / 2
                        )
                        .copy(minHeight = 0)
                )

        val sliderWidth =
            trackPlaceable.width + (startThumbPlaceable.width + endThumbPlaceable.width) / 2
        val sliderHeight =
            maxOf(trackPlaceable.height, startThumbPlaceable.height, endThumbPlaceable.height)

        state.trackWidth = trackPlaceable.width.toFloat()
        state.trackHeight = trackPlaceable.height.toFloat()
        state.totalWidth = sliderWidth

        state.updateMinMaxPx()

        val trackOffsetX = startThumbPlaceable.width / 2
        val startThumbOffsetX =
            (trackPlaceable.width * state.coercedActiveRangeStartAsFraction).roundToInt()
        // When start thumb and end thumb have different widths,
        // we need to add a correction for the centering of the slider.
        val endCorrection = (startThumbPlaceable.width - endThumbPlaceable.width) / 2
        val endThumbOffsetX =
            (trackPlaceable.width * state.coercedActiveRangeEndAsFraction + endCorrection)
                .roundToInt()
        val trackOffsetY = (sliderHeight - trackPlaceable.height) / 2
        val startThumbOffsetY = (sliderHeight - startThumbPlaceable.height) / 2
        val endThumbOffsetY = (sliderHeight - endThumbPlaceable.height) / 2

        layout(sliderWidth, sliderHeight) {
            trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)
            startThumbPlaceable.placeRelative(startThumbOffsetX, startThumbOffsetY)
            endThumbPlaceable.placeRelative(endThumbOffsetX, endThumbOffsetY)
        }
    }
}

/** Object to hold defaults used by [Slider] */
@Stable
object SliderDefaults {

    /**
     * Creates a [SliderColors] that represents the different colors used in parts of the [Slider]
     * in different states.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultSliderColors

    /**
     * Creates a [SliderColors] that represents the different colors used in parts of the [Slider]
     * in different states.
     *
     * For the name references below the words "active" and "inactive" are used. Active part of the
     * slider is filled with progress, so if slider's progress is 30% out of 100%, left (or right in
     * RTL) 30% of the track will be active, while the rest is inactive.
     *
     * @param thumbColor thumb color when enabled
     * @param activeTrackColor color of the track in the part that is "active", meaning that the
     *   thumb is ahead of it
     * @param activeTickColor colors to be used to draw tick marks on the active track, if `steps`
     *   is specified
     * @param inactiveTrackColor color of the track in the part that is "inactive", meaning that the
     *   thumb is before it
     * @param inactiveTickColor colors to be used to draw tick marks on the inactive track, if
     *   `steps` are specified on the Slider is specified
     * @param disabledThumbColor thumb colors when disabled
     * @param disabledActiveTrackColor color of the track in the "active" part when the Slider is
     *   disabled
     * @param disabledActiveTickColor colors to be used to draw tick marks on the active track when
     *   Slider is disabled and when `steps` are specified on it
     * @param disabledInactiveTrackColor color of the track in the "inactive" part when the Slider
     *   is disabled
     * @param disabledInactiveTickColor colors to be used to draw tick marks on the inactive part of
     *   the track when Slider is disabled and when `steps` are specified on it
     */
    @Composable
    fun colors(
        thumbColor: Color = Color.Unspecified,
        activeTrackColor: Color = Color.Unspecified,
        activeTickColor: Color = Color.Unspecified,
        inactiveTrackColor: Color = Color.Unspecified,
        inactiveTickColor: Color = Color.Unspecified,
        disabledThumbColor: Color = Color.Unspecified,
        disabledActiveTrackColor: Color = Color.Unspecified,
        disabledActiveTickColor: Color = Color.Unspecified,
        disabledInactiveTrackColor: Color = Color.Unspecified,
        disabledInactiveTickColor: Color = Color.Unspecified
    ): SliderColors =
        MaterialTheme.colorScheme.defaultSliderColors.copy(
            thumbColor = thumbColor,
            activeTrackColor = activeTrackColor,
            activeTickColor = activeTickColor,
            inactiveTrackColor = inactiveTrackColor,
            inactiveTickColor = inactiveTickColor,
            disabledThumbColor = disabledThumbColor,
            disabledActiveTrackColor = disabledActiveTrackColor,
            disabledActiveTickColor = disabledActiveTickColor,
            disabledInactiveTrackColor = disabledInactiveTrackColor,
            disabledInactiveTickColor = disabledInactiveTickColor
        )

    internal val ColorScheme.defaultSliderColors: SliderColors
        get() {
            return defaultSliderColorsCached
                ?: SliderColors(
                        thumbColor = fromToken(SliderTokens.HandleColor),
                        activeTrackColor = fromToken(SliderTokens.ActiveTrackColor),
                        activeTickColor = fromToken(SliderTokens.InactiveTrackColor),
                        inactiveTrackColor = fromToken(SliderTokens.InactiveTrackColor),
                        inactiveTickColor = fromToken(SliderTokens.ActiveTrackColor),
                        disabledThumbColor =
                            fromToken(SliderTokens.DisabledHandleColor)
                                .copy(alpha = SliderTokens.DisabledHandleOpacity)
                                .compositeOver(surface),
                        disabledActiveTrackColor =
                            fromToken(SliderTokens.DisabledActiveTrackColor)
                                .copy(alpha = SliderTokens.DisabledActiveTrackOpacity),
                        disabledActiveTickColor =
                            fromToken(SliderTokens.DisabledInactiveTrackColor)
                                .copy(alpha = SliderTokens.DisabledInactiveTrackOpacity),
                        disabledInactiveTrackColor =
                            fromToken(SliderTokens.DisabledInactiveTrackColor)
                                .copy(alpha = SliderTokens.DisabledInactiveTrackOpacity),
                        disabledInactiveTickColor =
                            fromToken(SliderTokens.DisabledActiveTrackColor)
                                .copy(alpha = SliderTokens.DisabledActiveTrackOpacity)
                    )
                    .also { defaultSliderColorsCached = it }
        }

    /**
     * The Default thumb for [Slider] and [RangeSlider]
     *
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     *   [Interaction]s for this thumb. You can create and pass in your own `remember`ed instance to
     *   observe
     * @param modifier the [Modifier] to be applied to the thumb.
     * @param colors [SliderColors] that will be used to resolve the colors used for this thumb in
     *   different states. See [SliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param thumbSize the size of the thumb.
     */
    @Composable
    fun Thumb(
        interactionSource: MutableInteractionSource,
        modifier: Modifier = Modifier,
        colors: SliderColors = colors(),
        enabled: Boolean = true,
        thumbSize: DpSize = ThumbSize
    ) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val size =
            if (interactions.isNotEmpty()) {
                thumbSize.copy(width = thumbSize.width / 2)
            } else {
                thumbSize
            }
        Spacer(
            modifier
                .size(size)
                .hoverable(interactionSource = interactionSource)
                .background(colors.thumbColor(enabled), SliderTokens.HandleShape.value)
        )
    }

    /**
     * The Default thumb for [Slider], [VerticalSlider] and [RangeSlider]
     *
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     *   [Interaction]s for this thumb. You can create and pass in your own `remember`ed instance to
     *   observe
     * @param sliderState [SliderState] which is used to obtain the current active track.
     * @param modifier the [Modifier] to be applied to the thumb.
     * @param colors [SliderColors] that will be used to resolve the colors used for this thumb in
     *   different states. See [SliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param thumbSize the size of the thumb.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun Thumb(
        interactionSource: MutableInteractionSource,
        sliderState: SliderState,
        modifier: Modifier = Modifier,
        colors: SliderColors = colors(),
        enabled: Boolean = true,
        thumbSize: DpSize = ThumbSize
    ) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val size =
            if (interactions.isNotEmpty()) {
                if (sliderState.orientation == Vertical) {
                    thumbSize.copy(height = thumbSize.height / 2)
                } else {
                    thumbSize.copy(width = thumbSize.width / 2)
                }
            } else {
                thumbSize
            }
        Spacer(
            modifier
                .size(size)
                .hoverable(interactionSource = interactionSource)
                .background(colors.thumbColor(enabled), SliderTokens.HandleShape.value)
        )
    }

    /**
     * The Default track for [Slider] and [RangeSlider]
     *
     * @param sliderPositions [SliderPositions] which is used to obtain the current active track and
     *   the tick positions if the slider is discrete.
     * @param modifier the [Modifier] to be applied to the track.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     */
    @Suppress("DEPRECATION")
    @Composable
    @Deprecated("Use version that supports slider state")
    fun Track(
        sliderPositions: SliderPositions,
        modifier: Modifier = Modifier,
        colors: SliderColors = colors(),
        enabled: Boolean = true,
    ) {
        val inactiveTrackColor = colors.trackColor(enabled, active = false)
        val activeTrackColor = colors.trackColor(enabled, active = true)
        val inactiveTickColor = colors.tickColor(enabled, active = false)
        val activeTickColor = colors.tickColor(enabled, active = true)
        Canvas(modifier.fillMaxWidth().height(TrackHeight)) {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val sliderLeft = Offset(0f, center.y)
            val sliderRight = Offset(size.width, center.y)
            val sliderStart = if (isRtl) sliderRight else sliderLeft
            val sliderEnd = if (isRtl) sliderLeft else sliderRight
            val tickSize = TickSize.toPx()
            val trackStrokeWidth = TrackHeight.toPx()
            drawLine(inactiveTrackColor, sliderStart, sliderEnd, trackStrokeWidth, StrokeCap.Round)
            val sliderValueEnd =
                Offset(
                    sliderStart.x +
                        (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.endInclusive,
                    center.y
                )

            val sliderValueStart =
                Offset(
                    sliderStart.x +
                        (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.start,
                    center.y
                )

            drawLine(
                activeTrackColor,
                sliderValueStart,
                sliderValueEnd,
                trackStrokeWidth,
                StrokeCap.Round
            )
            sliderPositions.tickFractions
                .groupBy {
                    it > sliderPositions.activeRange.endInclusive ||
                        it < sliderPositions.activeRange.start
                }
                .forEach { (outsideFraction, list) ->
                    drawPoints(
                        list.fastMap { Offset(lerp(sliderStart, sliderEnd, it).x, center.y) },
                        PointMode.Points,
                        (if (outsideFraction) inactiveTickColor else activeTickColor),
                        tickSize,
                        StrokeCap.Round
                    )
                }
        }
    }

    /**
     * The Default track for [Slider]
     *
     * @param sliderState [SliderState] which is used to obtain the current active track.
     * @param modifier the [Modifier] to be applied to the track.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     */
    @Deprecated(
        message =
            "Use the overload that takes `drawStopIndicator`, `drawTick`, " +
                "`thumbTrackGapSize` and `trackInsideCornerSize`, see `LegacySliderSample` " +
                "on how to restore the previous behavior",
        replaceWith =
            ReplaceWith(
                "Track(sliderState, modifier, enabled, colors, drawStopIndicator, " +
                    "drawTick, thumbTrackGapSize, trackInsideCornerSize)"
            ),
        level = DeprecationLevel.HIDDEN
    )
    @Composable
    @ExperimentalMaterial3Api
    fun Track(
        sliderState: SliderState,
        modifier: Modifier = Modifier,
        colors: SliderColors = colors(),
        enabled: Boolean = true
    ) {
        Track(
            sliderState,
            modifier,
            enabled,
            colors,
            thumbTrackGapSize = ThumbTrackGapSize,
            trackInsideCornerSize = TrackInsideCornerSize
        )
    }

    /**
     * The Default track for [Slider]
     *
     * @param sliderState [SliderState] which is used to obtain the current active track.
     * @param modifier the [Modifier] to be applied to the track.
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param drawStopIndicator lambda that will be called to draw the stop indicator at the end of
     *   the track.
     * @param drawTick lambda that will be called to draw the ticks if steps are greater than 0.
     * @param thumbTrackGapSize size of the gap between the thumb and the track.
     * @param trackInsideCornerSize size of the corners towards the thumb when a gap is set.
     */
    @ExperimentalMaterial3Api
    @Composable
    fun Track(
        sliderState: SliderState,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: SliderColors = colors(),
        drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
            drawStopIndicator(
                offset = it,
                color = colors.trackColor(enabled, active = true),
                size = TrackStopIndicatorSize
            )
        },
        drawTick: DrawScope.(Offset, Color) -> Unit = { offset, color ->
            drawStopIndicator(offset = offset, color = color, size = TickSize)
        },
        thumbTrackGapSize: Dp = ThumbTrackGapSize,
        trackInsideCornerSize: Dp = TrackInsideCornerSize
    ) {
        DrawTrack(
            sliderState = sliderState,
            trackCornerSize = TrackHeight / 2,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            drawStopIndicator = drawStopIndicator,
            drawTick = drawTick,
            thumbTrackGapSize = thumbTrackGapSize,
            trackInsideCornerSize = trackInsideCornerSize,
            enableCornerShrinking = false
        )
    }

    /**
     * The Default track for [Slider] and [VerticalSlider]
     *
     * This track has a different corner treatment where the corner size decreases as the thumb gets
     * closer.
     *
     * @param sliderState [SliderState] which is used to obtain the current active track.
     * @param trackCornerSize size of the external corners.
     * @param modifier the [Modifier] to be applied to the track.
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param drawStopIndicator lambda that will be called to draw the stop indicator at the end of
     *   the track.
     * @param drawTick lambda that will be called to draw the ticks if steps are greater than 0.
     * @param thumbTrackGapSize size of the gap between the thumb and the track.
     * @param trackInsideCornerSize size of the corners towards the thumb when a gap is set.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun Track(
        sliderState: SliderState,
        trackCornerSize: Dp,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: SliderColors = colors(),
        drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
            drawStopIndicator(
                offset = it,
                color = colors.trackColor(enabled, active = true),
                size = TrackStopIndicatorSize
            )
        },
        drawTick: DrawScope.(Offset, Color) -> Unit = { offset, color ->
            drawStopIndicator(offset = offset, color = color, size = TickSize)
        },
        thumbTrackGapSize: Dp = ThumbTrackGapSize,
        trackInsideCornerSize: Dp = TrackInsideCornerSize
    ) {
        DrawTrack(
            sliderState = sliderState,
            trackCornerSize = trackCornerSize,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            drawStopIndicator = drawStopIndicator,
            drawTick = drawTick,
            thumbTrackGapSize = thumbTrackGapSize,
            trackInsideCornerSize = trackInsideCornerSize,
            enableCornerShrinking = true
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DrawTrack(
        sliderState: SliderState,
        trackCornerSize: Dp,
        modifier: Modifier,
        enabled: Boolean,
        colors: SliderColors,
        drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
        drawTick: DrawScope.(Offset, Color) -> Unit,
        thumbTrackGapSize: Dp,
        trackInsideCornerSize: Dp,
        enableCornerShrinking: Boolean
    ) {
        val inactiveTrackColor = colors.trackColor(enabled = enabled, active = false)
        val activeTrackColor = colors.trackColor(enabled = enabled, active = true)
        val inactiveTickColor = colors.tickColor(enabled = enabled, active = false)
        val activeTickColor = colors.tickColor(enabled = enabled, active = true)
        Canvas(
            if (sliderState.orientation == Vertical) {
                modifier.width(TrackHeight).fillMaxHeight().let {
                    if (sliderState.reverseVerticalDirection) it.scale(1f, -1f) else it
                }
            } else {
                modifier.fillMaxWidth().height(TrackHeight).let {
                    if (sliderState.isRtl) it.scale(-1f, 1f) else it
                }
            }
        ) {
            drawTrack(
                tickFractions = sliderState.tickFractions,
                activeRangeStart = 0f,
                activeRangeEnd = sliderState.coercedValueAsFraction,
                inactiveTrackColor = inactiveTrackColor,
                activeTrackColor = activeTrackColor,
                inactiveTickColor = inactiveTickColor,
                activeTickColor = activeTickColor,
                width = sliderState.trackWidth.toDp(),
                height = sliderState.trackHeight.toDp(),
                startThumbWidth = 0.toDp(),
                startThumbHeight = 0.toDp(),
                endThumbWidth = sliderState.thumbWidth.toDp(),
                endThumbHeight = sliderState.thumbHeight.toDp(),
                thumbTrackGapSize = thumbTrackGapSize,
                trackInsideCornerSize = trackInsideCornerSize,
                trackCornerSize = trackCornerSize,
                drawStopIndicator = drawStopIndicator,
                drawTick = drawTick,
                isRangeSlider = false,
                enableCornerShrinking = enableCornerShrinking,
                orientation = sliderState.orientation
            )
        }
    }

    /**
     * The Default track for [RangeSlider]
     *
     * @param rangeSliderState [RangeSliderState] which is used to obtain the current active track.
     * @param modifier the [Modifier] to be applied to the track.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     */
    @Deprecated(
        message =
            "Use the overload that takes `drawStopIndicator`, `drawTick`, " +
                "`thumbTrackGapSize` and `trackInsideCornerSize`, see `LegacyRangeSliderSample` " +
                "on how to restore the previous behavior",
        replaceWith =
            ReplaceWith(
                "Track(rangeSliderState, modifier, colors, enabled, drawStopIndicator, " +
                    "drawTick, thumbTrackGapSize, trackInsideCornerSize)"
            ),
        level = DeprecationLevel.HIDDEN
    )
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Track(
        rangeSliderState: RangeSliderState,
        modifier: Modifier = Modifier,
        colors: SliderColors = colors(),
        enabled: Boolean = true
    ) {
        Track(
            rangeSliderState,
            modifier,
            enabled,
            colors,
            thumbTrackGapSize = ThumbTrackGapSize,
            trackInsideCornerSize = TrackInsideCornerSize
        )
    }

    /**
     * The Default track for [RangeSlider]
     *
     * @param rangeSliderState [RangeSliderState] which is used to obtain the current active track.
     * @param modifier the [Modifier] to be applied to the track.
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param colors [SliderColors] that will be used to resolve the colors used for this track in
     *   different states. See [SliderDefaults.colors].
     * @param drawStopIndicator lambda that will be called to draw the stop indicator at the
     *   start/end of the track.
     * @param drawTick lambda that will be called to draw the ticks if steps are greater than 0.
     * @param thumbTrackGapSize size of the gap between the thumbs and the track.
     * @param trackInsideCornerSize size of the corners towards the thumbs when a gap is set.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Track(
        rangeSliderState: RangeSliderState,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: SliderColors = colors(),
        drawStopIndicator: (DrawScope.(Offset) -> Unit)? = {
            drawStopIndicator(
                offset = it,
                color = colors.trackColor(enabled, active = true),
                size = TrackStopIndicatorSize
            )
        },
        drawTick: DrawScope.(Offset, Color) -> Unit = { offset, color ->
            drawStopIndicator(offset = offset, color = color, size = TickSize)
        },
        thumbTrackGapSize: Dp = ThumbTrackGapSize,
        trackInsideCornerSize: Dp = TrackInsideCornerSize
    ) {
        val inactiveTrackColor = colors.trackColor(enabled, active = false)
        val activeTrackColor = colors.trackColor(enabled, active = true)
        val inactiveTickColor = colors.tickColor(enabled, active = false)
        val activeTickColor = colors.tickColor(enabled, active = true)
        Canvas(
            modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .rotate(if (rangeSliderState.isRtl) 180f else 0f)
        ) {
            drawTrack(
                rangeSliderState.tickFractions,
                rangeSliderState.coercedActiveRangeStartAsFraction,
                rangeSliderState.coercedActiveRangeEndAsFraction,
                inactiveTrackColor,
                activeTrackColor,
                inactiveTickColor,
                activeTickColor,
                rangeSliderState.trackWidth.toDp(),
                rangeSliderState.trackHeight.toDp(),
                rangeSliderState.startThumbWidth.toDp(),
                rangeSliderState.startThumbHeight.toDp(),
                rangeSliderState.endThumbWidth.toDp(),
                rangeSliderState.endThumbHeight.toDp(),
                thumbTrackGapSize,
                trackInsideCornerSize,
                rangeSliderState.trackHeight.toDp() / 2,
                drawStopIndicator,
                drawTick,
                isRangeSlider = true,
            )
        }
    }

    private fun DrawScope.drawTrack(
        tickFractions: FloatArray,
        activeRangeStart: Float,
        activeRangeEnd: Float,
        inactiveTrackColor: Color,
        activeTrackColor: Color,
        inactiveTickColor: Color,
        activeTickColor: Color,
        width: Dp,
        height: Dp,
        startThumbWidth: Dp,
        startThumbHeight: Dp,
        endThumbWidth: Dp,
        endThumbHeight: Dp,
        thumbTrackGapSize: Dp,
        trackInsideCornerSize: Dp,
        trackCornerSize: Dp,
        drawStopIndicator: (DrawScope.(Offset) -> Unit)?,
        drawTick: DrawScope.(Offset, Color) -> Unit,
        isRangeSlider: Boolean,
        enableCornerShrinking: Boolean = false,
        orientation: Orientation = Horizontal
    ) {
        val isVertical = orientation == Vertical
        val sliderStart = 0f
        val sliderEnd = if (isVertical) size.height else size.width
        val sliderValueEnd = sliderStart + (sliderEnd - sliderStart) * activeRangeEnd
        val sliderValueStart = sliderStart + (sliderEnd - sliderStart) * activeRangeStart

        val cornerSize = trackCornerSize.toPx()
        val insideCornerSize = trackInsideCornerSize.toPx()
        var startGap = 0f
        var endGap = 0f
        if (thumbTrackGapSize > 0.dp) {
            if (isVertical) {
                startGap = startThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
                endGap = endThumbHeight.toPx() / 2 + thumbTrackGapSize.toPx()
            } else {
                startGap = startThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
                endGap = endThumbWidth.toPx() / 2 + thumbTrackGapSize.toPx()
            }
        }

        // inactive track (range slider)
        var rangeInactiveTrackThreshold = sliderStart + startGap
        if (!enableCornerShrinking) {
            rangeInactiveTrackThreshold += cornerSize
        }
        if (isRangeSlider && sliderValueStart > rangeInactiveTrackThreshold) {
            val start = sliderStart
            val end = sliderValueStart - startGap
            val size =
                if (isVertical) Size(width.toPx(), end - start)
                else Size(end - start, height.toPx())
            drawTrackPath(
                orientation,
                Offset.Zero,
                size,
                inactiveTrackColor,
                cornerSize,
                insideCornerSize
            )
            val stopIndicatorOffset =
                if (isVertical) Offset(center.x, start + cornerSize)
                else Offset(start + cornerSize, center.y)
            drawStopIndicator?.invoke(this, stopIndicatorOffset)
        }
        // inactive track
        var inactiveTrackThreshold = sliderEnd - endGap
        if (!enableCornerShrinking) {
            inactiveTrackThreshold -= cornerSize
        }
        if (sliderValueEnd < inactiveTrackThreshold) {
            val start = sliderValueEnd + endGap
            val end = sliderEnd
            val inactiveTrackWidth = end - start
            val trackOffset = if (isVertical) Offset(0f, start) else Offset(start, 0f)
            val size =
                if (isVertical) Size(width.toPx(), inactiveTrackWidth)
                else Size(inactiveTrackWidth, height.toPx())
            drawTrackPath(
                orientation,
                trackOffset,
                size,
                inactiveTrackColor,
                insideCornerSize,
                cornerSize
            )
            val stopIndicatorOffset =
                if (isVertical) Offset(center.x, end - cornerSize)
                else Offset(end - cornerSize, center.y)
            drawStopIndicator?.invoke(this, stopIndicatorOffset)
        }
        // active track
        val activeTrackStart = if (isRangeSlider) sliderValueStart + startGap else 0f
        val activeTrackEnd = sliderValueEnd - endGap
        val startCornerRadius = if (isRangeSlider) insideCornerSize else cornerSize
        val activeTrackWidth = activeTrackEnd - activeTrackStart
        val activeTrackThreshold = if (enableCornerShrinking) 0f else startCornerRadius
        if (activeTrackWidth > activeTrackThreshold) {
            val trackOffset =
                if (isVertical) Offset(0f, activeTrackStart) else Offset(activeTrackStart, 0f)
            val size =
                if (isVertical) Size(width.toPx(), activeTrackWidth)
                else Size(activeTrackWidth, height.toPx())
            drawTrackPath(
                orientation,
                trackOffset,
                size,
                activeTrackColor,
                startCornerRadius,
                insideCornerSize
            )
        }

        val start = sliderStart + cornerSize
        val end = sliderEnd - cornerSize
        val tickStartGap = sliderValueStart - startGap..sliderValueStart + startGap
        val tickEndGap = sliderValueEnd - endGap..sliderValueEnd + endGap
        tickFractions.forEachIndexed { index, tick ->
            // skip ticks that fall on the stop indicator
            if (drawStopIndicator != null) {
                if ((isRangeSlider && index == 0) || index == tickFractions.size - 1) {
                    return@forEachIndexed
                }
            }

            val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
            val centerTick = lerp(start, end, tick)
            // skip ticks that fall on a gap
            if ((isRangeSlider && centerTick in tickStartGap) || centerTick in tickEndGap) {
                return@forEachIndexed
            }
            val offset =
                if (isVertical) Offset(center.x, centerTick) else Offset(centerTick, center.y)
            drawTick(
                this,
                offset, // offset
                if (outsideFraction) inactiveTickColor else activeTickColor // color
            )
        }
    }

    private fun DrawScope.drawTrackPath(
        orientation: Orientation,
        offset: Offset,
        size: Size,
        color: Color,
        startCornerRadius: Float,
        endCornerRadius: Float
    ) {
        val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
        val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
        val track =
            if (orientation == Vertical) {
                RoundRect(
                    rect = Rect(offset, size = Size(size.width, size.height)),
                    topLeft = startCorner,
                    topRight = startCorner,
                    bottomRight = endCorner,
                    bottomLeft = endCorner
                )
            } else {
                RoundRect(
                    rect = Rect(offset, size = Size(size.width, size.height)),
                    topLeft = startCorner,
                    topRight = endCorner,
                    bottomRight = endCorner,
                    bottomLeft = startCorner
                )
            }
        trackPath.addRoundRect(track)
        drawPath(trackPath, color)
        trackPath.rewind()
    }

    /**
     * The Default stop indicator.
     *
     * @param offset the coordinate where the indicator is to be drawn.
     * @param size the size of the indicator.
     * @param color the color of the indicator.
     */
    fun DrawScope.drawStopIndicator(offset: Offset, size: Dp, color: Color) {
        drawCircle(color = color, center = offset, radius = size.toPx() / 2f)
    }

    /** The default size for the stop indicator at the end of the track. */
    val TrackStopIndicatorSize: Dp = SliderTokens.StopIndicatorSize

    /** The default size for the ticks if steps are greater than 0. */
    val TickSize: Dp = SliderTokens.StopIndicatorSize

    private val trackPath = Path()
}

private fun snapValueToTick(
    current: Float,
    tickFractions: FloatArray,
    minPx: Float,
    maxPx: Float
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) } ?: current
}

private suspend fun AwaitPointerEventScope.awaitSlop(
    id: PointerId,
    type: PointerType
): Pair<PointerInputChange, Float>? {
    var initialDelta = 0f
    val postPointerSlop = { pointerInput: PointerInputChange, offset: Float ->
        pointerInput.consume()
        initialDelta = offset
    }
    val afterSlopResult = awaitHorizontalPointerSlopOrCancellation(id, type, postPointerSlop)
    return if (afterSlopResult != null) afterSlopResult to initialDelta else null
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Scale x.start, x.endInclusive from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x: SliderRange, a2: Float, b2: Float) =
    SliderRange(scale(a1, b1, x.start, a2, b2), scale(a1, b1, x.endInclusive, a2, b2))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.sliderSemantics(state: SliderState, enabled: Boolean): Modifier {
    return semantics {
            if (!enabled) disabled()
            setProgress(
                action = { targetValue ->
                    var newValue =
                        targetValue.coerceIn(state.valueRange.start, state.valueRange.endInclusive)
                    val originalVal = newValue
                    val resolvedValue =
                        if (state.steps > 0) {
                            var distance: Float = newValue
                            for (i in 0..state.steps + 1) {
                                val stepValue =
                                    lerp(
                                        state.valueRange.start,
                                        state.valueRange.endInclusive,
                                        i.toFloat() / (state.steps + 1)
                                    )
                                if (abs(stepValue - originalVal) <= distance) {
                                    distance = abs(stepValue - originalVal)
                                    newValue = stepValue
                                }
                            }
                            newValue
                        } else {
                            newValue
                        }

                    // This is to keep it consistent with AbsSeekbar.java: return false if no
                    // change from current.
                    if (resolvedValue == state.value) {
                        false
                    } else {
                        if (resolvedValue != state.value) {
                            if (state.onValueChange != null) {
                                state.onValueChange?.let { it(resolvedValue) }
                            } else {
                                state.value = resolvedValue
                            }
                        }
                        state.onValueChangeFinished?.invoke()
                        true
                    }
                }
            )
        }
        .then(IncreaseHorizontalSemanticsBounds)
        .progressSemantics(
            state.value,
            state.valueRange.start..state.valueRange.endInclusive,
            state.steps
        )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.rangeSliderStartThumbSemantics(
    state: RangeSliderState,
    enabled: Boolean
): Modifier {
    val valueRange = state.valueRange.start..state.activeRangeEnd

    return semantics {
            if (!enabled) disabled()
            setProgress(
                action = { targetValue ->
                    var newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                    val originalVal = newValue
                    val resolvedValue =
                        if (state.startSteps > 0) {
                            var distance: Float = newValue
                            for (i in 0..state.startSteps + 1) {
                                val stepValue =
                                    lerp(
                                        valueRange.start,
                                        valueRange.endInclusive,
                                        i.toFloat() / (state.startSteps + 1)
                                    )
                                if (abs(stepValue - originalVal) <= distance) {
                                    distance = abs(stepValue - originalVal)
                                    newValue = stepValue
                                }
                            }
                            newValue
                        } else {
                            newValue
                        }

                    // This is to keep it consistent with AbsSeekbar.java: return false if no
                    // change from current.
                    if (resolvedValue == state.activeRangeStart) {
                        false
                    } else {
                        val resolvedRange = SliderRange(resolvedValue, state.activeRangeEnd)
                        val activeRange = SliderRange(state.activeRangeStart, state.activeRangeEnd)
                        if (resolvedRange != activeRange) {
                            if (state.onValueChange != null) {
                                state.onValueChange?.let { it(resolvedRange) }
                            } else {
                                state.activeRangeStart = resolvedRange.start
                                state.activeRangeEnd = resolvedRange.endInclusive
                            }
                        }
                        state.onValueChangeFinished?.invoke()
                        true
                    }
                }
            )
        }
        .then(IncreaseHorizontalSemanticsBounds)
        .progressSemantics(state.activeRangeStart, valueRange, state.startSteps)
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.rangeSliderEndThumbSemantics(
    state: RangeSliderState,
    enabled: Boolean
): Modifier {
    val valueRange = state.activeRangeStart..state.valueRange.endInclusive

    return semantics {
            if (!enabled) disabled()

            setProgress(
                action = { targetValue ->
                    var newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                    val originalVal = newValue
                    val resolvedValue =
                        if (state.endSteps > 0) {
                            var distance: Float = newValue
                            for (i in 0..state.endSteps + 1) {
                                val stepValue =
                                    lerp(
                                        valueRange.start,
                                        valueRange.endInclusive,
                                        i.toFloat() / (state.endSteps + 1)
                                    )
                                if (abs(stepValue - originalVal) <= distance) {
                                    distance = abs(stepValue - originalVal)
                                    newValue = stepValue
                                }
                            }
                            newValue
                        } else {
                            newValue
                        }

                    // This is to keep it consistent with AbsSeekbar.java: return false if no
                    // change from current.
                    if (resolvedValue == state.activeRangeEnd) {
                        false
                    } else {
                        val resolvedRange = SliderRange(state.activeRangeStart, resolvedValue)
                        val activeRange = SliderRange(state.activeRangeStart, state.activeRangeEnd)
                        if (resolvedRange != activeRange) {
                            if (state.onValueChange != null) {
                                state.onValueChange?.let { it(resolvedRange) }
                            } else {
                                state.activeRangeStart = resolvedRange.start
                                state.activeRangeEnd = resolvedRange.endInclusive
                            }
                        }
                        state.onValueChangeFinished?.invoke()
                        true
                    }
                }
            )
        }
        .then(IncreaseHorizontalSemanticsBounds)
        .progressSemantics(state.activeRangeEnd, valueRange, state.endSteps)
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private fun Modifier.sliderTapModifier(
    state: SliderState,
    interactionSource: MutableInteractionSource,
    enabled: Boolean
) =
    if (enabled) {
        pointerInput(state, interactionSource) {
            detectTapGestures(
                onPress = { state.onPress(it) },
                onTap = {
                    state.dispatchRawDelta(0f)
                    state.gestureEndAction()
                }
            )
        }
    } else {
        this
    }

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private fun Modifier.rangeSliderPressDragModifier(
    state: RangeSliderState,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    enabled: Boolean
): Modifier =
    if (enabled) {
        pointerInput(startInteractionSource, endInteractionSource, state) {
            val rangeSliderLogic =
                RangeSliderLogic(state, startInteractionSource, endInteractionSource)
            coroutineScope {
                awaitEachGesture {
                    val event = awaitFirstDown(requireUnconsumed = false)
                    val interaction = DragInteraction.Start()
                    var posX =
                        if (state.isRtl) state.totalWidth - event.position.x else event.position.x
                    val compare = rangeSliderLogic.compareOffsets(posX)
                    var draggingStart =
                        if (compare != 0) {
                            compare < 0
                        } else {
                            state.rawOffsetStart > posX
                        }

                    awaitSlop(event.id, event.type)?.let {
                        val slop = viewConfiguration.pointerSlop(event.type)
                        val shouldUpdateCapturedThumb =
                            abs(state.rawOffsetEnd - posX) < slop &&
                                abs(state.rawOffsetStart - posX) < slop
                        if (shouldUpdateCapturedThumb) {
                            val dir = it.second
                            draggingStart = if (state.isRtl) dir >= 0f else dir < 0f
                            posX += it.first.positionChange().x
                        }
                    }

                    rangeSliderLogic.captureThumb(
                        draggingStart,
                        posX,
                        interaction,
                        this@coroutineScope
                    )

                    val finishInteraction =
                        try {
                            val success =
                                horizontalDrag(pointerId = event.id) {
                                    val deltaX = it.positionChange().x
                                    state.onDrag(
                                        draggingStart,
                                        if (state.isRtl) -deltaX else deltaX
                                    )
                                }
                            if (success) {
                                DragInteraction.Stop(interaction)
                            } else {
                                DragInteraction.Cancel(interaction)
                            }
                        } catch (e: CancellationException) {
                            DragInteraction.Cancel(interaction)
                        }

                    state.gestureEndAction(draggingStart)
                    launch {
                        rangeSliderLogic.activeInteraction(draggingStart).emit(finishInteraction)
                    }
                }
            }
        }
    } else {
        this
    }

@OptIn(ExperimentalMaterial3Api::class)
private class RangeSliderLogic(
    val state: RangeSliderState,
    val startInteractionSource: MutableInteractionSource,
    val endInteractionSource: MutableInteractionSource
) {
    fun activeInteraction(draggingStart: Boolean): MutableInteractionSource =
        if (draggingStart) startInteractionSource else endInteractionSource

    fun compareOffsets(eventX: Float): Int {
        val diffStart = abs(state.rawOffsetStart - eventX)
        val diffEnd = abs(state.rawOffsetEnd - eventX)
        return diffStart.compareTo(diffEnd)
    }

    fun captureThumb(
        draggingStart: Boolean,
        posX: Float,
        interaction: Interaction,
        scope: CoroutineScope
    ) {
        state.onDrag(
            draggingStart,
            posX - if (draggingStart) state.rawOffsetStart else state.rawOffsetEnd
        )
        scope.launch { activeInteraction(draggingStart).emit(interaction) }
    }
}

/**
 * Represents the color used by a [Slider] in different states.
 *
 * @param thumbColor thumb color when enabled
 * @param activeTrackColor color of the track in the part that is "active", meaning that the thumb
 *   is ahead of it
 * @param activeTickColor colors to be used to draw tick marks on the active track, if `steps` is
 *   specified
 * @param inactiveTrackColor color of the track in the part that is "inactive", meaning that the
 *   thumb is before it
 * @param inactiveTickColor colors to be used to draw tick marks on the inactive track, if `steps`
 *   are specified on the Slider is specified
 * @param disabledThumbColor thumb colors when disabled
 * @param disabledActiveTrackColor color of the track in the "active" part when the Slider is
 *   disabled
 * @param disabledActiveTickColor colors to be used to draw tick marks on the active track when
 *   Slider is disabled and when `steps` are specified on it
 * @param disabledInactiveTrackColor color of the track in the "inactive" part when the Slider is
 *   disabled
 * @param disabledInactiveTickColor colors to be used to draw tick marks on the inactive part of the
 *   track when Slider is disabled and when `steps` are specified on it
 * @constructor create an instance with arbitrary colors. See [SliderDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
class SliderColors(
    val thumbColor: Color,
    val activeTrackColor: Color,
    val activeTickColor: Color,
    val inactiveTrackColor: Color,
    val inactiveTickColor: Color,
    val disabledThumbColor: Color,
    val disabledActiveTrackColor: Color,
    val disabledActiveTickColor: Color,
    val disabledInactiveTrackColor: Color,
    val disabledInactiveTickColor: Color
) {

    /**
     * Returns a copy of this SelectableChipColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        thumbColor: Color = this.thumbColor,
        activeTrackColor: Color = this.activeTrackColor,
        activeTickColor: Color = this.activeTickColor,
        inactiveTrackColor: Color = this.inactiveTrackColor,
        inactiveTickColor: Color = this.inactiveTickColor,
        disabledThumbColor: Color = this.disabledThumbColor,
        disabledActiveTrackColor: Color = this.disabledActiveTrackColor,
        disabledActiveTickColor: Color = this.disabledActiveTickColor,
        disabledInactiveTrackColor: Color = this.disabledInactiveTrackColor,
        disabledInactiveTickColor: Color = this.disabledInactiveTickColor,
    ) =
        SliderColors(
            thumbColor.takeOrElse { this.thumbColor },
            activeTrackColor.takeOrElse { this.activeTrackColor },
            activeTickColor.takeOrElse { this.activeTickColor },
            inactiveTrackColor.takeOrElse { this.inactiveTrackColor },
            inactiveTickColor.takeOrElse { this.inactiveTickColor },
            disabledThumbColor.takeOrElse { this.disabledThumbColor },
            disabledActiveTrackColor.takeOrElse { this.disabledActiveTrackColor },
            disabledActiveTickColor.takeOrElse { this.disabledActiveTickColor },
            disabledInactiveTrackColor.takeOrElse { this.disabledInactiveTrackColor },
            disabledInactiveTickColor.takeOrElse { this.disabledInactiveTickColor },
        )

    @Stable
    internal fun thumbColor(enabled: Boolean): Color =
        if (enabled) thumbColor else disabledThumbColor

    @Stable
    internal fun trackColor(enabled: Boolean, active: Boolean): Color =
        if (enabled) {
            if (active) activeTrackColor else inactiveTrackColor
        } else {
            if (active) disabledActiveTrackColor else disabledInactiveTrackColor
        }

    @Stable
    internal fun tickColor(enabled: Boolean, active: Boolean): Color =
        if (enabled) {
            if (active) activeTickColor else inactiveTickColor
        } else {
            if (active) disabledActiveTickColor else disabledInactiveTickColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SliderColors) return false

        if (thumbColor != other.thumbColor) return false
        if (activeTrackColor != other.activeTrackColor) return false
        if (activeTickColor != other.activeTickColor) return false
        if (inactiveTrackColor != other.inactiveTrackColor) return false
        if (inactiveTickColor != other.inactiveTickColor) return false
        if (disabledThumbColor != other.disabledThumbColor) return false
        if (disabledActiveTrackColor != other.disabledActiveTrackColor) return false
        if (disabledActiveTickColor != other.disabledActiveTickColor) return false
        if (disabledInactiveTrackColor != other.disabledInactiveTrackColor) return false
        if (disabledInactiveTickColor != other.disabledInactiveTickColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thumbColor.hashCode()
        result = 31 * result + activeTrackColor.hashCode()
        result = 31 * result + activeTickColor.hashCode()
        result = 31 * result + inactiveTrackColor.hashCode()
        result = 31 * result + inactiveTickColor.hashCode()
        result = 31 * result + disabledThumbColor.hashCode()
        result = 31 * result + disabledActiveTrackColor.hashCode()
        result = 31 * result + disabledActiveTickColor.hashCode()
        result = 31 * result + disabledInactiveTrackColor.hashCode()
        result = 31 * result + disabledInactiveTickColor.hashCode()
        return result
    }
}

// Internal to be referred to in tests
internal val TrackHeight = SliderTokens.InactiveTrackHeight
internal val ThumbWidth = SliderTokens.HandleWidth
private val ThumbHeight = SliderTokens.HandleHeight
private val ThumbSize = DpSize(ThumbWidth, ThumbHeight)
private val VerticalThumbSize = DpSize(ThumbHeight, ThumbWidth)
private val ThumbTrackGapSize: Dp = SliderTokens.ActiveHandleLeadingSpace
private val TrackInsideCornerSize: Dp = 2.dp
private const val SliderRangeTolerance = 0.0001

private enum class SliderComponents {
    THUMB,
    TRACK
}

private enum class RangeSliderComponents {
    ENDTHUMB,
    STARTTHUMB,
    TRACK
}

/**
 * Class that holds information about [Slider]'s and [RangeSlider]'s active track and fractional
 * positions where the discrete ticks should be drawn on the track.
 */
@Suppress("DEPRECATION")
@Deprecated("Not necessary with the introduction of Slider state")
@Stable
class SliderPositions(
    initialActiveRange: ClosedFloatingPointRange<Float> = 0f..1f,
    initialTickFractions: FloatArray = floatArrayOf()
) {
    /**
     * [ClosedFloatingPointRange] that indicates the current active range for the start to thumb for
     * a [Slider] and start thumb to end thumb for a [RangeSlider].
     */
    var activeRange: ClosedFloatingPointRange<Float> by mutableStateOf(initialActiveRange)
        internal set

    /**
     * The discrete points where a tick should be drawn on the track. Each value of tickFractions
     * should be within the range [0f, 1f]. If the track is continuous, then tickFractions will be
     * an empty [FloatArray].
     */
    var tickFractions: FloatArray by mutableStateOf(initialTickFractions)
        internal set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SliderPositions) return false

        if (activeRange != other.activeRange) return false
        if (!tickFractions.contentEquals(other.tickFractions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activeRange.hashCode()
        result = 31 * result + tickFractions.contentHashCode()
        return result
    }
}

/**
 * Class that holds information about [Slider]'s active range.
 *
 * @param value [Float] that indicates the initial position of the thumb. If outside of [valueRange]
 *   provided, value will be coerced to this range.
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param onValueChangeFinished lambda to be invoked when value change has ended. This callback
 *   shouldn't be used to update the range slider values (use [onValueChange] for that), but rather
 *   to know when the user has completed selecting a new value by ending a drag or a click.
 * @param valueRange range of values that Slider values can take. [value] will be coerced to this
 *   range.
 */
@ExperimentalMaterial3Api
class SliderState(
    value: Float = 0f,
    @IntRange(from = 0) val steps: Int = 0,
    var onValueChangeFinished: (() -> Unit)? = null,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) : DraggableState {

    private var valueState by mutableFloatStateOf(value)

    /** [Float] that indicates the value that the thumb currently is in respect to the track. */
    var value: Float
        set(newVal) {
            valueState = calculateSnappedValue(newVal)
        }
        get() = valueState

    private fun calculateSnappedValue(newVal: Float): Float {
        val coercedValue = newVal.coerceIn(valueRange.start, valueRange.endInclusive)
        return snapValueToTick(
            coercedValue,
            tickFractions,
            valueRange.start,
            valueRange.endInclusive
        )
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        val maxPx: Float
        val minPx: Float
        if (orientation == Vertical) {
            maxPx = max(totalHeight - thumbHeight / 2f, 0f)
            minPx = min(thumbHeight / 2f, maxPx)
        } else {
            maxPx = max(totalWidth - thumbWidth / 2f, 0f)
            minPx = min(thumbWidth / 2f, maxPx)
        }
        rawOffset = (rawOffset + delta + pressOffset)
        pressOffset = 0f
        val offsetInTrack = snapValueToTick(rawOffset, tickFractions, minPx, maxPx)
        val scaledUserValue = scaleToUserValue(minPx, maxPx, offsetInTrack)
        if (scaledUserValue != this.value) {
            if (onValueChange != null) {
                onValueChange?.let { it(scaledUserValue) }
            } else {
                this.value = scaledUserValue
            }
        }
    }

    /** Callback in which value should be updated. */
    var onValueChange: ((Float) -> Unit)? = null

    internal val tickFractions = stepsToTickFractions(steps)
    private var totalWidth by mutableIntStateOf(0)
    private var totalHeight by mutableIntStateOf(0)
    internal var isRtl = false
    internal var thumbWidth by mutableIntStateOf(0)
    internal var thumbHeight by mutableIntStateOf(0)
    internal var trackWidth by mutableIntStateOf(0)
    internal var trackHeight by mutableIntStateOf(0)
    internal var orientation = Horizontal
    internal var reverseVerticalDirection = false

    /** The fraction of the track that the thumb currently is in. */
    val coercedValueAsFraction: Float
        get() =
            calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                value.coerceIn(valueRange.start, valueRange.endInclusive)
            )

    var isDragging by mutableStateOf(false)
        private set

    internal fun updateDimensions(
        newTrackWidth: Int,
        newTrackHeight: Int,
        newTotalWidth: Int,
        newTotalHeight: Int
    ) {
        trackWidth = newTrackWidth
        trackHeight = newTrackHeight
        totalWidth = newTotalWidth
        totalHeight = newTotalHeight
    }

    internal val gestureEndAction = {
        if (!isDragging) {
            // check isDragging in case the change is still in progress (touch -> drag case)
            onValueChangeFinished?.invoke()
        }
    }

    internal fun onPress(pos: Offset) {
        val to =
            if (orientation == Vertical) {
                if (reverseVerticalDirection) totalHeight - pos.y else pos.y
            } else {
                if (isRtl) totalWidth - pos.x else pos.x
            }
        pressOffset = to - rawOffset
    }

    private var rawOffset by mutableFloatStateOf(scaleToOffset(0f, 0f, value))
    private var pressOffset by mutableFloatStateOf(0f)
    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    private val scrollMutex = MutatorMutex()

    private fun scaleToUserValue(minPx: Float, maxPx: Float, offset: Float) =
        scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    private fun scaleToOffset(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)
}

/**
 * Class that holds information about [RangeSlider]'s active range.
 *
 * @param activeRangeStart [Float] that indicates the initial start of the active range of the
 *   slider. If outside of [valueRange] provided, value will be coerced to this range.
 * @param activeRangeEnd [Float] that indicates the initial end of the active range of the slider.
 *   If outside of [valueRange] provided, value will be coerced to this range.
 * @param steps if positive, specifies the amount of discrete allowable values between the endpoints
 *   of [valueRange]. For example, a range from 0 to 10 with 4 [steps] allows 4 values evenly
 *   distributed between 0 and 10 (i.e., 2, 4, 6, 8). If [steps] is 0, the slider will behave
 *   continuously and allow any value from the range. Must not be negative.
 * @param onValueChangeFinished lambda to be invoked when value change has ended. This callback
 *   shouldn't be used to update the range slider values (use [onValueChange] for that), but rather
 *   to know when the user has completed selecting a new value by ending a drag or a click.
 * @param valueRange range of values that Range Slider values can take. [activeRangeStart] and
 *   [activeRangeEnd] will be coerced to this range.
 */
@ExperimentalMaterial3Api
class RangeSliderState(
    activeRangeStart: Float = 0f,
    activeRangeEnd: Float = 1f,
    @IntRange(from = 0) val steps: Int = 0,
    var onValueChangeFinished: (() -> Unit)? = null,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    private var activeRangeStartState by mutableFloatStateOf(activeRangeStart)
    private var activeRangeEndState by mutableFloatStateOf(activeRangeEnd)

    /** [Float] that indicates the start of the current active range for the [RangeSlider]. */
    var activeRangeStart: Float
        set(newVal) {
            val coercedValue = newVal.coerceIn(valueRange.start, activeRangeEnd)
            val snappedValue =
                snapValueToTick(
                    coercedValue,
                    tickFractions,
                    valueRange.start,
                    valueRange.endInclusive
                )
            activeRangeStartState = snappedValue
        }
        get() = activeRangeStartState

    /** [Float] that indicates the end of the current active range for the [RangeSlider]. */
    var activeRangeEnd: Float
        set(newVal) {
            val coercedValue = newVal.coerceIn(activeRangeStart, valueRange.endInclusive)
            val snappedValue =
                snapValueToTick(
                    coercedValue,
                    tickFractions,
                    valueRange.start,
                    valueRange.endInclusive
                )
            activeRangeEndState = snappedValue
        }
        get() = activeRangeEndState

    internal var onValueChange: ((SliderRange) -> Unit)? = null

    internal val tickFractions = stepsToTickFractions(steps)

    internal var trackWidth by mutableFloatStateOf(0f)
    internal var trackHeight by mutableFloatStateOf(0f)
    internal var startThumbWidth by mutableFloatStateOf(0f)
    internal var startThumbHeight by mutableFloatStateOf(0f)
    internal var endThumbWidth by mutableFloatStateOf(0f)
    internal var endThumbHeight by mutableFloatStateOf(0f)
    internal var totalWidth by mutableIntStateOf(0)
    internal var rawOffsetStart by mutableFloatStateOf(0f)
    internal var rawOffsetEnd by mutableFloatStateOf(0f)

    internal var isRtl by mutableStateOf(false)

    internal val gestureEndAction: (Boolean) -> Unit = { onValueChangeFinished?.invoke() }

    private var maxPx by mutableFloatStateOf(0f)
    private var minPx by mutableFloatStateOf(0f)

    internal fun onDrag(isStart: Boolean, offset: Float) {
        val offsetRange =
            if (isStart) {
                rawOffsetStart = (rawOffsetStart + offset)
                rawOffsetEnd = scaleToOffset(minPx, maxPx, activeRangeEnd)
                val offsetEnd = rawOffsetEnd
                var offsetStart = rawOffsetStart.coerceIn(minPx, offsetEnd)
                offsetStart = snapValueToTick(offsetStart, tickFractions, minPx, maxPx)
                SliderRange(offsetStart, offsetEnd)
            } else {
                rawOffsetEnd = (rawOffsetEnd + offset)
                rawOffsetStart = scaleToOffset(minPx, maxPx, activeRangeStart)
                val offsetStart = rawOffsetStart
                var offsetEnd = rawOffsetEnd.coerceIn(offsetStart, maxPx)
                offsetEnd = snapValueToTick(offsetEnd, tickFractions, minPx, maxPx)
                SliderRange(offsetStart, offsetEnd)
            }
        val scaledUserValue = scaleToUserValue(minPx, maxPx, offsetRange)
        if (scaledUserValue != SliderRange(activeRangeStart, activeRangeEnd)) {
            if (onValueChange != null) {
                onValueChange?.let { it(scaledUserValue) }
            } else {
                this.activeRangeStart = scaledUserValue.start
                this.activeRangeEnd = scaledUserValue.endInclusive
            }
        }
    }

    internal val coercedActiveRangeStartAsFraction
        get() = calcFraction(valueRange.start, valueRange.endInclusive, activeRangeStart)

    internal val coercedActiveRangeEndAsFraction
        get() = calcFraction(valueRange.start, valueRange.endInclusive, activeRangeEnd)

    internal val startSteps
        get() = floor(steps * coercedActiveRangeEndAsFraction).toInt()

    internal val endSteps
        get() = floor(steps * (1f - coercedActiveRangeStartAsFraction)).toInt()

    // scales range offset from within minPx..maxPx to within valueRange.start..valueRange.end
    private fun scaleToUserValue(minPx: Float, maxPx: Float, offset: SliderRange) =
        scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    // scales float userValue within valueRange.start..valueRange.end to within minPx..maxPx
    private fun scaleToOffset(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

    internal fun updateMinMaxPx() {
        val newMaxPx = max(totalWidth - endThumbWidth / 2, 0f)
        val newMinPx = min(startThumbWidth / 2, newMaxPx)
        if (minPx != newMinPx || maxPx != newMaxPx) {
            minPx = newMinPx
            maxPx = newMaxPx
            rawOffsetStart = scaleToOffset(minPx, maxPx, activeRangeStart)
            rawOffsetEnd = scaleToOffset(minPx, maxPx, activeRangeEnd)
        }
    }
}

/**
 * Immutable float range for [RangeSlider]
 *
 * Used in [RangeSlider] to determine the active track range for the component. The range is as
 * follows: SliderRange.start..SliderRange.endInclusive.
 */
@Immutable
@JvmInline
internal value class SliderRange(val packedValue: Long) {
    /** start of the [SliderRange] */
    @Stable
    val start: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "SliderRange is unspecified" }
            return unpackFloat1(packedValue)
        }

    /** End (inclusive) of the [SliderRange] */
    @Stable
    val endInclusive: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) { "SliderRange is unspecified" }
            return unpackFloat2(packedValue)
        }

    companion object {
        /**
         * Represents an unspecified [SliderRange] value, usually a replacement for `null` when a
         * primitive value is desired.
         */
        @Stable val Unspecified = SliderRange(Float.NaN, Float.NaN)
    }

    /** String representation of the [SliderRange] */
    override fun toString() =
        if (isSpecified) {
            "$start..$endInclusive"
        } else {
            "FloatRange.Unspecified"
        }
}

/**
 * Creates a [SliderRange] from a given start and endInclusive float. It requires endInclusive to
 * be >= start.
 *
 * @param start float that indicates the start of the range
 * @param endInclusive float that indicates the end of the range
 */
@Stable
internal fun SliderRange(start: Float, endInclusive: Float): SliderRange {
    val isUnspecified = start.isNaN() && endInclusive.isNaN()

    require(isUnspecified || start <= endInclusive + SliderRangeTolerance) {
        "start($start) must be <= endInclusive($endInclusive)"
    }
    return SliderRange(packFloats(start, endInclusive))
}

/**
 * Creates a [SliderRange] from a given [ClosedFloatingPointRange]. It requires
 * range.endInclusive >= range.start.
 *
 * @param range the ClosedFloatingPointRange<Float> for the range.
 */
@Stable
internal fun SliderRange(range: ClosedFloatingPointRange<Float>): SliderRange {
    val start = range.start
    val endInclusive = range.endInclusive
    val isUnspecified = start.isNaN() && endInclusive.isNaN()
    require(isUnspecified || start <= endInclusive + SliderRangeTolerance) {
        "ClosedFloatingPointRange<Float>.start($start) must be <= " +
            "ClosedFloatingPoint.endInclusive($endInclusive)"
    }
    return SliderRange(packFloats(start, endInclusive))
}

/** Check for if a given [SliderRange] is not [SliderRange.Unspecified]. */
@Stable
internal val SliderRange.isSpecified: Boolean
    get() = packedValue != SliderRange.Unspecified.packedValue
