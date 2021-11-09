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
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.BasicCurvedText
import androidx.wear.compose.material.TimeTextDefaults.CurvedTextSeparator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@ExperimentalWearMaterialApi
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

    @Test
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
    fun supports_leading_linear_text_only_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    leadingLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Leading content",
                        )
                    },
                    leadingCurvedContent = {
                        BasicCurvedText(
                            modifier = Modifier.testTag(CURVED_ITEM_TAG),
                            text = "Leading content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertExists()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_leading_curved_text_only_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    leadingLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Leading content",
                        )
                    },
                    leadingCurvedContent = {
                        BasicCurvedText(
                            modifier = Modifier.testTag(CURVED_ITEM_TAG),
                            text = "Leading content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertExists()
    }

    @Test
    fun supports_trailing_linear_text_only_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    trailingLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Trailing content",
                        )
                    },
                    trailingCurvedContent = {
                        BasicCurvedText(
                            modifier = Modifier.testTag(CURVED_ITEM_TAG),
                            text = "Trailing content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_ITEM_TAG).assertExists()
        rule.onNodeWithTag(CURVED_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_trailing_curved_text_only_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    trailingLinearContent = {
                        Text(
                            modifier = Modifier.testTag(LINEAR_ITEM_TAG),
                            text = "Leading content",
                        )
                    },
                    trailingCurvedContent = {
                        BasicCurvedText(
                            modifier = Modifier.testTag(CURVED_ITEM_TAG),
                            text = "Leading content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
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
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
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
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_leading_linear_separator_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    leadingLinearContent = {
                        Text(
                            text = "Leading content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(1)

        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_leading_curved_separator_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    leadingCurvedContent = {
                        Text(
                            text = "Leading content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(CURVED_SEPARATOR_ITEM_TAG).assertCountEquals(1)
    }

    @Test
    fun shows_only_trailing_linear_separator_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    trailingLinearContent = {
                        Text(
                            text = "Trailing content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(1)

        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_only_trailing_curved_separator_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    trailingCurvedContent = {
                        BasicCurvedText(
                            text = "Trailing content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onNodeWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertExists()
        rule.onAllNodesWithTag(CURVED_SEPARATOR_ITEM_TAG).assertCountEquals(1)
    }

    @Test
    fun shows_leading_and_trailing_linear_separators_on_square_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(false) {
                TimeText(
                    leadingLinearContent = {
                        Text(
                            text = "Leading content",
                        )
                    },
                    trailingLinearContent = {
                        Text(
                            text = "Trailing content",
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
                    }
                )
            }
        }
        rule.onAllNodesWithTag(LINEAR_SEPARATOR_ITEM_TAG).assertCountEquals(2)
        rule.onNodeWithTag(CURVED_SEPARATOR_ITEM_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_leading_and_trailing_curved_separators_on_round_device() {
        rule.setContentWithTheme {
            ConfiguredShapeScreen(true) {
                TimeText(
                    leadingCurvedContent = {
                        BasicCurvedText(
                            text = "Leading content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    },
                    trailingCurvedContent = {
                        BasicCurvedText(
                            text = "Trailing content",
                            style = TimeTextDefaults.timeCurvedTextStyle()
                        )
                    },
                    textLinearSeparator = {
                        TimeTextDefaults.TextSeparator(
                            modifier = Modifier.testTag(LINEAR_SEPARATOR_ITEM_TAG)
                        )
                    },
                    textCurvedSeparator = {
                        CurvedTextSeparator(
                            modifier = Modifier.testTag(CURVED_SEPARATOR_ITEM_TAG)
                        )
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
        val timeState = mutableStateOf("testState")

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
                            get() = timeState.value
                    },
                    timeTextStyle = testTextStyle
                )
            }
        }
        val actualStyle = rule.textStyleOf(timeState.value)
        assertEquals(testTextStyle.color, actualStyle.color)
        assertEquals(testTextStyle.background, actualStyle.background)
        assertEquals(testTextStyle.fontSize, actualStyle.fontSize)
    }
}

@ExperimentalWearMaterialApi
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
}

private const val LINEAR_ITEM_TAG = "LINEAR_ITEM_TAG"
private const val CURVED_ITEM_TAG = "CURVED_ITEM_TAG"
private const val LINEAR_SEPARATOR_ITEM_TAG = "LINEAR_SEPARATOR_ITEM_TAG"
private const val CURVED_SEPARATOR_ITEM_TAG = "CURVED_SEPARATOR_ITEM_TAG"