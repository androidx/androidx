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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Rule
import org.junit.Test

class ScaffoldTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun app_scaffold_supports_testtag() {
        rule.setContentWithTheme { AppScaffold(modifier = Modifier.testTag(TEST_TAG)) {} }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun screen_scaffold_supports_testtag() {
        rule.setContentWithTheme { ScreenScaffold(modifier = Modifier.testTag(TEST_TAG)) {} }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun app_scaffold_displays_content() {
        rule.setContentWithTheme {
            AppScaffold(modifier = Modifier.testTag(TEST_TAG)) { Text(CONTENT_MESSAGE) }
        }

        rule.onNodeWithText(CONTENT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun screen_scaffold_displays_content() {
        rule.setContentWithTheme {
            ScreenScaffold(modifier = Modifier.testTag(TEST_TAG)) { Text(CONTENT_MESSAGE) }
        }

        rule.onNodeWithText(CONTENT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun displays_app_time_text_by_default() {
        rule.setContentWithTheme {
            AppScaffold(timeText = { Text(TIME_TEXT_MESSAGE) }) { ScreenScaffold {} }
        }

        rule.onNodeWithText(TIME_TEXT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun displays_screen_time_text_when_provided() {
        rule.setContentWithTheme {
            AppScaffold(timeText = { Text("App Time Text") }) {
                ScreenScaffold(timeText = { Text(TIME_TEXT_MESSAGE) }) {}
            }
        }

        rule.onNodeWithText(TIME_TEXT_MESSAGE).assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun displays_scroll_indicator_initially_when_scrollable() {
        val scrollIndicatorColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffold(
                scrollIndicatorColor = scrollIndicatorColor,
                timeTextColor = Color.Blue
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(scrollIndicatorColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun hides_scroll_indicator_after_delay() {
        val scrollIndicatorColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffold(
                scrollIndicatorColor = scrollIndicatorColor,
                timeTextColor = Color.Blue
            )
        }

        // After a 2500 delay, the scroll indicator is animated away. Allow a little longer for the
        // animation to complete.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(4000)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(scrollIndicatorColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shows_time_text_after_delay() {
        val timeTextColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffold(scrollIndicatorColor = Color.Blue, timeTextColor = timeTextColor)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // After a 2500 delay, the time text is animated back in. Allow a little longer for the
        // animation to complete.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(4000)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(timeTextColor)
    }

    @Composable
    private fun TestScreenScaffold(scrollIndicatorColor: Color, timeTextColor: Color) {
        AppScaffold {
            val scrollState = rememberScalingLazyListState()
            ScreenScaffold(
                modifier = Modifier.testTag(TEST_TAG),
                scrollState = scrollState,
                scrollIndicator = {
                    Box(
                        modifier =
                            Modifier.size(20.dp)
                                .align(Alignment.CenterEnd)
                                .background(scrollIndicatorColor)
                    )
                },
                timeText = { Box(Modifier.size(20.dp).background(timeTextColor)) }
            ) {
                ScalingLazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize().background(Color.Black).testTag(SCROLL_TAG)
                ) {
                    items(10) {
                        Button(
                            onClick = {},
                            label = { Text("Item ${it + 1}") },
                        )
                    }
                }
            }
        }
    }
}

private const val CONTENT_MESSAGE = "The Content"
private const val TIME_TEXT_MESSAGE = "The Time Text"
private const val SCROLL_TAG = "ScrollTag"
