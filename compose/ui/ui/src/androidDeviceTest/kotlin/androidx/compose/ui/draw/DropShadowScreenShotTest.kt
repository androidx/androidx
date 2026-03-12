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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.GOLDEN_UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(Parameterized::class)
class DropShadowScreenShotTest(private val shape: Shape, private val shapeName: String) {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    private val DropShadowItemTag = "dropShadowItemTag"

    private val wrapperModifier = Modifier.testTag(DropShadowItemTag)

    companion object {
        private val genericShape = GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        @Parameterized.Parameters(name = "{1}")
        @JvmStatic
        fun parameters(): Collection<Array<Any>> =
            listOf(arrayOf(RectangleShape, "RectangleShape"), arrayOf(genericShape, "GenericShape"))
    }

    @Test
    fun testDropShadowWithOffset() {
        rule.setContent {
            Box(wrapperModifier.size(70.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .dropShadow(
                                shape = shape,
                                shadow =
                                    Shadow(
                                        radius = 6.dp,
                                        offset = DpOffset(10.dp, 10.dp),
                                        color = Color.Red,
                                    ),
                            )
                            .background(Color.White, RectangleShape)
                ) {}
            }
        }
        assertSelectableAgainstGolden("drop_shadow_with_offset_test")
    }

    @Test
    fun testDropShadowWithOffsetAndSpread() {
        rule.setContent {
            Box(wrapperModifier.size(70.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .dropShadow(
                                shape = shape,
                                shadow =
                                    Shadow(
                                        radius = 6.dp,
                                        spread = 3.dp,
                                        offset = DpOffset(10.dp, 10.dp),
                                        color = Color.Red,
                                    ),
                            )
                            .background(Color.White, RectangleShape)
                ) {}
            }
        }
        assertSelectableAgainstGolden("drop_shadow_with_offset_and_spread_test")
    }

    @Test
    fun testDropShadow() {
        rule.setContent {
            Box(wrapperModifier.size(70.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .dropShadow(
                                shape = shape,
                                shadow = Shadow(radius = 10.dp, color = Color.Red),
                            )
                            .background(Color.White, RectangleShape)
                ) {}
            }
        }

        assertSelectableAgainstGolden("drop_shadow_test")
    }

    @Test
    fun testDropShadowWithGradient() {
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

            Box(wrapperModifier.size(70.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .dropShadow(
                                shape = shape,
                                shadow = Shadow(radius = 6.dp, brush = sweepGradientBrush),
                            )
                            .background(Color.White, RectangleShape)
                ) {}
            }
        }
        assertSelectableAgainstGolden("drop_shadow_with_gradient_test")
    }

    @Test
    fun testDropShadowWithGradientWithOffset() {
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
            Box(wrapperModifier.size(70.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .dropShadow(
                                shape = shape,
                                shadow =
                                    Shadow(
                                        radius = 6.dp,
                                        spread = 0.dp,
                                        offset = DpOffset(7.dp, 7.dp),
                                        brush = sweepGradientBrush,
                                    ),
                            )
                            .background(Color.White, RectangleShape)
                ) {}
            }
        }

        assertSelectableAgainstGolden("drop_shadow_with_gradient_and_offset_test")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun assertSelectableAgainstGolden(goldenName: String) {
        val goldenIdentifier = "${goldenName}_${shapeName.lowercase()}"

        rule
            .onNodeWithTag(DropShadowItemTag)
            .captureToImage()
            .assertAgainstGolden(rule = screenshotRule, goldenIdentifier = goldenIdentifier)
    }
}
