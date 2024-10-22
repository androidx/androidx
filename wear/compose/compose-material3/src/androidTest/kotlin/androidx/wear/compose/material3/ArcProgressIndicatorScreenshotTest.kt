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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ArcProgressIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun arc_progress_indicator_starts_empty(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize, 0L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .align(Alignment.Center)
                        .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter),
            )
        }
    }

    @Test
    fun arc_progress_indicator_500ms(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize, 500L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .align(Alignment.Center)
                        .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter),
            )
        }
    }

    @Test
    fun arc_progress_indicator_500ms_clockwise(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize, 500L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .align(Alignment.Center)
                        .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter),
                angularDirection = AngularDirection.Clockwise
            )
        }
    }

    @Test
    fun arc_progress_indicator_angles(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize, 500L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .align(Alignment.Center)
                        .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter),
                startAngle = 0f,
                endAngle = 180f,
            )
        }
    }

    @Test
    fun arc_progress_indicator_diameter(@TestParameter screenSize: ScreenSize) {
        verifyScreenshot(screenSize, 500L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG).align(Alignment.Center).size(screenSize.size.dp),
            )
        }
    }

    @Test
    fun arc_progress_indicator_strokewidth() {
        verifyScreenshot(ScreenSize.LARGE, 500L) {
            ArcProgressIndicator(
                modifier =
                    Modifier.testTag(TEST_TAG)
                        .align(Alignment.Center)
                        .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter),
                strokeWidth = ArcProgressIndicatorDefaults.IndeterminateStrokeWidth * 2
            )
        }
    }

    private fun verifyScreenshot(
        screenSize: ScreenSize,
        milliseconds: Long,
        content: @Composable (BoxScope.() -> Unit)
    ) {
        rule.mainClock.autoAdvance = false

        rule.setContentWithTheme(modifier = Modifier.background(Color.Black)) {
            ScreenConfiguration(screenSize.size) {
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }

        rule.mainClock.advanceTimeBy(milliseconds)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }
}
