/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
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
class ButtonGroupScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun button_group_2_items() = verifyScreenshot(numItems = 2)

    @Test fun button_group_2_items_different_sizes() = verifyScreenshot(numItems = 2, weight2 = 2f)

    @Test fun button_group_3_items() = verifyScreenshot(numItems = 3)

    @Test
    fun button_group_3_items_different_sizes() =
        verifyScreenshot(numItems = 3, weight2 = 2f, weight3 = 3f)

    private fun verifyScreenshot(
        numItems: Int = 2,
        minWidth1: Dp = 48.dp,
        weight1: Float = 1f,
        minWidth2: Dp = 48.dp,
        weight2: Float = 1f,
        minWidth3: Dp = 48.dp,
        weight3: Float = 1f,
        spacing: Dp = ButtonGroupDefaults.Spacing,
        expansionWidth: Dp = ButtonGroupDefaults.ExpansionWidth,
    ) {
        require(numItems in 1..3)
        rule.setContentWithTheme {
            val interactionSource1 = remember { MutableInteractionSource() }
            val interactionSource2 = remember { MutableInteractionSource() }
            val interactionSource3 = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                ButtonGroup(
                    Modifier.testTag(TEST_TAG),
                    spacing = spacing,
                    expansionWidth = expansionWidth
                ) {
                    buttonGroupItem(interactionSource1, minWidth1, weight1) {
                        Text("A", Modifier.background(Color.Gray))
                    }
                    if (numItems >= 2)
                        buttonGroupItem(interactionSource2, minWidth2, weight2) {
                            Text("B", Modifier.background(Color.Gray))
                        }
                    if (numItems >= 3)
                        buttonGroupItem(interactionSource3, minWidth3, weight3) {
                            Text("C", Modifier.background(Color.Gray))
                        }
                }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
