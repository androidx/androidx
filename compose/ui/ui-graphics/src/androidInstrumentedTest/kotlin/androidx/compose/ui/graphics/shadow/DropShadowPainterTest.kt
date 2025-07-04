/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DropShadowPainterTest {

    @Test
    fun testDropShadowPainterWithColor() {
        val dropShadow = DropShadowPainter(RectangleShape, Shadow(200.dp, Color.Red))
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    { prevLeft, current -> assertTrue(current.red >= prevLeft.red) },
                    { prevTop, current -> assertTrue(current.red >= prevTop.red) },
                    { prevRight, current -> assertTrue(current.red >= prevRight.red) },
                    { prevBottom, current -> assertTrue(current.red >= prevBottom.red) },
                )
            },
        )
    }

    @Test
    fun testDropShadowPainterWithPathAndColor() {
        val dropShadow =
            DropShadowPainter(
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val path =
                            Path().apply {
                                lineTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                        return Outline.Generic(path)
                    }
                },
                Shadow(200.dp, Color.Red),
            )
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    { prevLeft, current -> assertTrue(current.red >= prevLeft.red) },
                    { prevTop, current -> assertTrue(current.red >= prevTop.red) },
                    { prevRight, current -> assertTrue(current.red >= prevRight.red) },
                    { prevBottom, current -> assertTrue(current.red >= prevBottom.red) },
                )
            },
        )
    }

    @Test
    fun testDropShadowPainterWithBrush() {
        val dropShadow =
            DropShadowPainter(RectangleShape, Shadow(200.dp, createTestImageShaderBrush()))
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    topToCenter = { prevTop, current -> assertTrue(current.red >= prevTop.red) },
                    bottomToCenter = { prevBottom, current ->
                        assertTrue(current.green >= prevBottom.green)
                    },
                )
            },
        )
    }

    @Test
    fun testDropShadowPainterPathWithBrush() {
        val dropShadow =
            DropShadowPainter(
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val path =
                            Path().apply {
                                lineTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                        return Outline.Generic(path)
                    }
                },
                Shadow(200.dp, createTestImageShaderBrush()),
            )
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    topToCenter = { prevTop, current -> assertTrue(current.red >= prevTop.red) },
                    bottomToCenter = { prevBottom, current ->
                        assertTrue(current.green >= prevBottom.green)
                    },
                )
            },
        )
    }

    @Test
    fun testDropShadowPainterWithBrushAndColorFilter() {
        val dropShadow =
            DropShadowPainter(RectangleShape, Shadow(200.dp, createTestImageShaderBrush()))
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size, colorFilter = ColorFilter.tint(Color.Red)) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    { prevLeft, current -> assertTrue(current.red >= prevLeft.red) },
                    { prevTop, current -> assertTrue(current.red >= prevTop.red) },
                    { prevRight, current -> assertTrue(current.red >= prevRight.red) },
                    { prevBottom, current -> assertTrue(current.red >= prevBottom.red) },
                )
            },
        )
    }

    @Test
    fun testDropShadowPainterWithBrushAndAlpha() {
        val dropShadow =
            DropShadowPainter(
                RectangleShape,
                Shadow(
                    200.dp,
                    Brush.verticalGradient(
                        0.0f to Color.Red,
                        0.5f to Color.Red,
                        0.5f to Color.Green,
                        1.0f to Color.Green,
                    ),
                ),
            )
        shadowTest(
            block = {
                drawRect(Color.Blue)
                with(dropShadow) { draw(size, alpha = 0.5f) }
            },
            verify = { pixelmap ->
                verifyShadow(
                    pixelmap,
                    topToCenter = { _, current ->
                        assertTrue(current.red <= 0.8f)
                        assertTrue(current.blue > 0f)
                    },
                    bottomToCenter = { _, current ->
                        assertTrue(current.green <= 0.8f)
                        assertTrue(current.blue > 0f)
                    },
                )
            },
        )
    }
}
