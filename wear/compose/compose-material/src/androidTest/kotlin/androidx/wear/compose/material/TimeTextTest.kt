/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.curvedComposable
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimeTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            TimeText(
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun updates_clock_when_source_changes_on_square_device() {
        val timeState = mutableStateOf("Unchanged")

        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    modifier = Modifier.testTag(TEST_TAG),
                    timeSource = object : TimeSource {
                        override val currentTime: String
                            @Composable
                            get() = timeState.value
                    },
                )
            }
        }
        timeState.value = "Changed"
        rule.onNodeWithText("Changed").assertIsDisplayed()
    }

    // TODO(220086395): Reimplement this test when we have the infraestructure
    // @Test
    fun updates_clock_when_source_changes_on_round_device() {
        val timeState = mutableStateOf("Unchanged")

        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    modifier = Modifier.testTag(TEST_TAG),
                    timeSource = object : TimeSource {
                        override val currentTime: String
                            @Composable
                            get() = timeState.value
                    },
                )
            }
        }
        timeState.value = "Changed"
        rule.onNodeWithText("Changed").assertIsDisplayed()
    }

    @Test
    fun supports_start_linear_text_only_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    startLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Start content",
                        )
                    },
                    startCurvedContent = {
                        // TODO(220086395): replace back with a curvedText
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_ITEM_TAG),
                                text = "Curved content",
                            )
                        }
                    }
                )
            }
        }

        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertExists()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_start_curved_text_only_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    startLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Start content",
                        )
                    },
                    startCurvedContent = {
                        // TODO(220086395): replace back with a curvedText
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_ITEM_TAG),
                                text = "Curved content",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertExists()
    }

    @Test
    fun supports_end_linear_text_only_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    endLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "End content",
                        )
                    },
                    endCurvedContent = {
                        // TODO(220086395): replace back with a curvedText
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_ITEM_TAG),
                                text = "Curved content",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertExists()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_end_curved_text_only_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    endLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Start content",
                        )
                    },
                    endCurvedContent = {
                        // TODO(220086395): replace back with a curvedText
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_ITEM_TAG),
                                text = "Curved content",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertExists()
    }

    @Test
    fun omits_separator_with_only_time_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun omits_separator_with_only_time_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_start_linear_separator_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    startLinearContent = {
                        Text(
                            text = "Start content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(1)

        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_start_curved_separator_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    startCurvedContent = {
                        curvedComposable {
                            Text(
                                text = "Start content",
                            )
                        }
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(CURVED_SEPARATOR_ITEM_TAG).assertCountEquals(1)
    }

    @Test
    fun shows_only_end_linear_separator_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    endLinearContent = {
                        Text(
                            text = "End content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(1)

        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_end_curved_separator_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    endCurvedContent = {
                        curvedText(
                            text = "End content"
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(CURVED_SEPARATOR_ITEM_TAG).assertCountEquals(1)
    }

    @Test
    fun shows_start_and_end_linear_separators_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    startLinearContent = {
                        Text(
                            text = "Start content",
                        )
                    },
                    endLinearContent = {
                        Text(
                            text = "End content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(2)
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_start_and_end_curved_separators_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    startCurvedContent = {
                        curvedText(
                            text = "Start content"
                        )
                    },
                    endCurvedContent = {
                        curvedText(
                            text = "End content"
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        // TODO(220086395): replace back with a CurvedTextSeparator
                        curvedComposable {
                            Text(
                                modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG),
                                text = ".",
                            )
                        }
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onAllNodesWithTag(CURVED_SEPARATOR_ITEM_TAG).assertCountEquals(2)
    }

    // TODO: currently testing on round device is problematic as there're no appropriate semantics
    // for CurvedText
    @Test
    fun changes_timeTextStyle_on_square_device() {
        val timeText = "testTime"

        val testTextStyle = TextStyle(
            color = Color.Green,
            background = Color.Black,
            fontSize = 20.sp
        )
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    timeSource = object : TimeSource {
                        override val currentTime: String
                            @Composable
                            get() = timeText
                    },
                    timeTextStyle = testTextStyle
                )
            }
        }
        val actualStyle = rule.textStyleOf(timeText)
        assertEquals(testTextStyle.color, actualStyle.color)
        assertEquals(testTextStyle.background, actualStyle.background)
        assertEquals(testTextStyle.fontSize, actualStyle.fontSize)
    }

    @Test
    fun changes_material_theme_on_square_device() {
        val timeText = "testTime"

        val testTextStyle = TextStyle(
            color = Color.Green,
            background = Color.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 25.sp,
            fontFamily = FontFamily.SansSerif
        )
        rule.setContent {
            MaterialTheme(
                typography = MaterialTheme.typography.copy(
                    caption1 = testTextStyle
                )
            ) {
                ConfiguredShapeScreen(false) {
                    TimeText(
                        timeSource = object : TimeSource {
                            override val currentTime: String
                                @Composable
                                get() = timeText
                        }
                    )
                }
            }
        }
        val actualStyle = rule.textStyleOf(timeText)
        assertEquals(testTextStyle.color, actualStyle.color)
        assertEquals(testTextStyle.background, actualStyle.background)
        assertEquals(testTextStyle.fontSize, actualStyle.fontSize)
        assertEquals(testTextStyle.fontStyle, actualStyle.fontStyle)
        assertEquals(testTextStyle.fontFamily, actualStyle.fontFamily)
    }
}

class TimeSourceTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun formats_current_time() {
        val currentTimeInMillis = 1631544258000L // 2021-09-13 14:44:18
        val format = "HH:mm:ss"
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeInMillis }
        val convertedTime = DateFormat.format(format, currentCalendar).toString()

        var actualTime: String? = null
        rule.setContentWithTheme {
            actualTime = currentTime({ currentTimeInMillis }, format).value
        }
        assertEquals(convertedTime, actualTime)
    }

    @Test
    fun formats_current_time_12H() {
        val currentTimeInMillis = 1631544258000L // 2021-09-13 14:44:18
        val expectedTime = "2:44"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        var actualTime: String? = null
        rule.setContentWithTheme {
            actualTime = currentTime(
                { currentTimeInMillis },
                TimeTextDefaults.TimeFormat12Hours
            ).value
        }
        assertEquals(expectedTime, actualTime)
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
        assertEquals(expectedTime, actualTime)
    }
}

private const val LINEAR_ITEM_TAG = "LINEAR_ITEM_TAG"
private const val CURVED_ITEM_TAG = "CURVED_ITEM_TAG"
private const val LINEAR_SEPARATOR_ITEM_TAG = "LINEAR_SEPARATOR_ITEM_TAG"
private const val CURVED_SEPARATOR_ITEM_TAG = "CURVED_SEPARATOR_ITEM_TAG"
