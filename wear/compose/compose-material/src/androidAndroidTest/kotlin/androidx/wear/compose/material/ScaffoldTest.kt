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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalAnimationApi::class)
class ScaffoldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Scaffold(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content() {
        rule.setContentWithTheme {
            Scaffold(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text(CONTENT_MESSAGE)
            }
        }

        rule.onNodeWithText(CONTENT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun displays_timetext() {
        rule.setContentWithTheme {
            Scaffold(
                modifier = Modifier.testTag(TEST_TAG),
                timeText = { Text(TIME_TEXT_MESSAGE) }
            ) {
                Text("Some text")
            }
        }

        rule.onNodeWithText(TIME_TEXT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun displays_vignette() {
        val showVignette = mutableStateOf(VignettePosition.TopAndBottom)
        rule.setContentWithTheme {
            Scaffold(
                modifier = Modifier.testTag(TEST_TAG),
                timeText = { Text(TIME_TEXT_MESSAGE) },
                vignette = {
                    Vignette(
                        vignettePosition = showVignette.value,
                        modifier = Modifier.testTag("VIGNETTE")
                    )
                },
            ) {
                Text("Some text")
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("VIGNETTE").assertIsDisplayed()
    }

    // TODO(http://b/221403412): re-enable when we implement proper fade in/out.
    @Ignore("Failing due to updated functionality.")
    @Test
    fun displays_scrollbar() {
        val showVignette = mutableStateOf(false)
        val vignetteValue = mutableStateOf(VignettePosition.TopAndBottom)

        rule.setContentWithTheme {
            val scrollState = rememberScalingLazyListState()

            Scaffold(
                modifier = Modifier.testTag(TEST_TAG).background(Color.Black),
                timeText = { Text(TIME_TEXT_MESSAGE) },
                vignette = {
                    if (showVignette.value) {
                        Vignette(vignettePosition = vignetteValue.value)
                    }
                },
                positionIndicator = {
                    PositionIndicator(
                        scalingLazyListState = scrollState,
                        modifier = Modifier.testTag("POSITION_INDICATOR")
                    )
                }
            ) {
                ScalingLazyColumn(modifier = Modifier.testTag("ScalingLazyColumn")) {
                    items(20) {
                        Text("" + it, modifier = Modifier.testTag("" + it))
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("POSITION_INDICATOR", true).assertExists()
        rule.onNodeWithTag("POSITION_INDICATOR", true).assertIsDisplayed()

        rule.mainClock.autoAdvance = false
        // TODO(b/196060592): Remove this duplication once advanceByTime is fixed
        rule.mainClock.advanceTimeBy(milliseconds = 4000)
        rule.mainClock.advanceTimeBy(milliseconds = 4000)
        rule.waitForIdle()

        rule.onNodeWithTag("POSITION_INDICATOR", true).assertDoesNotExist()
    }
}

private const val CONTENT_MESSAGE = "The Content"
private const val TIME_TEXT_MESSAGE = "The Time Text"
