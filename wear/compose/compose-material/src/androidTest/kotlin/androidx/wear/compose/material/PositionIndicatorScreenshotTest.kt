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
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
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
class PositionIndicatorScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun left_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.Left,
            value = 0.2f,
            ltr = true,
            goldenIdentifier = testName.methodName
        )

    @Test
    fun left_in_rtl_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.Left,
            value = 0.4f,
            ltr = false,
            goldenIdentifier = testName.methodName
        )

    @Test
    fun right_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.Right,
            value = 0.3f,
            ltr = true,
            goldenIdentifier = testName.methodName
        )

    @Test
    fun right_in_rtl_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.Right,
            value = 0.5f,
            ltr = false,
            goldenIdentifier = testName.methodName
        )

    @Test
    fun end_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.End,
            value = 0.1f,
            ltr = true,
            goldenIdentifier = testName.methodName
        )

    @Test
    fun end_in_rtl_position_indicator() =
        position_indicator_position_test(
            position = PositionIndicatorAlignment.End,
            value = 0.8f,
            ltr = false,
            goldenIdentifier = testName.methodName
        )

    private fun position_indicator_position_test(
        position: PositionIndicatorAlignment,
        value: Float,
        goldenIdentifier: String,
        ltr: Boolean = true,
    ) {
        rule.setContentWithTheme {
            val actualLayoutDirection =
                if (ltr) LayoutDirection.Ltr
                else LayoutDirection.Rtl
            CompositionLocalProvider(LocalLayoutDirection provides actualLayoutDirection) {
                PositionIndicator(
                    value = { value },
                    position = position,
                    modifier = Modifier.testTag(TEST_TAG).background(Color.Black)
                )
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
