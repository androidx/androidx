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

package androidx.compose.ui.draw

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.GOLDEN_UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class InnerShadowScreenShotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    private val InnerShadowItemTag = "innerShadowItemTag"

    private val wrapperModifier = Modifier.testTag(InnerShadowItemTag)

    @Test
    fun testInnerShadowWithOffset() {
        rule.setContent {
            Box(
                modifier =
                    wrapperModifier
                        .size(50.dp)
                        .innerShadow(
                            shape = RectangleShape,
                            shadow =
                                Shadow(
                                    radius = 6.dp,
                                    offset = DpOffset(10.dp, 10.dp),
                                    color = Color.Red,
                                ),
                        )
            ) {}
        }
        assertSelectableAgainstGolden("inner_shadow_with_offset_test")
    }

    @Test
    fun testInnerShadowWithOffsetAndSpread() {
        rule.setContent {
            Box(
                modifier =
                    wrapperModifier
                        .size(50.dp)
                        .innerShadow(
                            shape = RectangleShape,
                            shadow =
                                Shadow(
                                    radius = 4.dp,
                                    spread = 3.dp,
                                    offset = DpOffset(7.dp, 7.dp),
                                    color = Color.Red,
                                ),
                        )
            ) {}
        }
        assertSelectableAgainstGolden("inner_shadow_with_offset_and_spread_test")
    }

    @Test
    fun testInnerShadow() {
        rule.setContent {
            Box(
                modifier =
                    wrapperModifier
                        .size(50.dp)
                        .innerShadow(
                            shape = RectangleShape,
                            shadow = Shadow(radius = 6.dp, color = Color.Red),
                        )
            ) {}
        }
        assertSelectableAgainstGolden("inner_shadow_test")
    }

    @Test
    fun testInnerShadowWithGradient() {
        rule.setContent {
            val sweepGradientBrush =
                Brush.sweepGradient(
                    colors =
                        listOf(
                            Color(0xFFFF8C00),
                            Color(0xFFFF2D55),
                            Color(0xFFD400FF),
                            Color(0xFF4A00E0),
                            Color(0xFF4A00E0),
                            Color(0xFFD400FF),
                            Color(0xFFFF2D55),
                            Color(0xFFFF8C00),
                        )
                )

            Box(
                modifier =
                    wrapperModifier
                        .size(50.dp)
                        .innerShadow(
                            shape = RectangleShape,
                            shadow = Shadow(radius = 6.dp, brush = sweepGradientBrush),
                        )
            ) {}
        }
        assertSelectableAgainstGolden("inner_shadow_with_gradient_test")
    }

    @Test
    fun testInnerShadowWithGradientWithOffset() {
        rule.setContent {
            val sweepGradientBrush =
                Brush.sweepGradient(
                    colors =
                        listOf(
                            Color(0xFFFF8C00),
                            Color(0xFFFF2D55),
                            Color(0xFFD400FF),
                            Color(0xFF4A00E0),
                            Color(0xFF4A00E0),
                            Color(0xFFD400FF),
                            Color(0xFFFF2D55),
                            Color(0xFFFF8C00),
                        )
                )

            Box(
                modifier =
                    wrapperModifier
                        .size(50.dp)
                        .innerShadow(
                            shape = RectangleShape,
                            shadow =
                                Shadow(
                                    radius = 6.dp,
                                    offset = DpOffset(10.dp, 10.dp),
                                    brush = sweepGradientBrush,
                                ),
                        )
            ) {}
        }
        assertSelectableAgainstGolden("inner_shadow_with_gradient_and_offset_test")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertSelectableAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(InnerShadowItemTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
