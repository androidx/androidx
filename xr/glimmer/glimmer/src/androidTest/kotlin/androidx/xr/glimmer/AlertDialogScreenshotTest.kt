/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.samples.AlertDialogConfirmOnlySample
import androidx.xr.glimmer.samples.AlertDialogSample
import androidx.xr.glimmer.testutils.captureToImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class AlertDialogScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun alertDialog_twoAction() {
        rule.setGlimmerThemeContent {
            AlertDialogSample()
        }
        rule.assertDialogAgainstGolden("alertDialog_twoAction", screenshotRule)
    }

    @Test
    fun alertDialog_confirmOnly() {
        rule.setGlimmerThemeContent {
            AlertDialogConfirmOnlySample()
        }
        rule.assertDialogAgainstGolden("alertDialog_confirmOnly", screenshotRule)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.assertDialogAgainstGolden(
        goldenName: String,
        screenshotRule: AndroidXScreenshotTestRule,
    ) {
        onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(0.995))
    }
}
