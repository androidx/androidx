/*
 * Copyright 2023 The Android Open Source Project
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
@file:OptIn(ExperimentalMaterial3Api::class)

package androidx.compose.material3

import androidx.annotation.IntRange
import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.intListOf
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatePriority.PreventUserInput
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.TimeInputTokens
import androidx.compose.material3.tokens.TimeInputTokens.PeriodSelectorContainerHeight
import androidx.compose.material3.tokens.TimeInputTokens.PeriodSelectorContainerWidth
import androidx.compose.material3.tokens.TimeInputTokens.TimeFieldContainerHeight
import androidx.compose.material3.tokens.TimeInputTokens.TimeFieldContainerWidth
import androidx.compose.material3.tokens.TimeInputTokens.TimeFieldSeparatorColor
import androidx.compose.material3.tokens.TimePickerTokens
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialLabelTextFont
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorCenterContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorHandleContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorHandleContainerSize
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialSelectorTrackContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.ClockDialUnselectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.ContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorContainerShape
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorHorizontalContainerHeight
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorHorizontalContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorOutlineColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorSelectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorUnselectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorVerticalContainerHeight
import androidx.compose.material3.tokens.TimePickerTokens.PeriodSelectorVerticalContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerHeight
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerShape
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorContainerWidth
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorLabelTextFont
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorSelectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorSelectedLabelTextColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorUnselectedContainerColor
import androidx.compose.material3.tokens.TimePickerTokens.TimeSelectorUnselectedLabelTextColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/time-pickers/overview" class="external"
 * target="_blank">Material Design time picker</a>.
 *
 * Time pickers help users select and set a specific time.
 *
 * Shows a picker that allows the user to select time. Subscribe to updates through
 * [TimePickerState]
 *
 * ![Time picker
 * image](https://developer.android.com/images/reference/androidx/compose/material3/time-picker.png)
 *
 * @sample androidx.compose.material3.samples.TimePickerSample
 * @sample androidx.compose.material3.samples.TimePickerSwitchableSample
 *
 * [state] state for this timepicker, allows to subscribe to changes to [TimePickerState.hour] and
 * [TimePickerState.minute], and set the initial time for this picker.
 *
 * @param state state for this time input, allows to subscribe to changes to [TimePickerState.hour]
 *   and [TimePickerState.minute], and set the initial time for this input.
 * @param modifier the [Modifier] to be applied to this time input
 * @param colors colors [TimePickerColors] that will be used to resolve the colors used for this
 *   time picker in different states. See [TimePickerDefaults.colors].
 * @param layoutType, the different [TimePickerLayoutType] supported by this time picker, it will
 *   change the position and sizing of different components of the timepicker.
 */
@Composable
@ExperimentalMaterial3Api
fun TimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType(),
) {
    val a11yServicesEnabled by rememberAccessibilityServiceState()
    val analogState = remember(state) { AnalogTimePickerState(state) }
    if (layoutType == TimePickerLayoutType.Vertical) {
        VerticalTimePicker(
            state = analogState,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = !a11yServicesEnabled
        )
    } else {
        HorizontalTimePicker(
            state = analogState,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = !a11yServicesEnabled
        )
    }
}

/**
 * Time pickers help users select and set a specific time.
 *
 * Shows a time input that allows the user to enter the time via two text fields, one for minutes
 * and one for hours Subscribe to updates through [TimePickerState]
 *
 * @sample androidx.compose.material3.samples.TimeInputSample
 * @param state state for this timepicker, allows to subscribe to changes to [TimePickerState.hour]
 *   and [TimePickerState.minute], and set the initial time for this picker.
 * @param modifier the [Modifier] to be applied to this time input
 * @param colors colors [TimePickerColors] that will be used to resolve the colors used for this
 *   time input in different states. See [TimePickerDefaults.colors].
 */
@Composable
@ExperimentalMaterial3Api
fun TimeInput(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
) {
    TimeInputImpl(modifier, colors, state)
}

/** Contains the default values used by [TimePicker] */
@ExperimentalMaterial3Api
@Stable
object TimePickerDefaults {

    /** Default colors used by a [TimePicker] in different states */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultTimePickerColors

    /**
     * Default colors used by a [TimePicker] in different states
     *
     * @param clockDialColor The color of the clock dial.
     * @param clockDialSelectedContentColor the color of the numbers of the clock dial when they are
     *   selected or overlapping with the selector
     * @param clockDialUnselectedContentColor the color of the numbers of the clock dial when they
     *   are unselected
     * @param selectorColor The color of the clock dial selector.
     * @param containerColor The container color of the time picker.
     * @param periodSelectorBorderColor the color used for the border of the AM/PM toggle.
     * @param periodSelectorSelectedContainerColor the color used for the selected container of the
     *   AM/PM toggle
     * @param periodSelectorUnselectedContainerColor the color used for the unselected container of
     *   the AM/PM toggle
     * @param periodSelectorSelectedContentColor color used for the selected content of the AM/PM
     *   toggle
     * @param periodSelectorUnselectedContentColor color used for the unselected content of the
     *   AM/PM toggle
     * @param timeSelectorSelectedContainerColor color used for the selected container of the
     *   display buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContainerColor color used for the unselected container of the
     *   display buttons to switch between hour and minutes
     * @param timeSelectorSelectedContentColor color used for the selected content of the display
     *   buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContentColor color used for the unselected content of the
     *   display buttons to switch between hour and minutes
     */
    @Composable
    fun colors(
        clockDialColor: Color = Color.Unspecified,
        clockDialSelectedContentColor: Color = Color.Unspecified,
        clockDialUnselectedContentColor: Color = Color.Unspecified,
        selectorColor: Color = Color.Unspecified,
        containerColor: Color = Color.Unspecified,
        periodSelectorBorderColor: Color = Color.Unspecified,
        periodSelectorSelectedContainerColor: Color = Color.Unspecified,
        periodSelectorUnselectedContainerColor: Color = Color.Unspecified,
        periodSelectorSelectedContentColor: Color = Color.Unspecified,
        periodSelectorUnselectedContentColor: Color = Color.Unspecified,
        timeSelectorSelectedContainerColor: Color = Color.Unspecified,
        timeSelectorUnselectedContainerColor: Color = Color.Unspecified,
        timeSelectorSelectedContentColor: Color = Color.Unspecified,
        timeSelectorUnselectedContentColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultTimePickerColors.copy(
            clockDialColor = clockDialColor,
            clockDialSelectedContentColor = clockDialSelectedContentColor,
            clockDialUnselectedContentColor = clockDialUnselectedContentColor,
            selectorColor = selectorColor,
            containerColor = containerColor,
            periodSelectorBorderColor = periodSelectorBorderColor,
            periodSelectorSelectedContainerColor = periodSelectorSelectedContainerColor,
            periodSelectorUnselectedContainerColor = periodSelectorUnselectedContainerColor,
            periodSelectorSelectedContentColor = periodSelectorSelectedContentColor,
            periodSelectorUnselectedContentColor = periodSelectorUnselectedContentColor,
            timeSelectorSelectedContainerColor = timeSelectorSelectedContainerColor,
            timeSelectorUnselectedContainerColor = timeSelectorUnselectedContainerColor,
            timeSelectorSelectedContentColor = timeSelectorSelectedContentColor,
            timeSelectorUnselectedContentColor = timeSelectorUnselectedContentColor
        )

    internal val ColorScheme.defaultTimePickerColors: TimePickerColors
        get() {
            return defaultTimePickerColorsCached
                ?: TimePickerColors(
                        clockDialColor = fromToken(ClockDialColor),
                        clockDialSelectedContentColor = fromToken(ClockDialSelectedLabelTextColor),
                        clockDialUnselectedContentColor =
                            fromToken(ClockDialUnselectedLabelTextColor),
                        selectorColor = fromToken(ClockDialSelectorHandleContainerColor),
                        containerColor = fromToken(ContainerColor),
                        periodSelectorBorderColor = fromToken(PeriodSelectorOutlineColor),
                        periodSelectorSelectedContainerColor =
                            fromToken(PeriodSelectorSelectedContainerColor),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor =
                            fromToken(PeriodSelectorSelectedLabelTextColor),
                        periodSelectorUnselectedContentColor =
                            fromToken(PeriodSelectorUnselectedLabelTextColor),
                        timeSelectorSelectedContainerColor =
                            fromToken(TimeSelectorSelectedContainerColor),
                        timeSelectorUnselectedContainerColor =
                            fromToken(TimeSelectorUnselectedContainerColor),
                        timeSelectorSelectedContentColor =
                            fromToken(TimeSelectorSelectedLabelTextColor),
                        timeSelectorUnselectedContentColor =
                            fromToken(TimeSelectorUnselectedLabelTextColor),
                    )
                    .also { defaultTimePickerColorsCached = it }
        }

    /** Default layout type, uses the screen dimensions to choose an appropriate layout. */
    @ReadOnlyComposable
    @Composable
    fun layoutType(): TimePickerLayoutType = defaultTimePickerLayoutType
}

/**
 * Represents the colors used by a [TimePicker] in different states
 *
 * @param clockDialColor The color of the clock dial.
 * @param clockDialSelectedContentColor the color of the numbers of the clock dial when they are
 *   selected or overlapping with the selector
 * @param clockDialUnselectedContentColor the color of the numbers of the clock dial when they are
 *   unselected
 * @param selectorColor The color of the clock dial selector.
 * @param containerColor The container color of the time picker.
 * @param periodSelectorBorderColor the color used for the border of the AM/PM toggle.
 * @param periodSelectorSelectedContainerColor the color used for the selected container of the
 *   AM/PM toggle
 * @param periodSelectorUnselectedContainerColor the color used for the unselected container of the
 *   AM/PM toggle
 * @param periodSelectorSelectedContentColor color used for the selected content of the AM/PM toggle
 * @param periodSelectorUnselectedContentColor color used for the unselected content of the AM/PM
 *   toggle
 * @param timeSelectorSelectedContainerColor color used for the selected container of the display
 *   buttons to switch between hour and minutes
 * @param timeSelectorUnselectedContainerColor color used for the unselected container of the
 *   display buttons to switch between hour and minutes
 * @param timeSelectorSelectedContentColor color used for the selected content of the display
 *   buttons to switch between hour and minutes
 * @param timeSelectorUnselectedContentColor color used for the unselected content of the display
 *   buttons to switch between hour and minutes
 * @constructor create an instance with arbitrary colors. See [TimePickerDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
@ExperimentalMaterial3Api
class TimePickerColors
constructor(
    val clockDialColor: Color,
    val selectorColor: Color,
    val containerColor: Color,
    val periodSelectorBorderColor: Color,
    val clockDialSelectedContentColor: Color,
    val clockDialUnselectedContentColor: Color,
    val periodSelectorSelectedContainerColor: Color,
    val periodSelectorUnselectedContainerColor: Color,
    val periodSelectorSelectedContentColor: Color,
    val periodSelectorUnselectedContentColor: Color,
    val timeSelectorSelectedContainerColor: Color,
    val timeSelectorUnselectedContainerColor: Color,
    val timeSelectorSelectedContentColor: Color,
    val timeSelectorUnselectedContentColor: Color,
) {
    /**
     * Returns a copy of this TimePickerColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        clockDialColor: Color = this.containerColor,
        selectorColor: Color = this.selectorColor,
        containerColor: Color = this.containerColor,
        periodSelectorBorderColor: Color = this.periodSelectorBorderColor,
        clockDialSelectedContentColor: Color = this.clockDialSelectedContentColor,
        clockDialUnselectedContentColor: Color = this.clockDialUnselectedContentColor,
        periodSelectorSelectedContainerColor: Color = this.periodSelectorSelectedContainerColor,
        periodSelectorUnselectedContainerColor: Color = this.periodSelectorUnselectedContainerColor,
        periodSelectorSelectedContentColor: Color = this.periodSelectorSelectedContentColor,
        periodSelectorUnselectedContentColor: Color = this.periodSelectorUnselectedContentColor,
        timeSelectorSelectedContainerColor: Color = this.timeSelectorSelectedContainerColor,
        timeSelectorUnselectedContainerColor: Color = this.timeSelectorUnselectedContainerColor,
        timeSelectorSelectedContentColor: Color = this.timeSelectorSelectedContentColor,
        timeSelectorUnselectedContentColor: Color = this.timeSelectorUnselectedContentColor,
    ) =
        TimePickerColors(
            clockDialColor.takeOrElse { this.clockDialColor },
            selectorColor.takeOrElse { this.selectorColor },
            containerColor.takeOrElse { this.containerColor },
            periodSelectorBorderColor.takeOrElse { this.periodSelectorBorderColor },
            clockDialSelectedContentColor.takeOrElse { this.clockDialSelectedContentColor },
            clockDialUnselectedContentColor.takeOrElse { this.clockDialUnselectedContentColor },
            periodSelectorSelectedContainerColor.takeOrElse {
                this.periodSelectorSelectedContainerColor
            },
            periodSelectorUnselectedContainerColor.takeOrElse {
                this.periodSelectorUnselectedContainerColor
            },
            periodSelectorSelectedContentColor.takeOrElse {
                this.periodSelectorSelectedContentColor
            },
            periodSelectorUnselectedContentColor.takeOrElse {
                this.periodSelectorUnselectedContentColor
            },
            timeSelectorSelectedContainerColor.takeOrElse {
                this.timeSelectorSelectedContainerColor
            },
            timeSelectorUnselectedContainerColor.takeOrElse {
                this.timeSelectorUnselectedContainerColor
            },
            timeSelectorSelectedContentColor.takeOrElse { this.timeSelectorSelectedContentColor },
            timeSelectorUnselectedContentColor.takeOrElse {
                this.timeSelectorUnselectedContentColor
            },
        )

    @Stable
    internal fun periodSelectorContainerColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContainerColor
        } else {
            periodSelectorUnselectedContainerColor
        }

    @Stable
    internal fun periodSelectorContentColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContentColor
        } else {
            periodSelectorUnselectedContentColor
        }

    @Stable
    internal fun timeSelectorContainerColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContainerColor
        } else {
            timeSelectorUnselectedContainerColor
        }

    @Stable
    internal fun timeSelectorContentColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContentColor
        } else {
            timeSelectorUnselectedContentColor
        }

    @Stable
    internal fun clockDialContentColor(selected: Boolean) =
        if (selected) {
            clockDialSelectedContentColor
        } else {
            clockDialUnselectedContentColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as TimePickerColors

        if (clockDialColor != other.clockDialColor) return false
        if (selectorColor != other.selectorColor) return false
        if (containerColor != other.containerColor) return false
        if (periodSelectorBorderColor != other.periodSelectorBorderColor) return false
        if (periodSelectorSelectedContainerColor != other.periodSelectorSelectedContainerColor)
            return false
        if (periodSelectorUnselectedContainerColor != other.periodSelectorUnselectedContainerColor)
            return false
        if (periodSelectorSelectedContentColor != other.periodSelectorSelectedContentColor)
            return false
        if (periodSelectorUnselectedContentColor != other.periodSelectorUnselectedContentColor)
            return false
        if (timeSelectorSelectedContainerColor != other.timeSelectorSelectedContainerColor)
            return false
        if (timeSelectorUnselectedContainerColor != other.timeSelectorUnselectedContainerColor)
            return false
        if (timeSelectorSelectedContentColor != other.timeSelectorSelectedContentColor) return false
        if (timeSelectorUnselectedContentColor != other.timeSelectorUnselectedContentColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = clockDialColor.hashCode()
        result = 31 * result + selectorColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + periodSelectorBorderColor.hashCode()
        result = 31 * result + periodSelectorSelectedContainerColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + periodSelectorSelectedContentColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContentColor.hashCode()
        result = 31 * result + timeSelectorSelectedContainerColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + timeSelectorSelectedContentColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContentColor.hashCode()
        return result
    }
}

/**
 * Creates a [TimePickerState] for a time picker that is remembered across compositions and
 * configuration changes.
 *
 * @param initialHour starting hour for this state, will be displayed in the time picker when
 *   launched. Ranges from 0 to 23
 * @param initialMinute starting minute for this state, will be displayed in the time picker when
 *   launched. Ranges from 0 to 59
 * @param is24Hour The format for this time picker. `false` for 12 hour format with an AM/PM toggle
 *   or `true` for 24 hour format without toggle. Defaults to follow system setting.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = is24HourFormat,
): TimePickerState {
    val state: TimePickerStateImpl =
        rememberSaveable(saver = TimePickerStateImpl.Saver()) {
            TimePickerStateImpl(
                initialHour = initialHour,
                initialMinute = initialMinute,
                is24Hour = is24Hour,
            )
        }

    return state
}

/** Represents the different configurations for the layout of the Time Picker */
@Immutable
@JvmInline
@ExperimentalMaterial3Api
value class TimePickerLayoutType internal constructor(internal val value: Int) {

    companion object {
        /** Displays the Time picker with a horizontal layout. Should be used in landscape mode. */
        val Horizontal = TimePickerLayoutType(0)

        /** Displays the Time picker with a vertical layout. Should be used in portrait mode. */
        val Vertical = TimePickerLayoutType(1)
    }

    override fun toString() =
        when (this) {
            Horizontal -> "Horizontal"
            Vertical -> "Vertical"
            else -> "Unknown"
        }
}

/**
 * A state object that can be hoisted to observe the time picker state. It holds the current values
 * and allows for directly setting those values.
 *
 * @see rememberTimePickerState to construct the default implementation.
 */
@ExperimentalMaterial3Api
interface TimePickerState {

    /** The currently selected minute (0-59). */
    @get:IntRange(from = 0, to = 59) @setparam:IntRange(from = 0, to = 59) var minute: Int

    /** The currently selected hour (0-23). */
    @get:IntRange(from = 0, to = 23) @setparam:IntRange(from = 0, to = 23) var hour: Int

    /**
     * Indicates whether the time picker uses 24-hour format (`true`) or 12-hour format with AM/PM
     * (`false`).
     */
    var is24hour: Boolean

    /** Specifies whether the hour or minute component is being actively selected by the user. */
    var selection: TimePickerSelectionMode
}

/**
 * Indicates whether the selected time falls within the period from 12 PM inclusive to 12 AM non
 * inclusive.
 */
val TimePickerState.isPm
    get() = hour >= 12

/**
 * Factory function for the default implementation of [TimePickerState] [rememberTimePickerState]
 * should be used in most cases.
 *
 * @param initialHour starting hour for this state, will be displayed in the time picker when
 *   launched Ranges from 0 to 23
 * @param initialMinute starting minute for this state, will be displayed in the time picker when
 *   launched. Ranges from 0 to 59
 * @param is24Hour The format for this time picker. `false` for 12 hour format with an AM/PM toggle
 *   or `true` for 24 hour format without toggle. Defaults to follow system setting.
 */
@ExperimentalMaterial3Api
fun TimePickerState(initialHour: Int, initialMinute: Int, is24Hour: Boolean): TimePickerState =
    TimePickerStateImpl(initialHour, initialMinute, is24Hour)

/** The selection mode for the time picker */
@JvmInline
@ExperimentalMaterial3Api
value class TimePickerSelectionMode private constructor(val value: Int) {
    companion object {
        val Hour = TimePickerSelectionMode(0)
        val Minute = TimePickerSelectionMode(1)
    }

    override fun toString(): String =
        when (this) {
            Hour -> "Hour"
            Minute -> "Minute"
            else -> ""
        }
}

private class TimePickerStateImpl(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
) : TimePickerState {
    init {
        require(initialHour in 0..23) { "initialHour should in [0..23] range" }
        require(initialMinute in 0..59) { "initialMinute should be in [0..59] range" }
    }

    override var is24hour: Boolean = is24Hour

    override var selection by mutableStateOf(TimePickerSelectionMode.Hour)

    val hourState = mutableIntStateOf(initialHour)

    val minuteState = mutableIntStateOf(initialMinute)

    override var minute: Int
        get() = minuteState.intValue
        set(value) {
            minuteState.intValue = value
        }

    override var hour: Int
        get() = hourState.intValue
        set(value) {
            hourState.intValue = value
        }

    companion object {
        /** The default [Saver] implementation for [TimePickerState]. */
        fun Saver(): Saver<TimePickerStateImpl, *> =
            Saver(
                save = { listOf(it.hour, it.minute, it.is24hour) },
                restore = { value ->
                    TimePickerStateImpl(
                        initialHour = value[0] as Int,
                        initialMinute = value[1] as Int,
                        is24Hour = value[2] as Boolean
                    )
                }
            )
    }
}

internal class AnalogTimePickerState(val state: TimePickerState) : TimePickerState by state {

    val currentAngle: Float
        get() = anim.value

    private var hourAngle = RadiansPerHour * (state.hour % 12) - FullCircle / 4
    private var minuteAngle = RadiansPerMinute * state.minute - FullCircle / 4

    suspend fun animateToCurrent(animationSpec: AnimationSpec<Float>) {
        if (!isUpdated()) {
            return
        }

        val end =
            if (selection == TimePickerSelectionMode.Hour) {
                endValueForAnimation(hourAngle)
            } else {
                endValueForAnimation(minuteAngle)
            }
        mutex.mutate(priority = PreventUserInput) { anim.animateTo(end, animationSpec) }
    }

    private fun isUpdated(): Boolean {
        if (
            selection == TimePickerSelectionMode.Hour &&
                anim.targetValue.normalize() == hourAngle.normalize()
        ) {
            return false
        }

        if (
            selection == TimePickerSelectionMode.Minute &&
                anim.targetValue.normalize() == minuteAngle.normalize()
        ) {
            return false
        }

        return true
    }

    val clockFaceValues: IntList
        get() = if (selection == TimePickerSelectionMode.Minute) Minutes else Hours

    private fun endValueForAnimation(new: Float): Float {
        // Calculate the absolute angular difference
        var diff = anim.value - new
        // Normalize the angular difference to be between -π and π
        while (diff > HalfCircle) {
            diff -= FullCircle
        }
        while (diff <= -HalfCircle) {
            diff += FullCircle
        }

        return anim.value - diff
    }

    private var anim = Animatable(hourAngle)

    suspend fun onGestureEnd(animationSpec: AnimationSpec<Float>) {
        val end =
            endValueForAnimation(
                if (selection == TimePickerSelectionMode.Hour) {
                    hourAngle
                } else {
                    minuteAngle
                }
            )

        mutex.mutate(priority = PreventUserInput) { anim.animateTo(end, animationSpec) }
    }

    suspend fun rotateTo(
        angle: Float,
        animationSpec: AnimationSpec<Float>,
        animate: Boolean = false
    ) {
        mutex.mutate(MutatePriority.UserInput) {
            if (selection == TimePickerSelectionMode.Hour) {
                hourAngle = angle.toHour() % 12 * RadiansPerHour
                state.hour = hourAngle.toHour() % 12 + if (isPm) 12 else 0
            } else {
                minuteAngle = angle.toMinute() * RadiansPerMinute
                state.minute = minuteAngle.toMinute()
            }

            if (!animate) {
                anim.snapTo(offsetAngle(angle))
            } else {
                val endAngle = endValueForAnimation(offsetAngle(angle))
                anim.animateTo(endAngle, animationSpec)
            }
        }
    }

    override var minute: Int
        get() = state.minute
        set(value) {
            minuteAngle = RadiansPerMinute * value - FullCircle / 4
            state.minute = value
            if (selection == TimePickerSelectionMode.Minute) {
                anim = Animatable(minuteAngle)
            }
            updateBaseStateMinute()
        }

    private fun updateBaseStateMinute() = Snapshot.withoutReadObservation { state.minute = minute }

    override var hour: Int
        get() = state.hour
        set(value) {
            hourAngle = RadiansPerHour * (value % 12) - FullCircle / 4
            state.hour = value
            if (selection == TimePickerSelectionMode.Hour) {
                anim = Animatable(hourAngle)
            }
        }

    private fun Float.normalize(): Float {
        var normalizedAngle = this % (2 * PI)

        if (normalizedAngle < 0) {
            normalizedAngle += 2 * PI
        }

        return normalizedAngle.toFloat()
    }

    private val mutex = MutatorMutex()

    private fun Float.toHour(): Int {
        val hourOffset: Float = RadiansPerHour / 2
        val totalOffset = hourOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerHour).toInt() % 12
    }

    private fun Float.toMinute(): Int {
        val minuteOffset: Float = RadiansPerMinute / 2
        val totalOffset = minuteOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerMinute).toInt() % 60
    }

    private fun offsetAngle(angle: Float): Float {
        val ret = angle + QuarterCircle.toFloat()
        return if (ret < 0) ret + FullCircle else ret
    }
}

internal val TimePickerState.hourForDisplay: Int
    get() =
        when {
            is24hour -> hour % 24
            hour % 12 == 0 -> 12
            isPm -> hour - 12
            else -> hour
        }

private fun TimePickerState.moveSelector(x: Float, y: Float, maxDist: Float, center: IntOffset) {
    if (selection == TimePickerSelectionMode.Hour && is24hour) {
        val currentDist = dist(x, y, center.x, center.y)
        if (isPm) {
            hour -= if (currentDist >= maxDist) 12 else 0
        } else {
            hour += if (currentDist < maxDist) 12 else 0
        }
    }
}

private suspend fun AnalogTimePickerState.onTap(
    x: Float,
    y: Float,
    maxDist: Float,
    autoSwitchToMinute: Boolean,
    center: IntOffset,
    animationSpec: AnimationSpec<Float>,
) {
    var angle = atan(y - center.y, x - center.x)
    if (selection == TimePickerSelectionMode.Minute) {
        angle = round(angle / RadiansPerMinute / 5f) * 5f * RadiansPerMinute
    } else {
        angle = round(angle / RadiansPerHour) * RadiansPerHour
    }

    moveSelector(x, y, maxDist, center)
    rotateTo(angle, animationSpec = animationSpec, animate = true)

    if (selection == TimePickerSelectionMode.Hour && autoSwitchToMinute) {
        delay(100)
    }

    if (autoSwitchToMinute) {
        selection = TimePickerSelectionMode.Minute
    }
}

internal val AnalogTimePickerState.selectorPos: DpOffset
    get() {
        val handleRadiusPx = ClockDialSelectorHandleContainerSize / 2
        val selectorLength =
            if (is24hour && this.isPm && selection == TimePickerSelectionMode.Hour) {
                    InnerCircleRadius
                } else {
                    OuterCircleSizeRadius
                }
                .minus(handleRadiusPx)

        val length = selectorLength + handleRadiusPx
        val offsetX = length * cos(currentAngle) + ClockDialContainerSize / 2
        val offsetY = length * sin(currentAngle) + ClockDialContainerSize / 2

        return DpOffset(offsetX, offsetY)
    }

@Composable
@ExperimentalMaterial3Api
internal fun VerticalTimePicker(
    state: AnalogTimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean
) {
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VerticalClockDisplay(state, colors)
        Spacer(modifier = Modifier.height(ClockDisplayBottomMargin))
        ClockFace(state, colors, autoSwitchToMinute)
        Spacer(modifier = Modifier.height(ClockFaceBottomMargin))
    }
}

@Composable
internal fun HorizontalTimePicker(
    state: AnalogTimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean
) {
    Row(
        modifier = modifier.padding(bottom = ClockFaceBottomMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalClockDisplay(state, colors)
        Spacer(modifier = Modifier.width(ClockDisplayBottomMargin))
        ClockFace(state, colors, autoSwitchToMinute)
    }
}

@Composable
private fun TimeInputImpl(
    modifier: Modifier,
    colors: TimePickerColors,
    state: TimePickerState,
) {
    var hourValue by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(text = state.hourForDisplay.toLocalString(minDigits = 2)))
        }
    var minuteValue by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(text = state.minute.toLocalString(minDigits = 2)))
        }
    Row(
        modifier = modifier.padding(bottom = TimeInputBottomPadding),
        verticalAlignment = Alignment.Top
    ) {
        val textStyle =
            TimeInputTokens.TimeFieldLabelTextFont.value.copy(
                textAlign = TextAlign.Center,
                color = colors.timeSelectorContentColor(true)
            )

        CompositionLocalProvider(
            LocalTextStyle provides textStyle,
            // Always display the time input text field from left to right.
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Row {
                TimePickerTextField(
                    modifier =
                        Modifier.onKeyEvent { event ->
                            // Zero == 48, Nine == 57
                            val switchFocus =
                                event.utf16CodePoint in 48..57 &&
                                    hourValue.selection.start == 2 &&
                                    hourValue.text.length == 2

                            if (switchFocus) {
                                state.selection = TimePickerSelectionMode.Minute
                            }

                            false
                        },
                    value = hourValue,
                    onValueChange = { newValue ->
                        timeInputOnChange(
                            selection = TimePickerSelectionMode.Hour,
                            state = state,
                            value = newValue,
                            prevValue = hourValue,
                            max = if (state.is24hour) 23 else 12,
                        ) {
                            hourValue = it
                        }
                    },
                    state = state,
                    selection = TimePickerSelectionMode.Hour,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { state.selection = TimePickerSelectionMode.Minute }
                        ),
                    colors = colors,
                )
                DisplaySeparator(
                    Modifier.size(DisplaySeparatorWidth, PeriodSelectorContainerHeight)
                )
                TimePickerTextField(
                    modifier =
                        Modifier.onPreviewKeyEvent { event ->
                            // 0 == KEYCODE_DEL
                            val switchFocus =
                                event.utf16CodePoint == 0 && minuteValue.selection.start == 0

                            if (switchFocus) {
                                state.selection = TimePickerSelectionMode.Hour
                            }

                            switchFocus
                        },
                    value = minuteValue,
                    onValueChange = { newValue ->
                        timeInputOnChange(
                            selection = TimePickerSelectionMode.Minute,
                            state = state,
                            value = newValue,
                            prevValue = minuteValue,
                            max = 59,
                        ) {
                            minuteValue = it
                        }
                    },
                    state = state,
                    selection = TimePickerSelectionMode.Minute,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { state.selection = TimePickerSelectionMode.Minute }
                        ),
                    colors = colors,
                )
            }
        }

        if (!state.is24hour) {
            Box(Modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier =
                        Modifier.size(PeriodSelectorContainerWidth, PeriodSelectorContainerHeight),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun HorizontalClockDisplay(state: TimePickerState, colors: TimePickerColors) {
    Column(verticalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(top = PeriodToggleMargin)) {
                HorizontalPeriodToggle(
                    modifier =
                        Modifier.size(
                            PeriodSelectorHorizontalContainerWidth,
                            PeriodSelectorHorizontalContainerHeight
                        ),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun VerticalClockDisplay(state: TimePickerState, colors: TimePickerColors) {
    Row(horizontalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier =
                        Modifier.size(
                            PeriodSelectorVerticalContainerWidth,
                            PeriodSelectorVerticalContainerHeight
                        ),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun ClockDisplayNumbers(state: TimePickerState, colors: TimePickerColors) {
    CompositionLocalProvider(
        LocalTextStyle provides TimeSelectorLabelTextFont.value,
        // Always display the TimeSelectors from left to right.
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row {
            TimeSelector(
                modifier = Modifier.size(TimeSelectorContainerWidth, TimeSelectorContainerHeight),
                value = state.hourForDisplay,
                state = state,
                selection = TimePickerSelectionMode.Hour,
                colors = colors,
            )
            DisplaySeparator(
                Modifier.size(DisplaySeparatorWidth, PeriodSelectorVerticalContainerHeight)
            )
            TimeSelector(
                modifier = Modifier.size(TimeSelectorContainerWidth, TimeSelectorContainerHeight),
                value = state.minute,
                state = state,
                selection = TimePickerSelectionMode.Minute,
                colors = colors,
            )
        }
    }
}

@Composable
private fun HorizontalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
            val spacerPlaceable =
                spacer.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = TimePickerTokens.PeriodSelectorOutlineWidth.roundToPx(),
                    )
                )

            val items =
                measurables
                    .fastFilter { it.layoutId != "Spacer" }
                    .fastMap { item ->
                        item.measure(
                            constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth / 2)
                        )
                    }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(items[0].width, 0)
                spacerPlaceable.place(items[0].width - spacerPlaceable.width / 2, 0)
            }
        }
    }

    val shape = PeriodSelectorContainerShape.value as CornerBasedShape

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.start(),
        endShape = shape.end()
    )
}

@Composable
private fun VerticalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.fastFirst { it.layoutId == "Spacer" }
            val spacerPlaceable =
                spacer.measure(
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = TimePickerTokens.PeriodSelectorOutlineWidth.roundToPx()
                    )
                )

            val items =
                measurables
                    .fastFilter { it.layoutId != "Spacer" }
                    .fastMap { item ->
                        item.measure(
                            constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight / 2)
                        )
                    }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(0, items[0].height)
                spacerPlaceable.place(0, items[0].height - spacerPlaceable.height / 2)
            }
        }
    }

    val shape = PeriodSelectorContainerShape.value as CornerBasedShape

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.top(),
        endShape = shape.bottom()
    )
}

@Composable
private fun PeriodToggleImpl(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
    measurePolicy: MeasurePolicy,
    startShape: Shape,
    endShape: Shape,
) {
    val borderStroke =
        BorderStroke(TimePickerTokens.PeriodSelectorOutlineWidth, colors.periodSelectorBorderColor)

    val shape = PeriodSelectorContainerShape.value as CornerBasedShape
    val contentDescription = getString(Strings.TimePickerPeriodToggle)
    Layout(
        modifier =
            modifier
                .semantics {
                    isTraversalGroup = true
                    this.contentDescription = contentDescription
                }
                .selectableGroup()
                .border(border = borderStroke, shape = shape),
        measurePolicy = measurePolicy,
        content = {
            ToggleItem(
                checked = !state.isPm,
                shape = startShape,
                onClick = {
                    if (state.isPm) {
                        state.hour -= 12
                    }
                },
                colors = colors,
            ) {
                Text(text = getString(string = Strings.TimePickerAM))
            }
            Spacer(
                Modifier.layoutId("Spacer")
                    .zIndex(SeparatorZIndex)
                    .fillMaxSize()
                    .background(color = colors.periodSelectorBorderColor)
            )
            ToggleItem(
                checked = state.isPm,
                shape = endShape,
                onClick = {
                    if (!state.isPm) {
                        state.hour += 12
                    }
                },
                colors = colors,
            ) {
                Text(getString(string = Strings.TimePickerPM))
            }
        }
    )
}

@Composable
private fun ToggleItem(
    checked: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    colors: TimePickerColors,
    content: @Composable RowScope.() -> Unit,
) {
    val contentColor = colors.periodSelectorContentColor(checked)
    val containerColor = colors.periodSelectorContainerColor(checked)

    TextButton(
        modifier =
            Modifier.zIndex(if (checked) 0f else 1f).fillMaxSize().semantics { selected = checked },
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        onClick = onClick,
        content = content,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = contentColor,
                containerColor = containerColor
            )
    )
}

@Composable
private fun DisplaySeparator(modifier: Modifier) {
    val style =
        LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
        )

    Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(text = ":", color = TimeFieldSeparatorColor.value, style = style)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeSelector(
    modifier: Modifier,
    value: Int,
    state: TimePickerState,
    selection: TimePickerSelectionMode,
    colors: TimePickerColors,
) {
    val selected = state.selection == selection
    val selectorContentDescription =
        getString(
            if (selection == TimePickerSelectionMode.Hour) {
                Strings.TimePickerHourSelection
            } else {
                Strings.TimePickerMinuteSelection
            }
        )

    val containerColor = colors.timeSelectorContainerColor(selected)
    val contentColor = colors.timeSelectorContentColor(selected)
    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.contentDescription = selectorContentDescription
            },
        onClick = {
            if (selection != state.selection) {
                state.selection = selection
            }
        },
        selected = selected,
        shape = TimeSelectorContainerShape.value,
        color = containerColor,
    ) {
        val valueContentDescription =
            numberContentDescription(
                selection = selection,
                is24Hour = state.is24hour,
                number = value
            )

        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.semantics { contentDescription = valueContentDescription },
                text = value.toLocalString(minDigits = 2),
                color = contentColor,
            )
        }
    }
}

internal data class ClockDialModifier(
    private val state: AnalogTimePickerState,
    private val autoSwitchToMinute: Boolean,
    private val selection: TimePickerSelectionMode,
    private val animationSpec: AnimationSpec<Float>,
) : ModifierNodeElement<ClockDialNode>() {

    override fun create(): ClockDialNode =
        ClockDialNode(
            state = state,
            autoSwitchToMinute = autoSwitchToMinute,
            selection = selection,
            animationSpec = animationSpec,
        )

    override fun update(node: ClockDialNode) {
        node.updateNode(
            state = state,
            autoSwitchToMinute = autoSwitchToMinute,
            selection = selection,
            animationSpec = animationSpec,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class ClockDialNode(
    private var state: AnalogTimePickerState,
    private var autoSwitchToMinute: Boolean,
    private var selection: TimePickerSelectionMode,
    private var animationSpec: AnimationSpec<Float>,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode,
    LayoutAwareModifierNode {

    private var offsetX = 0f
    private var offsetY = 0f
    private var center: IntOffset = IntOffset.Zero
    private val maxDist
        get() = with(requireDensity()) { MaxDistance.toPx() }

    private val pointerInputTapNode =
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onPress = {
                        offsetX = it.x
                        offsetY = it.y
                    },
                    onTap = {
                        coroutineScope.launch {
                            state.onTap(
                                it.x,
                                it.y,
                                maxDist,
                                autoSwitchToMinute,
                                center,
                                animationSpec
                            )
                        }
                    },
                )
            }
        )

    private val pointerInputDragNode =
        delegate(
            SuspendingPointerInputModifierNode {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (autoSwitchToMinute) {
                                state.selection = TimePickerSelectionMode.Minute
                            }
                            state.onGestureEnd(animationSpec)
                        }
                    }
                ) { _, dragAmount ->
                    coroutineScope.launch {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        state.rotateTo(atan(offsetY - center.y, offsetX - center.x), animationSpec)
                    }
                    state.moveSelector(offsetX, offsetY, maxDist, center)
                }
            }
        )

    override fun onRemeasured(size: IntSize) {
        center = size.center
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputTapNode.onPointerEvent(pointerEvent, pass, bounds)
        pointerInputDragNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputTapNode.onCancelPointerInput()
        pointerInputDragNode.onCancelPointerInput()
    }

    fun updateNode(
        state: AnalogTimePickerState,
        autoSwitchToMinute: Boolean,
        selection: TimePickerSelectionMode,
        animationSpec: AnimationSpec<Float>
    ) {
        this.state = state
        this.autoSwitchToMinute = autoSwitchToMinute
        this.animationSpec = animationSpec
        if (this.selection != selection) {
            this.selection = selection
            coroutineScope.launch { state.animateToCurrent(animationSpec) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ClockFace(
    state: AnalogTimePickerState,
    colors: TimePickerColors,
    autoSwitchToMinute: Boolean
) {
    // TODO Load the motionScheme tokens from the component tokens file
    Crossfade(
        modifier =
            Modifier.background(shape = CircleShape, color = colors.clockDialColor)
                .then(
                    ClockDialModifier(
                        state,
                        autoSwitchToMinute,
                        state.selection,
                        MotionSchemeKeyTokens.DefaultSpatial.value()
                    )
                )
                .size(ClockDialContainerSize)
                .drawSelector(state, colors),
        targetState = state.clockFaceValues,
        animationSpec = MotionSchemeKeyTokens.DefaultEffects.value()
    ) { screen ->
        CircularLayout(
            modifier = Modifier.size(ClockDialContainerSize).semantics { selectableGroup() },
            radius = OuterCircleSizeRadius,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.clockDialContentColor(false)
            ) {
                repeat(screen.size) { index ->
                    val outerValue =
                        if (!state.is24hour || state.selection == TimePickerSelectionMode.Minute) {
                            screen[index]
                        } else {
                            screen[index] % 12
                        }
                    ClockText(
                        modifier = Modifier.semantics { traversalIndex = index.toFloat() },
                        state = state,
                        value = outerValue,
                        autoSwitchToMinute = autoSwitchToMinute
                    )
                }

                if (state.selection == TimePickerSelectionMode.Hour && state.is24hour) {
                    CircularLayout(
                        modifier =
                            Modifier.layoutId(LayoutId.InnerCircle)
                                .size(ClockDialContainerSize)
                                .background(shape = CircleShape, color = Color.Transparent),
                        radius = InnerCircleRadius
                    ) {
                        repeat(ExtraHours.size) { index ->
                            val innerValue = ExtraHours[index]
                            ClockText(
                                modifier =
                                    Modifier.semantics { traversalIndex = 12 + index.toFloat() },
                                state = state,
                                value = innerValue,
                                autoSwitchToMinute = autoSwitchToMinute
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawSelector(
    state: AnalogTimePickerState,
    colors: TimePickerColors,
): Modifier =
    this.drawWithContent {
        val selectorOffsetPx = Offset(state.selectorPos.x.toPx(), state.selectorPos.y.toPx())

        val selectorRadius = ClockDialSelectorHandleContainerSize.toPx() / 2
        val selectorColor = colors.selectorColor

        // clear out the selector section
        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = Color.Black,
            blendMode = BlendMode.Clear,
        )

        // draw the text composables
        drawContent()

        // draw the selector and clear out the numbers overlapping
        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = selectorColor,
            blendMode = BlendMode.Xor
        )

        val strokeWidth = ClockDialSelectorTrackContainerWidth.toPx()
        val lineLength =
            selectorOffsetPx.minus(
                Offset(
                    (selectorRadius * cos(state.currentAngle)),
                    (selectorRadius * sin(state.currentAngle))
                )
            )

        // draw the selector line
        drawLine(
            start = size.center,
            strokeWidth = strokeWidth,
            end = lineLength,
            color = selectorColor,
            blendMode = BlendMode.SrcOver
        )

        // draw the selector small dot
        drawCircle(
            radius = ClockDialSelectorCenterContainerSize.toPx() / 2,
            center = size.center,
            color = selectorColor,
        )

        // draw the portion of the number that was overlapping
        drawCircle(
            radius = selectorRadius,
            center = selectorOffsetPx,
            color = colors.clockDialContentColor(selected = true),
            blendMode = BlendMode.DstOver
        )
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClockText(
    modifier: Modifier,
    state: AnalogTimePickerState,
    value: Int,
    autoSwitchToMinute: Boolean
) {
    val style = ClockDialLabelTextFont.value
    val maxDist = with(LocalDensity.current) { MaxDistance.toPx() }
    var center by remember { mutableStateOf(Offset.Zero) }
    var parentCenter by remember { mutableStateOf(IntOffset.Zero) }
    val scope = rememberCoroutineScope()
    val contentDescription =
        numberContentDescription(
            selection = state.selection,
            is24Hour = state.is24hour,
            number = value
        )

    val text = value.toLocalString()
    val selected =
        if (state.selection == TimePickerSelectionMode.Minute) {
            state.minute.toLocalString() == text
        } else {
            state.hour.toLocalString() == text
        }

    // TODO Load the motionScheme tokens from the component tokens file
    val animationSpec: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(MinimumInteractiveSize)
                .onGloballyPositioned {
                    parentCenter = it.parentCoordinates?.size?.center ?: IntOffset.Zero
                    center = it.boundsInParent().center
                }
                .focusable()
                .semantics(mergeDescendants = true) {
                    onClick {
                        scope.launch {
                            state.onTap(
                                center.x,
                                center.y,
                                maxDist,
                                autoSwitchToMinute,
                                parentCenter,
                                animationSpec
                            )
                        }
                        true
                    }
                    this.selected = selected
                }
    ) {
        Text(
            modifier =
                Modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
            text = text,
            style = style,
        )
    }
}

private fun timeInputOnChange(
    selection: TimePickerSelectionMode,
    state: TimePickerState,
    value: TextFieldValue,
    prevValue: TextFieldValue,
    max: Int,
    onNewValue: (value: TextFieldValue) -> Unit
) {
    if (value.text == prevValue.text) {
        // just selection change
        onNewValue(value)
        return
    }

    if (value.text.isEmpty()) {
        if (selection == TimePickerSelectionMode.Hour) {
            state.hour = if (state.isPm && !state.is24hour) 12 else 0
        } else {
            state.minute = 0
        }
        onNewValue(value.copy(text = ""))
        return
    }

    try {
        val newValue =
            if (value.text.length == 3 && value.selection.start == 1) {
                value.text[0].digitToInt()
            } else {
                value.text.toInt()
            }

        if (newValue <= max) {
            if (selection == TimePickerSelectionMode.Hour) {
                state.hour = newValue + if (state.isPm && !state.is24hour) 12 else 0
                if (newValue > 1 && !state.is24hour) {
                    state.selection = TimePickerSelectionMode.Minute
                }
            } else {
                state.minute = newValue
            }

            onNewValue(
                if (value.text.length <= 2) {
                    value
                } else {
                    value.copy(text = value.text[0].toString())
                }
            )
        }
    } catch (_: NumberFormatException) {} catch (_: IllegalArgumentException) {
        // do nothing no state update
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerTextField(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    state: TimePickerState,
    selection: TimePickerSelectionMode,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TimePickerColors,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val textFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.timeSelectorContainerColor(true),
            unfocusedContainerColor = colors.timeSelectorContainerColor(true),
            focusedTextColor = colors.timeSelectorContentColor(true),
        )
    val selected = selection == state.selection
    Column(modifier = modifier) {
        if (!selected) {
            TimeSelector(
                modifier = Modifier.size(TimeFieldContainerWidth, TimeFieldContainerHeight),
                value =
                    if (selection == TimePickerSelectionMode.Hour) {
                        state.hourForDisplay
                    } else {
                        state.minute
                    },
                state = state,
                selection = selection,
                colors = colors,
            )
        }

        val contentDescription =
            getString(
                if (selection == TimePickerSelectionMode.Minute) {
                    Strings.TimePickerMinuteTextField
                } else {
                    Strings.TimePickerHourTextField
                }
            )

        Box(Modifier.visible(selected)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier.focusRequester(focusRequester)
                        .size(TimeFieldContainerWidth, TimeFieldContainerHeight)
                        .semantics { this.contentDescription = contentDescription },
                interactionSource = interactionSource,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = LocalTextStyle.current,
                enabled = true,
                singleLine = true,
                cursorBrush =
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.10f to Color.Transparent,
                        0.10f to MaterialTheme.colorScheme.primary,
                        0.90f to MaterialTheme.colorScheme.primary,
                        0.90f to Color.Transparent,
                        1.00f to Color.Transparent
                    )
            ) {
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = it,
                    singleLine = true,
                    colors = textFieldColors,
                    enabled = true,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(0.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            shape = TimeInputTokens.TimeFieldContainerShape.value,
                            colors = textFieldColors,
                        )
                    }
                )
            }
        }

        Text(
            modifier = Modifier.offset(y = SupportLabelTop).clearAndSetSemantics {},
            text =
                getString(
                    if (selection == TimePickerSelectionMode.Hour) {
                        Strings.TimePickerHour
                    } else {
                        Strings.TimePickerMinute
                    }
                ),
            color = TimeInputTokens.TimeFieldSupportingTextColor.value,
            style = TimeInputTokens.TimeFieldSupportingTextFont.value
        )
    }

    LaunchedEffect(state.selection) {
        if (state.selection == selection) {
            focusRequester.requestFocus()
        }
    }
}

/** Distribute elements evenly on a circle of [radius] */
@Composable
private fun CircularLayout(
    modifier: Modifier = Modifier,
    radius: Dp,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val radiusPx = radius.toPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables =
            measurables
                .fastFilter {
                    it.layoutId != LayoutId.Selector && it.layoutId != LayoutId.InnerCircle
                }
                .fastMap { measurable -> measurable.measure(itemConstraints) }
        val selectorMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.Selector }
        val innerMeasurable = measurables.fastFirstOrNull { it.layoutId == LayoutId.InnerCircle }
        val theta = FullCircle / (placeables.count())
        val selectorPlaceable = selectorMeasurable?.measure(itemConstraints)
        val innerCirclePlaceable = innerMeasurable?.measure(itemConstraints)

        layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
        ) {
            selectorPlaceable?.place(0, 0)

            placeables.fastForEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
                it.place(x = offsetX.roundToInt(), y = offsetY.roundToInt())
            }

            innerCirclePlaceable?.place(
                (constraints.minWidth - innerCirclePlaceable.width) / 2,
                (constraints.minHeight - innerCirclePlaceable.height) / 2
            )
        }
    }
}

@Composable
@ReadOnlyComposable
internal fun numberContentDescription(
    selection: TimePickerSelectionMode,
    is24Hour: Boolean,
    number: Int
): String {
    val id =
        if (selection == TimePickerSelectionMode.Minute) {
            Strings.TimePickerMinuteSuffix
        } else if (is24Hour) {
            Strings.TimePicker24HourSuffix
        } else {
            Strings.TimePickerHourSuffix
        }

    return getString(id, number)
}

private fun dist(x1: Float, y1: Float, x2: Int, y2: Int): Float {
    val x = x2 - x1
    val y = y2 - y1
    return hypot(x.toDouble(), y.toDouble()).toFloat()
}

private fun atan(y: Float, x: Float): Float {
    val ret = atan2(y, x) - QuarterCircle.toFloat()
    return if (ret < 0) ret + FullCircle else ret
}

private enum class LayoutId {
    Selector,
    InnerCircle,
}

internal expect val defaultTimePickerLayoutType: TimePickerLayoutType
    @Composable @ReadOnlyComposable get

private const val FullCircle: Float = (PI * 2).toFloat()
private const val HalfCircle: Float = FullCircle / 2f
private const val QuarterCircle = PI / 2
private const val RadiansPerMinute: Float = FullCircle / 60
private const val RadiansPerHour: Float = FullCircle / 12f
private const val SeparatorZIndex = 2f

private val OuterCircleSizeRadius = 101.dp
private val InnerCircleRadius = 69.dp
private val ClockDisplayBottomMargin = 36.dp
private val ClockFaceBottomMargin = 24.dp
private val DisplaySeparatorWidth = 24.dp

private val SupportLabelTop = 7.dp
private val TimeInputBottomPadding = 24.dp
private val MaxDistance = 74.dp
private val MinimumInteractiveSize = 48.dp
private val Minutes = intListOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
private val Hours = intListOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
private val ExtraHours: IntList =
    MutableIntList(Hours.size).apply { Hours.forEach { add((it % 12 + 12)) } }
private val PeriodToggleMargin = 12.dp

/**
 * Measure the composable with 0,0 so that it stays on the screen. Necessary to correctly handle
 * focus
 */
@Stable
private fun Modifier.visible(visible: Boolean) =
    this.then(
        VisibleModifier(
            visible,
            debugInspectorInfo {
                name = "visible"
                properties["visible"] = visible
            }
        )
    )

private class VisibleModifier(val visible: Boolean, inspectorInfo: InspectorInfo.() -> Unit) :
    LayoutModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        if (!visible) {
            return layout(0, 0) {}
        }
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun hashCode(): Int = visible.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? VisibleModifier ?: return false
        return visible == otherModifier.visible
    }
}
