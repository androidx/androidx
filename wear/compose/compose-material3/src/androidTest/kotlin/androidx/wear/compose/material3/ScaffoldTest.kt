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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
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
    fun displays_scroll_indicator_initially_when_scrollable_lazycolumn() {
        val scrollIndicatorColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffoldWithLazyColumn(
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
    fun hides_scroll_indicator_after_delay_lazycolumn() {
        val scrollIndicatorColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffoldWithLazyColumn(
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shows_time_text_after_delay_lazycolumn() {
        val timeTextColor = Color.Red

        rule.setContentWithTheme {
            TestScreenScaffoldWithLazyColumn(
                scrollIndicatorColor = Color.Blue,
                timeTextColor = timeTextColor
            )
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp(durationMillis = 10) }

        // After a 2500 delay, the time text is animated back in. Allow a little longer for the
        // animation to complete.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(4000)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(timeTextColor)
    }

    @Test
    fun no_initial_room_for_bottom_button() {
        var spaceAvailable: Int = Int.MAX_VALUE

        rule.setContentWithTheme {
            // Ensure we use the same size no mater where this is run.
            Box(Modifier.size(300.dp)) {
                TestScreenScaffold(scrollIndicatorColor = Color.Blue, timeTextColor = Color.Red) {
                    BoxWithConstraints {
                        // Check how much space we have for the bottom button
                        spaceAvailable = constraints.maxHeight
                    }
                }
            }
        }

        assertEquals(0, spaceAvailable)
    }

    @Test
    fun no_initial_room_for_bottom_button_wear_lazy_column() {
        var spaceAvailable: Int = Int.MAX_VALUE

        rule.setContentWithTheme {
            // Ensure we use the same size no mater where this is run.
            Box(Modifier.size(300.dp)) {
                TestScreenScaffoldWithLazyColumn(
                    scrollIndicatorColor = Color.Blue,
                    timeTextColor = Color.Red
                ) {
                    BoxWithConstraints {
                        // Check how much space we have for the bottom button
                        spaceAvailable = constraints.maxHeight
                    }
                }
            }
        }

        assertEquals(0, spaceAvailable)
    }

    @Test
    fun plenty_of_room_for_bottom_button_after_scroll() {
        var spaceAvailable: Int = Int.MAX_VALUE
        var expectedSpace = 0f

        val screenSize = 300.dp
        rule.setContentWithTheme {
            // The available space is half the screen size minus half a Button height (converting
            // dps to pixels).
            expectedSpace =
                with(LocalDensity.current) { ((screenSize - ButtonDefaults.Height) / 2).toPx() }

            Box(Modifier.size(screenSize)) {
                TestScreenScaffold(scrollIndicatorColor = Color.Blue, timeTextColor = Color.Red) {
                    // Check how much space we have for the bottom button
                    BoxWithConstraints { spaceAvailable = constraints.maxHeight }
                }
            }
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { repeat(5) { swipeUp() } }
        rule.waitForIdle()

        // Use floats so we can specify a pixel of tolerance.
        assertThat(spaceAvailable.toFloat()).isWithin(1f).of(expectedSpace)
    }

    @Test
    fun no_initial_room_for_bottom_button_lc() {
        var spaceAvailable: Int = Int.MAX_VALUE

        rule.setContentWithTheme {
            // Ensure we use the same size no mater where this is run.
            Box(Modifier.size(300.dp)) {
                TestBottomButtonLC {
                    BoxWithConstraints {
                        // Check how much space we have for the bottom button
                        spaceAvailable = constraints.maxHeight
                    }
                }
            }
        }

        assertEquals(0, spaceAvailable)
    }

    @Test fun no_room_for_bottom_button_after_scroll_lc() = check_bottom_button_lc(0.dp)

    @Test fun some_room_for_bottom_button_after_scroll_lc() = check_bottom_button_lc(50.dp)

    private fun check_bottom_button_lc(verticalPadding: Dp = 0.dp) {
        var spaceAvailable: Int = Int.MAX_VALUE
        var expectedSpace: Float = Float.MAX_VALUE

        val screenSize = 300.dp
        rule.setContentWithTheme {
            expectedSpace = with(LocalDensity.current) { verticalPadding.toPx() }

            Box(Modifier.size(screenSize)) {
                TestBottomButtonLC(verticalPadding) {
                    // Check how much space we have for the bottom button
                    BoxWithConstraints { spaceAvailable = constraints.maxHeight }
                }
            }
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { repeat(5) { swipeUp() } }
        rule.waitForIdle()

        // Use floats so we can specify a pixel of tolerance.
        assertThat(spaceAvailable.toFloat()).isWithin(1f).of(expectedSpace)
    }

    @Composable
    private fun TestScreenScaffold(
        scrollIndicatorColor: Color,
        timeTextColor: Color,
        bottomButton: @Composable BoxScope.() -> Unit = {}
    ) {
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
                timeText = { Box(Modifier.size(20.dp).background(timeTextColor)) },
                bottomButton = bottomButton
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

    @Composable
    private fun TestScreenScaffoldWithLazyColumn(
        scrollIndicatorColor: Color,
        timeTextColor: Color,
        bottomButton: @Composable BoxScope.() -> Unit = {}
    ) {
        AppScaffold {
            val scrollState = rememberTransformingLazyColumnState()
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
                timeText = { Box(Modifier.size(20.dp).background(timeTextColor)) },
                bottomButton = bottomButton
            ) {
                TransformingLazyColumn(
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

    @Composable
    private fun TestBottomButtonLC(
        verticalPadding: Dp = 0.dp,
        bottomButton: @Composable BoxScope.() -> Unit = {}
    ) {
        AppScaffold {
            val scrollState = rememberLazyListState()
            ScreenScaffold(
                modifier = Modifier.testTag(TEST_TAG),
                scrollState = scrollState,
                bottomButton = bottomButton
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize().background(Color.Black).testTag(SCROLL_TAG),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = verticalPadding)
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
