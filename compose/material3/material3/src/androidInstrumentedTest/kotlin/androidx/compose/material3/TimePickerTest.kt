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

package androidx.compose.material3

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.SelectableGroup
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.math.PI
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TimePickerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun timePicker_vertical_layout() {
        rule.setMaterialContent(lightColorScheme()) {
            val newConfiguration = Configuration(LocalConfiguration.current)
            newConfiguration.screenHeightDp = 800
            newConfiguration.screenWidthDp = 500
            CompositionLocalProvider(LocalConfiguration provides newConfiguration) {
                assertThat(defaultTimePickerLayoutType).isEqualTo(TimePickerLayoutType.Vertical)
            }
        }
    }

    @Test
    fun timePicker_horizontal_layout() {
        rule.setMaterialContent(lightColorScheme()) {
            val newConfiguration = Configuration(LocalConfiguration.current)
            newConfiguration.screenHeightDp = 500
            newConfiguration.screenWidthDp = 800
            CompositionLocalProvider(LocalConfiguration provides newConfiguration) {
                assertThat(defaultTimePickerLayoutType).isEqualTo(TimePickerLayoutType.Horizontal)
            }
        }
    }

    @Test
    fun timePicker_initialState() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule
            .onNodeWithTimeValue(number = 2, selection = TimePickerSelectionMode.Hour)
            .assertIsSelected()

        rule
            .onNodeWithTimeValue(number = 23, selection = TimePickerSelectionMode.Minute)
            .assertExists()

        rule.onNodeWithText("AM").assertExists()

        rule.onNodeWithText("PM").assertExists().assertIsSelected()
    }

    @Test
    fun timePicker_switchToMinutes() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule
            .onNodeWithTimeValue(number = 23, selection = TimePickerSelectionMode.Minute)
            .performClick()

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TimePickerSelectionMode.Minute) }
    }

    @Test
    fun timePicker_selectHour() {
        rule.mainClock.autoAdvance = false
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule
            .onNodeWithTimeValue(number = 6, selection = TimePickerSelectionMode.Hour)
            .performClick()

        rule.mainClock.advanceTimeBy(1000)
        // shows 06 in display
        rule.onNodeWithText("06").assertExists()

        // switches to minutes
        rule.onNodeWithText("23").assertIsSelected()

        // state updated
        assertThat(state.hour).isEqualTo(18)
    }

    @Test
    fun timePicker_selectHour_a11y() {
        rule.mainClock.autoAdvance = false
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule
            .onNodeWithTimeValue(number = 9, selection = TimePickerSelectionMode.Hour)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        rule.mainClock.advanceTimeBy(1000)

        // switches to minutes
        rule.onNodeWithText("23").assertIsSelected()

        // state updated
        assertThat(state.hour).isEqualTo(21)
    }

    @Test
    fun timePicker_switchToAM() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        assertThat(state.hour).isEqualTo(14)

        rule.onNodeWithText("AM").performClick()

        assertThat(state.hour).isEqualTo(2)
    }

    @Test
    fun timePicker_dragging() {
        val state = TimePickerState(initialHour = 0, initialMinute = 23, is24Hour = false)
        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule
            .onAllNodes(keyIsDefined(SelectableGroup), useUnmergedTree = true)
            .onLast()
            .performTouchInput {
                down(topCenter)
                // 3 O'Clock
                moveTo(centerRight)
                up()
            }

        rule.runOnIdle { assertThat(state.hour).isEqualTo(3) }
    }

    @Test
    fun timePickerState_format_12h_fromConfig() {
        lateinit var state: TimePickerState

        rule.setMaterialContent(lightColorScheme()) {
            val context = LocalContext.current
            val config = LocalConfiguration.current
            config.setLocale(Locale.US) // 12 hour clock
            val newContext = context.createConfigurationContext(config)

            CompositionLocalProvider(
                LocalContext provides newContext,
                LocalConfiguration provides config,
            ) {
                state = rememberTimePickerState()
            }
        }

        assertThat(state.is24hour).isFalse()
    }

    @Test
    fun timePickerState_format_24h_fromConfig() {
        lateinit var state: TimePickerState

        rule.setMaterialContent(lightColorScheme()) {
            val context = LocalContext.current
            val config = LocalConfiguration.current
            config.setLocale(Locale.FRANCE) // 24 hour clock
            val newContext = context.createConfigurationContext(config)

            CompositionLocalProvider(
                LocalContext provides newContext,
                LocalConfiguration provides config,
            ) {
                state = rememberTimePickerState()
            }
        }

        assertThat(state.is24hour).isTrue()
    }

    @Test
    fun timePicker_toggle_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var contentDescription: String
        rule.setMaterialContent(lightColorScheme()) {
            contentDescription = getString(Strings.TimePickerPeriodToggle)
            TimePicker(state)
        }

        rule
            .onNodeWithContentDescription(contentDescription)
            .onChildren()
            .filter(expectValue(SemanticsProperties.Role, Role.Button))
            .assertAll(isSelectable())
    }

    @Test
    fun timePicker_display_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var minuteDescription: String
        lateinit var hourDescription: String
        rule.setMaterialContent(lightColorScheme()) {
            minuteDescription = getString(Strings.TimePickerMinuteSelection)
            hourDescription = getString(Strings.TimePickerHourSelection)
            TimePicker(state)
        }

        rule
            .onNodeWithContentDescription(minuteDescription)
            .assertIsSelectable()
            .assertIsNotSelected()
            .assert(expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHasClickAction()

        rule
            .onNodeWithContentDescription(hourDescription)
            .assertIsSelectable()
            .assertIsSelected()
            .assert(expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHasClickAction()
    }

    @Test
    fun timePicker_clockFace_hour_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var hourDescription: String

        rule.setMaterialContent(lightColorScheme()) {
            hourDescription = getString(Strings.TimePickerHourSuffix, 2)
            TimePicker(state)
        }

        rule
            .onAllNodesWithContentDescription(hourDescription)
            .onLast()
            .onSiblings()
            .filter(isFocusable())
            .assertCountEquals(11)
            .assertAll(
                hasContentDescription(value = "o'clock", substring = true, ignoreCase = true)
            )
    }

    @Test
    fun timePicker_clockFace_selected_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule.onAllNodesWithText("14").filter(isFocusable()).assertAll(isSelected())
    }

    @Test
    fun timePicker_clockFace_minutes_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = false)
        lateinit var minuteDescription: String

        rule.setMaterialContent(lightColorScheme()) {
            minuteDescription = getString(Strings.TimePickerMinuteSuffix, 55)
            TimePicker(state)
        }

        // Switch to minutes
        rule.onNodeWithText("23").performClick()

        rule.waitForIdle()

        rule
            .onNodeWithContentDescription(minuteDescription)
            .assertExists()
            .onSiblings()
            .assertCountEquals(11)
            .assertAll(
                hasContentDescription(value = "minutes", substring = true, ignoreCase = true)
            )
    }

    @Test
    fun timeInput_semantics() {
        val state = TimePickerState(initialHour = 14, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule
            .onNodeWithText("14")
            .assert(isFocusable())
            .assertContentDescriptionContains("for hour")
            .assert(hasImeAction(ImeAction.Next))
            .assert(isFocused())

        rule
            .onAllNodesWithText("23")
            .filterToOne(isSelectable())
            .assert(isNotSelected())
            .performClick()

        rule.onNodeWithText("23").assertContentDescriptionContains("for minutes")
    }

    @Test
    fun timeInput_userOverride_updates() {
        val state = TimePickerState(initialHour = 14, initialMinute = 0, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("14").assert(isFocusable()).assertContentDescriptionContains("for hour")

        rule.runOnIdle {
            state.hour = 20
            state.minute = 12
        }

        rule.waitForIdle()

        rule.onNodeWithText("20").assertContentDescriptionContains("for hour")

        rule.onAllNodesWithText("12").assertCountEquals(2)
    }

    @Test
    fun timePicker_userOverride_updates() {
        val state = TimePickerState(initialHour = 14, initialMinute = 0, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimePicker(state) }

        rule.onNodeWithText("14").assertIsSelected()

        rule.runOnIdle { state.hour = 20 }

        rule.waitForIdle()

        rule.onNodeWithText("20").assertIsSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_valid() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.Zero)
            pressKey(Key.Four)
        }

        rule.waitForIdle()

        // Switched to minutes text field
        rule.onNodeWithText("23").performKeyInput {
            pressKey(Key.Five)
            pressKey(Key.Two)
        }

        assertThat(state.minute).isEqualTo(52)
        assertThat(state.hour).isEqualTo(4)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_outOfRange() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.Four)
            pressKey(Key.Four)
        }

        // only the first 4 is accepted
        assertThat(state.hour).isEqualTo(4)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_Nan() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.A)
            pressKey(Key.B)
            pressKey(Key.C)
            pressKey(Key.NumPadDot)
            pressKey(Key.Comma)
            pressKey(Key.NumPadComma)
        }

        // Value didn't change
        assertThat(state.hour).isEqualTo(10)
    }

    @Test
    fun timeInput_keyboardInput_switchAmPm() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("PM").performClick()

        // Value didn't change
        assertThat(state.hour).isEqualTo(22)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_keyboardInput_maintainsPm() {
        val state = TimePickerState(initialHour = 23, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        assertThat(state.isPm).isTrue()

        rule.onNodeWithText("11").performKeyInput { pressKey(Key.Four) }

        assertThat(state.isPm).isTrue()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_input12_maintainsAm() {
        val state = TimePickerState(initialHour = 10, initialMinute = 0, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.One)
            pressKey(Key.Two)
        }

        assertThat(state.isPm).isFalse()
        assertThat(state.hour).isEqualTo(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_input12_resultsIn23() {
        val state = TimePickerState(initialHour = 14, initialMinute = 0, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("02").performKeyInput {
            pressKey(Key.One)
            pressKey(Key.Two)
        }

        assertThat(state.isPm).isTrue()
        assertThat(state.hour).isEqualTo(12)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timeInput_deleting_maintainsPm() {
        val state = TimePickerState(initialHour = 23, initialMinute = 23, is24Hour = false)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        assertThat(state.isPm).isTrue()

        rule.onNodeWithText("11").performKeyInput {
            pressKey(Key.Delete)
            pressKey(Key.Delete)
        }

        assertThat(state.isPm).isTrue()
    }

    @Test
    fun timeInput_24Hour_noAmPm_Toggle() {
        val state = TimePickerState(initialHour = 22, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("PM").assertDoesNotExist()

        rule.onNodeWithText("AM").assertDoesNotExist()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun timeInput_24Hour_writePmHour() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.Two)
            pressKey(Key.Two)
        }

        assertThat(state.hour).isEqualTo(22)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun timeInput_24HourStartingPm_writePmHour() {
        val state = TimePickerState(initialHour = 20, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("20").performKeyInput {
            pressKey(Key.Two)
            pressKey(Key.Two)
        }

        assertThat(state.hour).isEqualTo(22)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun timeInput_24Hour_writeNoon() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.One)
            pressKey(Key.Two)
        }

        assertThat(state.hour).isEqualTo(12)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun timeInput_writeMinute_updatesCurrentAngle() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)
            )

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.One)
            pressKey(Key.Two)
        }

        rule.onNodeWithText("00").performKeyInput {
            pressKey(Key.Four)
            pressKey(Key.Five)
        }

        assertThat(state.currentAngle).isWithin(0.0001f).of(PI.toFloat())
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun timeInput_24Hour_writeMidnight() {
        val state = TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)

        rule.setMaterialContent(lightColorScheme()) { TimeInput(state) }

        rule.onNodeWithText("10").performKeyInput {
            pressKey(Key.Zero)
            pressKey(Key.Zero)
        }

        assertThat(state.hour).isEqualTo(0)
    }

    @Test
    fun state_restoresTimePickerState() {
        val restorationTester = StateRestorationTester(rule)
        var state: TimePickerState?
        restorationTester.setContent {
            state = rememberTimePickerState(initialHour = 14, initialMinute = 54, is24Hour = true)
        }

        state = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state?.hour).isEqualTo(14)
            assertThat(state?.minute).isEqualTo(54)
            assertThat(state?.is24hour).isTrue()
        }
    }

    @Test
    fun state_setHour_updatesIsPm() {
        val state = TimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
        state.hour = 20

        assertThat(state.isPm).isTrue()
    }

    @Test
    fun analogState_setHour_updatesIsPm() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)
            )
        state.hour = 20

        assertThat(state.isPm).isTrue()
    }

    @Test
    fun state_12h_hourInitializationMatches() {
        repeat(24) {
            val state = TimePickerState(initialHour = it, initialMinute = 0, is24Hour = false)

            assertThat(state.hour).isEqualTo(it)
        }
    }

    @Test
    fun state_24h_hourInitializationMatches() {
        repeat(24) {
            val state = TimePickerState(initialHour = it, initialMinute = 0, is24Hour = true)

            assertThat(state.hour).isEqualTo(it)
        }
    }

    @Test
    fun clockFace_24Hour_everyValue() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)
            )

        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(
                modifier = Modifier,
                state = state,
                colors = TimePickerDefaults.colors(),
                autoSwitchToMinute = true,
            )
        }

        repeat(24) { number ->
            rule
                .onNodeWithTimeValue(number, TimePickerSelectionMode.Hour, is24Hour = true)
                .performClick()

            rule.runOnIdle {
                state.selection = TimePickerSelectionMode.Hour
                assertThat(state.hour).isEqualTo(number)
            }

            rule
                .onNodeWithTimeValue(number, TimePickerSelectionMode.Hour, is24Hour = true)
                .assertIsSelected()
        }
    }

    @Test
    fun clockFace_12Hour_everyValue() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 0, initialMinute = 0, is24Hour = false)
            )

        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(
                modifier = Modifier,
                state = state,
                colors = TimePickerDefaults.colors(),
                autoSwitchToMinute = true,
            )
        }

        repeat(12) { number ->
            val hour =
                when {
                    number == 0 -> 12
                    else -> number
                }

            rule.onNodeWithTimeValue(hour, TimePickerSelectionMode.Hour).performClick()
            rule.runOnIdle {
                state.selection = TimePickerSelectionMode.Hour
                assertThat(state.hour).isEqualTo(number)
            }

            rule
                .onNodeWithTimeValue(hour, TimePickerSelectionMode.Hour, is24Hour = false)
                .assertIsSelected()
        }
    }

    @Test
    fun clockFace_12Hour_traversalIndex() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 0, initialMinute = 0, is24Hour = false)
            )

        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(
                modifier = Modifier,
                state = state,
                colors = TimePickerDefaults.colors(),
                autoSwitchToMinute = true,
            )
        }

        repeat(12) { number ->
            val hour =
                when {
                    number == 0 -> 12
                    else -> number
                }

            rule
                .onNodeWithTimeValue(hour, TimePickerSelectionMode.Hour)
                .assert(
                    SemanticsMatcher("Index of nodes in timepicker") {
                        it.config.getOrNull(SemanticsProperties.TraversalIndex) == number + 1f
                    }
                )
                .performClick()
            rule.runOnIdle {
                state.selection = TimePickerSelectionMode.Hour
                assertThat(state.hour).isEqualTo(number)
            }
        }
    }

    @Test
    fun clockFace_12Hour_initAtNoon() {
        val state = TimePickerState(initialHour = 12, initialMinute = 0, is24Hour = false)

        assertThat(state.isPm).isTrue()

        assertThat(state.hour).isEqualTo(12)
    }

    @Test
    fun clockFace_24HourMinutes_everyValue() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = true)
            )
        state.selection = TimePickerSelectionMode.Minute
        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(
                modifier = Modifier,
                state = state,
                colors = TimePickerDefaults.colors(),
                autoSwitchToMinute = true,
            )
        }

        repeat(11) { number ->
            rule
                .onNodeWithTimeValue(number * 5, TimePickerSelectionMode.Minute, is24Hour = true)
                .performClick()
            rule.runOnIdle { assertThat(state.minute).isEqualTo(number * 5) }
        }
    }

    @Test
    fun clockFace_12HourMinutes_everyValue() {
        val state =
            AnalogTimePickerState(
                TimePickerState(initialHour = 10, initialMinute = 23, is24Hour = false)
            )
        state.selection = TimePickerSelectionMode.Minute
        rule.setMaterialContent(lightColorScheme()) {
            ClockFace(
                modifier = Modifier,
                state = state,
                colors = TimePickerDefaults.colors(),
                autoSwitchToMinute = true,
            )
        }

        repeat(11) { number ->
            rule.onNodeWithTimeValue(number * 5, TimePickerSelectionMode.Minute).performClick()
            rule.runOnIdle { assertThat(state.minute).isEqualTo(number * 5) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun SemanticsNodeInteractionsProvider.onNodeWithTimeValue(
    number: Int,
    selection: TimePickerSelectionMode,
    is24Hour: Boolean = false,
): SemanticsNodeInteraction =
    onAllNodesWithContentDescription(
            contentDescriptionForValue(
                InstrumentationRegistry.getInstrumentation().context.resources,
                selection,
                is24Hour,
                number,
            )
        )
        .onFirst()

@OptIn(ExperimentalMaterial3Api::class)
internal fun contentDescriptionForValue(
    resources: Resources,
    selection: TimePickerSelectionMode,
    is24Hour: Boolean,
    number: Int,
): String {

    val id =
        if (selection == TimePickerSelectionMode.Minute) {
            R.string.m3c_time_picker_minute_suffix
        } else if (is24Hour) {
            R.string.m3c_time_picker_hour_24h_suffix
        } else {
            R.string.m3c_time_picker_hour_suffix
        }

    return resources.getString(id, number)
}
