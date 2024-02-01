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
package androidx.wear.compose.material

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class VignetteScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    private val screenSize = 340.dp

    @Test
    fun vignette_circular_top() = verifyScreenshot {
        DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isScreenRound = true)) {
            sampleVignette(
                VignettePosition.Top,
                modifier = Modifier.size(screenSize).clip(CircleShape)
            )
        }
    }

    @Test
    fun vignette_circular_bottom() = verifyScreenshot {
        DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isScreenRound = true)) {
            sampleVignette(
                VignettePosition.Bottom,
                modifier = Modifier.size(screenSize).clip(CircleShape)
            )
        }
    }

    @Test
    fun vignette_circular_top_and_bottom() = verifyScreenshot {
        DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isScreenRound = true)) {
            sampleVignette(
                VignettePosition.TopAndBottom,
                modifier = Modifier.size(screenSize).clip(CircleShape)
            )
        }
    }

    @Test
    fun vignette_square_top() = verifyScreenshot {
        sampleVignette(VignettePosition.Top)
    }

    @Test
    fun vignette_square_bottom() = verifyScreenshot {
        sampleVignette(VignettePosition.Bottom)
    }

    @Test
    fun vignette_square_top_and_bottom() = verifyScreenshot {
        sampleVignette(VignettePosition.TopAndBottom)
    }

    @Composable
    fun sampleVignette(
        vignettePosition: VignettePosition,
        modifier: Modifier = Modifier.size(screenSize)
    ) {
        Scaffold(
            vignette = { Vignette(vignettePosition = vignettePosition) },
            modifier = Modifier.testTag(TEST_TAG)
        ) {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
            }
        }
    }

    private fun verifyScreenshot(
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
