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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ProgressIndicatorScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun progress_indicator_fullscreen() = verifyScreenshot {
        CircularProgressIndicator(
            progress = { 0.25f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            startAngle = 120f,
            endAngle = 60f,
        )
    }

    @Test
    fun progress_indicator_custom_color() = verifyScreenshot {
        CircularProgressIndicator(
            progress = { 0.75f },
            modifier = Modifier.size(200.dp).testTag(TEST_TAG),
            startAngle = 120f,
            endAngle = 60f,
            colors =
                ProgressIndicatorDefaults.colors(
                    indicatorColor = Color.Green,
                    trackColor = Color.Red.copy(alpha = 0.5f)
                )
        )
    }

    @Test
    fun progress_indicator_wrapping_media_button() = verifyScreenshot {
        val progressPadding = 4.dp
        Box(
            modifier =
                Modifier.size(IconButtonDefaults.DefaultButtonSize + progressPadding)
                    .testTag(TEST_TAG)
        ) {
            CircularProgressIndicator(progress = { 0.75f }, strokeWidth = progressPadding)
            IconButton(
                modifier =
                    Modifier.align(Alignment.Center)
                        .padding(progressPadding)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play/pause button icon"
                )
            }
        }
    }

    @Test
    fun progress_indicator_overflow() = verifyScreenshot {
        CircularProgressIndicator(
            progress = { 0.2f },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            startAngle = 120f,
            endAngle = 60f,
            colors =
                ProgressIndicatorDefaults.colors(
                    trackBrush =
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                )
        )
    }

    @Test
    fun segmented_progress_indicator_with_progress() = verifyScreenshot {
        SegmentedCircularProgressIndicator(
            progress = { 0.5f },
            segmentCount = 5,
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            startAngle = 120f,
            endAngle = 60f,
        )
    }

    @Test
    fun segmented_progress_indicator_on_off() = verifyScreenshot {
        SegmentedCircularProgressIndicator(
            segmentCount = 6,
            completed = { it % 2 == 0 },
            modifier = Modifier.aspectRatio(1f).testTag(TEST_TAG),
            startAngle = 120f,
            endAngle = 60f,
        )
    }

    private fun verifyScreenshot(content: @Composable () -> Unit) {
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                content = content
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
