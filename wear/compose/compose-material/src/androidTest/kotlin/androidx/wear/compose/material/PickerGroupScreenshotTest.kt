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

package androidx.wear.compose.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
class PickerGroupScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    private val screenHeight = 150.dp

    @Test
    fun pickerGroup() = verifyScreenshot {
        samplePickerGroup()
    }

    @Test
    fun pickerGroup_withAutoCentering() = verifyScreenshot {
        samplePickerGroup(autoCenter = true)
    }

    @Test
    fun pickerGroup_withManyColumns_withAutoCentering() = verifyScreenshot {
        samplePickerGroup(
            pickerCount = 5,
            autoCenter = true
        )
    }

    @Test
    fun pickerGroup_withRtlAndManyColumns_withAutoCentering() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            samplePickerGroup(
                pickerCount = 5,
                autoCenter = true
            )
    }

    @Composable
    private fun samplePickerGroup(
        pickerCount: Int = 2,
        autoCenter: Boolean = false
    ) {
        Box(
            modifier = Modifier
                .height(screenHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
                .testTag(TEST_TAG),
            contentAlignment = Alignment.Center
        ) {
            PickerGroup(
                pickers = getPickerColumns(pickerCount),
                autoCenter = autoCenter,
                separator = {
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(":")
                    Spacer(modifier = Modifier.size(6.dp))
                },
            )
        }
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                content()
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    private fun getPickerColumns(count: Int): Array<PickerGroupItem> = Array(count) {
        PickerGroupItem(
            pickerState = PickerState(10),
            option = { optionIndex, _ -> Text("%02d".format(optionIndex)) }
        )
    }
}
