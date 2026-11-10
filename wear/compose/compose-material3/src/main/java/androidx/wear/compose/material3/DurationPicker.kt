/*
 * Copyright 2026 The Android Open Source Project
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
import android.icu.text.MeasureFormat
import android.icu.text.MeasureFormat.FormatWidth
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Build
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Plurals
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.DurationPickerTokens
import androidx.wear.compose.materialcore.isLargeScreen
import java.time.Duration
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield

/**
 * A full screen DurationPicker that allows users to select a duration.
 *
 * [DurationPicker] allows the caller to provide the initial duration, the columns presented (such
 * as hours/minutes or minutes/seconds) and validation of minimum/maximum durations.
 *
 * This component is designed to take most/all of the screen and utilizes large fonts.
 *
 * [minValidDuration] is rounded up and [maxValidDuration] is rounded down to the smallest visible
 * unit for the given [durationPickerType] (whole minutes for [DurationPickerType.HoursMinutes], and
 * whole seconds for all other types), so every value the user can pick is between
 * [minValidDuration] and [maxValidDuration]. [initialDuration] is rounded down to the smallest
 * visible unit.
 *
 * Individual column options outside `[minValidDuration, maxValidDuration]` are displayed using
 * [DurationPickerColors.invalidPickerContentColor].
 *
 * If [initialDuration] (for example, [Duration.ZERO]) is less than [minValidDuration] or greater
 * than [maxValidDuration] after rounding, the picker opens at [initialDuration] with the confirm
 * button disabled. Auto-scrolling is enabled after the first scroll: whenever scrolling settles on
 * an out-of-range duration, each invalid column automatically animates to its nearest valid option.
 *
 * Example of a [DurationPicker] with hours, minutes, and seconds:
 *
 * @sample androidx.wear.compose.material3.samples.DurationPickerHoursMinutesSecondsSample
 *
 * ![DurationPickerHoursMinutesSecondsSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_DurationPickerHoursMinutesSecondsSample_CompositeImage.png)
 *
 * Example of a [DurationPicker] with minutes and seconds:
 *
 * @sample androidx.wear.compose.material3.samples.DurationPickerMinutesSecondsSample
 *
 * ![DurationPickerMinutesSecondsSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_DurationPickerMinutesSecondsSample_CompositeImage.png)
 *
 * Example of a [DurationPicker] with hours and minutes:
 *
 * @sample androidx.wear.compose.material3.samples.DurationPickerHoursMinutesSample
 *
 * ![DurationPickerHoursMinutesSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_DurationPickerHoursMinutesSample_CompositeImage.png)
 *
 * Example of a [DurationPicker] with a maximum duration exceeding 24 hours:
 *
 * @sample androidx.wear.compose.material3.samples.DurationPickerMaximumDurationSample
 *
 * <video
 * src=https://developer.android.com/wear/images/design/WearComposeM3_DurationPickerMaximumDurationSample_CompositeImage.mp4
 * autoplay loop muted playsinline style=border-radius:2.4%/6.8%;overflow:hidden; />
 *
 * @param initialDuration The initial duration to be displayed in the DurationPicker. Must be
 *   non-negative and not exceed the limit for [durationPickerType]. Changing [initialDuration]
 *   after the first composition does not reset the picker. Changing [minValidDuration],
 *   [maxValidDuration] or [durationPickerType] resets the picker to [initialDuration] and
 *   [initialSelection].
 * @param onDurationPicked The callback that is called when the user confirms the duration
 *   selection. It provides the selected duration as [Duration]. Any units not displayed in the
 *   picker (e.g. seconds for [DurationPickerType.HoursMinutes]) will be 0 in the returned
 *   [Duration].
 * @param modifier Modifier to be applied to the `Box` containing the UI elements.
 * @param minValidDuration The minimum valid duration that can be confirmed. Must be non-negative
 *   and not exceed [maxValidDuration]. See [DurationPickerType] for default values.
 * @param maxValidDuration The maximum valid duration that can be confirmed. Must be non-negative
 *   and not exceed the limit for [durationPickerType]. When null, the default is the
 *   max([minValidDuration], [durationPickerType]'s default maximum), up to the
 *   [durationPickerType]'s limit. See [DurationPickerType] for default values.
 * @param durationPickerType The different [DurationPickerType] supported by this duration picker.
 * @param initialSelection The initial component to be selected. If [initialSelection] is a column
 *   that is not shown for the given [durationPickerType] (for example,
 *   [DurationPickerSelection.Seconds] with [DurationPickerType.HoursMinutes]), the first column is
 *   selected instead. If [initialSelection] is the confirm button but the displayed duration is
 *   invalid, the confirm button is disabled, so the first column is selected instead. When touch
 *   exploration services (such as TalkBack) are enabled, no component is selected initially.
 * @param colors [DurationPickerColors] to be applied to the DurationPicker.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
public fun DurationPicker(
    initialDuration: Duration,
    onDurationPicked: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    minValidDuration: Duration? = null,
    maxValidDuration: Duration? = null,
    durationPickerType: DurationPickerType = DurationPickerDefaults.type,
    initialSelection: DurationPickerSelection =
        DurationPickerDefaults.initialSelection(durationPickerType),
    colors: DurationPickerColors = DurationPickerDefaults.colors(),
) {
    val inspectionMode = LocalInspectionMode.current
    val fullyDrawn = remember { Animatable(if (inspectionMode) 1f else 0f) }

    // The durations are validated and rounded only when the state is created (first composition or
    // a change to minValidDuration, maxValidDuration or durationPickerType), so changing
    // initialDuration alone has no effect. Keyed on the raw bounds: resolving them also validates
    // initialDuration, which must only run when the state is created.
    val durationPickerState =
        rememberSaveable(
            minValidDuration,
            maxValidDuration,
            durationPickerType,
            saver = DurationPickerState.Saver,
        ) {
            val effectiveDurations =
                verifyAndRoundDurations(
                    initialDuration = initialDuration,
                    minValidDuration = minValidDuration,
                    maxValidDuration = maxValidDuration,
                    durationPickerType = durationPickerType,
                )
            DurationPickerState(
                initialDuration = effectiveDurations.initial,
                minValidDuration = effectiveDurations.min,
                maxValidDuration = effectiveDurations.max,
                durationPickerType = durationPickerType,
            )
        }

    // Auto-scrolling is active after the first scroll (skipped on initial composition so an
    // initialDuration outside [minValidDuration, maxValidDuration] stays displayed with the confirm
    // button disabled until the user interacts). Once hasScrolled is true, settling on an
    // out-of-range duration animates invalid columns from left to right (hours -> minutes ->
    // seconds) to a valid option and stops as soon as the total duration is within
    // [minValidDuration, maxValidDuration]. Each column scrolls to its nearest valid option, the
    // shortest way around the wheel (see closestValidOption), so it passes only through invalid
    // options. On a tie, it scrolls to the min if the selected option is below the range, or to the
    // max if it is above.
    //
    // Observe scroll settle and validity via snapshotFlow rather than LaunchedEffect keys so that
    // starting animateScrollToOption (which sets isScrollInProgress = true) does not cancel the
    // effect mid-flight. The while loop retries immediately if a synchronous scrollToOption (e.g.
    // on column focus change) interrupts an animation without flipping isScrollInProgress, and
    // ensureActive() yields to user gestures (MutatePriority.UserInput) while keeping the collector
    // alive to re-adjust when the gesture settles.
    LaunchedEffect(durationPickerState) {
        val initialSelectedDuration = durationPickerState.selectedDuration
        snapshotFlow {
            if (durationPickerState.isScrollInProgress) null
            else durationPickerState.selectedDuration
        }
            .collect { settledDuration ->
                if (settledDuration == null || settledDuration != initialSelectedDuration) {
                    durationPickerState.hasScrolled = true
                }
                while (
                    durationPickerState.hasScrolled &&
                        !durationPickerState.isScrollInProgress &&
                        !durationPickerState.isSelectedDurationValid
                ) {
                    val durationBeforeAdjust = durationPickerState.selectedDuration
                    try {
                        when {
                            !durationPickerState.isSelectedHourValid ->
                                durationPickerState.adjustHourOptionIfInvalid()
                            !durationPickerState.isSelectedMinuteValid ->
                                durationPickerState.adjustMinuteOptionIfInvalid()
                            !durationPickerState.isSelectedSecondValid ->
                                durationPickerState.adjustSecondOptionIfInvalid()
                        }
                    } catch (e: CancellationException) {
                        currentCoroutineContext().ensureActive()
                        yield()
                        continue
                    }
                    // Avoid spinning without suspending if an adjustment completes without
                    // changing the selection. The next settled change re-triggers the collector.
                    if (durationPickerState.selectedDuration == durationBeforeAdjust) break
                }
            }
    }

    val touchExplorationServicesEnabled by
        LocalTouchExplorationStateProvider.current.touchExplorationState()

    val focusRequesterConfirmButton = remember { FocusRequester() }

    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale

    @SuppressLint("PrimitiveInCollection")
    val focusableOrder =
        remember(durationPickerType) {
            when (durationPickerType) {
                DurationPickerType.HoursMinutesSeconds ->
                    listOf(
                        DurationPickerSelection.Hours,
                        DurationPickerSelection.Minutes,
                        DurationPickerSelection.Seconds,
                    )
                DurationPickerType.MinutesSeconds ->
                    listOf(
                        DurationPickerSelection.Minutes,
                        DurationPickerSelection.Seconds,
                    )
                DurationPickerType.HoursMinutes ->
                    listOf(
                        DurationPickerSelection.Hours,
                        DurationPickerSelection.Minutes,
                    )
                else -> emptyList()
            }
        }

    var selectedElement: DurationPickerSelection by
        rememberSaveable(
            touchExplorationServicesEnabled,
            focusableOrder,
            initialSelection,
            durationPickerState,
            stateSaver =
                Saver(
                    save = { it.value },
                    restore = { DurationPickerSelection(it) },
                ),
        ) {
            mutableStateOf(
                when {
                    touchExplorationServicesEnabled -> DurationPickerSelection.None
                    initialSelection == DurationPickerSelection.ConfirmButton &&
                        durationPickerState.isSelectedDurationValid ->
                        DurationPickerSelection.ConfirmButton
                    initialSelection in focusableOrder -> initialSelection
                    else -> focusableOrder.firstOrNull() ?: DurationPickerSelection.None
                }
            )
        }

    val isValid by
        remember(durationPickerState) {
            derivedStateOf { durationPickerState.isSelectedDurationValid }
        }

    val instructionHeadingString = getString(Strings.DurationPickerHeading)
    val hoursString = getString(Strings.DurationPickerHours)
    val minutesString = getString(Strings.DurationPickerMinutes)
    val secondsString = getString(Strings.DurationPickerSeconds)

    val hoursContentDescription = {
        createDescription(
            context,
            selectedElement,
            durationPickerState.selectedHours,
            hoursString,
            Plurals.DurationPickerHoursContentDescription,
        )
    }
    val minutesContentDescription = {
        createDescription(
            context,
            selectedElement,
            durationPickerState.selectedMinutes,
            minutesString,
            Plurals.DurationPickerMinutesContentDescription,
        )
    }
    val secondsContentDescription = {
        createDescription(
            context,
            selectedElement,
            durationPickerState.selectedSeconds,
            secondsString,
            Plurals.DurationPickerSecondsContentDescription,
        )
    }

    // Don't move to the disabled confirm button while the selected duration is invalid; wrap back
    // to the first column so that a column stays selected and rotary input keeps working.
    val findNextElement = { current: DurationPickerSelection ->
        val currentIndex = focusableOrder.indexOf(current)
        focusableOrder.getOrNull(currentIndex + 1)
            ?: if (durationPickerState.isSelectedDurationValid) {
                DurationPickerSelection.ConfirmButton
            } else {
                focusableOrder.first()
            }
    }

    val onPickerSelected = { current: DurationPickerSelection ->
        if (selectedElement != current) {
            selectedElement = current
        } else {
            selectedElement = findNextElement(current)
        }
    }

    LaunchedEffect(selectedElement, isValid) {
        if (selectedElement == DurationPickerSelection.ConfirmButton && isValid) {
            focusRequesterConfirmButton.requestFocus()
        }
    }

    Box(
        modifier =
            modifier.suppressStatusBar().fillMaxSize().graphicsLayer { alpha = fullyDrawn.value }
    ) {
        // Allow more room for the initial instruction heading under TalkBack
        val maxTextLines = if (selectedElement == DurationPickerSelection.None) 2 else 1
        val textPaddingPercentage = 30f
        val topPadding = if (selectedElement == DurationPickerSelection.None) 0.dp else 14.dp
        val headingHeight = 38.dp - topPadding

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(topPadding))

            FontScaleIndependent {
                val layoutConfig =
                    rememberDurationPickerLayoutConfig(
                        durationPickerType = durationPickerType,
                        locale = locale,
                    )
                val headingText =
                    when (selectedElement) {
                        DurationPickerSelection.Hours -> hoursString
                        DurationPickerSelection.Minutes -> minutesString
                        DurationPickerSelection.Seconds -> secondsString
                        DurationPickerSelection.None ->
                            if (touchExplorationServicesEnabled) instructionHeadingString else ""
                        else -> null
                    }

                FadeLabel(
                    text = headingText ?: "",
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    modifier =
                        Modifier.height(headingHeight)
                            .padding(
                                horizontal =
                                    PaddingDefaults.horizontalContentPadding(textPaddingPercentage)
                            )
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .then(
                                if (headingText != null) {
                                    Modifier.semantics(mergeDescendants = true) { heading() }
                                } else {
                                    Modifier.drawWithContent {}.clearAndSetSemantics {}
                                }
                            ),
                    color = colors.pickerLabelColor,
                    style = layoutConfig.labelTextStyle,
                    maxLines = maxTextLines,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(layoutConfig.sectionVerticalPadding))

                DurationPickerContent(
                    selectedElement = selectedElement,
                    onPickerSelected = onPickerSelected,
                    durationPickerState = durationPickerState,
                    hoursContentDescription = hoursContentDescription,
                    minutesContentDescription = minutesContentDescription,
                    secondsContentDescription = secondsContentDescription,
                    colors = colors,
                    layoutConfig = layoutConfig,
                    locale = locale,
                )
                Spacer(Modifier.height(layoutConfig.sectionVerticalPadding))
            }

            val minFormatted =
                remember(locale, durationPickerState) {
                    formatDuration(
                        locale,
                        durationPickerState.minValidDuration,
                        durationPickerState.durationPickerType,
                    )
                }
            val maxFormatted =
                remember(locale, durationPickerState) {
                    formatDuration(
                        locale,
                        durationPickerState.maxValidDuration,
                        durationPickerState.durationPickerType,
                    )
                }
            val disabledDescription =
                if (!isValid) {
                    getString(
                        Strings.DurationPickerDisabledRange,
                        minFormatted,
                        maxFormatted,
                    )
                } else {
                    null
                }

            EdgeButton(
                onClick = {
                    // Ignore clicks while a fling or an auto-adjust animation is in progress, so
                    // that an intermediate duration is never reported.
                    if (
                        durationPickerState.isSelectedDurationValid &&
                            !durationPickerState.isScrollInProgress
                    ) {
                        onDurationPicked(durationPickerState.selectedDuration)
                    }
                },
                modifier =
                    Modifier.semantics {
                            if (!isValid && disabledDescription != null) {
                                stateDescription = disabledDescription
                            }
                        }
                        .focusRequester(focusRequesterConfirmButton)
                        .focusable(enabled = isValid),
                buttonSize = EdgeButtonSize.Small,
                enabled = isValid,
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

    val isReduceMotionEnabled = LocalReduceMotion.current

    if (!inspectionMode) {
        LaunchedEffect(Unit) {
            if (isReduceMotionEnabled) {
                // Await one frame to allow the ScalingLazyColumn layout positioning to settle
                // silently before revealing the UI, avoiding a 1-frame visual stutter where
                // static elements (like the colon separator) appear before the digit columns.
                withFrameNanos {}
                fullyDrawn.snapTo(1f)
            } else {
                fullyDrawn.animateTo(1f)
            }
        }
    }
}

/** Specifies the types of columns to display in the DurationPicker. */
@Immutable
@JvmInline
public value class DurationPickerType internal constructor(internal val value: Int) {
    public companion object {
        /**
         * Displays three columns: Hours, Minutes, and Seconds. The default minimum valid duration
         * is 1 second, the default maximum valid duration is 23h 59m 59s, and the limit is 99h 59m
         * 59s.
         */
        public val HoursMinutesSeconds: DurationPickerType = DurationPickerType(0)

        /**
         * Displays two columns: Minutes and Seconds. The default minimum valid duration is 1
         * second, and both the default maximum valid duration and the limit are 59m 59s.
         */
        public val MinutesSeconds: DurationPickerType = DurationPickerType(1)

        /**
         * Displays two columns: Hours and Minutes. The default minimum valid duration is 1 minute,
         * the default maximum valid duration is 23h 59m, and the limit is 99h 59m.
         */
        public val HoursMinutes: DurationPickerType = DurationPickerType(2)
    }

    override fun toString(): String =
        when (this) {
            HoursMinutesSeconds -> "HoursMinutesSeconds"
            MinutesSeconds -> "MinutesSeconds"
            HoursMinutes -> "HoursMinutes"
            else -> "Unknown"
        }
}

/** Specifies which element is selected in the DurationPicker. */
@Immutable
@JvmInline
public value class DurationPickerSelection internal constructor(internal val value: Int) {
    public companion object {
        /** Represents the hours component of the duration. */
        public val Hours: DurationPickerSelection = DurationPickerSelection(0)

        /** Represents the minutes component of the duration. */
        public val Minutes: DurationPickerSelection = DurationPickerSelection(1)

        /** Represents the seconds component of the duration. */
        public val Seconds: DurationPickerSelection = DurationPickerSelection(2)

        /** Represents the confirmation button component used to confirm the selected duration. */
        public val ConfirmButton: DurationPickerSelection = DurationPickerSelection(3)

        /** Indicates that no specific component is selected. Used primarily for accessibility. */
        public val None: DurationPickerSelection = DurationPickerSelection(4)
    }

    override fun toString(): String =
        when (this) {
            Hours -> "Hours"
            Minutes -> "Minutes"
            Seconds -> "Seconds"
            ConfirmButton -> "ConfirmButton"
            None -> "None"
            else -> "Unknown"
        }
}

/** Contains the default values used by [DurationPicker]. */
public object DurationPickerDefaults {
    /** The default [DurationPickerType] for [DurationPicker]. */
    public val type: DurationPickerType = DurationPickerType.HoursMinutesSeconds

    /**
     * The default [DurationPickerSelection] for [DurationPicker] is set to the first available
     * duration component based on the provided [DurationPickerType].
     */
    public fun initialSelection(
        durationPickerType: DurationPickerType = this.type
    ): DurationPickerSelection =
        when (durationPickerType) {
            DurationPickerType.MinutesSeconds -> DurationPickerSelection.Minutes
            DurationPickerType.HoursMinutes,
            DurationPickerType.HoursMinutesSeconds -> DurationPickerSelection.Hours
            else ->
                throw IllegalArgumentException("Unknown DurationPickerType: $durationPickerType")
        }

    /** Creates a [DurationPickerColors] for a [DurationPicker]. */
    @Composable
    public fun colors(): DurationPickerColors =
        MaterialTheme.colorScheme.defaultDurationPickerColors

    /**
     * Creates a [DurationPickerColors] for a [DurationPicker].
     *
     * @param selectedPickerContentColor The content color of the selected picker.
     * @param unselectedPickerContentColor The content color of unselected pickers.
     * @param invalidPickerContentColor The content color of invalid picker options.
     * @param separatorColor The color of separator between the pickers.
     * @param pickerLabelColor The color of the picker label.
     * @param confirmButtonContentColor The content color of the confirm button.
     * @param confirmButtonContainerColor The container color of the confirm button.
     * @param pickerBackgroundColor The background color of the picker, used for the top and bottom
     *   gradient fade on the picker columns. Defaults to [Color.Unspecified], which makes the fade
     *   transparent, so it works on any background, including gradients and images.
     */
    @Composable
    public fun colors(
        selectedPickerContentColor: Color = Color.Unspecified,
        unselectedPickerContentColor: Color = Color.Unspecified,
        invalidPickerContentColor: Color = Color.Unspecified,
        separatorColor: Color = Color.Unspecified,
        pickerLabelColor: Color = Color.Unspecified,
        confirmButtonContentColor: Color = Color.Unspecified,
        confirmButtonContainerColor: Color = Color.Unspecified,
        pickerBackgroundColor: Color = Color.Unspecified,
    ): DurationPickerColors =
        MaterialTheme.colorScheme.defaultDurationPickerColors.copy(
            selectedPickerContentColor = selectedPickerContentColor,
            unselectedPickerContentColor = unselectedPickerContentColor,
            invalidPickerContentColor = invalidPickerContentColor,
            separatorColor = separatorColor,
            pickerLabelColor = pickerLabelColor,
            confirmButtonContentColor = confirmButtonContentColor,
            confirmButtonContainerColor = confirmButtonContainerColor,
            pickerBackgroundColor = pickerBackgroundColor,
        )

    private val ColorScheme.defaultDurationPickerColors: DurationPickerColors
        get() {
            return defaultDurationPickerColorsCached
                ?: DurationPickerColors(
                        selectedPickerContentColor =
                            fromToken(DurationPickerTokens.SelectedContentColor),
                        unselectedPickerContentColor =
                            fromToken(DurationPickerTokens.UnselectedContentColor),
                        invalidPickerContentColor =
                            fromToken(DurationPickerTokens.InvalidContentColor)
                                .toDisabledColor(
                                    disabledAlpha = DurationPickerTokens.InvalidContentOpacity
                                ),
                        separatorColor = fromToken(DurationPickerTokens.SeparatorColor),
                        pickerLabelColor = fromToken(DurationPickerTokens.LabelColor),
                        confirmButtonContentColor =
                            fromToken(DurationPickerTokens.ConfirmButtonContentColor),
                        confirmButtonContainerColor =
                            fromToken(DurationPickerTokens.ConfirmButtonContainerColor),
                        pickerBackgroundColor = Color.Unspecified,
                    )
                    .also { defaultDurationPickerColorsCached = it }
        }
}

/**
 * Represents the colors used by a [DurationPicker].
 *
 * @param selectedPickerContentColor The content color of selected picker.
 * @param unselectedPickerContentColor The content color of unselected pickers.
 * @param invalidPickerContentColor The content color of invalid picker options.
 * @param separatorColor The color of separator between the pickers.
 * @param pickerLabelColor The color of the picker label.
 * @param confirmButtonContentColor The content color of the confirm button.
 * @param confirmButtonContainerColor The container color of the confirm button.
 * @param pickerBackgroundColor The background color of the picker, used for the top and bottom
 *   gradient fade on the picker columns. [Color.Unspecified] makes the fade transparent, so it
 *   works on any background, including gradients and images.
 */
@Immutable
public class DurationPickerColors(
    public val selectedPickerContentColor: Color,
    public val unselectedPickerContentColor: Color,
    public val invalidPickerContentColor: Color,
    public val separatorColor: Color,
    public val pickerLabelColor: Color,
    public val confirmButtonContentColor: Color,
    public val confirmButtonContainerColor: Color,
    public val pickerBackgroundColor: Color,
) {
    /**
     * Returns a copy of this DurationPickerColors, optionally overriding some of the values.
     *
     * @param selectedPickerContentColor The content color of selected picker.
     * @param unselectedPickerContentColor The content color of unselected pickers.
     * @param invalidPickerContentColor The content color of invalid picker options.
     * @param separatorColor The color of separator between the pickers.
     * @param pickerLabelColor The color of the picker label.
     * @param confirmButtonContentColor The content color of the confirm button.
     * @param confirmButtonContainerColor The container color of the confirm button.
     * @param pickerBackgroundColor The background color of the picker, used for the top and bottom
     *   gradient fade on the picker columns.
     */
    public fun copy(
        selectedPickerContentColor: Color = this.selectedPickerContentColor,
        unselectedPickerContentColor: Color = this.unselectedPickerContentColor,
        invalidPickerContentColor: Color = this.invalidPickerContentColor,
        separatorColor: Color = this.separatorColor,
        pickerLabelColor: Color = this.pickerLabelColor,
        confirmButtonContentColor: Color = this.confirmButtonContentColor,
        confirmButtonContainerColor: Color = this.confirmButtonContainerColor,
        pickerBackgroundColor: Color = this.pickerBackgroundColor,
    ): DurationPickerColors =
        DurationPickerColors(
            selectedPickerContentColor =
                selectedPickerContentColor.takeOrElse { this.selectedPickerContentColor },
            unselectedPickerContentColor =
                unselectedPickerContentColor.takeOrElse { this.unselectedPickerContentColor },
            invalidPickerContentColor =
                invalidPickerContentColor.takeOrElse { this.invalidPickerContentColor },
            separatorColor = separatorColor.takeOrElse { this.separatorColor },
            pickerLabelColor = pickerLabelColor.takeOrElse { this.pickerLabelColor },
            confirmButtonContentColor =
                confirmButtonContentColor.takeOrElse { this.confirmButtonContentColor },
            confirmButtonContainerColor =
                confirmButtonContainerColor.takeOrElse { this.confirmButtonContainerColor },
            pickerBackgroundColor = pickerBackgroundColor.takeOrElse { this.pickerBackgroundColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DurationPickerColors) return false

        if (selectedPickerContentColor != other.selectedPickerContentColor) return false
        if (unselectedPickerContentColor != other.unselectedPickerContentColor) return false
        if (invalidPickerContentColor != other.invalidPickerContentColor) return false
        if (separatorColor != other.separatorColor) return false
        if (pickerLabelColor != other.pickerLabelColor) return false
        if (confirmButtonContentColor != other.confirmButtonContentColor) return false
        if (confirmButtonContainerColor != other.confirmButtonContainerColor) return false
        if (pickerBackgroundColor != other.pickerBackgroundColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedPickerContentColor.hashCode()
        result = 31 * result + unselectedPickerContentColor.hashCode()
        result = 31 * result + invalidPickerContentColor.hashCode()
        result = 31 * result + separatorColor.hashCode()
        result = 31 * result + pickerLabelColor.hashCode()
        result = 31 * result + confirmButtonContentColor.hashCode()
        result = 31 * result + confirmButtonContainerColor.hashCode()
        result = 31 * result + pickerBackgroundColor.hashCode()

        return result
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ColumnScope.DurationPickerContent(
    selectedElement: DurationPickerSelection,
    onPickerSelected: (DurationPickerSelection) -> Unit,
    durationPickerState: DurationPickerState,
    hoursContentDescription: () -> String,
    minutesContentDescription: () -> String,
    secondsContentDescription: () -> String,
    colors: DurationPickerColors,
    layoutConfig: DurationPickerLayoutConfig,
    locale: Locale,
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PickerGroup(
            selectedPickerState =
                when (selectedElement) {
                    DurationPickerSelection.Hours -> durationPickerState.hourState
                    DurationPickerSelection.Minutes -> durationPickerState.minuteState
                    DurationPickerSelection.Seconds -> durationPickerState.secondState
                    else -> null
                },
            modifier = Modifier.fillMaxWidth(),
            autoCenter = false,
        ) {
            // Force LTR layout direction to prevent columns from reversing (e.g.
            // seconds:minutes:hours)
            // in right-to-left (RTL) locales. Elapsed durations are universally represented with
            // descending unit significance from left to right across all layouts (HH:MM:SS, HH:MM,
            // MM:SS).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (durationPickerState.hourState != null) {
                        DurationColumnPicker(
                            pickerState = durationPickerState.hourState,
                            selected = selectedElement == DurationPickerSelection.Hours,
                            onSelected = { onPickerSelected(DurationPickerSelection.Hours) },
                            contentDescription = hoursContentDescription,
                            layoutConfig = layoutConfig,
                            colors = colors,
                            isValid = durationPickerState::isHourValid,
                            locale = locale,
                        )
                        Separator(
                            textStyle = layoutConfig.optionTextStyle,
                            color = colors.separatorColor,
                            separatorPadding = layoutConfig.separatorPadding,
                            text = layoutConfig.separatorText,
                            optionHeight = layoutConfig.optionHeight,
                            optionBaseline = layoutConfig.optionBaseline,
                        )
                    }

                    DurationColumnPicker(
                        pickerState = durationPickerState.minuteState,
                        selected = selectedElement == DurationPickerSelection.Minutes,
                        onSelected = { onPickerSelected(DurationPickerSelection.Minutes) },
                        contentDescription = minutesContentDescription,
                        layoutConfig = layoutConfig,
                        colors = colors,
                        isValid = durationPickerState::isMinuteValid,
                        locale = locale,
                    )

                    if (durationPickerState.secondState != null) {
                        Separator(
                            textStyle = layoutConfig.optionTextStyle,
                            color = colors.separatorColor,
                            separatorPadding = layoutConfig.separatorPadding,
                            text = layoutConfig.separatorText,
                            optionHeight = layoutConfig.optionHeight,
                            optionBaseline = layoutConfig.optionBaseline,
                        )
                        DurationColumnPicker(
                            pickerState = durationPickerState.secondState,
                            selected = selectedElement == DurationPickerSelection.Seconds,
                            onSelected = { onPickerSelected(DurationPickerSelection.Seconds) },
                            contentDescription = secondsContentDescription,
                            layoutConfig = layoutConfig,
                            colors = colors,
                            isValid = durationPickerState::isSecondValid,
                            locale = locale,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerGroupScope.DurationColumnPicker(
    pickerState: PickerState,
    selected: Boolean,
    onSelected: () -> Unit,
    contentDescription: () -> String,
    layoutConfig: DurationPickerLayoutConfig,
    colors: DurationPickerColors,
    isValid: (Int) -> Boolean,
    locale: Locale,
) {
    PickerGroupItem(
        pickerState = pickerState,
        modifier = Modifier.width(layoutConfig.twoDigitsOptionWidth).fillMaxHeight(),
        selected = selected,
        onSelected = onSelected,
        contentDescription = contentDescription,
        option =
            pickerTextOption(
                textStyle = layoutConfig.optionTextStyle,
                indexToText = { "%02d".format(locale, it) },
                optionHeight = layoutConfig.optionHeight,
                optionBaseline = layoutConfig.optionBaseline,
                selectedContentColor = colors.selectedPickerContentColor,
                unselectedContentColor = colors.unselectedPickerContentColor,
                invalidContentColor = colors.invalidPickerContentColor,
                isValid = isValid,
            ),
        verticalSpacing = layoutConfig.optionSpacing,
        gradientColor = colors.pickerBackgroundColor,
    )
}

@Composable
private fun rememberDurationPickerLayoutConfig(
    durationPickerType: DurationPickerType,
    locale: Locale,
): DurationPickerLayoutConfig {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val isLargeScreen = isLargeScreen()
    val labelTextStyle =
        if (isLargeScreen) {
                DurationPickerTokens.LabelLargeTypography
            } else {
                DurationPickerTokens.LabelTypography
            }
            .value

    val optionTextStyle =
        if (isLargeScreen) {
                DurationPickerTokens.ContentLargeTypography
            } else {
                DurationPickerTokens.ContentTypography
            }
            .value
            .copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum")

    return remember(
        durationPickerType,
        locale,
        isLargeScreen,
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
        val isTwoColumn =
            durationPickerType == DurationPickerType.HoursMinutes ||
                durationPickerType == DurationPickerType.MinutesSeconds
        val separatorPadding =
            when {
                isTwoColumn && isLargeScreen -> 12.dp
                isTwoColumn && !isLargeScreen -> 8.dp
                isLargeScreen -> 6.dp
                else -> 2.dp
            }

        val localizedDigits = buildString { (0..9).forEach { append("%d".format(locale, it)) } }
        val measureResult =
            measurer.measure(
                localizedDigits,
                style = optionTextStyle,
                density = density,
            )

        val digitCharWidthPx =
            (0 until localizedDigits.length).maxOf { measureResult.getBoundingBox(it).width }
        val twoDigitsOptionWidth =
            with(density) {
                (digitCharWidthPx * 2).toDp() + 1.dp
            }

        val measuredOptionHeight =
            with(density) {
                (measureResult.getLineBottom(0) - measureResult.getLineTop(0)).toDp()
            }
        val optionHeight = measuredOptionHeight.coerceIn(minimumOptionHeight, maximumOptionHeight)
        val optionBaseline =
            calculateBaseline(
                measuredOptionBaselinePx = measureResult.getLineBaseline(0),
                measuredOptionHeight = measuredOptionHeight,
                maximumOptionHeight = maximumOptionHeight,
                minimumOptionHeight = minimumOptionHeight,
                density = density,
            )

        DurationPickerLayoutConfig(
            labelTextStyle = labelTextStyle,
            optionTextStyle = optionTextStyle,
            twoDigitsOptionWidth = max(twoDigitsOptionWidth, minimumInteractiveComponentSize),
            optionHeight = optionHeight,
            optionBaseline = optionBaseline,
            optionSpacing = optionSpacing,
            separatorPadding = separatorPadding,
            separatorText = getTimeSeparator(locale),
            sectionVerticalPadding = if (isLargeScreen) 6.dp else 4.dp,
        )
    }
}

private class DurationPickerLayoutConfig(
    val labelTextStyle: TextStyle,
    val optionTextStyle: TextStyle,
    val twoDigitsOptionWidth: Dp,
    val optionHeight: Dp,
    val optionBaseline: Int,
    val optionSpacing: Dp,
    val separatorPadding: Dp,
    val separatorText: String,
    val sectionVerticalPadding: Dp,
)

private fun calculateBaseline(
    measuredOptionBaselinePx: Float,
    measuredOptionHeight: Dp,
    maximumOptionHeight: Dp,
    minimumOptionHeight: Dp,
    density: Density,
): Int {
    return if (measuredOptionHeight > maximumOptionHeight) {
        (measuredOptionBaselinePx +
                with(density) {
                    min(0.dp, (maximumOptionHeight - measuredOptionHeight) / 2).toPx()
                })
            .toInt()
    } else {
        (measuredOptionBaselinePx +
                with(density) {
                    max(0.dp, (minimumOptionHeight - measuredOptionHeight) / 2).toPx()
                })
            .toInt()
    }
}

@Composable
private fun Separator(
    textStyle: TextStyle,
    color: Color,
    separatorPadding: Dp,
    text: String,
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
                Modifier.wrapContentHeight()
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
    selectedElement: DurationPickerSelection,
    selectedValue: Int,
    label: String,
    plurals: Plurals,
) =
    if (selectedElement == DurationPickerSelection.None) {
        label
    } else {
        context.resources.getQuantityString(plurals.value, selectedValue, selectedValue)
    }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDuration(
    locale: Locale,
    duration: Duration,
    durationPickerType: DurationPickerType,
): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    val measures = buildList {
        if (hours > 0) {
            add(Measure(hours, MeasureUnit.HOUR))
        }
        if (minutes > 0) {
            add(Measure(minutes, MeasureUnit.MINUTE))
        }
        if (seconds > 0) {
            add(Measure(seconds, MeasureUnit.SECOND))
        }
        if (isEmpty()) {
            // A zero duration is shown in the smallest unit of the picker.
            add(
                Measure(
                    0,
                    if (durationPickerType == DurationPickerType.HoursMinutes) MeasureUnit.MINUTE
                    else MeasureUnit.SECOND,
                )
            )
        }
    }
    return MeasureFormat.getInstance(locale, FormatWidth.WIDE)
        .formatMeasures(*measures.toTypedArray())
}

internal fun getTimeSeparator(locale: Locale): String {
    val formatted =
        MeasureFormat.getInstance(locale, FormatWidth.NUMERIC)
            .formatMeasures(Measure(1, MeasureUnit.HOUR), Measure(1, MeasureUnit.MINUTE))
    val separator = formatted.firstOrNull { char ->
        !char.isDigit() &&
            !char.isLetter() &&
            !char.isWhitespace() &&
            Character.getType(char) != Character.FORMAT.toInt()
    }
    return separator?.toString() ?: ":"
}

private val SeparatorWidth = 12.dp

@RequiresApi(Build.VERSION_CODES.O)
private class DurationPickerState(
    val initialDuration: Duration,
    val minValidDuration: Duration,
    val maxValidDuration: Duration,
    val durationPickerType: DurationPickerType,
    var hasScrolled: Boolean = false,
    initiallySelectedDuration: Duration = initialDuration,
) {
    private val initialHours = initiallySelectedDuration.toHours().toInt()
    private val initialMinutes = (initiallySelectedDuration.toMinutes() % 60).toInt()
    private val initialSeconds = (initiallySelectedDuration.seconds % 60).toInt()

    private val minHours = minValidDuration.toHours().toInt()
    private val minMinutes = (minValidDuration.toMinutes() % 60).toInt()
    private val minSeconds = (minValidDuration.seconds % 60).toInt()

    private val maxHours = maxValidDuration.toHours().toInt()
    private val maxMinutes = (maxValidDuration.toMinutes() % 60).toInt()
    private val maxSeconds = (maxValidDuration.seconds % 60).toInt()

    // The leading column shows options up to maxValidDuration or initialDuration, whichever is
    // larger, so an initialDuration above maxValidDuration is displayed. initialDuration is
    // validated to not exceed the type limit, so this is at most 100 options (hours 0..99) or 60
    // options (minutes 0..59).
    private val leadingNumberOfOptions =
        if (durationPickerType == DurationPickerType.MinutesSeconds) {
            maxOf(maxMinutes, initialDuration.toMinutes().toInt()) + 1
        } else {
            maxOf(maxHours, initialDuration.toHours().toInt()) + 1
        }

    val hourState: PickerState? =
        when (durationPickerType) {
            DurationPickerType.HoursMinutesSeconds,
            DurationPickerType.HoursMinutes ->
                PickerState(
                    initialNumberOfOptions = leadingNumberOfOptions,
                    initiallySelectedIndex = initialHours,
                    shouldRepeatOptions = leadingNumberOfOptions >= 3,
                )
            else -> null
        }

    val minuteState: PickerState =
        when (durationPickerType) {
            DurationPickerType.MinutesSeconds ->
                PickerState(
                    initialNumberOfOptions = leadingNumberOfOptions,
                    initiallySelectedIndex = initialMinutes,
                    shouldRepeatOptions = leadingNumberOfOptions >= 3,
                )
            else ->
                PickerState(
                    initialNumberOfOptions = 60,
                    initiallySelectedIndex = initialMinutes,
                    shouldRepeatOptions = true,
                )
        }

    val secondState: PickerState? =
        when (durationPickerType) {
            DurationPickerType.HoursMinutesSeconds,
            DurationPickerType.MinutesSeconds ->
                PickerState(
                    initialNumberOfOptions = 60,
                    initiallySelectedIndex = initialSeconds,
                    shouldRepeatOptions = true,
                )
            else -> null
        }

    val selectedHours: Int
        get() = hourState?.selectedOptionIndex ?: 0

    val selectedMinutes: Int
        get() = minuteState.selectedOptionIndex

    val selectedSeconds: Int
        get() = secondState?.selectedOptionIndex ?: 0

    val selectedDuration: Duration
        get() =
            Duration.ofHours(selectedHours.toLong())
                .plusMinutes(selectedMinutes.toLong())
                .plusSeconds(selectedSeconds.toLong())

    val isMinHourSelected: Boolean
        get() = selectedHours == minHours

    val isMaxHourSelected: Boolean
        get() = selectedHours == maxHours

    private val isMinMinuteSelected: Boolean
        get() = isMinHourSelected && selectedMinutes == minMinutes

    private val isMaxMinuteSelected: Boolean
        get() = isMaxHourSelected && selectedMinutes == maxMinutes

    private val minValidHour: Int
        get() = minHours

    private val maxValidHour: Int
        get() = maxHours

    private val minValidMinute: Int
        get() = if (isMinHourSelected) minMinutes else 0

    private val maxValidMinute: Int
        get() = if (isMaxHourSelected) maxMinutes else 59

    private val minValidSecond: Int
        get() = if (isMinMinuteSelected) minSeconds else 0

    private val maxValidSecond: Int
        get() = if (isMaxMinuteSelected) maxSeconds else 59

    fun isHourValid(hour: Int): Boolean = hour in minValidHour..maxValidHour

    val isSelectedHourValid: Boolean
        get() = isHourValid(selectedHours)

    fun isMinuteValid(minute: Int): Boolean =
        isSelectedHourValid && minute in minValidMinute..maxValidMinute

    val isSelectedMinuteValid: Boolean
        get() = isMinuteValid(selectedMinutes)

    fun isSecondValid(second: Int): Boolean =
        isSelectedMinuteValid && second in minValidSecond..maxValidSecond

    val isSelectedSecondValid: Boolean
        get() = isSecondValid(selectedSeconds)

    val isSelectedDurationValid: Boolean
        get() = if (secondState != null) isSelectedSecondValid else isSelectedMinuteValid

    val isScrollInProgress: Boolean
        get() =
            (hourState?.isScrollInProgress == true) ||
                minuteState.isScrollInProgress ||
                (secondState?.isScrollInProgress == true)

    suspend fun adjustHourOptionIfInvalid() {
        if (!isSelectedHourValid) {
            hourState?.let {
                it.animateScrollToOption(closestValidOption(it, minValidHour, maxValidHour))
            }
        }
    }

    suspend fun adjustMinuteOptionIfInvalid() {
        if (isSelectedHourValid && !isSelectedMinuteValid) {
            minuteState.animateScrollToOption(
                closestValidOption(minuteState, minValidMinute, maxValidMinute)
            )
        }
    }

    suspend fun adjustSecondOptionIfInvalid() {
        if (isSelectedMinuteValid && !isSelectedSecondValid) {
            secondState?.let {
                it.animateScrollToOption(closestValidOption(it, minValidSecond, maxValidSecond))
            }
        }
    }

    /**
     * Returns the option to auto-scroll to with [PickerState.animateScrollToOption] when the
     * selected option of [state] is outside `[minValid, maxValid]`.
     *
     * On a repeating column, this is the nearest valid option the shortest way around the wheel, so
     * the scroll passes only through invalid options. On a tie, or on a non-repeating column, it is
     * the selected option clamped to `[minValid, maxValid]`.
     */
    private fun closestValidOption(state: PickerState, minValid: Int, maxValid: Int): Int {
        val selected = state.selectedOptionIndex
        val clamped = selected.coerceIn(minValid, maxValid)
        if (!state.shouldRepeatOptions || clamped == selected) return clamped
        val stepsForwardToMin = Math.floorMod(minValid - selected, state.numberOfOptions)
        val stepsBackToMax = Math.floorMod(selected - maxValid, state.numberOfOptions)
        return when {
            stepsForwardToMin < stepsBackToMax -> minValid
            stepsBackToMax < stepsForwardToMin -> maxValid
            else -> clamped
        }
    }

    companion object {
        val Saver: Saver<DurationPickerState, Any> =
            listSaver(
                save = {
                    listOf(
                        it.selectedDuration.seconds,
                        it.minValidDuration.seconds,
                        it.maxValidDuration.seconds,
                        it.durationPickerType.value,
                        it.hasScrolled,
                        it.initialDuration.seconds,
                    )
                },
                restore = { saved ->
                    DurationPickerState(
                        initialDuration = Duration.ofSeconds(saved[5] as Long),
                        minValidDuration = Duration.ofSeconds(saved[1] as Long),
                        maxValidDuration = Duration.ofSeconds(saved[2] as Long),
                        durationPickerType = DurationPickerType(saved[3] as Int),
                        hasScrolled = saved[4] as Boolean,
                        initiallySelectedDuration = Duration.ofSeconds(saved[0] as Long),
                    )
                },
            )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Duration.roundDownToVisibleUnit(type: DurationPickerType): Duration {
    val secondsDuration = Duration.ofSeconds(seconds)
    return if (type == DurationPickerType.HoursMinutes) {
        Duration.ofMinutes(secondsDuration.toMinutes())
    } else {
        secondsDuration
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Duration.roundUpToVisibleUnit(type: DurationPickerType): Duration {
    val roundedDown = roundDownToVisibleUnit(type)
    return if (roundedDown == this) {
        roundedDown
    } else if (type == DurationPickerType.HoursMinutes) {
        roundedDown.plusMinutes(1)
    } else {
        roundedDown.plusSeconds(1)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private class EffectiveDurations(
    val initial: Duration,
    val min: Duration,
    val max: Duration,
)

@RequiresApi(Build.VERSION_CODES.O)
private fun verifyAndRoundDurations(
    initialDuration: Duration,
    minValidDuration: Duration?,
    maxValidDuration: Duration?,
    durationPickerType: DurationPickerType,
): EffectiveDurations {
    val typeLimit =
        when (durationPickerType) {
            DurationPickerType.MinutesSeconds -> Duration.ofMinutes(59).plusSeconds(59)
            DurationPickerType.HoursMinutes -> Duration.ofHours(99).plusMinutes(59)
            DurationPickerType.HoursMinutesSeconds ->
                Duration.ofHours(99).plusMinutes(59).plusSeconds(59)
            else ->
                throw IllegalArgumentException("Unknown DurationPickerType: $durationPickerType")
        }
    require(!initialDuration.isNegative) {
        "initialDuration must not be negative: $initialDuration"
    }
    require(minValidDuration == null || !minValidDuration.isNegative) {
        "minValidDuration must not be negative: $minValidDuration"
    }
    require(maxValidDuration == null || !maxValidDuration.isNegative) {
        "maxValidDuration must not be negative: $maxValidDuration"
    }

    val resolvedMinValidDuration = minValidDuration ?: defaultMinValidDuration(durationPickerType)
    // Grow a null maxValidDuration to fit the rounded-up minValidDuration, so a sub-unit
    // minValidDuration above the type default still leaves a selectable value.
    val resolvedMaxValidDuration =
        maxValidDuration
            ?: maxOf(
                    defaultMaxValidDuration(durationPickerType),
                    resolvedMinValidDuration
                        // Cap before rounding up so that a very large minValidDuration can't
                        // overflow.
                        .coerceAtMost(typeLimit)
                        .roundUpToVisibleUnit(durationPickerType),
                )
                .coerceAtMost(typeLimit)
    require(resolvedMinValidDuration <= resolvedMaxValidDuration) {
        if (maxValidDuration == null) {
            "minValidDuration ($resolvedMinValidDuration) must be <= $typeLimit for " +
                "$durationPickerType"
        } else {
            "minValidDuration ($resolvedMinValidDuration) must be <= maxValidDuration " +
                "($resolvedMaxValidDuration)"
        }
    }

    val effectiveMaxValidDuration =
        resolvedMaxValidDuration.roundDownToVisibleUnit(durationPickerType)
    require(effectiveMaxValidDuration <= typeLimit) {
        "maxValidDuration ($resolvedMaxValidDuration) must be <= $typeLimit for $durationPickerType"
    }

    val effectiveInitialDuration = initialDuration.roundDownToVisibleUnit(durationPickerType)
    val effectiveMinValidDuration =
        resolvedMinValidDuration.roundUpToVisibleUnit(durationPickerType)
    // initialDuration may exceed maxValidDuration (the picker then opens in the invalid state),
    // but not the type limit.
    require(effectiveInitialDuration <= typeLimit) {
        "initialDuration ($initialDuration) must be <= $typeLimit for $durationPickerType"
    }
    require(effectiveMinValidDuration <= effectiveMaxValidDuration) {
        "No selectable duration between minValidDuration ($resolvedMinValidDuration) and " +
            "maxValidDuration ($resolvedMaxValidDuration) for $durationPickerType"
    }

    return EffectiveDurations(
        initial = effectiveInitialDuration,
        min = effectiveMinValidDuration,
        max = effectiveMaxValidDuration,
    )
}

/** Default minimum valid duration: the smallest visible non-zero unit for [durationPickerType]. */
@RequiresApi(Build.VERSION_CODES.O)
private fun defaultMinValidDuration(durationPickerType: DurationPickerType): Duration =
    if (durationPickerType == DurationPickerType.HoursMinutes) Duration.ofMinutes(1)
    else Duration.ofSeconds(1)

/** Default maximum valid duration (before growing to fit the rounded-up min duration). */
@RequiresApi(Build.VERSION_CODES.O)
private fun defaultMaxValidDuration(durationPickerType: DurationPickerType): Duration =
    when (durationPickerType) {
        DurationPickerType.HoursMinutes -> Duration.ofHours(23).plusMinutes(59)
        DurationPickerType.MinutesSeconds -> Duration.ofMinutes(59).plusSeconds(59)
        else -> Duration.ofHours(23).plusMinutes(59).plusSeconds(59)
    }
