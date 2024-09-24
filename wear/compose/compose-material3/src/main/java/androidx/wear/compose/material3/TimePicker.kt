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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Plurals
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.internal.getPlurals
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.TimePickerTokens
import androidx.wear.compose.materialcore.is24HourFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

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
 * @param initialTime The initial time to be displayed in the TimePicker.
 * @param onTimePicked The callback that is called when the user confirms the time selection. It
 *   provides the selected time as [LocalTime].
 * @param modifier Modifier to be applied to the `Box` containing the UI elements.
 * @param timePickerType The different [TimePickerType] supported by this time picker. It indicates
 *   whether to show seconds or AM/PM selector as well as hours and minutes.
 * @param colors [TimePickerColors] be applied to the TimePicker.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimePicker(
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    timePickerType: TimePickerType = TimePickerDefaults.timePickerType,
    colors: TimePickerColors = TimePickerDefaults.timePickerColors(),
) {
    val inspectionMode = LocalInspectionMode.current
    val fullyDrawn = remember { Animatable(if (inspectionMode) 1f else 0f) }

    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    /** The current selected [Picker] index. */
    var selectedIndex by
        remember(touchExplorationServicesEnabled) {
            // When the time picker loads, none of the individual pickers are selected in talkback
            // mode,
            // otherwise hours picker should be focused.
            val initiallySelectedIndex =
                if (touchExplorationServicesEnabled) {
                    null
                } else {
                    FocusableElements.Hours.index
                }
            mutableStateOf(initiallySelectedIndex)
        }

    val focusRequesterConfirmButton = remember { FocusRequester() }

    val hourString = getString(Strings.TimePickerHour)
    val minuteString = getString(Strings.TimePickerMinute)

    val is12hour = timePickerType == TimePickerType.HoursMinutesAmPm12H
    val hourState =
        if (is12hour) {
            rememberPickerState(
                initialNumberOfOptions = 12,
                initiallySelectedIndex = initialTime[ChronoField.CLOCK_HOUR_OF_AMPM] - 1,
            )
        } else {
            rememberPickerState(
                initialNumberOfOptions = 24,
                initiallySelectedIndex = initialTime.hour,
            )
        }
    val minuteState =
        rememberPickerState(
            initialNumberOfOptions = 60,
            initiallySelectedIndex = initialTime.minute,
        )

    val hoursContentDescription =
        createDescription(
            selectedIndex,
            if (is12hour) hourState.selectedOptionIndex + 1 else hourState.selectedOptionIndex,
            hourString,
            Plurals.TimePickerHoursContentDescription,
        )
    val minutesContentDescription =
        createDescription(
            selectedIndex,
            minuteState.selectedOptionIndex,
            minuteString,
            Plurals.TimePickerMinutesContentDescription,
        )

    val thirdPicker = getOptionalThirdPicker(timePickerType, selectedIndex, initialTime)

    val onPickerSelected = { current: FocusableElements, next: FocusableElements ->
        if (selectedIndex != current.index) {
            selectedIndex = current.index
        } else {
            selectedIndex = next.index
            if (next == FocusableElements.ConfirmButton) {
                focusRequesterConfirmButton.requestFocus()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().alpha(fullyDrawn.value)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(14.dp))
            val focusedPicker = FocusableElements(selectedIndex)
            FontScaleIndependent {
                val styles = getTimePickerStyles(timePickerType, thirdPicker)
                Text(
                    text =
                        when {
                            focusedPicker == FocusableElements.Hours -> hourString
                            focusedPicker == FocusableElements.Minutes -> minuteString
                            focusedPicker == FocusableElements.SecondsOrPeriod &&
                                thirdPicker != null -> thirdPicker.label
                            else -> ""
                        },
                    color = colors.pickerLabelColor,
                    style = styles.labelTextStyle,
                    maxLines = 1,
                    modifier =
                        Modifier.height(24.dp)
                            .fillMaxWidth(0.76f)
                            .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(styles.sectionVerticalPadding))
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Pass a negative value as the selected picker index when none is selected.
                    PickerGroup(
                        selectedPickerIndex = selectedIndex ?: -1,
                        onPickerSelected = { selectedIndex = it },
                        modifier = Modifier.fillMaxWidth(),
                        separator = {
                            Separator(
                                textStyle = styles.optionTextStyle,
                                color = colors.separatorColor,
                                separatorPadding = styles.separatorPadding,
                                text = if (it == 0 || !is12hour) ":" else ""
                            )
                        },
                        autoCenter = false,
                    ) {
                        // Hours Picker
                        pickerGroupItem(
                            pickerState = hourState,
                            modifier = Modifier.width(styles.optionWidth).fillMaxHeight(),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElements.Hours,
                                    FocusableElements.Minutes,
                                )
                            },
                            contentDescription = hoursContentDescription,
                            option =
                                pickerTextOption(
                                    textStyle = styles.optionTextStyle,
                                    selectedContentColor = colors.selectedPickerContentColor,
                                    unselectedContentColor = colors.unselectedPickerContentColor,
                                    indexToText = { "%02d".format(if (is12hour) it + 1 else it) },
                                    optionHeight = styles.optionHeight,
                                ),
                            spacing = styles.optionSpacing
                        )

                        // Minutes Picker
                        pickerGroupItem(
                            pickerState = minuteState,
                            modifier = Modifier.width(styles.optionWidth).fillMaxHeight(),
                            onSelected = {
                                onPickerSelected(
                                    FocusableElements.Minutes,
                                    if (timePickerType == TimePickerType.HoursMinutes24H) {
                                        FocusableElements.ConfirmButton
                                    } else {
                                        FocusableElements.SecondsOrPeriod
                                    }
                                )
                            },
                            contentDescription = minutesContentDescription,
                            option =
                                pickerTextOption(
                                    textStyle = styles.optionTextStyle,
                                    indexToText = { "%02d".format(it) },
                                    selectedContentColor = colors.selectedPickerContentColor,
                                    unselectedContentColor = colors.unselectedPickerContentColor,
                                    optionHeight = styles.optionHeight,
                                ),
                            spacing = styles.optionSpacing
                        )

                        // Seconds or Period picker
                        if (thirdPicker != null) {
                            pickerGroupItem(
                                pickerState = thirdPicker.state,
                                modifier = Modifier.width(styles.optionWidth).fillMaxHeight(),
                                onSelected = {
                                    onPickerSelected(
                                        FocusableElements.SecondsOrPeriod,
                                        FocusableElements.ConfirmButton,
                                    )
                                },
                                contentDescription = thirdPicker.contentDescription,
                                option =
                                    pickerTextOption(
                                        textStyle = styles.optionTextStyle,
                                        indexToText = thirdPicker.indexToText,
                                        selectedContentColor = colors.selectedPickerContentColor,
                                        unselectedContentColor =
                                            colors.unselectedPickerContentColor,
                                        optionHeight = styles.optionHeight,
                                    ),
                                spacing = styles.optionSpacing
                            )
                        }
                    }
                }
                Spacer(Modifier.height(styles.sectionVerticalPadding))
            }
            EdgeButton(
                onClick = {
                    val secondOrPeriodSelectedOption = thirdPicker?.state?.selectedOptionIndex ?: 0
                    val confirmedTime =
                        if (is12hour) {
                            LocalTime.of(
                                    hourState.selectedOptionIndex + 1,
                                    minuteState.selectedOptionIndex,
                                    0,
                                )
                                .with(
                                    ChronoField.AMPM_OF_DAY,
                                    secondOrPeriodSelectedOption.toLong()
                                )
                        } else {
                            LocalTime.of(
                                hourState.selectedOptionIndex,
                                minuteState.selectedOptionIndex,
                                secondOrPeriodSelectedOption,
                            )
                        }
                    onTimePicked(confirmedTime)
                },
                modifier =
                    Modifier.semantics {
                            focused = (selectedIndex == FocusableElements.ConfirmButton.index)
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable(),
                buttonSize = EdgeButtonSize.Small,
                colors =
                    buttonColors(
                        contentColor = colors.confirmButtonContentColor,
                        containerColor = colors.confirmButtonContainerColor
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

/** Specifies the types of columns to display in the TimePicker. */
@Immutable
@JvmInline
value class TimePickerType internal constructor(internal val value: Int) {
    companion object {
        /** Displays two columns for hours (24-hour format) and minutes. */
        val HoursMinutes24H = TimePickerType(0)
        /** Displays three columns for hours (24-hour format), minutes and seconds. */
        val HoursMinutesSeconds24H = TimePickerType(1)
        /** Displays three columns for hours (12-hour format), minutes and AM/PM label. */
        val HoursMinutesAmPm12H = TimePickerType(2)
    }

    override fun toString() =
        when (this) {
            HoursMinutes24H -> "HoursMinutes24H"
            HoursMinutesSeconds24H -> "HoursMinutesSeconds24H"
            HoursMinutesAmPm12H -> "HoursMinutesAmPm12H"
            else -> "Unknown"
        }
}

/** Contains the default values used by [TimePicker] */
object TimePickerDefaults {

    /** The default [TimePickerType] for [TimePicker] aligns with the current system time format. */
    val timePickerType: TimePickerType
        @Composable
        get() =
            if (is24HourFormat()) {
                TimePickerType.HoursMinutes24H
            } else {
                TimePickerType.HoursMinutesAmPm12H
            }

    /** Creates a [TimePickerColors] for a [TimePicker]. */
    @Composable fun timePickerColors() = MaterialTheme.colorScheme.defaultTimePickerColors

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
    fun timePickerColors(
        selectedPickerContentColor: Color = Color.Unspecified,
        unselectedPickerContentColor: Color = Color.Unspecified,
        separatorColor: Color = Color.Unspecified,
        pickerLabelColor: Color = Color.Unspecified,
        confirmButtonContentColor: Color = Color.Unspecified,
        confirmButtonContainerColor: Color = Color.Unspecified,
    ) =
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
class TimePickerColors(
    val selectedPickerContentColor: Color,
    val unselectedPickerContentColor: Color,
    val separatorColor: Color,
    val pickerLabelColor: Color,
    val confirmButtonContentColor: Color,
    val confirmButtonContainerColor: Color,
) {
    internal fun copy(
        selectedPickerContentColor: Color,
        unselectedPickerContentColor: Color,
        separatorColor: Color,
        pickerLabelColor: Color,
        confirmButtonContentColor: Color,
        confirmButtonContainerColor: Color,
    ) =
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

@Composable
private fun getTimePickerStyles(
    timePickerType: TimePickerType,
    optionalThirdPicker: PickerData?
): TimePickerStyles {
    val isLargeScreen = LocalConfiguration.current.screenWidthDp > 225
    val labelTextStyle =
        if (isLargeScreen) {
                TimePickerTokens.LabelLargeTypography
            } else {
                TimePickerTokens.LabelTypography
            }
            .value

    val optionTextStyle =
        if (isLargeScreen || timePickerType == TimePickerType.HoursMinutes24H) {
                TimePickerTokens.ContentLargeTypography
            } else {
                TimePickerTokens.ContentTypography
            }
            .value
            .copy(textAlign = TextAlign.Center)

    val optionHeight =
        if (isLargeScreen || timePickerType == TimePickerType.HoursMinutes24H) {
            40.dp
        } else {
            30.dp
        }
    val optionSpacing = if (isLargeScreen) 6.dp else 4.dp
    val separatorPadding =
        when {
            timePickerType == TimePickerType.HoursMinutes24H && isLargeScreen -> 12.dp
            timePickerType == TimePickerType.HoursMinutes24H && !isLargeScreen -> 8.dp
            timePickerType == TimePickerType.HoursMinutesAmPm12H && isLargeScreen -> 0.dp
            isLargeScreen -> 6.dp
            else -> 2.dp
        }

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val indexToText = optionalThirdPicker?.indexToText ?: { "" }

    val (twoDigitsWidth, textLabelWidth) =
        remember(
            density.density,
            LocalConfiguration.current.screenWidthDp,
        ) {
            val mm =
                measurer.measure(
                    "0123456789\n${indexToText(0)}\n${indexToText(1)}",
                    style = optionTextStyle,
                    density = density,
                )

            (0..9).maxOf { mm.getBoundingBox(it).width } * 2 to
                (1..2).maxOf { mm.getLineRight(it) - mm.getLineLeft(it) }
        }
    val measuredOptionWidth =
        with(LocalDensity.current) {
            if (timePickerType == TimePickerType.HoursMinutesAmPm12H) {
                max(twoDigitsWidth.toDp(), textLabelWidth.toDp())
            } else {
                twoDigitsWidth.toDp()
            } + 1.dp // Add 1dp buffer to compensate for potential conversion loss
        }

    return TimePickerStyles(
        labelTextStyle = labelTextStyle,
        optionTextStyle = optionTextStyle,
        optionWidth = max(measuredOptionWidth, minimumInteractiveComponentSize),
        optionHeight = optionHeight,
        optionSpacing = optionSpacing,
        separatorPadding = separatorPadding,
        sectionVerticalPadding = if (isLargeScreen) 6.dp else 4.dp
    )
}

/* Returns the picker data for the third column (AM/PM or seconds) based on the time picker type. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun getOptionalThirdPicker(
    timePickerType: TimePickerType,
    selectedIndex: Int?,
    time: LocalTime
): PickerData? =
    when (timePickerType) {
        TimePickerType.HoursMinutesSeconds24H -> {
            val secondString = getString(Strings.TimePickerSecond)
            val secondState =
                rememberPickerState(
                    initialNumberOfOptions = 60,
                    initiallySelectedIndex = time.second,
                )
            val secondsContentDescription =
                createDescription(
                    selectedIndex,
                    secondState.selectedOptionIndex,
                    secondString,
                    Plurals.TimePickerSecondsContentDescription,
                )
            PickerData(
                state = secondState,
                contentDescription = secondsContentDescription,
                label = secondString,
                indexToText = { "%02d".format(it) }
            )
        }
        TimePickerType.HoursMinutesAmPm12H -> {
            val periodString = getString(Strings.TimePickerPeriod)
            val periodState =
                rememberPickerState(
                    initialNumberOfOptions = 2,
                    initiallySelectedIndex = time[ChronoField.AMPM_OF_DAY],
                    shouldRepeatOptions = false,
                )
            val primaryLocale = LocalConfiguration.current.locales[0]
            val (amString, pmString) =
                remember(primaryLocale) {
                    DateTimeFormatter.ofPattern("a", primaryLocale).let { formatter ->
                        LocalTime.of(0, 0).format(formatter) to
                            LocalTime.of(12, 0).format(formatter)
                    }
                }
            val periodContentDescription by
                remember(
                    selectedIndex,
                    periodState.selectedOptionIndex,
                ) {
                    derivedStateOf {
                        if (selectedIndex == null) {
                            periodString
                        } else if (periodState.selectedOptionIndex == 0) {
                            amString
                        } else {
                            pmString
                        }
                    }
                }
            PickerData(
                state = periodState,
                contentDescription = periodContentDescription,
                label = "",
                indexToText = { if (it == 0) amString else pmString }
            )
        }
        else -> null
    }

private class PickerData(
    val state: PickerState,
    val contentDescription: String,
    val label: String,
    val indexToText: (Int) -> String,
)

private class TimePickerStyles(
    val labelTextStyle: TextStyle,
    val optionTextStyle: TextStyle,
    val optionWidth: Dp,
    val optionHeight: Dp,
    val optionSpacing: Dp,
    val separatorPadding: Dp,
    val sectionVerticalPadding: Dp,
)

@Composable
private fun Separator(
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    separatorPadding: Dp,
    text: String = ":",
) {
    Box(modifier = Modifier.padding(horizontal = separatorPadding)) {
        Text(
            text = text,
            style = textStyle,
            color = color,
            modifier = modifier.width(12.dp).clearAndSetSemantics {},
        )
    }
}

@Composable
private fun createDescription(
    selectedIndex: Int?,
    selectedValue: Int,
    label: String,
    plurals: Plurals,
) =
    if (selectedIndex == null) {
        label
    } else {
        getPlurals(plurals, selectedValue, selectedValue)
    }

@Immutable
@JvmInline
private value class FocusableElements(val index: Int?) {
    companion object {
        val Hours = FocusableElements(0)
        val Minutes = FocusableElements(1)
        val SecondsOrPeriod = FocusableElements(2)
        val ConfirmButton = FocusableElements(3)
        val None = FocusableElements(null)
    }

    override fun toString() =
        when (this) {
            Hours -> "HOURS"
            Minutes -> "MINUTES"
            SecondsOrPeriod -> "SECONDS_OR_PERIOD"
            ConfirmButton -> "CONFIRM_BUTTON"
            None -> "NONE"
            else -> "Unknown"
        }
}
