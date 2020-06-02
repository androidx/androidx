/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.material

import android.os.Build
import androidx.compose.state
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.find
import androidx.ui.test.isToggleable
import androidx.ui.test.runOnIdleCompose
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class CheckboxScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    @Test
    fun checkBoxTest_checked() {
        composeTestRule.setMaterialContent {
            Checkbox(modifier = wrap, checked = true, onCheckedChange = { })
        }
        find(isToggleable())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "checkbox_checked")
    }

    @Test
    fun checkBoxTest_unchecked() {
        composeTestRule.setMaterialContent {
            Checkbox(modifier = wrap, checked = false, onCheckedChange = { })
        }
        find(isToggleable())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "checkbox_unchecked")
    }

    @Test
    fun checkBoxTest_unchecked_animateToChecked() {
        composeTestRule.setMaterialContent {
            val isChecked = state { false }
            Checkbox(modifier = wrap, checked = isChecked.value,
                onCheckedChange = { isChecked.value = it })
        }

        composeTestRule.clockTestRule.pauseClock()

        find(isToggleable())
            .doClick()

        runOnIdleCompose { }

        composeTestRule.clockTestRule.advanceClock(120)

        find(isToggleable())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "checkbox_animateToChecked")
    }

    @Test
    fun checkBoxTest_checked_animateToUnchecked() {
        composeTestRule.setMaterialContent {
            val isChecked = state { true }
            Checkbox(modifier = wrap, checked = isChecked.value,
                onCheckedChange = { isChecked.value = it })
        }

        composeTestRule.clockTestRule.pauseClock()

        find(isToggleable())
            .doClick()

        runOnIdleCompose { }

        composeTestRule.clockTestRule.advanceClock(120)

        find(isToggleable())
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, "checkbox_animateToUnchecked")
    }
}