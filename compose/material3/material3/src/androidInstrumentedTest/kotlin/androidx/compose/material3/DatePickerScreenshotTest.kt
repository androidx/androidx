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

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.dx.mockito.inline.extended.MockedMethod
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.quality.Strictness

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class DatePickerScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.Center)
    private val wrapperTestTag = "datePickerWrapper"

    @Test
    fun datePicker_initialMonth() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 1, dayOfMonth = 1)
                DatePicker(
                    state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("datePicker_initialMonth_${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun datePicker_todayMarker() {
        val year = 2021
        val month = 1
        val day = 10
        runWithMockedLocalDate(mockedToday = LocalDate.of(year, month, day)) {
            rule.setMaterialContent(scheme.colorScheme) {
                Box(wrap.testTag(wrapperTestTag)) {
                    val monthInUtcMillis =
                        dayInUtcMilliseconds(year = year, month = month, dayOfMonth = day)
                    DatePicker(
                        state =
                            rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis),
                        showModeToggle = false
                    )
                }
            }
            assertAgainstGolden("datePicker_todayMarker_${scheme.name}")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun datePicker_disabledTodayMarker() {
        val year = 2021
        val month = 1
        val day = 10
        runWithMockedLocalDate(mockedToday = LocalDate.of(year, month, day)) {
            rule.setMaterialContent(scheme.colorScheme) {
                Box(wrap.testTag(wrapperTestTag)) {
                    val monthInUtcMillis =
                        dayInUtcMilliseconds(year = year, month = month, dayOfMonth = day)
                    DatePicker(
                        state =
                            rememberDatePickerState(
                                initialDisplayedMonthMillis = monthInUtcMillis,
                                selectableDates =
                                    object : SelectableDates {
                                        override fun isSelectableDate(
                                            utcTimeMillis: Long
                                        ): Boolean = false
                                    }
                            ),
                        showModeToggle = false
                    )
                }
            }
            assertAgainstGolden("datePicker_disabledTodayMarker_${scheme.name}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runWithMockedLocalDate(mockedToday: LocalDate, test: () -> Unit) {
        val session =
            mockitoSession()
                .spyStatic(LocalDate::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        try {
            // Mock `LocalDate.now()` to return a specific date. This will mark the today marker
            // on the month displayed in this test.
            doReturn(mockedToday).`when`(MockedMethod { LocalDate.now() })
            // Run the test
            test()
        } finally {
            session.finishMocking()
        }
    }

    @Test
    fun datePicker_withModeToggle() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 1, dayOfMonth = 1)
                DatePicker(
                    state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
                )
            }
        }
        assertAgainstGolden("datePicker_withModeToggle_${scheme.name}")
    }

    @Test
    fun datePicker_initialMonthAndSelection() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
                val selectedDayMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialDisplayedMonthMillis = monthInUtcMillis,
                            initialSelectedDateMillis = selectedDayMillis
                        ),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("datePicker_initialMonthAndSelection_${scheme.name}")
    }

    @Test
    fun datePicker_invalidDateSelection() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2000, month = 6, dayOfMonth = 1)
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialDisplayedMonthMillis = monthInUtcMillis,
                            selectableDates =
                                object : SelectableDates {
                                    // All dates are invalid for the sake of this test.
                                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                        false
                                }
                        ),
                    showModeToggle = false
                )
            }
        }
        assertAgainstGolden("datePicker_invalidDateSelection_${scheme.name}")
    }

    @Test
    fun datePicker_yearPicker() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                val monthInUtcMillis = dayInUtcMilliseconds(year = 2000, month = 5, dayOfMonth = 1)
                DatePicker(
                    state = rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis),
                    showModeToggle = false
                )
            }
        }
        rule.onNodeWithText("May 2000").performClick()

        assertAgainstGolden("datePicker_yearPicker_${scheme.name}")
    }

    @Test
    fun datePicker_inDialog() {
        rule.setMaterialContent(scheme.colorScheme) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
            val selectedDayMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
            DatePickerDialog(
                onDismissRequest = {},
                confirmButton = { TextButton(onClick = {}) { Text("OK") } },
                dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
            ) {
                DatePicker(
                    state =
                        rememberDatePickerState(
                            initialDisplayedMonthMillis = monthInUtcMillis,
                            initialSelectedDateMillis = selectedDayMillis
                        ),
                    showModeToggle = false
                )
            }
        }
        rule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(
                rule = screenshotRule,
                goldenIdentifier = "datePicker_inDialog_${scheme.name}"
            )
    }

    @Test
    fun datePicker_noMinimumInteractiveSize() {
        rule.setMaterialContent(scheme.colorScheme) {
            val monthInUtcMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 1)
            val selectedDayMillis = dayInUtcMilliseconds(year = 2021, month = 3, dayOfMonth = 6)
            CompositionLocalProvider(
                // Removes the min 48dp requirement clickable components.
                value = LocalMinimumInteractiveComponentSize provides Dp.Unspecified
            ) {
                DatePickerDialog(
                    onDismissRequest = {},
                    confirmButton = { TextButton(onClick = {}) { Text("OK") } },
                    dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
                ) {
                    DatePicker(
                        state =
                            rememberDatePickerState(
                                initialDisplayedMonthMillis = monthInUtcMillis,
                                initialSelectedDateMillis = selectedDayMillis
                            ),
                        showModeToggle = false
                    )
                }
            }
        }
        rule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(
                rule = screenshotRule,
                goldenIdentifier = "datePicker_noMinimumInteractiveSize_${scheme.name}"
            )
    }

    @Test
    fun datePicker_customLocale() {
        rule.setMaterialContent(scheme.colorScheme) {
            val preferredLocales = LocaleList.forLanguageTags("HE")
            val config = Configuration()
            config.setLocales(preferredLocales)
            val newContext = LocalContext.current.createConfigurationContext(config)
            CompositionLocalProvider(
                LocalContext provides newContext,
                LocalConfiguration provides config,
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Box(wrap.testTag(wrapperTestTag)) {
                    val monthInUtcMillis =
                        dayInUtcMilliseconds(year = 2021, month = 1, dayOfMonth = 1)
                    val state =
                        rememberDatePickerState(initialDisplayedMonthMillis = monthInUtcMillis)
                    DatePicker(state = state, showModeToggle = false)
                }
            }
        }
        // Expecting the content of the DatePicker to be in Hebrew.
        assertAgainstGolden("datePicker_customLocale_${scheme.name}")
    }

    // Returns the given date's day as milliseconds from epoch. The returned value is for the day's
    // start on midnight.
    private fun dayInUtcMilliseconds(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth)
            .atTime(LocalTime.MIDNIGHT)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

    private fun assertAgainstGolden(goldenName: String) {
        // TODO: replace with find(isToggeable()) after b/157687898 is fixed
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
