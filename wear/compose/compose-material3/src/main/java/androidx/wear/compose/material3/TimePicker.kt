/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapNotNull
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Plurals
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.TimePickerTokens
import androidx.wear.compose.materialcore.is24HourFormat
import androidx.wear.compose.materialcore.isLargeScreen
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * A full screen TimePicker with configurable columns that allows users to select a time.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts.
 *
 * For custom backgrounds like gradients or images wrap the TimePicker in a MaterialTheme with the
 * colorScheme background set to [Color.Unspecified].
 *
 * Example of a [TimePicker]:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerSample
 *
 * Example of a [TimePicker] with seconds:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWithSecondsSample
 *
 * Example of a 12 hour clock [TimePicker]:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWith12HourClockSample
 *
 * Example of a [TimePicker] with just minutes and seconds:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWithMinutesAndSecondsSample
 * @param initialTime The initial time to be displayed in the TimePicker.
 * @param onTimePicked The callback that is called when the user confirms the time selection. It
 *   provides the selected time as [LocalTime]. Note that any time components not displayed in the
 *   picker (e.g. the hour for [TimePickerType.MinutesSeconds], or the second for
 *   [TimePickerType.HoursMinutes24H]) will have a default value of 0 in the returned [LocalTime].
 * @param modifier Modifier to be applied to the `Box` containing the UI elements.
 * @param timePickerType The different [TimePickerType] supported by this time picker. It indicates
 *   whether to show seconds or AM/PM selector as well as hours and minutes.
 * @param colors [TimePickerColors] be applied to the TimePicker.
 * @param initialSelection The initial time component to be selected when the `TimePicker` is first
 *   displayed. By default, this is the first available time component based on the [timePickerType]
 *   and the device's locale (e.g., the hour component for a [TimePickerType.HoursMinutes24H]
 *   picker). If a [TimePickerSelection] is provided that is not applicable to the current
 *   [timePickerType] (such as providing `TimePickerSelection.Second` for a picker that does not
 *   display seconds), the selection will fall back to the first available time component.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun TimePicker(
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    timePickerType: TimePickerType = TimePickerDefaults.timePickerType,
    colors: TimePickerColors = TimePickerDefaults.timePickerColors(),
    initialSelection: TimePickerSelection = TimePickerDefaults.timePickerSelection(timePickerType),
) {
    val inspectionMode = LocalInspectionMode.current
    val fullyDrawn = remember { Animatable(if (inspectionMode) 1f else 0f) }

    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    val focusRequesterConfirmButton = remember { FocusRequester() }

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val localeConfig =
        remember(locale, timePickerType) { PickerLocaleConfig(locale, timePickerType) }

    var selectedElement: TimePickerSelection by
        remember(touchExplorationServicesEnabled, localeConfig.focusableOrder) {
            mutableStateOf(
                localeConfig.toInitialSelection(initialSelection, touchExplorationServicesEnabled)
            )
        }

    val hourState =
        when {
            timePickerType == TimePickerType.MinutesSeconds -> null
            localeConfig.is12hour ->
                rememberPickerState(
                    initialNumberOfOptions = 12,
                    initiallySelectedIndex =
                        initialTime[ChronoField.CLOCK_HOUR_OF_AMPM] - localeConfig.hourValueOffset,
                )
            else ->
                rememberPickerState(
                    initialNumberOfOptions = 24,
                    initiallySelectedIndex = initialTime.hour - localeConfig.hourValueOffset,
                )
        }
    val minuteState =
        rememberPickerState(
            initialNumberOfOptions = 60,
            initiallySelectedIndex = initialTime.minute,
        )
    val secondState =
        when (timePickerType) {
            TimePickerType.HoursMinutesSeconds24H,
            TimePickerType.MinutesSeconds ->
                rememberPickerState(
                    initialNumberOfOptions = 60,
                    initiallySelectedIndex = initialTime.second,
                )
            else -> null
        }
    val periodState =
        if (timePickerType == TimePickerType.HoursMinutesAmPm12H) {
            rememberPickerState(
                initialNumberOfOptions = 2,
                initiallySelectedIndex = initialTime[ChronoField.AMPM_OF_DAY],
                shouldRepeatOptions = false,
            )
        } else {
            null
        }

    val instructionHeadingString = getString(Strings.TimePickerHeading)
    val hourString = getString(Strings.TimePickerHour)
    val minuteString = getString(Strings.TimePickerMinute)
    val secondString = getString(Strings.TimePickerSecond)
    val periodString = getString(Strings.TimePickerPeriod)

    val hoursContentDescription = {
        createDescription(
            context,
            selectedElement,
            hourState?.run { selectedOptionIndex + localeConfig.hourValueOffset } ?: 0,
            hourString,
            Plurals.TimePickerHoursContentDescription,
        )
    }
    val minutesContentDescription = {
        createDescription(
            context,
            selectedElement,
            minuteState.selectedOptionIndex,
            minuteString,
            Plurals.TimePickerMinutesContentDescription,
        )
    }
    val secondsContentDescription = {
        createDescription(
            context,
            selectedElement,
            secondState?.selectedOptionIndex ?: 0,
            secondString,
            Plurals.TimePickerSecondsContentDescription,
        )
    }
    val periodContentDescription = {
        if (selectedElement == TimePickerSelection.None) {
            periodString
        } else if (periodState?.selectedOptionIndex == 0) {
            localeConfig.localizedAmText
        } else {
            localeConfig.localizedPmText
        }
    }

    val findNextElement = { current: TimePickerSelection ->
        val currentIndex = localeConfig.focusableOrder.indexOf(current)
        localeConfig.focusableOrder.getOrNull(currentIndex + 1) ?: TimePickerSelection.ConfirmButton
    }

    val onPickerSelected = { current: TimePickerSelection ->
        if (selectedElement != current) {
            selectedElement = current
        } else {
            selectedElement = findNextElement(current)
            if (selectedElement == TimePickerSelection.ConfirmButton) {
                focusRequesterConfirmButton.requestFocus()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().alpha(fullyDrawn.value)) {

        // Allow more room for the initial instruction heading under TalkBack
        val maxTextLines = if (selectedElement == TimePickerSelection.None) 2 else 1
        val textPaddingPercentage = 30f
        val topPadding = if (selectedElement == TimePickerSelection.None) 0.dp else 14.dp
        val headingHeight = 38.dp - topPadding

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(topPadding))

            FontScaleIndependent {
                val layoutConfig = rememberPickerLayoutConfig(timePickerType, localeConfig)
                val heading =
                    when (selectedElement) {
                        TimePickerSelection.Hour -> hourString
                        TimePickerSelection.Minute -> minuteString
                        TimePickerSelection.Second -> secondString
                        TimePickerSelection.None ->
                            if (touchExplorationServicesEnabled) instructionHeadingString else ""
                        else -> ""
                    }

                FadeLabel(
                    text = heading,
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    modifier =
                        Modifier.height(headingHeight)
                            .padding(
                                horizontal =
                                    PaddingDefaults.horizontalContentPadding(textPaddingPercentage)
                            )
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .semantics(mergeDescendants = true) { heading() },
                    color = colors.pickerLabelColor,
                    style = layoutConfig.labelTextStyle,
                    maxLines = maxTextLines,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(layoutConfig.sectionVerticalPadding))
                TimePickerContent(
                    localeConfig = localeConfig,
                    selectedElement = selectedElement,
                    onPickerSelected = onPickerSelected,
                    hourState = hourState,
                    minuteState = minuteState,
                    secondState = secondState,
                    periodState = periodState,
                    hoursContentDescription = hoursContentDescription,
                    minutesContentDescription = minutesContentDescription,
                    secondsContentDescription = secondsContentDescription,
                    periodContentDescription = periodContentDescription,
                    colors = colors,
                    layoutConfig = layoutConfig,
                )
                Spacer(Modifier.height(layoutConfig.sectionVerticalPadding))
            }
            EdgeButton(
                onClick = {
                    val timeWithoutPeriod =
                        LocalTime.of(
                            hourState?.run { selectedOptionIndex + localeConfig.hourValueOffset }
                                ?: 0,
                            minuteState.selectedOptionIndex,
                            secondState?.selectedOptionIndex ?: 0,
                        )
                    val confirmedTime =
                        if (localeConfig.is12hour) {
                            timeWithoutPeriod.with(
                                ChronoField.AMPM_OF_DAY,
                                (periodState?.selectedOptionIndex ?: 0).toLong(),
                            )
                        } else timeWithoutPeriod

                    onTimePicked(confirmedTime)
                },
                modifier =
                    Modifier.semantics {
                            focused = (selectedElement == TimePickerSelection.ConfirmButton)
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable(),
                buttonSize = EdgeButtonSize.Small,
                colors =
                    buttonColors(
                        contentColor = colors.confirmButtonContentColor,
                        containerColor = colors.confirmButtonContainerColor,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Check,
                    contentDescription = getString(Strings.PickerConfirmButtonContentDescription),
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            }
        }
    }

    if (!inspectionMode) {
        LaunchedEffect(Unit) { fullyDrawn.animateTo(1f) }
    }
}

/**
 * A full screen TimePicker with configurable columns that allows users to select a time.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts.
 *
 * Example of a [TimePicker]:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerSample
 *
 * Example of a [TimePicker] with seconds:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWithSecondsSample
 *
 * Example of a 12 hour clock [TimePicker]:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWith12HourClockSample
 *
 * Example of a [TimePicker] with just minutes and seconds:
 *
 * @sample androidx.wear.compose.material3.samples.TimePickerWithMinutesAndSecondsSample
 * @param initialTime The initial time to be displayed in the TimePicker.
 * @param onTimePicked The callback that is called when the user confirms the time selection. It
 *   provides the selected time as [LocalTime]. Note that any time components not displayed in the
 *   picker (e.g. the hour for [TimePickerType.MinutesSeconds], or the second for
 *   [TimePickerType.HoursMinutes24H]) will have a default value of 0 in the returned [LocalTime].
 * @param modifier Modifier to be applied to the `Box` containing the UI elements.
 * @param timePickerType The different [TimePickerType] supported by this time picker. It indicates
 *   whether to show seconds or AM/PM selector as well as hours and minutes.
 * @param colors [TimePickerColors] be applied to the TimePicker.
 */
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.5. " +
        "A newer overload is available with an additional initialSelection parameter.",
    level = DeprecationLevel.HIDDEN,
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun TimePicker(
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    timePickerType: TimePickerType = TimePickerDefaults.timePickerType,
    colors: TimePickerColors = TimePickerDefaults.timePickerColors(),
): Unit =
    TimePicker(
        initialTime = initialTime,
        onTimePicked = onTimePicked,
        modifier = modifier,
        timePickerType = timePickerType,
        initialSelection = TimePickerDefaults.timePickerSelection(timePickerType),
        colors = colors,
    )

/** Specifies the types of columns to display in the TimePicker. */
@Immutable
@JvmInline
public value class TimePickerType internal constructor(internal val value: Int) {
    public companion object {
        /** Displays two columns for hours (24-hour format) and minutes. */
        public val HoursMinutes24H: TimePickerType = TimePickerType(0)
        /** Displays three columns for hours (24-hour format), minutes and seconds. */
        public val HoursMinutesSeconds24H: TimePickerType = TimePickerType(1)
        /** Displays three columns for hours (12-hour format), minutes and AM/PM label. */
        public val HoursMinutesAmPm12H: TimePickerType = TimePickerType(2)
        /** Displays two columns for minutes and seconds */
        public val MinutesSeconds: TimePickerType = TimePickerType(3)
    }

    override fun toString(): String =
        when (this) {
            HoursMinutes24H -> "HoursMinutes24H"
            HoursMinutesSeconds24H -> "HoursMinutesSeconds24H"
            HoursMinutesAmPm12H -> "HoursMinutesAmPm12H"
            MinutesSeconds -> "MinutesSeconds"
            else -> "Unknown"
        }
}

@Immutable
@JvmInline
public value class TimePickerSelection internal constructor(internal val value: Int) {
    public companion object {
        /** Represents the hour component of the time. */
        public val Hour: TimePickerSelection = TimePickerSelection(0)

        /** Represents the minute component of the time. */
        public val Minute: TimePickerSelection = TimePickerSelection(1)

        /** Represents the second component of the time. */
        public val Second: TimePickerSelection = TimePickerSelection(2)

        /** Represents the AM/PM period component of the time for 12-hour formats. */
        public val Period: TimePickerSelection = TimePickerSelection(3)

        /** Represents the confirmation button component used to confirm the selected time. */
        public val ConfirmButton: TimePickerSelection = TimePickerSelection(4)

        /** Indicates that no specific component is selected. Used primarily for accessibility. */
        public val None: TimePickerSelection = TimePickerSelection(5)
    }

    override fun toString(): String =
        when (this) {
            Hour -> "Hour"
            Minute -> "Minute"
            Second -> "Second"
            Period -> "Period"
            ConfirmButton -> "ConfirmButton"
            None -> "None"
            else -> "Unknown"
        }
}

/** Contains the default values used by [TimePicker] */
public object TimePickerDefaults {

    /** The default [TimePickerType] for [TimePicker] aligns with the current system time format. */
    public val timePickerType: TimePickerType
        @Composable
        get() =
            if (is24HourFormat()) {
                TimePickerType.HoursMinutes24H
            } else {
                TimePickerType.HoursMinutesAmPm12H
            }

    /**
     * The default [TimePickerSelection] for [TimePicker] is set to the first available time
     * component based on the provided [TimePickerType] and current system time format.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    public fun timePickerSelection(timePickerType: TimePickerType): TimePickerSelection {
        val locale = LocalConfiguration.current.locales[0]
        val localeConfig = PickerLocaleConfig(locale, timePickerType)
        return localeConfig.toDefaultSelection()
    }

    /** Creates a [TimePickerColors] for a [TimePicker]. */
    @Composable
    public fun timePickerColors(): TimePickerColors =
        MaterialTheme.colorScheme.defaultTimePickerColors

    /**
     * Creates a [TimePickerColors] for a [TimePicker].
     *
     * @param selectedPickerContentColor The content color of selected picker.
     * @param unselectedPickerContentColor The content color of unselected pickers.
     * @param separatorColor The color of separator between the pickers.
     * @param pickerLabelColor The color of the picker label.
     * @param confirmButtonContentColor The content color of the confirm button.
     * @param confirmButtonContainerColor The container color of the confirm button.
     */
    @Composable
    public fun timePickerColors(
        selectedPickerContentColor: Color = Color.Unspecified,
        unselectedPickerContentColor: Color = Color.Unspecified,
        separatorColor: Color = Color.Unspecified,
        pickerLabelColor: Color = Color.Unspecified,
        confirmButtonContentColor: Color = Color.Unspecified,
        confirmButtonContainerColor: Color = Color.Unspecified,
    ): TimePickerColors =
        MaterialTheme.colorScheme.defaultTimePickerColors.copy(
            selectedPickerContentColor = selectedPickerContentColor,
            unselectedPickerContentColor = unselectedPickerContentColor,
            separatorColor = separatorColor,
            pickerLabelColor = pickerLabelColor,
            confirmButtonContentColor = confirmButtonContentColor,
            confirmButtonContainerColor = confirmButtonContainerColor,
        )

    private val ColorScheme.defaultTimePickerColors: TimePickerColors
        get() {
            return defaultTimePickerColorsCached
                ?: TimePickerColors(
                        selectedPickerContentColor =
                            fromToken(TimePickerTokens.SelectedContentColor),
                        unselectedPickerContentColor =
                            fromToken(TimePickerTokens.UnselectedContentColor),
                        separatorColor = fromToken(TimePickerTokens.SeparatorColor),
                        pickerLabelColor = fromToken(TimePickerTokens.LabelColor),
                        confirmButtonContentColor =
                            fromToken(TimePickerTokens.ConfirmButtonContentColor),
                        confirmButtonContainerColor =
                            fromToken(TimePickerTokens.ConfirmButtonContainerColor),
                    )
                    .also { defaultTimePickerColorsCached = it }
        }
}

/**
 * Represents the colors used by a [TimePicker].
 *
 * @param selectedPickerContentColor The content color of selected picker.
 * @param unselectedPickerContentColor The content color of unselected pickers.
 * @param separatorColor The color of separator between the pickers.
 * @param pickerLabelColor The color of the picker label.
 * @param confirmButtonContentColor The content color of the confirm button.
 * @param confirmButtonContainerColor The container color of the confirm button.
 */
@Immutable
public class TimePickerColors(
    public val selectedPickerContentColor: Color,
    public val unselectedPickerContentColor: Color,
    public val separatorColor: Color,
    public val pickerLabelColor: Color,
    public val confirmButtonContentColor: Color,
    public val confirmButtonContainerColor: Color,
) {
    /**
     * Returns a copy of this TimePickerColors( optionally overriding some of the values.
     *
     * @param selectedPickerContentColor The content color of selected picker.
     * @param unselectedPickerContentColor The content color of unselected pickers.
     * @param separatorColor The color of separator between the pickers.
     * @param pickerLabelColor The color of the picker label.
     * @param confirmButtonContentColor The content color of the confirm button.
     * @param confirmButtonContainerColor The container color of the confirm button.
     */
    public fun copy(
        selectedPickerContentColor: Color = this.selectedPickerContentColor,
        unselectedPickerContentColor: Color = this.unselectedPickerContentColor,
        separatorColor: Color = this.separatorColor,
        pickerLabelColor: Color = this.pickerLabelColor,
        confirmButtonContentColor: Color = this.confirmButtonContentColor,
        confirmButtonContainerColor: Color = this.confirmButtonContainerColor,
    ): TimePickerColors =
        TimePickerColors(
            selectedPickerContentColor =
                selectedPickerContentColor.takeOrElse { this.selectedPickerContentColor },
            unselectedPickerContentColor =
                unselectedPickerContentColor.takeOrElse { this.unselectedPickerContentColor },
            separatorColor = separatorColor.takeOrElse { this.separatorColor },
            pickerLabelColor = pickerLabelColor.takeOrElse { this.pickerLabelColor },
            confirmButtonContentColor =
                confirmButtonContentColor.takeOrElse { this.confirmButtonContentColor },
            confirmButtonContainerColor =
                confirmButtonContainerColor.takeOrElse { this.confirmButtonContainerColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TimePickerColors) return false

        if (selectedPickerContentColor != other.selectedPickerContentColor) return false
        if (unselectedPickerContentColor != other.unselectedPickerContentColor) return false
        if (separatorColor != other.separatorColor) return false
        if (pickerLabelColor != other.pickerLabelColor) return false
        if (confirmButtonContentColor != other.confirmButtonContentColor) return false
        if (confirmButtonContainerColor != other.confirmButtonContainerColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedPickerContentColor.hashCode()
        result = 31 * result + unselectedPickerContentColor.hashCode()
        result = 31 * result + separatorColor.hashCode()
        result = 31 * result + pickerLabelColor.hashCode()
        result = 31 * result + confirmButtonContentColor.hashCode()
        result = 31 * result + confirmButtonContainerColor.hashCode()

        return result
    }
}

private val TimePickerType.isTwoColumnPicker
    get() = this == TimePickerType.HoursMinutes24H || this == TimePickerType.MinutesSeconds

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ColumnScope.TimePickerContent(
    selectedElement: TimePickerSelection,
    onPickerSelected: (TimePickerSelection) -> Unit,
    hourState: PickerState?,
    minuteState: PickerState,
    secondState: PickerState?,
    periodState: PickerState?,
    hoursContentDescription: () -> String,
    minutesContentDescription: () -> String,
    secondsContentDescription: () -> String,
    periodContentDescription: () -> String,
    colors: TimePickerColors,
    localeConfig: PickerLocaleConfig,
    layoutConfig: PickerLayoutConfig,
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PickerGroup(
            selectedPickerState =
                when (selectedElement) {
                    TimePickerSelection.Hour -> hourState
                    TimePickerSelection.Minute -> minuteState
                    TimePickerSelection.Second -> secondState
                    TimePickerSelection.Period -> periodState
                    else -> null
                },
            modifier = Modifier.fillMaxWidth(),
            autoCenter = false,
        ) {
            localeConfig.layoutElements.fastForEach { element ->
                when (element) {
                    is TimeLayoutElement.Standalone -> {
                        when (val part = element.part) {
                            is TimePatternPart.ComponentPart -> {
                                if (
                                    part.component == TimePickerSelection.Period &&
                                        periodState != null
                                ) {
                                    PeriodPicker(
                                        periodState = periodState,
                                        selected = selectedElement == TimePickerSelection.Period,
                                        onSelected = {
                                            onPickerSelected(TimePickerSelection.Period)
                                        },
                                        contentDescription = periodContentDescription,
                                        layoutConfig = layoutConfig,
                                        colors = colors,
                                    )
                                }
                            }
                            is TimePatternPart.SeparatorPart -> {
                                Separator(
                                    textStyle = layoutConfig.optionTextStyle,
                                    color = colors.separatorColor,
                                    separatorPadding = layoutConfig.separatorPadding,
                                    text = part.separatorText,
                                    optionHeight = layoutConfig.optionHeight,
                                    optionBaseline = layoutConfig.optionBaseline,
                                )
                            }
                        }
                    }
                    is TimeLayoutElement.TimeGroup -> {
                        // Render the h:m:s group inside a forced-LTR Row.
                        // The top-level Row respects the global layout direction (LTR/RTL),
                        // which correctly orders the TimeGroup against other elements like AM/PM.
                        // However, we must force the inner Row containing the time digits to LTR
                        // to prevent its children (hour, separator, minute) from being incorrectly
                        // reversed in an RTL locale. This preserves the logical h:m sequence.
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                element.parts.fastForEach { part ->
                                    when (part) {
                                        is TimePatternPart.ComponentPart -> {
                                            when (part.component) {
                                                TimePickerSelection.Hour ->
                                                    if (hourState != null) {
                                                        HourPicker(
                                                            hourState = hourState,
                                                            selected =
                                                                selectedElement ==
                                                                    TimePickerSelection.Hour,
                                                            onSelected = {
                                                                onPickerSelected(
                                                                    TimePickerSelection.Hour
                                                                )
                                                            },
                                                            contentDescription =
                                                                hoursContentDescription,
                                                            hourValueOffset =
                                                                localeConfig.hourValueOffset,
                                                            layoutConfig = layoutConfig,
                                                            colors = colors,
                                                            locale = localeConfig.locale,
                                                        )
                                                    }
                                                TimePickerSelection.Minute ->
                                                    MinutePicker(
                                                        minuteState = minuteState,
                                                        selected =
                                                            selectedElement ==
                                                                TimePickerSelection.Minute,
                                                        onSelected = {
                                                            onPickerSelected(
                                                                TimePickerSelection.Minute
                                                            )
                                                        },
                                                        contentDescription =
                                                            minutesContentDescription,
                                                        layoutConfig = layoutConfig,
                                                        colors = colors,
                                                        locale = localeConfig.locale,
                                                    )
                                                TimePickerSelection.Second -> {
                                                    if (secondState != null) {
                                                        SecondPicker(
                                                            secondState = secondState,
                                                            selected =
                                                                selectedElement ==
                                                                    TimePickerSelection.Second,
                                                            onSelected = {
                                                                onPickerSelected(
                                                                    TimePickerSelection.Second
                                                                )
                                                            },
                                                            contentDescription =
                                                                secondsContentDescription,
                                                            layoutConfig = layoutConfig,
                                                            colors = colors,
                                                            locale = localeConfig.locale,
                                                        )
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                        is TimePatternPart.SeparatorPart -> {
                                            Separator(
                                                textStyle = layoutConfig.optionTextStyle,
                                                color = colors.separatorColor,
                                                separatorPadding = layoutConfig.separatorPadding,
                                                text = part.separatorText,
                                                optionHeight = layoutConfig.optionHeight,
                                                optionBaseline = layoutConfig.optionBaseline,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerGroupScope.HourPicker(
    hourState: PickerState,
    selected: Boolean,
    onSelected: () -> Unit,
    contentDescription: () -> String,
    hourValueOffset: Int,
    layoutConfig: PickerLayoutConfig,
    colors: TimePickerColors,
    locale: Locale,
) {
    PickerGroupItem(
        pickerState = hourState,
        modifier = Modifier.width(layoutConfig.twoDigitsOptionWidth).fillMaxHeight(),
        selected = selected,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option =
            pickerTextOption(
                textStyle = layoutConfig.optionTextStyle,
                selectedContentColor = colors.selectedPickerContentColor,
                unselectedContentColor = colors.unselectedPickerContentColor,
                indexToText = { "%02d".format(locale, it + hourValueOffset) },
                optionHeight = layoutConfig.optionHeight,
                optionBaseline = layoutConfig.optionBaseline,
            ),
        verticalSpacing = layoutConfig.optionSpacing,
    )
}

@Composable
private fun PickerGroupScope.MinutePicker(
    minuteState: PickerState,
    selected: Boolean,
    onSelected: () -> Unit,
    contentDescription: () -> String,
    layoutConfig: PickerLayoutConfig,
    colors: TimePickerColors,
    locale: Locale,
) {
    PickerGroupItem(
        pickerState = minuteState,
        modifier = Modifier.width(layoutConfig.twoDigitsOptionWidth).fillMaxHeight(),
        selected = selected,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option =
            pickerTextOption(
                textStyle = layoutConfig.optionTextStyle,
                indexToText = { "%02d".format(locale, it) },
                selectedContentColor = colors.selectedPickerContentColor,
                unselectedContentColor = colors.unselectedPickerContentColor,
                optionHeight = layoutConfig.optionHeight,
                optionBaseline = layoutConfig.optionBaseline,
            ),
        verticalSpacing = layoutConfig.optionSpacing,
    )
}

@Composable
private fun PickerGroupScope.SecondPicker(
    secondState: PickerState,
    selected: Boolean,
    onSelected: () -> Unit,
    contentDescription: () -> String,
    layoutConfig: PickerLayoutConfig,
    colors: TimePickerColors,
    locale: Locale,
) {
    PickerGroupItem(
        pickerState = secondState,
        modifier = Modifier.width(layoutConfig.twoDigitsOptionWidth).fillMaxHeight(),
        selected = selected,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option =
            pickerTextOption(
                textStyle = layoutConfig.optionTextStyle,
                indexToText = { "%02d".format(locale, it) },
                selectedContentColor = colors.selectedPickerContentColor,
                unselectedContentColor = colors.unselectedPickerContentColor,
                optionHeight = layoutConfig.optionHeight,
                optionBaseline = layoutConfig.optionBaseline,
            ),
        verticalSpacing = layoutConfig.optionSpacing,
    )
}

@Composable
private fun PickerGroupScope.PeriodPicker(
    periodState: PickerState,
    selected: Boolean,
    onSelected: () -> Unit,
    contentDescription: () -> String,
    layoutConfig: PickerLayoutConfig,
    colors: TimePickerColors,
) {
    PickerGroupItem(
        pickerState = periodState,
        modifier = Modifier.width(layoutConfig.periodOptionWidth).fillMaxHeight(),
        selected = selected,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option =
            pickerTextOption(
                textStyle = layoutConfig.optionTextStyle,
                indexToText = {
                    if (it == 0) layoutConfig.displayAmText else layoutConfig.displayPmText
                },
                selectedContentColor = colors.selectedPickerContentColor,
                unselectedContentColor = colors.unselectedPickerContentColor,
                optionHeight = layoutConfig.optionHeight,
                optionBaseline = layoutConfig.optionBaseline,
            ),
        verticalSpacing = layoutConfig.optionSpacing,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun rememberPickerLayoutConfig(
    timePickerType: TimePickerType,
    localeConfig: PickerLocaleConfig,
): PickerLayoutConfig {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val isLargeScreen = isLargeScreen()
    val labelTextStyle =
        if (isLargeScreen) {
                TimePickerTokens.LabelLargeTypography
            } else {
                TimePickerTokens.LabelTypography
            }
            .value

    val optionTextStyle =
        if (isLargeScreen) {
                TimePickerTokens.ContentLargeTypography
            } else {
                TimePickerTokens.ContentTypography
            }
            .value
            .copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum")

    // This remember block caches the entire layout configuration. It is keyed on the
    // fundamental "sources of truth" that can affect the layout's appearance or metrics.
    //
    // - `timePickerType`: Controls which pickers are shown and influences text styles.
    // - `localeConfig`: Encapsulates all locale-specific formatting and text.
    // - `screenWidth`: Determines `isLargeScreen` and is used for fallback logic.
    // - `density.density`: Ensures recalculation on rare screen density changes.
    // - `LocalTypography.current`: Ensures the layout adapts if the app's theme provides
    //   a different typography, as this affects all text measurements.
    //
    // We DO NOT need to key on `density.fontScale` because the `FontScaleIndependent`
    // wrapper ensures it is always 1.0f in this scope.
    return remember(
        timePickerType,
        localeConfig,
        screenWidth,
        density.density,
        LocalTypography.current,
    ) {
        val (minimumOptionHeight, maximumOptionHeight) =
            if (isLargeScreen) {
                46.dp to 58.dp
            } else {
                36.dp to 48.dp
            }

        val optionSpacing = if (isLargeScreen) 6.dp else 4.dp
        val separatorPadding =
            when {
                timePickerType.isTwoColumnPicker && isLargeScreen -> 12.dp
                timePickerType.isTwoColumnPicker && !isLargeScreen -> 8.dp
                timePickerType == TimePickerType.HoursMinutesAmPm12H && isLargeScreen -> 0.dp
                isLargeScreen -> 6.dp
                else -> 2.dp
            }

        val measuredMetrics =
            measurePickerMetrics(
                measurer = measurer,
                optionTextStyle = optionTextStyle,
                localeConfig = localeConfig,
                density = density,
            )
        val twoDigitsOptionWidth =
            with(density) {
                measuredMetrics.twoDigitsWidthPx.toDp() +
                    1.dp // Add 1dp buffer to compensate for potential conversion loss
            }
        val measuredPeriodOptionWidth =
            with(density) {
                measuredMetrics.periodTextWidthPx.toDp() + 1.dp // Add 1dp buffer
            }
        val fallbackPeriodOptionWidth =
            with(density) {
                measuredMetrics.fallbackPeriodWidthPx.toDp() + 1.dp // Add 1dp buffer
            }
        val measuredOptionHeight = with(density) { measuredMetrics.optionHeightPx.toDp() }
        val optionHeight = measuredOptionHeight.coerceIn(minimumOptionHeight, maximumOptionHeight)
        val optionBaseline =
            calculateBaseline(
                measuredOptionBaselinePx = measuredMetrics.optionBaselinePx,
                measuredOptionHeight = measuredOptionHeight,
                maximumOptionHeight = maximumOptionHeight,
                minimumOptionHeight = minimumOptionHeight,
                density,
            )

        val separatorTotalWidth = SeparatorWidth + (separatorPadding * 2)
        val useFallbackPeriodText =
            measuredPeriodOptionWidth >
                screenWidth - separatorTotalWidth * 2 - twoDigitsOptionWidth * 2
        val periodOptionWidth =
            if (useFallbackPeriodText) fallbackPeriodOptionWidth else measuredPeriodOptionWidth
        val displayAmText =
            if (useFallbackPeriodText) FallbackAmText else localeConfig.localizedAmText
        val displayPmText =
            if (useFallbackPeriodText) FallbackPmText else localeConfig.localizedPmText

        PickerLayoutConfig(
            labelTextStyle = labelTextStyle,
            optionTextStyle = optionTextStyle,
            twoDigitsOptionWidth = max(twoDigitsOptionWidth, minimumInteractiveComponentSize),
            periodOptionWidth = max(periodOptionWidth, minimumInteractiveComponentSize),
            optionHeight = optionHeight,
            optionBaseline = optionBaseline,
            optionSpacing = optionSpacing,
            separatorPadding = separatorPadding,
            sectionVerticalPadding = if (isLargeScreen) 6.dp else 4.dp,
            displayAmText = displayAmText,
            displayPmText = displayPmText,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun measurePickerMetrics(
    measurer: TextMeasurer,
    optionTextStyle: TextStyle,
    density: Density,
    localeConfig: PickerLocaleConfig,
): PickerMeasuredMetrics {
    val widthMeasureResult =
        measurer.measure(
            "${localeConfig.localizedDigits}\n${localeConfig.localizedAmText}\n${localeConfig.localizedPmText}\n$FallbackAmText\n$FallbackPmText",
            style = optionTextStyle,
            density = density,
        )

    val singleLineHeightMeasureResult =
        measurer.measure(
            "${localeConfig.localizedDigits}${localeConfig.localizedAmText}${localeConfig.localizedPmText}$FallbackAmText$FallbackPmText",
            style = optionTextStyle,
            density = density,
        )

    return PickerMeasuredMetrics(
        twoDigitsWidthPx =
            (0 until localeConfig.localizedDigits.length).maxOf {
                widthMeasureResult.getBoundingBox(it).width
            } * 2,
        periodTextWidthPx =
            (1..2).maxOf {
                widthMeasureResult.getLineRight(it) - widthMeasureResult.getLineLeft(it)
            },
        fallbackPeriodWidthPx =
            (3..4).maxOf {
                widthMeasureResult.getLineRight(it) - widthMeasureResult.getLineLeft(it)
            },
        optionHeightPx =
            singleLineHeightMeasureResult.getLineBottom(0) -
                singleLineHeightMeasureResult.getLineTop(0),
        optionBaselinePx = singleLineHeightMeasureResult.getLineBaseline(0),
    )
}

// This logic calculates the baseline for the picker text to ensure it is vertically
// centered within the component's height constraints.
private fun calculateBaseline(
    measuredOptionBaselinePx: Float,
    measuredOptionHeight: Dp,
    maximumOptionHeight: Dp,
    minimumOptionHeight: Dp,
    density: Density,
): Int {
    // This branch handles the edge case where the measured text is TALLER than the
    // maximum allowed component height.
    return if (measuredOptionHeight > maximumOptionHeight) {
        (measuredOptionBaselinePx +
                with(density) {
                    // Since measuredOptionHeight > maximumOptionHeight, this subtraction
                    // results in a NEGATIVE value.
                    // This negative offset is used to shift the oversized text UPWARDS,
                    // ensuring it's optically centered within the clipped area, rather
                    // than just having its bottom clipped off.
                    min(0.dp, (maximumOptionHeight - measuredOptionHeight) / 2).toPx()
                })
            .toInt()
    } else {
        // This is the standard case. It centers the text within the minimum component height.
        (measuredOptionBaselinePx +
                with(density) {
                    // This calculates the extra vertical padding required to center the text.
                    // It correctly handles two sub-cases:
                    // 1. If text is smaller than the minimum height, this yields a POSITIVE
                    //    padding to center the text within the larger minimum touch target.
                    // 2. If text is larger than the minimum height, the subtraction is
                    // negative, and max(0.dp, ...) correctly clamps the padding to zero.
                    max(0.dp, (minimumOptionHeight - measuredOptionHeight) / 2).toPx()
                })
            .toInt()
    }
}

@Composable
private fun Separator(
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    separatorPadding: Dp,
    text: String = ":",
    optionHeight: Dp,
    optionBaseline: Int,
) {
    Box(
        modifier =
            Modifier.wrapContentWidth().height(optionHeight).padding(horizontal = separatorPadding)
    ) {
        Text(
            text = text,
            style = textStyle,
            color = color,
            modifier =
                modifier
                    .wrapContentHeight()
                    .width(SeparatorWidth)
                    .align(Alignment.Center)
                    .clearAndSetSemantics {}
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val baseline = placeable[FirstBaseline]
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeRelative(
                                x = (constraints.maxWidth - placeable.width) / 2,
                                y = optionBaseline - baseline,
                            )
                        }
                    },
        )
    }
}

private fun createDescription(
    context: Context,
    selectedElement: TimePickerSelection,
    selectedValue: Int,
    label: String,
    plurals: Plurals,
) =
    if (selectedElement == TimePickerSelection.None) {
        label
    } else {
        context.resources.getQuantityString(plurals.value, selectedValue, selectedValue)
    }

@RequiresApi(Build.VERSION_CODES.O)
@Immutable
private class PickerLocaleConfig(val locale: Locale, val timePickerType: TimePickerType) {
    val is12hour: Boolean = timePickerType == TimePickerType.HoursMinutesAmPm12H

    val skeleton: String =
        when (timePickerType) {
            TimePickerType.HoursMinutesAmPm12H -> "h:mm a"
            TimePickerType.HoursMinutesSeconds24H -> "H:mm:ss"
            TimePickerType.MinutesSeconds -> "mm:ss"
            else -> "H:mm"
        }

    val pattern: String = DateFormat.getBestDateTimePattern(locale, skeleton)

    val layoutElements: List<TimeLayoutElement> = groupTimeParts(parsePattern(pattern))

    @SuppressLint("PrimitiveInCollection")
    val focusableOrder: List<TimePickerSelection> =
        layoutElements
            .fastFlatMap { element ->
                when (element) {
                    is TimeLayoutElement.Standalone -> listOf(element.part)
                    is TimeLayoutElement.TimeGroup -> element.parts
                }
            }
            .fastMapNotNull { part -> (part as? TimePatternPart.ComponentPart)?.component }

    // The hour value offset is used to map the picker's 0-based index to the correct hour
    // value. Hour format patterns can be 0-based (e.g., H for 0-23, K for 0-11) or 1-based
    // (e.g., k for 1-24, h for 1-12). This offset accounts for that difference.
    val hourValueOffset: Int = if (pattern.contains('H') || pattern.contains('K')) 0 else 1

    val localizedDigits = buildString { (0..9).forEach { append("%d".format(locale, it)) } }

    val localizedAmText: String
    val localizedPmText: String

    init {
        if (is12hour) {
            val formatter = DateTimeFormatter.ofPattern("a", locale)
            localizedAmText = formatter.format(LocalTime.of(0, 0))
            localizedPmText = formatter.format(LocalTime.of(12, 0))
        } else {
            localizedAmText = ""
            localizedPmText = ""
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun PickerLocaleConfig.toInitialSelection(
    initialSelection: TimePickerSelection,
    touchExplorationServicesEnabled: Boolean,
) =
    when {
        touchExplorationServicesEnabled -> TimePickerSelection.None
        initialSelection == TimePickerSelection.ConfirmButton -> TimePickerSelection.ConfirmButton
        initialSelection in focusableOrder -> initialSelection
        else -> focusableOrder.firstOrNull() ?: TimePickerSelection.None
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun PickerLocaleConfig.toDefaultSelection() =
    focusableOrder.firstOrNull() ?: TimePickerSelection.None

private class PickerLayoutConfig(
    val labelTextStyle: TextStyle,
    val optionTextStyle: TextStyle,
    val twoDigitsOptionWidth: Dp,
    val periodOptionWidth: Dp,
    val optionHeight: Dp,
    val optionBaseline: Int,
    val optionSpacing: Dp,
    val separatorPadding: Dp,
    val sectionVerticalPadding: Dp,
    val displayAmText: String,
    val displayPmText: String,
)

/** A private data class to hold the measured raw pixel metrics for picker options. */
private data class PickerMeasuredMetrics(
    val twoDigitsWidthPx: Float,
    val periodTextWidthPx: Float,
    val fallbackPeriodWidthPx: Float,
    val optionHeightPx: Float,
    val optionBaselinePx: Float,
)

// Represents a high-level layout element
internal sealed interface TimeLayoutElement {
    // A group of components that must maintain a fixed LTR order (h:m:s)
    data class TimeGroup(val parts: List<TimePatternPart>) : TimeLayoutElement

    // A standalone part that can be reordered by the parent layout direction
    data class Standalone(val part: TimePatternPart) : TimeLayoutElement
}

// Helper data classes to represent parts of a parsed time pattern
internal sealed interface TimePatternPart {
    data class ComponentPart(val component: TimePickerSelection) : TimePatternPart

    data class SeparatorPart(val separatorText: String) : TimePatternPart
}

/**
 * Returns true if this [TimePatternPart] is a component that belongs in the LTR-forced time group
 * (Hour, Minute, or Second).
 */
private fun TimePatternPart.isTimeGroupComponent(): Boolean =
    this is TimePatternPart.ComponentPart &&
        (component == TimePickerSelection.Hour ||
            component == TimePickerSelection.Minute ||
            component == TimePickerSelection.Second)

/**
 * Groups a list of [TimePatternPart]s into layout elements. Hour, minute, and second components
 * (and the literals between them) are bundled into a single [TimeLayoutElement.TimeGroup].
 */
internal fun groupTimeParts(parts: List<TimePatternPart>): List<TimeLayoutElement> {
    val elements = mutableListOf<TimeLayoutElement>()
    var timeGroupParts = mutableListOf<TimePatternPart>()

    for (i in parts.indices) {
        val part = parts[i]

        // An internal separator is one that is followed by a time component.
        val isInternalSeparator =
            part is TimePatternPart.SeparatorPart &&
                (parts.getOrNull(i + 1)?.isTimeGroupComponent() ?: false)

        // A part should be added to the group if it's a time component itself,
        // OR if it's an internal separator AND the group has already been started.
        val shouldAddToGroup =
            part.isTimeGroupComponent() || (isInternalSeparator && timeGroupParts.isNotEmpty())

        if (shouldAddToGroup) {
            timeGroupParts.add(part)
        } else {
            // This part does not belong in the group. First, flush the existing group if it's
            // not empty.
            if (timeGroupParts.isNotEmpty()) {
                elements.add(TimeLayoutElement.TimeGroup(timeGroupParts))
                timeGroupParts = mutableListOf()
            }
            // Then, add the current part as a standalone element.
            elements.add(TimeLayoutElement.Standalone(part))
        }
    }

    // Flush any remaining group at the end of the loop.
    if (timeGroupParts.isNotEmpty()) {
        elements.add(TimeLayoutElement.TimeGroup(timeGroupParts))
    }
    return elements
}

/**
 * Parses a time pattern from [DateFormat.getBestDateTimePattern] into a list of structured
 * [TimePatternPart]s. It also inserts a space literal between any two consecutive components that
 * don't have a literal separator.
 */
internal fun parsePattern(originalPattern: String): List<TimePatternPart> {
    // Sanitize the pattern by removing all quoted literals.
    // This handles edge cases like fr-CA ("HH 'h' mm") by turning them into "HHmm",
    // preventing the 'h' from being parsed as an Hour component.
    val pattern: String = originalPattern.replace(Regex("\\s*'.*?'\\s*"), "")
    val parts = mutableListOf<TimePatternPart>()
    val separatorText = StringBuilder()
    pattern.forEach { char ->
        val component =
            when (char) {
                'h',
                'H',
                'k',
                'K' -> TimePickerSelection.Hour
                'm' -> TimePickerSelection.Minute
                's' -> TimePickerSelection.Second
                'a' -> TimePickerSelection.Period
                else -> null
            }

        if (component != null) {
            // Found a component, first flush any pending literal
            if (separatorText.isNotEmpty()) {
                // Sanitize long, unquoted literals.
                // If the separator is longer than 1 char, replace it with a blank string.
                // This allows the heuristic below to ensure a simple space for a clean UI.
                // Otherwise, keep simple separators like ":" or ".".
                val sanitizedSeparator =
                    if (separatorText.length > 1) " " else separatorText.toString()
                if (parts.isNotEmpty()) {
                    parts.add(TimePatternPart.SeparatorPart(sanitizedSeparator))
                }
                separatorText.clear()
            }
            // Add the component, avoiding duplicates
            if (parts.lastOrNull() != TimePatternPart.ComponentPart(component)) {
                // Heuristic: Add a space if two components are adjacent (e.g., "aK")
                if (parts.isNotEmpty() && parts.last() is TimePatternPart.ComponentPart) {
                    parts.add(TimePatternPart.SeparatorPart(" "))
                }
                parts.add(TimePatternPart.ComponentPart(component))
            }
        } else {
            // It's a literal character
            separatorText.append(char)
        }
    }
    // Trim trailing separators.
    // This handles cases where a literal was at the start or end of the original pattern,
    // ensuring our UI only displays separators *between* components.
    return parts.dropLastWhile { it is TimePatternPart.SeparatorPart }
}

private const val FallbackAmText = "AM"
private const val FallbackPmText = "PM"
private val SeparatorWidth = 12.dp
