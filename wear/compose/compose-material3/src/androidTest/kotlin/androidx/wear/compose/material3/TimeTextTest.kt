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
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class TimeTextTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme { TimeText(modifier = Modifier.testTag(TEST_TAG)) { time() } }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun updates_clock_when_source_changes_on_non_round_device() {
        val timeState = mutableStateOf("Unchanged")

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText(
                    modifier = Modifier.testTag(TEST_TAG),
                    timeSource =
                        object : TimeSource {
                            @Composable override fun currentTime(): String = timeState.value
                        },
                ) {
                    time()
                }
            }
        }
        timeState.value = "Changed"
        rule.onNodeWithText("Changed").assertIsDisplayed()
    }

    @Test
    fun updates_clock_when_source_changes_on_round_device() {
        val timeState = mutableStateOf("Unchanged")

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                TimeText(
                    modifier = Modifier.testTag(TEST_TAG),
                    timeSource =
                        object : TimeSource {
                            @Composable override fun currentTime(): String = timeState.value
                        },
                ) {
                    time()
                }
            }
        }
        timeState.value = "Changed"
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Changed").assertIsDisplayed()
    }

    @Test
    fun checks_status_displayed_on_non_round_device() {
        val statusText = "Status"

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText {
                    text(statusText)
                    separator()
                    time()
                }
            }
        }

        rule.onNodeWithText(statusText).assertIsDisplayed()
    }

    @Test
    fun checks_status_displayed_on_round_device() {
        val statusText = "Status"

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                TimeText {
                    text(statusText)
                    separator()
                    time()
                }
            }
        }

        rule.onNodeWithContentDescription(statusText).assertIsDisplayed()
    }

    @Test
    fun checks_separator_displayed_on_non_round_device() {
        val statusText = "Status"
        val separatorText = "·"

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText {
                    text(statusText)
                    separator()
                    time()
                }
            }
        }

        rule.onNodeWithText(separatorText).assertIsDisplayed()
    }

    @Test
    fun checks_separator_displayed_on_round_device() {
        val statusText = "Status"
        val separatorText = "·"

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                TimeText {
                    text(statusText)
                    separator()
                    time()
                }
            }
        }

        rule.onNodeWithContentDescription(separatorText).assertIsDisplayed()
    }

    @Test
    fun checks_composable_displayed_on_non_round_device() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText {
                    time()
                    separator()
                    composable {
                        Text(
                            modifier = Modifier.testTag(TEST_TAG),
                            text = "Compose",
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun checks_composable_displayed_on_round_device() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                TimeText {
                    time()
                    separator()
                    composable {
                        Text(
                            modifier = Modifier.testTag(TEST_TAG),
                            text = "Compose",
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun changes_timeTextStyle_on_non_round_device() {
        val timeText = "testTime"

        val testTextStyle =
            TextStyle(color = Color.Green, background = Color.Black, fontSize = 20.sp)
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText(
                    timeSource =
                        object : TimeSource {
                            @Composable override fun currentTime(): String = timeText
                        },
                    timeTextStyle = testTextStyle
                ) {
                    time()
                }
            }
        }
        val actualStyle = rule.textStyleOf(timeText)
        Assert.assertEquals(testTextStyle.color, actualStyle.color)
        Assert.assertEquals(testTextStyle.background, actualStyle.background)
        Assert.assertEquals(testTextStyle.fontSize, actualStyle.fontSize)
    }

    @Test
    fun changes_material_theme_on_non_round_device_except_color() {
        val timeText = "testTime"

        val testTextStyle =
            TextStyle(
                color = Color.Green,
                background = Color.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 25.sp,
                fontFamily = FontFamily.SansSerif
            )
        rule.setContent {
            MaterialTheme(typography = MaterialTheme.typography.copy(arcMedium = testTextStyle)) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                    TimeText(
                        timeSource =
                            object : TimeSource {
                                @Composable override fun currentTime(): String = timeText
                            },
                    ) {
                        time()
                    }
                }
            }
        }
        val actualStyle = rule.textStyleOf(timeText)
        Assert.assertEquals(testTextStyle.background, actualStyle.background)
        Assert.assertEquals(testTextStyle.fontSize, actualStyle.fontSize)
        Assert.assertEquals(testTextStyle.fontStyle, actualStyle.fontStyle)
        Assert.assertEquals(testTextStyle.fontFamily, actualStyle.fontFamily)
        Assert.assertNotEquals(testTextStyle.color, actualStyle.color)
    }

    @Test
    fun color_remains_onBackground_when_material_theme_changed_on_non_round_device() {
        val timeText = "testTime"
        var onBackgroundColor = Color.Unspecified

        val testTextStyle =
            TextStyle(
                color = Color.Green,
                background = Color.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 25.sp,
                fontFamily = FontFamily.SansSerif
            )
        rule.setContent {
            MaterialTheme(typography = MaterialTheme.typography.copy(labelSmall = testTextStyle)) {
                onBackgroundColor = MaterialTheme.colorScheme.onBackground
                DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                    TimeText(
                        timeSource =
                            object : TimeSource {
                                @Composable override fun currentTime(): String = timeText
                            },
                    ) {
                        time()
                    }
                }
            }
        }
        val actualStyle = rule.textStyleOf(timeText)
        Assert.assertEquals(onBackgroundColor, actualStyle.color)
    }

    @Test
    fun has_correct_default_leading_text_color_on_non_round_device() {
        val leadingText = "leadingText"
        var primaryColor = Color.Unspecified

        rule.setContentWithTheme {
            primaryColor = MaterialTheme.colorScheme.primary
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText {
                    text(leadingText)
                    separator()
                    time()
                }
            }
        }
        val actualStyle = rule.textStyleOf(leadingText)
        Assert.assertEquals(primaryColor, actualStyle.color)
    }

    @Test
    fun supports_custom_leading_text_color_on_non_round_device() {
        val leadingText = "leadingText"
        val customColor = Color.Green

        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText(
                    contentColor = customColor,
                ) {
                    text(leadingText)
                    separator()
                    time()
                }
            }
        }
        val actualStyle = rule.textStyleOf(leadingText)
        Assert.assertEquals(customColor, actualStyle.color)
    }

    @Test
    fun supports_custom_text_style_on_non_round_device() {
        val leadingText = "leadingText"

        val timeTextStyle = TextStyle(background = Color.Blue, fontSize = 14.sp)
        val contentTextStyle =
            TextStyle(color = Color.Green, background = Color.Black, fontSize = 20.sp)
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                TimeText(contentColor = Color.Red, timeTextStyle = timeTextStyle) {
                    text(leadingText, contentTextStyle)
                    separator()
                    time()
                }
            }
        }
        val actualStyle = rule.textStyleOf(leadingText)
        Assert.assertEquals(contentTextStyle.color, actualStyle.color)
        Assert.assertEquals(contentTextStyle.background, actualStyle.background)
        Assert.assertEquals(contentTextStyle.fontSize, actualStyle.fontSize)
    }

    @Test
    fun formats_current_time() {
        val currentTimeInMillis = 1631544258000L // 2021-09-13 14:44:18
        val format = "HH:mm:ss"
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeInMillis }
        val convertedTime = DateFormat.format(format, currentCalendar).toString()

        var actualTime: String? = null
        rule.setContentWithTheme { actualTime = currentTime({ currentTimeInMillis }, format).value }
        Assert.assertEquals(convertedTime, actualTime)
    }

    @Test
    fun formats_current_time_12H() {
        val currentTimeInMillis = 1631544258000L // 2021-09-13 14:44:18
        val expectedTime = "2:44"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        var actualTime: String? = null
        rule.setContentWithTheme {
            actualTime =
                currentTime({ currentTimeInMillis }, TimeTextDefaults.TimeFormat12Hours).value
        }
        Assert.assertEquals(expectedTime, actualTime)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun formats_current_time_12H_french_locale() {
        val currentTimeInMillis = 1631544258000L // 2021-09-13 14:44:18
        val expectedTime = "2 h 44"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        var actualTime: String? = null
        Locale.setDefault(Locale.CANADA_FRENCH)

        rule.setContentWithTheme {
            val format = TimeTextDefaults.timeFormat()
            actualTime = currentTime({ currentTimeInMillis }, format).value
        }
        Assert.assertEquals(expectedTime, actualTime)
    }
}
