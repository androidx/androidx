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
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.curvedComposable
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
        rule.setContentWithTheme { TimeText(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun shows_time_by_default() {
        val timeText = "time"

        rule.setContentWithTheme {
            TimeText(
                timeSource =
                    object : TimeSource {
                        @Composable override fun currentTime(): String = timeText
                    }
            ) { time ->
                // Use 'curvedText' instead of 'timeTextCurvedText' so that we get a content
                // description that can be verified.
                curvedText(time)
            }
        }

        rule.onNodeWithContentDescription(timeText).assertIsDisplayed()
    }

    @Test
    fun updates_clock_when_source_changes() {
        val timeState = mutableStateOf("Unchanged")

        rule.setContentWithTheme {
            TimeText(
                modifier = Modifier.testTag(TEST_TAG),
                timeSource =
                    object : TimeSource {
                        @Composable override fun currentTime(): String = timeState.value
                    },
            ) { time ->
                // Use 'curvedText' instead of 'timeTextCurvedText' so that we get a content
                // description that can be verified.
                curvedText(time)
            }
        }
        timeState.value = "Changed"
        rule.onNodeWithContentDescription("Changed").assertIsDisplayed()
    }

    @Test
    fun checks_status_displayed() {
        val statusText = "Status"

        rule.setContentWithTheme() {
            TimeText { time ->
                curvedText(statusText)
                timeTextSeparator()
                curvedText(time)
            }
        }

        rule.onNodeWithContentDescription(statusText).assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun checks_separator_displayed() {
        val separatorColor = Color.Red

        rule.setContentWithTheme {
            val style = TimeTextDefaults.timeTextStyle(color = separatorColor)

            TimeText(modifier = Modifier.testTag(TEST_TAG)) { timeTextSeparator(style) }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(separatorColor)
    }

    @Test
    fun checks_composable_displayed() {
        rule.setContentWithTheme {
            TimeText { time ->
                curvedText(time)
                timeTextSeparator()
                curvedComposable { Text(modifier = Modifier.testTag(TEST_TAG), text = "Compose") }
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun does_not_announce_time() {
        rule.setContentWithTheme {
            TimeText {
                timeTextCurvedText("time")
                timeTextSeparator()
            }
        }

        rule.onNodeWithContentDescription("time").assertDoesNotExist()
        rule.onNodeWithContentDescription(".").assertDoesNotExist()
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

    @Composable
    private fun BasicTimeTextWithStatus(statusText: String) {
        TimeText { time ->
            curvedText(statusText)
            timeTextSeparator()
            curvedText(time)
        }
    }
}
