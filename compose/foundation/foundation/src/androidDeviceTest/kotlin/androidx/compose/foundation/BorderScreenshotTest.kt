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

package androidx.compose.foundation

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Screenshot tests for [border], covering every outline type handled by the border implementation
 * (rectangle, simple rounded rectangle, complex rounded rectangle, and simple / complex generic
 * shapes), drawn with a solid color, a gradient, and a runtime shader brush. Each screenshot shows
 * the border at several widths, including [Dp.Hairline].
 *
 * Screenshots are compared pixel perfectly with [PixelPerfectMatcher], instead of the default MSSIM
 * threshold, so that any change in rendering (including antialiasing) is caught.
 */
@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class BorderScreenshotTest(
    private val shapeName: String,
    private val shape: Shape,
    private val brushName: String,
    private val brush: Brush,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "shape={0},brush={2}")
        fun initParameters(): List<Array<Any>> {
            val shapes =
                listOf(
                    // Outline.Rectangle
                    "Rectangle" to RectangleShape,
                    // Outline.Rounded with equal corner radii (RoundRect.isSimple)
                    "SimpleRoundedRect" to RoundedCornerShape(16.dp),
                    // Outline.Rounded with different corner radii
                    "ComplexRoundedRect" to
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 8.dp,
                            bottomStart = 24.dp,
                        ),
                    // Simple Outline.Generic
                    "SimpleGeneric" to
                        GenericShape { size, _ ->
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        },
                    // Complex Outline.Generic
                    "ComplexGeneric" to
                        PolygonShape.star(
                            numPoints = 9,
                            innerRadiusRatio = 0.9f,
                            outerRounding =
                                androidx.compose.foundation.shape.CornerRounding.fraction(0.2f),
                        ),
                )

            val solidColorBrush = SolidColor(Color.Red)

            val sweepGradientBrush: Brush =
                Brush.sweepGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    )
                )

            val runtimeShaderBrush =
                ShaderBrush(
                    RuntimeShader(
                        "half4 main(float2 c) { return half4(0.5 + 0.5 * sin(length(c) / 6.0 + float3(0, 2, 4)), 1); }"
                    )
                )

            val brushes =
                listOf(
                    "Color" to solidColorBrush,
                    "Gradient" to sweepGradientBrush,
                    "RuntimeShader" to runtimeShaderBrush,
                )

            return shapes.flatMap { (shapeName, shape) ->
                brushes.map { (brushName, brush) ->
                    arrayOf(shapeName, shape, brushName, brush)
                }
            }
        }
    }

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_FOUNDATION)

    private val testTag = "border"

    @Test
    fun border() {
        rule.setContent {
            Box(
                Modifier.padding(8.dp)
                    .testTag(testTag)
                    .background(Color.White)
                    .size(128.dp)
                    .border(8.dp, brush, shape)
                    .padding(24.dp)
                    .border(4.dp, brush, shape)
                    .padding(12.dp)
                    .border(2.dp, brush, shape)
                    .padding(12.dp)
                    .border(Dp.Hairline, brush, shape)
            )
        }
        rule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "border_${shapeName}_$brushName",
                PixelPerfectMatcher(),
            )
    }
}
