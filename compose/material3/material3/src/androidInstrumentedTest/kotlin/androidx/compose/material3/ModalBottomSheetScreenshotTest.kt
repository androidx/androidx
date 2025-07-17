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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ModalBottomSheetScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "ModalBottomSheet"

    @Test
    fun modalBottomSheet_predictiveBack_progress0() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalBottomSheetPredictiveBack(progress = 0f)
        }
        assertAgainstGolden("modalBottomSheet_predictiveBack_progress0")
    }

    @Test
    fun modalBottomSheet_predictiveBack_progress25() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalBottomSheetPredictiveBack(progress = 0.25f)
        }
        assertAgainstGolden("modalBottomSheet_predictiveBack_progress25")
    }

    @Test
    fun modalBottomSheet_predictiveBack_progress50() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalBottomSheetPredictiveBack(progress = 0.5f)
        }
        assertAgainstGolden("modalBottomSheet_predictiveBack_progress50")
    }

    @Test
    fun modalBottomSheet_predictiveBack_progress75() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalBottomSheetPredictiveBack(progress = 0.75f)
        }
        assertAgainstGolden("modalBottomSheet_predictiveBack_progress75")
    }

    @Test
    fun modalBottomSheet_predictiveBack_progress100() {
        rule.setMaterialContent(lightColorScheme()) {
            ModalBottomSheetPredictiveBack(progress = 1f)
        }
        assertAgainstGolden("modalBottomSheet_predictiveBack_progress100")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
    }

    @Composable
    private fun ModalBottomSheetPredictiveBack(progress: Float) {
        Box(Modifier.fillMaxSize(), propagateMinConstraints = true) {
            ModalBottomSheetContent(
                modifier = Modifier.testTag(testTag),
                predictiveBackProgress = remember { Animatable(initialValue = progress) },
                scope = rememberCoroutineScope(),
                sheetState = rememberSheetState(initialValue = SheetValue.Expanded),
                animateToDismiss = {},
                settleToDismiss = {},
            ) {
                Text(
                    "Modal Bottom Sheet Predictive Back\nProgress: $progress",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
