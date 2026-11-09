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

import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.intl.LocaleList
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.internal.Plurals
import androidx.wear.compose.material3.internal.Strings
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.time.Duration
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@MediumTest
@RunWith(TestParameterInjector::class)
class DurationPickerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun durationPicker_withTestTag_rendersRootContainer() {
        val testDuration = Duration.ofMinutes(5)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = testDuration,
                onDurationPicked = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun durationPicker_customCeilingHoursMinutes_rendersExpectedLeadingHour() {
        val maxDuration = Duration.ofHours(40)
        val initialDuration = Duration.ofHours(35)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 35,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()
    }

    @Test
    fun durationPicker_hoursMinutesSecondsInitialState_focusesHoursAndDisplaysColumns() {
        val initialDuration = Duration.ofHours(14).plusMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 14,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(
                selectedValue = 31,
                selectionMode = SelectionMode.Seconds,
            )
            .assertIsDisplayed()
    }

    @Test
    fun durationPicker_hoursMinutesInitialState_focusesHoursAndExcludesSeconds() {
        val initialDuration = Duration.ofHours(14).plusMinutes(23)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 14,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Seconds,
            )
            .assertDoesNotExist()
        rule.onAllNodes(hasContentDescription("Second", substring = true)).assertCountEquals(0)
    }

    @Test
    fun durationPicker_minutesSecondsInitialState_focusesMinutesAndExcludesHours() {
        val initialDuration = Duration.ofMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 31,
                selectionMode = SelectionMode.Seconds,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .assertDoesNotExist()
        rule.onAllNodes(hasContentDescription("Hour", substring = true)).assertCountEquals(0)
    }

    @Test
    fun durationPicker_hoursMinutesWithSeconds_roundsDownSecondsAndEmitsMinuteAlignedDuration() {
        val initialDuration = Duration.ofMinutes(1).plusSeconds(45)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(1))
            assertThat(confirmedDuration?.seconds?.rem(60)).isEqualTo(0)
        }
    }

    @Test
    fun durationPicker_withSubseconds_roundsDownToWholeSecondsOnConfirm() {
        val initialDuration = Duration.ofSeconds(15).plusMillis(850).plusNanos(123456)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofSeconds(15))
            assertThat(confirmedDuration?.nano).isEqualTo(0)
        }
    }

    @Test
    fun durationPicker_withInitialSelectionMinutes_focusesMinutesColumn() {
        val initialDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(30)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
                initialSelection = DurationPickerSelection.Minutes,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 15,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_initialDurationBelowMin_startsDisabledAndAutoScrollsAfterScroll() {
        val initialDuration = Duration.ZERO
        val minDuration = Duration.ofHours(1).plusMinutes(15).plusSeconds(30)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule.waitForIdle()

        // On initial display, 00:00:00 remains displayed with Confirm disabled.
        rule.confirmButton().assertIsNotEnabled()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()

        // Scrolling enables auto-scroll to valid bounds when settling on an invalid duration.
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(10)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        // 00:00:10 auto-adjusts hours 0 -> 1 (1 step either way to the valid hours 1..23, so it
        // takes the min) and minutes 0 -> 59 (1 step backwards, versus 15 steps forwards to 15).
        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(1).plusMinutes(59).plusSeconds(10))
        }
    }

    @Test
    fun durationPicker_nonZeroInitialDurationBelowMin_startsDisabledAndAutoScrollsAfterScroll() {
        val initialDuration = Duration.ofMinutes(30)
        val minDuration = Duration.ofHours(1).plusMinutes(15).plusSeconds(30)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule.waitForIdle()

        // Non-zero initialDuration below minValidDuration also starts parked with Confirm disabled.
        rule.confirmButton().assertIsNotEnabled()
        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()

        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(10)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        // 00:10:00 auto-adjusts hours 0 -> 1 and minutes 10 -> 15. Seconds 0 is below the min
        // second (30), and the nearest valid second is 59, 1 step backwards.
        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(1).plusMinutes(15).plusSeconds(59))
        }
    }

    @Test
    fun durationPicker_scrolledBelowMinimumBound_autoScrollsToMinimumBound() {
        val initialDuration = Duration.ofMinutes(10).plusSeconds(20)
        val minDuration = Duration.ofMinutes(10).plusSeconds(15)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 20,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(12)
        rule.waitForIdle()

        // Valid seconds are 15..59. 12 is 3 steps forwards from 15 and 13 steps backwards from 59.

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(minDuration)
        }
    }

    @Test
    fun durationPicker_negativeInitialDuration_throwsIllegalArgumentException() {
        val negativeDuration = Duration.ofSeconds(-10)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = negativeDuration,
                    onDurationPicked = {},
                )
            }
        }
    }

    @Test
    fun durationPicker_minGreaterThanMax_throwsIllegalArgumentException() {
        val minDuration = Duration.ofHours(2)
        val maxDuration = Duration.ofHours(1)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(5),
                    onDurationPicked = {},
                    minValidDuration = minDuration,
                    maxValidDuration = maxDuration,
                )
            }
        }
    }

    @Test
    fun durationPicker_subUnitMinGreaterThanMax_throwsIllegalArgumentException() {
        val minDuration = Duration.ofSeconds(50)
        val maxDuration = Duration.ofSeconds(20)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = minDuration,
                    maxValidDuration = maxDuration,
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_hoursMinutesMinWithSeconds_roundsMinUp() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(1),
                onDurationPicked = {},
                minValidDuration = Duration.ofMinutes(1).plusSeconds(10),
                maxValidDuration = Duration.ofMinutes(5),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 2 minutes and 5 minutes.",
                )
            )
    }

    @Test
    fun durationPicker_hoursMinutesMinBelowOneMinute_keepsZeroDisabled() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = Duration.ofSeconds(30),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
    }

    @Test
    fun durationPicker_minutesSecondsMinWithMillis_roundsMinUp() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofSeconds(1),
                onDurationPicked = {},
                minValidDuration = Duration.ofSeconds(1).plusMillis(200),
                maxValidDuration = Duration.ofSeconds(5),
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 2 seconds and 5 seconds.",
                )
            )
    }

    @Test
    fun durationPicker_noWholeMinuteBetweenMinAndMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = Duration.ofMinutes(1).plusSeconds(10),
                    maxValidDuration = Duration.ofMinutes(1).plusSeconds(50),
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_equalMinAndMaxWithSubUnits_throwsIllegalArgumentException() {
        val duration = Duration.ofMinutes(1).plusSeconds(30)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = duration,
                    maxValidDuration = duration,
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_negativeMinValidDuration_throwsIllegalArgumentException() {
        val negativeMin = Duration.ofSeconds(-1)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(5),
                    onDurationPicked = {},
                    minValidDuration = negativeMin,
                )
            }
        }
    }

    @Test
    fun durationPicker_negativeMaxValidDuration_throwsIllegalArgumentException() {
        val negativeMax = Duration.ofSeconds(-30)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = Duration.ZERO,
                    maxValidDuration = negativeMax,
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_initialExceedingExplicitMax_opensDisabledWithoutAutoScroll() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(25),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(23),
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule.waitForIdle()

        // The picker opens at 25h, above the max, and stays there until the user scrolls.
        rule
            .onNodeWithDurationValue(selectedValue = 25, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 1 second and 23 hours.",
                )
            )
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(confirmedDuration).isNull() }
    }

    @Test
    fun durationPicker_minutesSecondsExceedingCapacity_throwsIllegalArgumentException() {
        val invalidCapacityMax = Duration.ofMinutes(60)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(10),
                    onDurationPicked = {},
                    maxValidDuration = invalidCapacityMax,
                    durationPickerType = DurationPickerType.MinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPicker_hoursMinutesExceedingCapacity_throwsIllegalArgumentException() {
        val invalidCapacityMax = Duration.ofHours(100)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(10),
                    onDurationPicked = {},
                    maxValidDuration = invalidCapacityMax,
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_hoursMinutesSecondsExceedingCapacity_throwsIllegalArgumentException() {
        val invalidCapacityMax = Duration.ofHours(100)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(10),
                    onDurationPicked = {},
                    maxValidDuration = invalidCapacityMax,
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPickerDefaults_initialSelection_unknownType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            DurationPickerDefaults.initialSelection(DurationPickerType(-1))
        }
    }

    @Test
    fun durationPicker_unknownTypeWithExplicitBounds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(5),
                    onDurationPicked = {},
                    minValidDuration = Duration.ofSeconds(1),
                    maxValidDuration = Duration.ofMinutes(10),
                    durationPickerType = DurationPickerType(-1),
                )
            }
        }
    }

    @Test
    fun durationPicker_disabledConfirmButton_setsStateDescriptionWithBounds() {
        val minDuration = Duration.ofSeconds(1)
        val maxDuration = Duration.ofMinutes(59).plusSeconds(59)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = minDuration,
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 1 second and 59 minutes, 59 seconds.",
                )
            )
    }

    @Test
    fun durationPicker_minutesSecondsCustomCeiling_focusesMinutesAndDisplaysSeconds() {
        val initialDuration = Duration.ofMinutes(30).plusSeconds(15)
        val maxDuration = Duration.ofMinutes(45)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 15,
                selectionMode = SelectionMode.Seconds,
            )
            .assertIsDisplayed()
    }

    @Test
    fun durationPicker_hoursMinutesScrolled_confirmsExpectedDuration() {
        val initialDuration = Duration.ofHours(5)
        val expectedDuration = Duration.ofHours(40)
        lateinit var confirmedDuration: Duration

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(50),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 5,
                selectionMode = SelectionMode.Hours,
            )
            .performScrollToIndex(40)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(expectedDuration)
        }
    }

    @Test
    fun durationPicker_hoursMinutesSecondsClickMinutes_shiftsFocusToMinutes() {
        val initialDuration = Duration.ofHours(14).plusMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .performClick()

        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_hoursMinutesSecondsClickSeconds_shiftsFocusToSeconds() {
        val initialDuration = Duration.ofHours(14).plusMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 31,
                selectionMode = SelectionMode.Seconds,
            )
            .performClick()

        rule
            .onNodeWithDurationValue(
                selectedValue = 31,
                selectionMode = SelectionMode.Seconds,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_hoursMinutesSecondsClickHours_shiftsFocusToHours() {
        val initialDuration = Duration.ofHours(14).plusMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
                initialSelection = DurationPickerSelection.Minutes,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 14,
                selectionMode = SelectionMode.Hours,
            )
            .performClick()

        rule
            .onNodeWithDurationValue(
                selectedValue = 14,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_minutesSecondsClickMinutes_shiftsFocusToMinutes() {
        val initialDuration = Duration.ofMinutes(23).plusSeconds(31)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Seconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .performClick()

        rule
            .onNodeWithDurationValue(
                selectedValue = 23,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_hoursMinutesSecondsScrolled_confirmsExpectedDuration() {
        val initialDuration = Duration.ofHours(2).plusMinutes(10).plusSeconds(20)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 2,
                selectionMode = SelectionMode.Hours,
            )
            .performScrollToIndex(5)
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(
                selectedValue = 10,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(15)
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(
                selectedValue = 20,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(25)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(5).plusMinutes(15).plusSeconds(25))
        }
    }

    @Test
    fun durationPicker_minutesSecondsScrolled_confirmsExpectedDuration() {
        val initialDuration = Duration.ofMinutes(10).plusSeconds(20)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 10,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(12)
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(
                selectedValue = 20,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(30)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(12).plusSeconds(30))
        }
    }

    @Test
    fun durationPicker_scrolledAboveMaximumBound_autoScrollsToMaximumBound() {
        val initialDuration = Duration.ofMinutes(10).plusSeconds(20)
        val maxDuration = Duration.ofMinutes(10).plusSeconds(30)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 20,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(40)
        rule.waitForIdle()

        // Valid seconds are 00..30. 40 is 10 steps backwards from 30 and 20 steps forwards from 00.

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(maxDuration)
        }
    }

    @Test
    fun durationPicker_scrolledToMaxHourWithTiedMinute_autoScrollsMinuteToMaxValidMinute() {
        val initialDuration = Duration.ofHours(36).plusMinutes(45)
        val maxDuration = Duration.ofHours(45).plusMinutes(30)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }
        rule
            .onNodeWithDurationValue(
                selectedValue = 36,
                selectionMode = SelectionMode.Hours,
            )
            .performScrollToIndex(45)
        rule.waitForIdle()

        // At 45h, valid minutes are 00..30. Minute 45 is 15 steps from both 30 and 00, so the tie
        // goes to the max (30), the minute clamped to the valid range.
        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(maxDuration)
        }
    }

    @Test
    fun durationPicker_userScrollInterruptingAutoScroll_stillAutoScrollsOnSettle() {
        val minDuration = Duration.ofMinutes(10)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(15),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                maxValidDuration = Duration.ofMinutes(20),
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Seconds,
            )
        }

        // Pause auto-advance so we can catch the auto-scroll animation mid-flight.
        rule.mainClock.autoAdvance = false

        // Scroll minutes to 5 (invalid, below minDuration of 10m). Since initialSelection was
        // Seconds, scrollToIndex's onSelected() callback focuses Minutes.
        rule
            .onNodeWithDurationValue(
                selectedValue = 15,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(5)

        // Advance frames until LaunchedEffect starts the auto-scroll to the nearest valid minute
        // (10, 5 steps forwards, versus 6 steps backwards to 20), which sets
        // minuteState.isScrollInProgress = true (clearing ContentDescription on the focused
        // picker).
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) {
            rule
                .onAllNodes(
                    isFocused()
                        .and(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Interrupt the in-progress auto-scroll on the focused Minutes column with a user drag
        // gesture (MutatePriority.UserInput) while the animation is mid-flight. Each event is
        // injected in its own performTouchInput call with a frame advanced in between, so the drag
        // coroutine preempts MutatorMutex before the gesture ends. Release at an invalid minute
        // (< 10m).
        val focusedColumn = rule.onNode(isFocused())
        focusedColumn.performTouchInput {
            down(center)
            moveBy(Offset(0f, -30f))
        }
        rule.mainClock.advanceTimeByFrame()
        focusedColumn.performTouchInput { moveBy(Offset(0f, -30f)) }
        rule.mainClock.advanceTimeByFrame()
        focusedColumn.performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        // Valid minutes are 10..20, so wherever the drag settles below 10, the nearest valid
        // minute is either the min or the max.
        rule.runOnIdle {
            assertThat(confirmedDuration).isAnyOf(minDuration, Duration.ofMinutes(20))
        }
    }

    @Test
    fun durationPicker_focusSwitchInterruptingAutoScroll_retriesAndAutoScrollsOnSettle() {
        val minDuration = Duration.ofMinutes(10)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(15),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                maxValidDuration = Duration.ofMinutes(20),
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Seconds,
            )
        }

        rule.mainClock.autoAdvance = false

        // Scroll minutes to 5 (invalid, below minDuration of 10m), which focuses Minutes.
        rule
            .onNodeWithDurationValue(
                selectedValue = 15,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(5)

        // Advance frames until LaunchedEffect starts animateScrollToOption(10) on Minutes.
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) {
            rule
                .onAllNodes(
                    isFocused()
                        .and(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Switch focus to Seconds mid-animation. Minutes becomes read-only, which triggers
        // Picker's synchronous scrollToOption on Minutes and interrupts the auto-scroll to 10.
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Seconds,
            )
            .performClick()

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(minDuration)
        }
    }

    @Test
    fun durationPicker_leadingColumnWithFewerThanThreeOptions_doesNotRepeatOptions() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(30),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(1),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // With maxHours = 1 (options 0 and 1, < 3), shouldRepeatOptions is false on the leading
        // hours column, so swiping down from 0 stays at 0 (does not wrap circularly to 1).
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .performTouchInput { swipeDown() }
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(30))
        }
    }

    @Test
    fun durationPicker_leadingColumnWithThreeOrMoreOptions_repeatsOptions() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(2),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // With maxHours = 2 (options 0, 1, 2 -> >= 3), shouldRepeatOptions is true on the leading
        // hours column, so scrolling to repeating index 4 wraps modulo 3 to 1 Hour.
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .performScrollToIndex(4)
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsDisplayed()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(1))
        }
    }

    @Test
    fun durationPicker_nonRepeatingHourBelowMin_autoScrollsToClampedMinHour() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1).plusMinutes(30),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(1),
                maxValidDuration = Duration.ofHours(1).plusMinutes(59),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // With options 0 and 1, the hours column does not repeat, so hour 0 (below the min hour)
        // auto-scrolls to the clamped hour 1.
        rule
            .onNodeWithDurationValue(selectedValue = 1, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(0)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(1).plusMinutes(30))
        }
    }

    @Test
    fun durationPicker_minutesAndSecondsColumns_wrapAcross59And00Boundary() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1).plusMinutes(59).plusSeconds(1),
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        // Scroll minutes from 59 across the 59 <-> 00 boundary via repeating index 60 (60 % 60 = 0)
        rule
            .onNodeWithDurationValue(
                selectedValue = 59,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(60)
        rule.waitForIdle()

        // Scroll seconds from 1 across the 00 <-> 59 boundary via repeating index 119 (119 % 60 =
        // 59)
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(119)
        rule.waitForIdle()

        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(1).plusSeconds(59))
        }
    }

    @Test
    fun durationPicker_sameMinAndMaxHour_clampsMinutesAndSecondsWithinClosedBounds() {
        val minDuration = Duration.ofHours(2).plusMinutes(15).plusSeconds(20)
        val maxDuration = Duration.ofHours(2).plusMinutes(45).plusSeconds(40)
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(2).plusMinutes(30).plusSeconds(30),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minDuration,
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        // Scroll minutes below minValidMinute (15) and seconds below minValidSecond (20) while
        // minHours == maxHours == 2; both must auto-adjust to minDuration (02:15:20).
        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(10)
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Seconds,
            )
            .performScrollToIndex(10)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(minDuration)
        }
    }

    @Test
    fun durationPicker_localizedSeparator_resolvesPeriodInDanishAndOmitsFromSemantics() {
        val danishLocale = Locale.forLanguageTag("da-DK")
        val finnishLocale = Locale.forLanguageTag("fi-FI")
        val usLocale = Locale.US

        assertThat(getTimeSeparator(danishLocale)).isEqualTo(".")
        assertThat(getTimeSeparator(finnishLocale)).isEqualTo(".")
        assertThat(getTimeSeparator(usLocale)).isEqualTo(":")

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList("da-DK"))) {
                DurationPicker(
                    initialDuration = Duration.ofHours(1).plusMinutes(30),
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }

        // Separator glyph is excluded from TalkBack semantics via clearAndSetSemantics.
        rule.onAllNodesWithText(".", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText(":", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun durationPicker_negativeSubSecondDuration_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMillis(-500),
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_nearMaxDuration_throwsIllegalArgumentException() {
        val nearMaxDuration = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L)

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(5),
                    onDurationPicked = {},
                    minValidDuration = nearMaxDuration,
                    maxValidDuration = nearMaxDuration,
                )
            }
        }
    }

    @Test
    fun durationPickerColors_pickerBackgroundColor_defaultsToUnspecifiedAndCustomIsPreserved() {
        lateinit var defaultColors: DurationPickerColors
        lateinit var copiedUnspecifiedColors: DurationPickerColors
        lateinit var customBackgroundColors: DurationPickerColors
        lateinit var copiedUnspecifiedCustomColors: DurationPickerColors

        rule.setContentWithTheme {
            defaultColors = DurationPickerDefaults.colors()
            copiedUnspecifiedColors = defaultColors.copy(pickerBackgroundColor = Color.Unspecified)
            customBackgroundColors =
                DurationPickerDefaults.colors(pickerBackgroundColor = Color.Red)
            copiedUnspecifiedCustomColors =
                customBackgroundColors.copy(pickerBackgroundColor = Color.Unspecified)
        }

        rule.runOnIdle {
            // The default fades to transparent, so it works on any background.
            assertThat(defaultColors.pickerBackgroundColor).isEqualTo(Color.Unspecified)
            assertThat(copiedUnspecifiedColors.pickerBackgroundColor).isEqualTo(Color.Unspecified)
            assertThat(customBackgroundColors.pickerBackgroundColor).isEqualTo(Color.Red)
            // copy(Unspecified) keeps the current color, like the other colors.
            assertThat(copiedUnspecifiedCustomColors.pickerBackgroundColor).isEqualTo(Color.Red)
        }
    }

    @Test
    fun durationPicker_invalidInitialSelection_fallsBackToFirstAvailableColumn() {
        val initialDuration = Duration.ofMinutes(10).plusSeconds(20)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Hours,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 10,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_confirmButtonInitialSelection_focusesConfirmButton() {
        val initialDuration = Duration.ofHours(1)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutes,
                initialSelection = DurationPickerSelection.ConfirmButton,
            )
        }

        rule.confirmButton().assertIsFocused()
    }

    @Test
    fun durationPicker_whenTouchExplorationEnabled_clearsFocusAndDisplaysInstructionHeading() {
        val initialDuration = Duration.ofHours(1).plusMinutes(30)
        val talkBackEnabled = mutableStateOf(false)
        val fakeTouchExplorationStateProvider = TouchExplorationStateProvider {
            remember { talkBackEnabled }
        }

        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalTouchExplorationStateProvider provides fakeTouchExplorationStateProvider
            ) {
                DurationPicker(
                    initialDuration = initialDuration,
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
        talkBackEnabled.value = true
        rule.waitForIdle()

        rule.heading().assertIsDisplayed()
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Hours,
                talkBackEnabled = true,
            )
            .assertIsNotFocused()
    }

    @Test
    fun durationPicker_disabledStateDescription_formatsSecondsOnlyDuration() {
        val minDuration = Duration.ofSeconds(5)
        val maxDuration = Duration.ofSeconds(30)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = minDuration,
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 5 seconds and 30 seconds.",
                )
            )
    }

    @Test
    fun durationPicker_disabledStateDescription_formatsHoursAndMinutesDuration() {
        val minDuration = Duration.ofHours(1).plusMinutes(15)
        val maxDuration = Duration.ofHours(3).plusMinutes(45)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = minDuration,
                maxValidDuration = maxDuration,
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 1 hour, 15 minutes and 3 hours, 45 minutes.",
                )
            )
    }

    @Test
    fun durationPicker_hoursMinutesZeroMin_formatsZeroBoundInMinutes() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1),
                onDurationPicked = {},
                minValidDuration = Duration.ZERO,
                maxValidDuration = Duration.ofHours(1).plusMinutes(30),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // Scroll to 1h 45m, above the max. Stop the clock so that the invalid duration is
        // observed before it auto-adjusts.
        rule.mainClock.autoAdvance = false
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(45)
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) { !rule.isConfirmButtonEnabled() }

        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 0 minutes and 1 hour, 30 minutes.",
                )
            )
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun durationPicker_zeroBoundsAllowed_enablesConfirmButton() {
        val zeroDuration = Duration.ZERO

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = zeroDuration,
                onDurationPicked = {},
                minValidDuration = zeroDuration,
                maxValidDuration = zeroDuration,
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule.confirmButton().assertIsEnabled()
    }

    @Test
    fun durationPicker_singularAndPluralQuantities_announcesDistinctContentDescriptions() {
        val initialDuration = Duration.ofHours(1).plusMinutes(1).plusSeconds(1)

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Hours,
            )
            .assertExists()
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Minutes,
            )
            .assertExists()
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Seconds,
            )
            .assertExists()

        // Only the selected item is announced, so select 2 in each column to check plurals.
        SelectionMode.entries.forEach { selectionMode ->
            rule
                .onNodeWithDurationValue(selectedValue = 1, selectionMode = selectionMode)
                .performScrollToIndex(2)
            rule.waitForIdle()
        }

        rule
            .onNodeWithDurationValue(
                selectedValue = 2,
                selectionMode = SelectionMode.Hours,
            )
            .assertExists()
        rule
            .onNodeWithDurationValue(
                selectedValue = 2,
                selectionMode = SelectionMode.Minutes,
            )
            .assertExists()
        rule
            .onNodeWithDurationValue(
                selectedValue = 2,
                selectionMode = SelectionMode.Seconds,
            )
            .assertExists()
    }

    @Test
    fun durationPicker_stateRestorationWithoutScroll_keepsInvalidDurationAndDisabledConfirm() {
        val restorationTester = StateRestorationTester(rule)

        restorationTester.setContent {
            MaterialTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = Duration.ofSeconds(1),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
        rule.confirmButton().assertIsNotEnabled()

        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // Auto-adjust stays inactive after restoration because the user has not scrolled yet.
        rule.confirmButton().assertIsNotEnabled()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Seconds,
            )
            .assertIsDisplayed()
    }

    @Test
    fun durationPicker_stateRestorationAfterScroll_restoresSelectedDurationAndSelectedColumn() {
        val restorationTester = StateRestorationTester(rule)
        var confirmedDuration: Duration? = null

        restorationTester.setContent {
            MaterialTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(1).plusMinutes(10).plusSeconds(20),
                    onDurationPicked = { confirmedDuration = it },
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
        // Scrolling the minutes column also selects it.
        rule
            .onNodeWithDurationValue(
                selectedValue = 10,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(30)
        rule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(
                selectedValue = 30,
                selectionMode = SelectionMode.Minutes,
            )
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(
                selectedValue = 1,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsNotFocused()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(1).plusMinutes(30).plusSeconds(20))
        }
    }

    @Test
    fun durationPicker_invalidDuration_tappingSelectedLastColumnWrapsToFirstColumn() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = Duration.ofSeconds(1),
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        // Clicking the selected column (e.g. via accessibility) moves selection to the next
        // column.
        rule
            .onNodeWithDurationValue(0, SelectionMode.Hours)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithDurationValue(0, SelectionMode.Minutes).assertIsFocused()
        rule
            .onNodeWithDurationValue(0, SelectionMode.Minutes)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithDurationValue(0, SelectionMode.Seconds).assertIsFocused()

        // While the duration is invalid, the disabled confirm button is skipped.
        rule
            .onNodeWithDurationValue(0, SelectionMode.Seconds)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        rule.onNodeWithDurationValue(0, SelectionMode.Hours).assertIsFocused()
        rule.onNodeWithDurationValue(0, SelectionMode.Seconds).assertIsNotFocused()
        rule.confirmButton().assert(isFocused().not())
        rule.confirmButton().assertIsNotEnabled()
    }

    @Test
    fun durationPicker_validDuration_tappingSelectedLastColumnFocusesConfirmButton() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(10).plusSeconds(20),
                onDurationPicked = {},
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Seconds,
            )
        }

        rule
            .onNodeWithDurationValue(20, SelectionMode.Seconds)
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        rule.confirmButton().assertIsFocused()
    }

    @Test
    fun durationPicker_confirmButtonInitialSelectionWithInvalidDuration_selectsFirstColumn() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutes,
                initialSelection = DurationPickerSelection.ConfirmButton,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule.confirmButton().assert(isFocused().not())
        rule
            .onNodeWithDurationValue(
                selectedValue = 0,
                selectionMode = SelectionMode.Hours,
            )
            .assertIsFocused()
    }

    @Test
    fun durationPicker_initialDurationAboveDefaultMax_opensDisabledAndKeepsDefaultMax() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(30),
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // The null max stays at the 23h 59m default, so 30h is shown but invalid.
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 1 minute and 23 hours, 59 minutes.",
                )
            )
    }

    @Test
    fun durationPicker_initialAtTypeLimitAboveMax_opensAtInitialWithConfirmDisabled() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(99).plusMinutes(59),
                onDurationPicked = {},
                maxValidDuration = Duration.ofHours(1),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 99, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(selectedValue = 59, selectionMode = SelectionMode.Minutes)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()
    }

    @Test
    fun durationPicker_initialMinuteAboveMinutesSecondsMax_widensMinutesColumn() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(50).plusSeconds(10),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofMinutes(10),
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 50, selectionMode = SelectionMode.Minutes)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()

        // The minutes column shows 0..50 (51 options), so repeating index 52 wraps to minute 1.
        rule
            .onNodeWithDurationValue(selectedValue = 50, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(52)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(1).plusSeconds(10))
        }
    }

    @Test
    fun durationPicker_initialAboveMaxScrolledLowerColumn_autoScrollsHourThroughWrapToZero() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(30).plusMinutes(45),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(24),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()

        // Scrolling the minutes column enables auto-scroll. The hours column shows 0..30, so hour
        // 30 is 1 step forwards (through the wrap) from the valid hour 0 and 6 steps backwards
        // from 24.
        rule
            .onNodeWithDurationValue(selectedValue = 45, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(10)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(10)) }
    }

    @Test
    fun durationPicker_initialAboveMaxScrolledHourNearMax_autoScrollsToMaxHourAndMinute() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(30).plusMinutes(45),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(24),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // Hour 26 is 2 steps backwards from the max hour 24 and 5 steps forwards from 0 (0..30).
        // At hour 24 only minute 0 is valid; minute 45 is 15 steps forwards to 0 and 45 steps
        // backwards.
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(26)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(24)) }
    }

    @Test
    fun durationPicker_hoursMinutesSubMinuteMinAboveDefaultMax_roundsMinUpAndDoesNotThrow() {
        // The effective min rounds up to 30h01m, so the default max must grow to fit it.
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = {},
                minValidDuration = Duration.ofHours(30).plusSeconds(30),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule.confirmButton().assertIsNotEnabled()
        rule
            .confirmButton()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Duration must be between 30 hours, 1 minute and 30 hours, 1 minute.",
                )
            )
    }

    @Test
    fun durationPicker_hoursMinutesInitialAboveTypeLimitWithDefaultMax_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(100),
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_minutesSecondsInitialAboveTypeLimitWithDefaultMax_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofMinutes(60),
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.MinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPicker_hoursMinutesSecondsInitialAboveTypeLimitWithDefaultMax_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(100),
                    onDurationPicked = {},
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPicker_initialAboveTypeLimitWithExplicitMax_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(100),
                    onDurationPicked = {},
                    maxValidDuration = Duration.ofHours(10),
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    @Test
    fun durationPicker_hoursMinutesInitialWithSecondsAboveTypeLimit_roundsDownAndDoesNotThrow() {
        // 99h 59m 30s rounds down to 99h 59m, the HoursMinutes limit.
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(99).plusMinutes(59).plusSeconds(30),
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule
            .onNodeWithDurationValue(selectedValue = 99, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule
            .onNodeWithDurationValue(selectedValue = 59, selectionMode = SelectionMode.Minutes)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()
    }

    @Test
    fun durationPicker_minuteAboveMaxValidMinute_autoScrollsToNearestMinMinuteViaWrap() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(2).plusMinutes(5),
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(2).plusMinutes(10),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // At 2h, valid minutes are 00..10. The nearest valid minute to 59 is 00, 1 step forwards,
        // versus 49 steps backwards to the max (10).
        rule
            .onNodeWithDurationValue(
                selectedValue = 5,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(59)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(2))
        }
    }

    @Test
    fun durationPicker_minuteOutsideNarrowRange_autoScrollsToNearestMinMinuteViaWrap() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(2).plusMinutes(10),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(2).plusMinutes(5),
                maxValidDuration = Duration.ofHours(2).plusMinutes(20),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // Valid minutes are 05..20. The nearest valid minute to 59 is 05, 6 steps forwards, versus
        // 39 steps backwards to the max (20).
        rule
            .onNodeWithDurationValue(
                selectedValue = 10,
                selectionMode = SelectionMode.Minutes,
            )
            .performScrollToIndex(59)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(2).plusMinutes(5))
        }
    }

    @Test
    fun durationPicker_hourBelowMinValidHour_autoScrollsToNearestMaxHourViaWrap() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(2),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // Hours 0..23 repeat and valid hours are 02..23. The nearest valid hour to 0 is 23, 1 step
        // backwards, versus 2 steps forwards to the min (02).
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(30)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(23).plusMinutes(30))
        }
    }

    @Test
    fun durationPicker_minuteJustAboveZeroBelowMin_autoScrollsToNearestMaxMinuteViaWrap() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofMinutes(10),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // At hour 0, valid minutes are 10..59. The nearest valid minute to 01 is 59, 2 steps
        // backwards, versus 9 steps forwards to the min (10).
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(1)
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofMinutes(59))
        }
    }

    @Test
    fun durationPicker_hourBelowMinValidHour_autoScrollsForwardToNearestMinHour() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(20),
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        // Hours 0..23 repeat and valid hours are 20..23. The nearest valid hour to 15 is 20, 5
        // steps forwards through the invalid hours 16..19, versus 16 steps backwards to 23.
        rule.mainClock.autoAdvance = false
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(15)
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) { rule.isConfirmButtonEnabled() }

        // Stop the auto-scroll as soon as the duration becomes valid, so that the hour it settles
        // on is the first valid hour on its path. Tapping the selected hours column moves the
        // selection to the minutes column, which makes Picker snap the hours column to its
        // centered option.
        rule
            .onAllNodes(isValuePicker(), useUnmergedTree = true)[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofHours(20))
        }
    }

    @Test
    fun durationPicker_secondFarBelowMinValidSecond_autoScrollsBackwardToNearestMaxSecond() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofSeconds(45),
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        // At minute 0, valid seconds are 45..59. The nearest valid second to 10 is 59, 11 steps
        // backwards through the invalid seconds 09..00, versus 35 steps forwards to the min (45).
        rule.mainClock.autoAdvance = false
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Seconds)
            .performScrollToIndex(10)
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) { rule.isConfirmButtonEnabled() }

        // Stop the auto-scroll as soon as the duration becomes valid, so that the second it
        // settles on is the first valid second on its path. Tapping the selected seconds column
        // moves the selection to the confirm button, which makes Picker snap the seconds column
        // to its centered option.
        rule
            .onAllNodes(isValuePicker(), useUnmergedTree = true)[1]
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isEqualTo(Duration.ofSeconds(59))
        }
    }

    @Test
    fun durationPicker_confirmClickedWhileScrollInProgress_doesNotInvokeCallback() {
        var confirmedDuration: Duration? = null

        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofMinutes(10).plusSeconds(30),
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule.mainClock.autoAdvance = false
        // Fling the minutes column (all minutes are valid for this duration), advancing a frame
        // between moves so the column is scrolling before the finger lifts.
        val minutesColumn = rule.onNode(isFocused())
        minutesColumn.performTouchInput { down(center) }
        repeat(5) {
            minutesColumn.performTouchInput { moveBy(Offset(0f, -40f)) }
            rule.mainClock.advanceTimeByFrame()
        }
        minutesColumn.performTouchInput { up() }
        // Advance frames until the minutes column is scrolling, which clears the
        // ContentDescription of the focused picker.
        rule.mainClock.advanceTimeUntil(timeoutMillis = 5_000) {
            rule
                .onAllNodes(
                    isFocused()
                        .and(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        rule.confirmButton().performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(confirmedDuration).isNull()
        }
    }

    @Test
    fun durationPicker_initialDurationChangedWithNullMax_keepsSelection() {
        var initialDuration by mutableStateOf(Duration.ofHours(30))
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(5)
        rule.waitForIdle()

        initialDuration = Duration.ofHours(10)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 5, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(5)) }
    }

    @Test
    fun durationPicker_initialDurationChangedAboveTypeLimit_keepsOriginalDuration() {
        var initialDuration by mutableStateOf(Duration.ofHours(5))
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = Duration.ofHours(10),
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        // Changing initialDuration after the first composition is ignored, so a value above the
        // type limit doesn't throw.
        initialDuration = Duration.ofHours(100)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 5, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(5)) }
    }

    @Test
    fun durationPicker_boundsChanged_resetsToInitialDurationAndInitialSelection() {
        var initialDuration by mutableStateOf(Duration.ofHours(30))
        var maxValidDuration: Duration? by mutableStateOf(null)
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = initialDuration,
                onDurationPicked = { confirmedDuration = it },
                maxValidDuration = maxValidDuration,
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        // Scrolling the minutes column also selects it.
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(15)
        rule.waitForIdle()
        initialDuration = Duration.ofHours(10)
        rule.waitForIdle()

        maxValidDuration = Duration.ofHours(40)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 10, selectionMode = SelectionMode.Hours)
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .assertIsNotFocused()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(10)) }
    }

    @Test
    fun durationPicker_minValidDurationChanged_resetsToInitialDurationAndInitialSelection() {
        var minValidDuration by mutableStateOf(Duration.ofSeconds(1))
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = minValidDuration,
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
        // Scrolling the minutes column also selects it.
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(15)
        rule.waitForIdle()

        minValidDuration = Duration.ofMinutes(30)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 1, selectionMode = SelectionMode.Hours)
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .assertIsNotFocused()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(1)) }
    }

    @Test
    fun durationPicker_durationPickerTypeChanged_resetsToInitialDurationAndInitialSelection() {
        var durationPickerType by mutableStateOf(DurationPickerType.HoursMinutesSeconds)
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1),
                onDurationPicked = { confirmedDuration = it },
                durationPickerType = durationPickerType,
            )
        }
        // Scrolling the minutes column also selects it.
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(15)
        rule.waitForIdle()

        durationPickerType = DurationPickerType.HoursMinutes
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 1, selectionMode = SelectionMode.Hours)
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Minutes)
            .assertIsNotFocused()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(1)) }
    }

    @Test
    fun durationPicker_initialSelectionChanged_selectsNewComponent() {
        var initialSelection by mutableStateOf(DurationPickerSelection.Hours)
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(1).plusMinutes(2).plusSeconds(3),
                onDurationPicked = {},
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
                initialSelection = initialSelection,
            )
        }
        rule
            .onNodeWithDurationValue(selectedValue = 1, selectionMode = SelectionMode.Hours)
            .assertIsFocused()

        initialSelection = DurationPickerSelection.Seconds
        rule.waitForIdle()

        rule
            .onNodeWithDurationValue(selectedValue = 3, selectionMode = SelectionMode.Seconds)
            .assertIsFocused()
        rule
            .onNodeWithDurationValue(selectedValue = 1, selectionMode = SelectionMode.Hours)
            .assertIsNotFocused()
    }

    @Test
    fun durationPicker_confirmButtonInitialSelectionBelowRoundedMin_selectsFirstColumn() {
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofSeconds(45),
                onDurationPicked = {},
                minValidDuration = Duration.ofSeconds(30),
                durationPickerType = DurationPickerType.HoursMinutes,
                initialSelection = DurationPickerSelection.ConfirmButton,
            )
        }

        // 45s rounds down to 00:00 and 30s rounds up to 00:01, so the initial duration is below
        // the min even though the unrounded 45s is above 30s.
        rule.confirmButton().assertIsNotEnabled()
        rule.confirmButton().assert(isFocused().not())
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Hours)
            .assertIsFocused()
    }

    @Test
    fun durationPicker_middleHour_allowsMinutesAndSecondsOutsideBoundaryHourRanges() {
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(2).plusMinutes(30).plusSeconds(50),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(1).plusMinutes(40).plusSeconds(30),
                maxValidDuration = Duration.ofHours(3).plusMinutes(30).plusSeconds(40),
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }

        // At hour 2, every minute and second is valid, including minutes below the min hour's
        // min minute (40) and above the max hour's max minute (30), and likewise for seconds.
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(10)
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(selectedValue = 50, selectionMode = SelectionMode.Seconds)
            .performScrollToIndex(10)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()
        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(2).plusMinutes(10).plusSeconds(10))
        }

        rule
            .onNodeWithDurationValue(selectedValue = 10, selectionMode = SelectionMode.Minutes)
            .performScrollToIndex(50)
        rule.waitForIdle()
        rule
            .onNodeWithDurationValue(selectedValue = 10, selectionMode = SelectionMode.Seconds)
            .performScrollToIndex(50)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()
        rule.runOnIdle {
            assertThat(confirmedDuration)
                .isEqualTo(Duration.ofHours(2).plusMinutes(50).plusSeconds(50))
        }
    }

    @Test
    fun durationPicker_dragReturningToInvalidInitialDuration_autoScrollsOnSettle() {
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofSeconds(1),
                durationPickerType = DurationPickerType.MinutesSeconds,
                initialSelection = DurationPickerSelection.Seconds,
            )
        }

        // A slow drag that returns to its start settles on the unchanged initial 00:00. Pausing
        // before lifting the finger avoids a fling.
        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Seconds)
            .performTouchInput {
                down(center)
                repeat(3) { moveBy(Offset(0f, -10f)) }
                repeat(3) { moveBy(Offset(0f, 10f)) }
                advanceEventTime(500)
                up()
            }
        rule.waitForIdle()

        // The drag counts as a scroll, so settling on the invalid 00:00 auto-scrolls to the min
        // (1 step either way to the valid seconds 01..59, so the tie goes to the min).
        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofSeconds(1)) }
    }

    @Test
    fun durationPicker_scrolledBackToInvalidInitialDuration_autoScrollsToNearestValidSecond() {
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ZERO,
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofSeconds(10),
                durationPickerType = DurationPickerType.MinutesSeconds,
            )
        }

        rule
            .onNodeWithDurationValue(selectedValue = 0, selectionMode = SelectionMode.Seconds)
            .performScrollToIndex(20)
        rule.waitForIdle()
        // Auto-scroll stays enabled after the first scroll, even when returning to the initial
        // duration.
        rule
            .onNodeWithDurationValue(selectedValue = 20, selectionMode = SelectionMode.Seconds)
            .performScrollToIndex(0)
        rule.waitForIdle()

        // The nearest valid second to 00 is 59, 1 step backwards, versus 10 steps forwards to 10.
        rule.confirmButton().assertIsEnabled()
        rule.confirmButton().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofSeconds(59)) }
    }

    @Test
    fun durationPicker_stateRestorationAfterScrollIntoRange_keepsWidenedLeadingColumn() {
        val restorationTester = StateRestorationTester(rule)
        var confirmedDuration: Duration? = null
        restorationTester.setContent {
            MaterialTheme {
                DurationPicker(
                    initialDuration = Duration.ofHours(30),
                    onDurationPicked = { confirmedDuration = it },
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
        rule
            .onNodeWithDurationValue(selectedValue = 30, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(5)
        rule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        // The restored hours column still shows 0..30 (31 options), so index 26 is hour 26, 3
        // steps backwards from the default max hour 23 and 5 steps forwards from 0. With only the
        // default 0..23, index 26 would wrap to hour 2.
        rule
            .onNodeWithDurationValue(selectedValue = 5, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(26)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(23)) }
    }

    @Test
    fun durationPicker_minAboveTypeLimitWithNullMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = Duration.ofHours(100),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPicker_nearMaxMinWithNullMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = Duration.ofSeconds(Long.MAX_VALUE),
                    durationPickerType = DurationPickerType.HoursMinutesSeconds,
                )
            }
        }
    }

    @Test
    fun durationPicker_minAboveDefaultMaxWithNullMax_raisesMaxToMin() {
        var confirmedDuration: Duration? = null
        rule.setContentWithTheme {
            DurationPicker(
                initialDuration = Duration.ofHours(23),
                onDurationPicked = { confirmedDuration = it },
                minValidDuration = Duration.ofHours(30),
                maxValidDuration = null,
                durationPickerType = DurationPickerType.HoursMinutes,
            )
        }

        rule
            .onNodeWithDurationValue(selectedValue = 23, selectionMode = SelectionMode.Hours)
            .assertIsDisplayed()
        rule.confirmButton().assertIsNotEnabled()

        // Without the raise, the default max 23:59 is below the min and the picker throws.
        rule
            .onNodeWithDurationValue(selectedValue = 23, selectionMode = SelectionMode.Hours)
            .performScrollToIndex(30)
        rule.waitForIdle()
        rule.confirmButton().performClick()
        rule.waitForIdle()

        rule.runOnIdle { assertThat(confirmedDuration).isEqualTo(Duration.ofHours(30)) }
    }

    @Test
    fun durationPicker_hoursMinutesNullMinWithSubMinuteMax_throws() {
        // A null min keeps its 1-minute default, which is above the 30-second max.
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContentWithTheme {
                DurationPicker(
                    initialDuration = Duration.ZERO,
                    onDurationPicked = {},
                    minValidDuration = null,
                    maxValidDuration = Duration.ofSeconds(30),
                    durationPickerType = DurationPickerType.HoursMinutes,
                )
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.confirmButton(): SemanticsNodeInteraction =
        onAllNodesWithContentDescription(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.PickerConfirmButtonContentDescription.value)
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.isConfirmButtonEnabled(): Boolean =
        !confirmButton().fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)

    private fun isValuePicker(): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.ValuePicker)

    private fun SemanticsNodeInteractionsProvider.heading(): SemanticsNodeInteraction =
        onAllNodesWithText(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .resources
                    .getString(Strings.DurationPickerHeading.value)
            )
            .onFirst()

    private fun SemanticsNodeInteractionsProvider.onNodeWithDurationValue(
        selectedValue: Int,
        selectionMode: SelectionMode,
        talkBackEnabled: Boolean = false,
    ): SemanticsNodeInteraction {
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        if (talkBackEnabled) {
            val contentDescription =
                when (selectionMode) {
                    SelectionMode.Hours -> resources.getString(Strings.DurationPickerHours.value)
                    SelectionMode.Minutes ->
                        resources.getString(Strings.DurationPickerMinutes.value)
                    SelectionMode.Seconds ->
                        resources.getString(Strings.DurationPickerSeconds.value)
                }
            return onAllNodesWithContentDescription(contentDescription).onFirst()
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
        Hours(Plurals.DurationPickerHoursContentDescription),
        Minutes(Plurals.DurationPickerMinutesContentDescription),
        Seconds(Plurals.DurationPickerSecondsContentDescription),
    }
}
