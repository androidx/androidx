/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.stack

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GOLDEN_DIRECTORY
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.assertRootAgainstGolden
import androidx.xr.glimmer.samples.VerticalStackSample
import androidx.xr.glimmer.setGlimmerThemeContent
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class VerticalStackScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun verticalStack_fixedSizeItems_initialState() {
        rule.setGlimmerThemeContent { VerticalStackSample() }
        assertRootAgainstGolden("verticalStack_fixedSizeItems_initialState")
    }

    @Test
    fun verticalStack_fixedSizeItems_scrollHalfWay() {
        rule.setGlimmerThemeContent { VerticalStackSample() }
        rule.onRoot().performTouchInput {
            down(Offset(x = centerX, y = centerY))
            moveTo(Offset(x = centerX, y = 0f))
        }
        assertRootAgainstGolden("verticalStack_fixedSizeItems_scrollHalfWay")
    }

    @Test
    fun verticalStack_varyingSizeItems_initialState() {
        rule.setGlimmerThemeContent {
            VerticalStack(modifier = Modifier.height(300.dp)) {
                items(10) { index ->
                    Card(modifier = Modifier.fillMaxHeight(if (index % 2 == 0) 0.5f else 1f)) {
                        Text("Item-$index")
                    }
                }
            }
        }
        assertRootAgainstGolden("verticalStack_varyingSizeItems_initialState")
    }

    @Test
    fun verticalStack_varyingSizeItems_scrollHalfWay() {
        rule.setGlimmerThemeContent {
            VerticalStack(modifier = Modifier.height(300.dp)) {
                items(10) { index ->
                    Card(modifier = Modifier.fillMaxHeight(if (index % 2 == 0) 0.5f else 1f)) {
                        Text("Item-$index")
                    }
                }
            }
        }
        rule.onRoot().performTouchInput {
            down(Offset(x = centerX, y = centerY))
            moveTo(Offset(x = centerX, y = 0f))
        }
        assertRootAgainstGolden("verticalStack_varyingSizeItems_scrollHalfWay")
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        rule.assertRootAgainstGolden(goldenName, screenshotRule)
    }
}
