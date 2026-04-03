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

import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.internal.Plurals
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.samples.TimePickerSample
import androidx.wear.compose.material3.samples.TimePickerWith12HourClockSample
import androidx.wear.compose.material3.samples.TimePickerWithMinutesAndSecondsSample
import androidx.wear.compose.material3.samples.TimePickerWithSecondsSample
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@MediumTest
@RunWith(TestParameterInjector::class)
class TimePickerTest {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @Test
    fun timePicker_supports_testtag() {
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                modifier = Modifier.testTag(TEST_TAG),
                initialTime = LocalTime.now(),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun timePicker_samples_build() {
        rule.setContentWithTheme {
            TimePickerSample()
            TimePickerWithSecondsSample()
            TimePickerWith12HourClockSample()
            TimePickerWithMinutesAndSecondsSample()
        }
    }

    @Test
    fun timePicker_hhmmss_initial_state() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutesSeconds24H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .assertIsDisplayed()
        rule.onNodeWithText("AM", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("PM", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun timePicker_hhmm_initial_state() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutes24H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .assertDoesNotExist()
        rule.onNodeWithText("AM", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("PM", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun timePicker_mmss_initial_state() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 45)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.MinutesSeconds,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .assertDoesNotExist()
        rule.onNodeWithText("AM", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("PM", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun timePicker_hhmm12h_initial_state() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                modifier = Modifier.testTag(TEST_TAG),
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutesAmPm12H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.get(ChronoField.CLOCK_HOUR_OF_AMPM),
                selectionMode = SelectionMode.Hour,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .assertDoesNotExist()
        rule.onNodeWithText("AM", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("PM", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun timePicker_switch_to_minutes() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        rule.setContentWithTheme { TimePicker(onTimePicked = {}, initialTime = initialTime) }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .performClick()

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsFocused()
    }

    @Test
    fun timePicker_select_hour() {
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        val expectedHour = 9
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutesSeconds24H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .performScrollToIndex(expectedHour)
        rule.waitForIdle()

        rule
            .onNodeWithTimeValue(selectedValue = expectedHour, selectionMode = SelectionMode.Hour)
            .assertIsDisplayed()
    }

    @Test
    fun timePicker_hhmmss_confirmed() {
        lateinit var confirmedTime: LocalTime
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        val expectedTime = LocalTime.of(/* hour= */ 9, /* minute= */ 11, /* second= */ 59)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = { confirmedTime = it },
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutesSeconds24H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .performScrollToIndex(expectedTime.hour)
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .performScrollToIndex(expectedTime.minute)
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .performScrollToIndex(expectedTime.second)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(confirmedTime).isEqualTo(expectedTime)
    }

    @Test
    fun timePicker_hhmm_confirmed() {
        lateinit var confirmedTime: LocalTime
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        val expectedTime = LocalTime.of(/* hour= */ 9, /* minute= */ 11, /* second= */ 0)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = { confirmedTime = it },
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutes24H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .performScrollToIndex(expectedTime.hour)
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .performScrollToIndex(expectedTime.minute)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(confirmedTime).isEqualTo(expectedTime)
    }

    @Test
    fun timePicker_12h_confirmed() {
        lateinit var confirmedTime: LocalTime
        val initialTime = LocalTime.of(/* hour= */ 14, /* minute= */ 23, /* second= */ 31)
        val expectedTime = LocalTime.of(/* hour= */ 9, /* minute= */ 11, /* second= */ 0)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = { confirmedTime = it },
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutesAmPm12H,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.get(ChronoField.CLOCK_HOUR_OF_AMPM),
                selectionMode = SelectionMode.Hour,
            )
            .performScrollToIndex(expectedTime.get(ChronoField.CLOCK_HOUR_OF_AMPM) - 1)
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .performScrollToIndex(expectedTime.minute)
        rule.onNodeWithContentDescription("PM").performScrollToIndex(0)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(confirmedTime).isEqualTo(expectedTime)
    }

    @Test
    fun timePicker_mmss_confirmed() {
        lateinit var confirmedTime: LocalTime
        val initialTime = LocalTime.of(/* hour= */ 10, /* minute= */ 23, /* second= */ 45)
        val expectedTime = LocalTime.of(/* hour= */ 0, /* minute= */ 11, /* second= */ 20)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = { confirmedTime = it },
                initialTime = initialTime,
                timePickerType = TimePickerType.MinutesSeconds,
            )
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .performScrollToIndex(expectedTime.minute)
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.second,
                selectionMode = SelectionMode.Second,
            )
            .performScrollToIndex(expectedTime.second)
        rule.confirmButton().performClick()
        rule.waitForIdle()

        assertThat(confirmedTime).isEqualTo(expectedTime)
    }

    @Test
    fun parsing_standard12hPattern_isCorrect() {
        val pattern = "h:mm a"

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Period),
            )
            .inOrder()
        assertThat(grouped)
            .containsExactly(
                TimeLayoutElement.TimeGroup(
                    listOf(
                        TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                        TimePatternPart.SeparatorPart(":"),
                        TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                    )
                ),
                TimeLayoutElement.Standalone(TimePatternPart.SeparatorPart(" ")),
                TimeLayoutElement.Standalone(
                    TimePatternPart.ComponentPart(TimePickerSelection.Period)
                ),
            )
            .inOrder()
    }

    @Test
    fun parsing_periodFirstPattern_isCorrect() {
        val pattern = "a h:mm"

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Period),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
            )
            .inOrder()
        assertThat(grouped)
            .containsExactly(
                TimeLayoutElement.Standalone(
                    TimePatternPart.ComponentPart(TimePickerSelection.Period)
                ),
                TimeLayoutElement.Standalone(TimePatternPart.SeparatorPart(" ")),
                TimeLayoutElement.TimeGroup(
                    listOf(
                        TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                        TimePatternPart.SeparatorPart(":"),
                        TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                    )
                ),
            )
            .inOrder()
    }

    @Test
    fun parsing_24hWithSecondsPattern_isCorrect() {
        val pattern = "HH:mm:ss"

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Second),
            )
            .inOrder()
        assertThat(grouped)
            .containsExactly(
                TimeLayoutElement.TimeGroup(
                    listOf(
                        TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                        TimePatternPart.SeparatorPart(":"),
                        TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                        TimePatternPart.SeparatorPart(":"),
                        TimePatternPart.ComponentPart(TimePickerSelection.Second),
                    )
                )
            )
            .inOrder()
    }

    @Test
    fun parsing_patternWithNoSpaceSeparator_isCorrect() {
        val pattern = "aK:mm" // e.g. Japanese

        val parsed = parsePattern(pattern)

        // The heuristic should inject a space separator between 'a' and 'K'.
        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Period),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
            )
            .inOrder()
    }

    @Test
    fun parsing_patternWithDotSeparator_isCorrect() {
        val pattern = "H.mm" // e.g. Finnish

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart("."),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
            )
            .inOrder()
        assertThat(grouped)
            .containsExactly(
                TimeLayoutElement.TimeGroup(
                    listOf(
                        TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                        TimePatternPart.SeparatorPart("."),
                        TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                    )
                )
            )
            .inOrder()
    }

    @Test
    fun parsing_quotedLiterals_isCorrect() {
        val pattern = "HH 'h' mm 'min' ss 's'" // fr-CA

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Second),
            )
            .inOrder()

        assertThat(grouped).containsExactly(TimeLayoutElement.TimeGroup(parsed)).inOrder()
    }

    @Test
    fun parsing_quotedSuffix_isCorrect() {
        val pattern = "H:mm 'hodź'." // hsb

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(":"),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
            )
            .inOrder()

        assertThat(grouped).containsExactly(TimeLayoutElement.TimeGroup(parsed)).inOrder()
    }

    @Test
    fun parsing_unquotedLiteral_isCorrect() {
        val pattern = "ཆུ་ཚོད་ h སྐར་མ་ mm a" // dz

        val parsed = parsePattern(pattern)
        val grouped = groupTimeParts(parsed)

        assertThat(parsed)
            .containsExactly(
                TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                TimePatternPart.SeparatorPart(" "),
                TimePatternPart.ComponentPart(TimePickerSelection.Period),
            )
            .inOrder()

        assertThat(grouped)
            .containsExactly(
                TimeLayoutElement.TimeGroup(
                    listOf(
                        TimePatternPart.ComponentPart(TimePickerSelection.Hour),
                        TimePatternPart.SeparatorPart(" "),
                        TimePatternPart.ComponentPart(TimePickerSelection.Minute),
                    )
                ),
                TimeLayoutElement.Standalone(TimePatternPart.SeparatorPart(" ")),
                TimeLayoutElement.Standalone(
                    TimePatternPart.ComponentPart(TimePickerSelection.Period)
                ),
            )
            .inOrder()
    }

    @Test
    fun unspecified_initial_selection_is_first_focusable_element(
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter isAmPmFirst: Boolean,
    ) {
        val initialTime = LocalTime.of(10, 20, 30)
        val newLocale =
            if (isAmPmFirst) Locale("ko", "Kr") // "a h:mm"
            else Locale("us", "US")
        val timePickerType = TimePickerType.HoursMinutesAmPm12H

        rule.setContentWithTheme {
            val newConfiguration = LocalConfiguration.current.apply { setLocale(newLocale) }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalConfiguration provides newConfiguration,
            ) {
                TimePicker(
                    onTimePicked = {},
                    initialTime = initialTime,
                    timePickerType = timePickerType,
                )
            }
        }

        val hourNode =
            rule.onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )

        val localizedAmContentDescription =
            DateTimeFormatter.ofPattern("a", newLocale).format(LocalTime.of(0, 0))
        val periodNode = rule.onNodeWithContentDescription(localizedAmContentDescription)

        // Time components are NOT reversed in a RTL configuration, preserving the natural h:m:s
        // order,
        // except for the period element (AM/PM) which is allowed to be reversed in RTL.
        when {
            layoutDirection == LayoutDirection.Ltr && !isAmPmFirst ->
                hourNode.assertIsFocused() // HH:MM AM/PM
            layoutDirection == LayoutDirection.Ltr && isAmPmFirst ->
                periodNode.assertIsFocused() // AM/PM HH:MM
            layoutDirection == LayoutDirection.Rtl && !isAmPmFirst ->
                hourNode.assertIsFocused() // AM/PM HH:MM
            layoutDirection == LayoutDirection.Rtl && isAmPmFirst ->
                periodNode.assertIsFocused() // HH:MM AM/PM
        }
    }

    @Test
    fun valid_initial_selection_is_correctly_set(
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter isAmPmFirst: Boolean,
    ) {
        val initialTime = LocalTime.of(10, 20, 30)
        val newLocale =
            if (isAmPmFirst) Locale("ko", "Kr") // "a h:mm"
            else Locale("us", "US")
        val timePickerType = TimePickerType.HoursMinutesAmPm12H

        rule.setContentWithTheme {
            val newConfiguration = LocalConfiguration.current.apply { setLocale(newLocale) }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalConfiguration provides newConfiguration,
            ) {
                TimePicker(
                    onTimePicked = {},
                    initialTime = initialTime,
                    timePickerType = timePickerType,
                    initialSelection = TimePickerSelection.Minute,
                )
            }
        }

        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.minute,
                selectionMode = SelectionMode.Minute,
            )
            .assertIsFocused()
    }

    @Test
    fun invalid_initial_selection_falls_back_to_first_element() {
        val initialTime = LocalTime.of(10, 20, 30)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutes24H,
                initialSelection = TimePickerSelection.Second,
            )
        }

        // Should fall back to the first available element, which is Hour.
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .assertIsFocused()
    }

    @Test
    fun selection_is_none_when_touch_exploration_is_enabled() {
        val initialTime = LocalTime.of(10, 20)
        val talkBackEnabled = mutableStateOf(false)
        val fakeTouchExplorationStateProvider = TouchExplorationStateProvider {
            remember { talkBackEnabled }
        }

        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalTouchExplorationStateProvider provides fakeTouchExplorationStateProvider
            ) {
                TimePicker(
                    onTimePicked = {},
                    initialTime = initialTime,
                    timePickerType = TimePickerType.HoursMinutes24H,
                )
            }
        }

        talkBackEnabled.value = false
        rule.waitForIdle()

        // Initially, with touch exploration disabled, the hour picker should be focused.
        rule.heading().assertDoesNotExist()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
            )
            .assertIsFocused()

        // Enable touch exploration
        talkBackEnabled.value = true
        rule.waitForIdle()

        // Now, the selection should be 'None', and the instruction heading should be displayed.
        rule.heading().assertIsDisplayed()
        rule
            .onNodeWithTimeValue(
                selectedValue = initialTime.hour,
                selectionMode = SelectionMode.Hour,
                talkBackEnabled = talkBackEnabled.value,
            )
            .assertIsNotFocused()
    }

    @Test
    fun confirm_button_initial_selection_is_correctly_set() {
        val initialTime = LocalTime.of(10, 20, 30)
        rule.setContentWithTheme {
            TimePicker(
                onTimePicked = {},
                initialTime = initialTime,
                timePickerType = TimePickerType.HoursMinutes24H,
                initialSelection = TimePickerSelection.ConfirmButton,
            )
        }

        rule.confirmButton().assertIsFocused()
    }

    private fun SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
        label: String
    ): SemanticsNodeInteraction = onAllNodesWithContentDescription(label).onFirst()

    private fun SemanticsNodeInteractionsProvider.heading(): SemanticsNodeInteraction =
        onAllNodesWithText(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.TimePickerHeading.value)
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.confirmButton(): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.PickerConfirmButtonContentDescription.value)
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.onNodeWithTimeValue(
        selectedValue: Int,
        selectionMode: SelectionMode,
        talkBackEnabled: Boolean = false,
    ): SemanticsNodeInteraction {
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        if (talkBackEnabled) {
            val contentDescription =
                when (selectionMode) {
                    SelectionMode.Hour -> resources.getString(Strings.TimePickerHour.value)
                    SelectionMode.Minute -> resources.getString(Strings.TimePickerMinute.value)
                    SelectionMode.Second -> resources.getString(Strings.TimePickerSecond.value)
                }
            return onNodeWithContentDescription(contentDescription)
        }

        return onAllNodesWithContentDescription(
                contentDescriptionForValue(
                    resources,
                    selectedValue,
                    selectionMode.contentDescriptionResource,
                )
            )
            .onFirst()
    }

    private fun contentDescriptionForValue(
        resources: Resources,
        selectedValue: Int,
        contentDescriptionResource: Plurals,
    ): String =
        resources.getQuantityString(contentDescriptionResource.value, selectedValue, selectedValue)

    private enum class SelectionMode(val contentDescriptionResource: Plurals) {
        Hour(Plurals.TimePickerHoursContentDescription),
        Minute(Plurals.TimePickerMinutesContentDescription),
        Second(Plurals.TimePickerSecondsContentDescription),
    }
}
