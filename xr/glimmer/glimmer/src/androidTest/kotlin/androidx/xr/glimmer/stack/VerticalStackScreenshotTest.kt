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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.CardDefaults
import androidx.xr.glimmer.GOLDEN_DIRECTORY
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.samples.VerticalStackSample
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.captureToImage
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
    fun verticalStack_fixedSizeItems_pressAndHold() {
        rule.setGlimmerThemeContent { VerticalStackSample() }
        rule.onRoot().performTouchInput { down(Offset(x = centerX, y = centerY)) }
        assertRootAgainstGolden("verticalStack_fixedSizeItems_pressAndHold")
    }

    @Test
    fun verticalStack_fixedSizeItems_scrollToNextItem() {
        rule.setGlimmerThemeContent { VerticalStackSample() }
        rule.onRoot().performTouchInput { swipeUp() }
        assertRootAgainstGolden("verticalStack_fixedSizeItems_scrollToNextItem")
    }

    @Test
    fun verticalStack_varyingSizeItems_initialState() {
        rule.setGlimmerThemeContent { VerticalStackWithVaryingSizeItems() }
        assertRootAgainstGolden("verticalStack_varyingSizeItems_initialState")
    }

    @Test
    fun verticalStack_varyingSizeItems_scrollHalfWay() {
        rule.setGlimmerThemeContent { VerticalStackWithVaryingSizeItems() }
        rule.onRoot().performTouchInput {
            down(Offset(x = centerX, y = centerY))
            moveTo(Offset(x = centerX, y = 0f))
        }
        assertRootAgainstGolden("verticalStack_varyingSizeItems_scrollHalfWay")
    }

    @Test
    fun verticalStack_varyingSizeItems_scrollToNextItem() {
        rule.setGlimmerThemeContent { VerticalStackWithVaryingSizeItems() }
        rule.onRoot().performTouchInput { swipeUp() }
        assertRootAgainstGolden("verticalStack_varyingSizeItems_scrollToNextItem")
    }

    @Test
    fun verticalStack_multipleShapes_clipsToWidest() {
        rule.setGlimmerThemeContent {
            VerticalStack(modifier = Modifier.height(300.dp)) {
                item {
                    Column {
                        Card(
                            modifier =
                                Modifier.fillMaxWidth(fraction = 0.5f)
                                    .itemDecoration(CardDefaults.shape)
                        ) {
                            Text("Item-0: narrow shape")
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().itemDecoration(CardDefaults.shape)
                        ) {
                            Text("Item-0: wide shape")
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxSize().itemDecoration(CardDefaults.shape)) {
                        Text("Item-1")
                    }
                }
            }
        }
        assertRootAgainstGolden("verticalStack_multipleShapes_clipsToWidest")
    }

    @Test
    fun verticalStack_multipleShapes_clipsToTopMostWidest() {
        rule.setGlimmerThemeContent {
            VerticalStack(modifier = Modifier.height(300.dp)) {
                item {
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth().itemDecoration(CardDefaults.shape)
                        ) {
                            Text("Item-0: wide shape 1")
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().itemDecoration(CardDefaults.shape)
                        ) {
                            Text("Item-0: wide shape 2")
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxSize().itemDecoration(CardDefaults.shape)) {
                        Text("Item-1")
                    }
                }
            }
        }
        assertRootAgainstGolden("verticalStack_multipleShapes_clipsToTopMostWidest")
    }

    @Test
    fun verticalStack_genericShape_clipsToHighestWidestPoint() {
        val indentedRhombusShape = GenericShape { size, _ ->
            apply {
                moveTo(size.width * 0.5f, 0f)
                lineTo(size.width, size.height * 0.3f)
                lineTo(size.width * 0.6f, size.height * 0.5f)
                lineTo(size.width, size.height * 0.7f)
                lineTo(size.width * 0.5f, size.height)
                lineTo(0f, size.height * 0.7f)
                lineTo(size.width * 0.4f, size.height * 0.5f)
                lineTo(0f, size.height * 0.3f)
                close()
            }
        }
        rule.setGlimmerThemeContent {
            VerticalStack(modifier = Modifier.height(300.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxSize().itemDecoration(indentedRhombusShape),
                        shape = indentedRhombusShape,
                    ) {}
                }
                item {
                    Card(modifier = Modifier.fillMaxSize().itemDecoration(CardDefaults.shape)) {
                        Text("Item-1")
                    }
                }
            }
        }
        assertRootAgainstGolden("verticalStack_genericShape_clipsToHighestWidestPoint")
    }

    @Composable
    private fun VerticalStackWithVaryingSizeItems() {
        VerticalStack(modifier = Modifier.height(300.dp)) {
            items(10) { index ->
                Card(
                    modifier =
                        Modifier.fillMaxHeight(if (index % 2 == 0) 0.5f else 1f)
                            .itemDecoration(CardDefaults.shape)
                ) {
                    Text("Item-$index")
                }
            }
        }
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        // Increase the matcher threshold to ensure that diffs in shadows are captured.
        val matcherThreshold = 0.998
        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(matcherThreshold))
    }
}
