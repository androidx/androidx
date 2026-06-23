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

// A copy from
// compose/ui/ui/src/androidDeviceTest/kotlin/androidx/compose/ui/graphics/MeshGradientTest.kt
// adapted to the Skiko test harness. The Android-only software-layer variant
// (testSoftwareLayerMeshGradientWithControlPoints) is dropped as it relies on
// View.LAYER_TYPE_SOFTWARE. The mesh content paints an explicit white background so the
// assertions do not depend on the (platform specific) default window background color.

package androidx.compose.ui.graphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalTestApi::class)
class MeshGradientTest {

    @Test
    fun testSimpleMeshGradient() =
        runSkikoComposeUiTest(Size(200f, 200f)) {
            val width = 200
            val height = 200
            val block: MeshGradientScope.() -> Unit = {
                setVertex(row = 0, column = 0, position = Offset(0f, 0f), color = Color.Red)
                setVertex(row = 0, column = 1, position = Offset(1f, 0f), color = Color.Blue)
                setVertex(row = 1, column = 0, position = Offset(0f, 1f), color = Color.Green)
                setVertex(row = 1, column = 1, position = Offset(1f, 1f), color = Color.Yellow)
            }

            setContent { MeshGradientTestContent(1, 1, false, IntSize(width, height), block) }
            waitForIdle()
            val pixelMap = onRoot().captureToImage().toPixelMap()
            assertEqualsWithTolerance(Color.Red, pixelMap[0, 0], 0.03f)
            assertEqualsWithTolerance(Color.Blue, pixelMap[width - 1, 0], 0.03f)
            assertEqualsWithTolerance(Color.Green, pixelMap[0, height - 1], 0.03f)
            assertEqualsWithTolerance(Color.Yellow, pixelMap[width - 1, height - 1], 0.03f)
        }

    @Test
    fun testMeshGradientWithControlPoints() =
        runSkikoComposeUiTest(Size(200f, 50f)) {
            val width = 200
            val height = 50
            val block: MeshGradientScope.() -> Unit = {
                // Creating a gradient of a solid red color and using the bezier offsets to pull the
                // mesh down at the top edge
                setVertex(
                    row = 0,
                    column = 0,
                    position = Offset(0f, 0f),
                    color = Color.Red,
                    rightControlPoint = Offset(0.25f, 0.25f),
                )
                setVertex(
                    row = 0,
                    column = 1,
                    position = Offset(1f, 0f),
                    color = Color.Red,
                    leftControlPoint = Offset(-0.25f, 0.25f),
                )
                setVertex(row = 1, column = 0, position = Offset(0f, 1f), color = Color.Red)
                setVertex(row = 1, column = 1, position = Offset(1f, 1f), color = Color.Red)
            }
            setContent { MeshGradientTestContent(1, 1, false, IntSize(width, height), block) }
            waitForIdle()
            val meshGradientPixelMap = onRoot().captureToImage().toPixelMap()
            // This path draws a rect whose top edge is a cubic bezier with control points exactly
            // equal to what is given in the mesh gradient above
            val cubicBezierPath =
                Path().apply {
                    moveTo(0f, 0f)
                    cubicTo(
                        0.25f * width,
                        0.25f * height,
                        width.toFloat() - 0.25f * width,
                        0.25f * height,
                        width.toFloat(),
                        0f,
                    )
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }

            val pathImageBitmap =
                ImageBitmap(width, height).apply {
                    drawInto {
                        drawRect(SolidColor(Color.White))
                        drawPath(cubicBezierPath, Color.Red)
                    }
                }
            val pathPixelMap = pathImageBitmap.toPixelMap()

            for (i in 0 until width) {
                for (j in 0 until height) {
                    val pathColor = pathPixelMap[i, j]
                    val meshColor = meshGradientPixelMap[i, j]
                    if (pathColor != Color.Red && pathColor != Color.White) {
                        // Since we have not provided any alpha, this must be due to the
                        // antialiasing. Canvas.drawVertices does not support antialiasing so the
                        // mesh gradient has no antialiasing at the curved edges. Skipping these
                        // pixels.
                        continue
                    }
                    assertEqualsWithTolerance(pathColor, meshColor, 0.03f)
                }
            }
        }

    @Test
    fun testMeshGradientBilinearInterpolation() =
        runSkikoComposeUiTest(Size(200f, 200f)) {
            val width = 200
            val height = 200
            val block: MeshGradientScope.() -> Unit = {
                setVertex(row = 0, column = 0, position = Offset(0f, 0f), color = Color.Red)
                setVertex(row = 0, column = 1, position = Offset(1f, 0f), color = Color.Blue)
                setVertex(row = 1, column = 0, position = Offset(0f, 1f), color = Color.Yellow)
                setVertex(row = 1, column = 1, position = Offset(1f, 1f), color = Color.Magenta)
            }
            setContent { MeshGradientTestContent(1, 1, false, IntSize(width, height), block) }
            waitForIdle()
            val pixelMap = onRoot().captureToImage().toPixelMap()
            assertEqualsWithTolerance(Color.Red, pixelMap[0, 0], 0.03f)
            assertEqualsWithTolerance(Color.Blue, pixelMap[width - 1, 0], 0.03f)
            assertEqualsWithTolerance(Color.Yellow, pixelMap[0, height - 1], 0.03f)
            assertEqualsWithTolerance(Color.Magenta, pixelMap[width - 1, height - 1], 0.03f)

            // Mix of all 4 corner colors in middle
            val expectedColor =
                lerp(
                    lerp(Color.Red, Color.Blue, 0.5f),
                    lerp(Color.Yellow, Color.Magenta, 0.5f),
                    0.5f,
                )
            assertEqualsWithTolerance(expectedColor, pixelMap[width / 2 - 1, height / 2 - 1], 0.03f)
        }

    @Test
    fun testMeshGradientInvalidRows() =
        runSkikoComposeUiTest {
            assertFailsWith<IllegalArgumentException> {
                setContent { MeshGradientTestContent(0, 1, false, IntSize(1, 1)) {} }
                waitForIdle()
            }
        }

    @Test
    fun testMeshGradientInvalidColumns() =
        runSkikoComposeUiTest {
            assertFailsWith<IllegalArgumentException> {
                setContent { MeshGradientTestContent(1, 0, false, IntSize(1, 1)) {} }
                waitForIdle()
            }
        }

    @Test
    fun testMeshGradientWithUnspecifiedColorIsTransparent() =
        runSkikoComposeUiTest(Size(100f, 100f)) {
            val block: MeshGradientScope.() -> Unit = {
                for (r in 0..rows) {
                    for (c in 0..columns) {
                        setVertex(
                            row = r,
                            column = c,
                            position = Offset(r.toFloat(), c.toFloat()),
                            color = Color.Unspecified,
                        )
                    }
                }
            }
            setContent { MeshGradientTestContent(1, 1, false, IntSize(100, 100), block) }
            waitForIdle()
            val imageBitmap = onRoot().captureToImage()
            imageBitmap.assertPixels { Color.White }
        }

    @Test
    fun testMeshGradientInfersControlPointIfNotProvided() =
        runSkikoComposeUiTest(Size(200f, 400f)) {
            val colors =
                listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Yellow, Color.Magenta)
            val rows = 5
            val columns = 6
            val width = 200
            val height = 200

            // Uniformly distributing the points
            val setPositionAndColor: MeshGradientScope.() -> Unit = {
                for (i in 0..rows) {
                    for (j in 0..columns) {
                        setVertex(
                            i,
                            j,
                            Offset(j / columns.toFloat(), i / rows.toFloat()),
                            colors[(i * (columns + 1) + j) % colors.size],
                        )
                    }
                }
            }
            val lengthOfHorizontalEdge = 1f / columns
            val lengthOfVerticalEdge = 1f / rows
            val explicitBezierGradientBlock: MeshGradientScope.() -> Unit = {
                for (i in 0..rows) {
                    for (j in 0..columns) {
                        setVertex(
                            row = i,
                            column = j,
                            position = Offset(j / columns.toFloat(), i / rows.toFloat()),
                            color = colors[(i * (columns + 1) + j) % colors.size],
                            rightControlPoint = Offset(0.33f * lengthOfHorizontalEdge, 0f),
                            leftControlPoint = Offset(-0.33f * lengthOfHorizontalEdge, 0f),
                            topControlPoint = Offset(0f, -0.33f * lengthOfVerticalEdge),
                            bottomControlPoint = Offset(0f, 0.33f * lengthOfVerticalEdge),
                        )
                    }
                }
            }
            setContent {
                Layout(
                    content = {
                        MeshGradientTestContent(
                            rows,
                            columns,
                            false,
                            IntSize(width, height),
                            setPositionAndColor,
                        )
                        MeshGradientTestContent(
                            rows,
                            columns,
                            false,
                            IntSize(width, height),
                            explicitBezierGradientBlock,
                        )
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val totalHeight = placeables.sumOf { it.height }
                    val maxWidth = placeables.maxOf { it.width }
                    layout(maxWidth, totalHeight) {
                        var yPosition = 0
                        placeables.forEach { placeable ->
                            placeable.placeWithLayer(0, yPosition)
                            yPosition += placeable.height
                        }
                    }
                }
            }
            waitForIdle()
            val contentBitmap = onRoot().captureToImage()

            val inferredGradientPixelMap = contentBitmap.toPixelMap(0, 0, width, height)
            val explicitGradientPixelMap = contentBitmap.toPixelMap(0, height, width, height)

            for (i in 0 until 100) {
                for (j in 0 until 100) {
                    assertEqualsWithTolerance(
                        inferredGradientPixelMap[i, j],
                        explicitGradientPixelMap[i, j],
                        0.03f,
                    )
                }
            }
        }

    @Test
    fun testMeshGradientWithAlpha() =
        runSkikoComposeUiTest(Size(200f, 10f)) {
            val width = 200
            val height = 10
            val block: MeshGradientScope.() -> Unit = {
                // Creating a linear horizontal gradient
                setVertex(0, 0, position = Offset(0f, 0f), color = Color.Red.copy(alpha = 0.5f))
                setVertex(0, 1, position = Offset(1f, 0f), color = Color.Blue.copy(alpha = 0.5f))
                setVertex(1, 0, position = Offset(0f, 1f), color = Color.Red.copy(alpha = 0.5f))
                setVertex(1, 1, position = Offset(1f, 1f), color = Color.Blue.copy(alpha = 0.5f))
            }
            setContent { MeshGradientTestContent(1, 1, false, IntSize(width, height), block) }
            waitForIdle()
            val pixelMap = onRoot().captureToImage().toPixelMap()

            val compositedRedColor = Color.Red.copy(alpha = 0.5f).compositeOver(Color.White)
            val compositedBlueColor = Color.Blue.copy(alpha = 0.5f).compositeOver(Color.White)
            val compositedMiddleColor =
                lerp(Color.Red.copy(alpha = 0.5f), Color.Blue.copy(alpha = 0.5f), 0.5f)
                    .compositeOver(Color.White)

            assertEqualsWithTolerance(compositedRedColor, pixelMap[0, 0], 0.03f)
            assertEqualsWithTolerance(compositedRedColor, pixelMap[0, height - 1], 0.03f)
            assertEqualsWithTolerance(compositedBlueColor, pixelMap[width - 1, 0], 0.03f)
            assertEqualsWithTolerance(compositedBlueColor, pixelMap[width - 1, height - 1], 0.03f)
            assertEqualsWithTolerance(compositedMiddleColor, pixelMap[width / 2, height / 2], 0.03f)
        }

    @Test
    fun testMeshGradientConvertsColorSpaceToSRGB() =
        runSkikoComposeUiTest(Size(200f, 400f)) {
            val sRGBColors =
                listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Yellow, Color.Magenta)
            val okLabColors = sRGBColors.map { it.convert(ColorSpaces.Oklab) }
            val rows = 3
            val columns = 3
            val width = 200
            val height = 200

            // Uniformly distributing the points
            fun MeshGradientScope.setPositionAndColor(colors: List<Color>) {
                for (i in 0..rows) {
                    for (j in 0..columns) {
                        setVertex(
                            i,
                            j,
                            Offset(j / columns.toFloat(), i / rows.toFloat()),
                            colors[(i * (columns + 1) + j) % colors.size],
                        )
                    }
                }
            }
            val sRGBBlock: MeshGradientScope.() -> Unit = { setPositionAndColor(sRGBColors) }
            val okLabBlock: MeshGradientScope.() -> Unit = { setPositionAndColor(okLabColors) }
            setContent {
                Layout(
                    content = {
                        MeshGradientTestContent(
                            rows,
                            columns,
                            false,
                            IntSize(width, height),
                            sRGBBlock,
                        )
                        MeshGradientTestContent(
                            rows,
                            columns,
                            false,
                            IntSize(width, height),
                            okLabBlock,
                        )
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val totalHeight = placeables.sumOf { it.height }
                    val maxWidth = placeables.maxOf { it.width }
                    layout(maxWidth, totalHeight) {
                        var yPosition = 0
                        placeables.forEach { placeable ->
                            placeable.placeRelative(0, yPosition)
                            yPosition += placeable.height
                        }
                    }
                }
            }
            waitForIdle()
            val contentBitmap = onRoot().captureToImage()
            val sRGBGradientPixelMap = contentBitmap.toPixelMap(0, 0, width, height)
            val okLabGradientPixelMap = contentBitmap.toPixelMap(0, height, width, height)

            for (i in 0 until width) {
                for (j in 0 until height) {
                    assertEqualsWithTolerance(
                        sRGBGradientPixelMap[i, j],
                        okLabGradientPixelMap[i, j],
                        0.03f,
                    )
                }
            }
        }

    @Test
    fun testMeshGradientInvalidIndices() =
        runSkikoComposeUiTest(Size(1f, 1f)) {
            val width = 1
            val height = 1
            val block: MeshGradientScope.() -> Unit = { setVertex(2, 0, Offset.Zero, Color.Red) }

            assertFailsWith<IllegalArgumentException> {
                setContent { MeshGradientTestContent(1, 1, false, IntSize(width, height), block) }
                waitForIdle()
            }
        }

    private fun ImageBitmap.drawInto(block: DrawScope.() -> Unit) =
        CanvasDrawScope()
            .draw(
                Density(1.0f),
                LayoutDirection.Ltr,
                Canvas(this),
                Size(width.toFloat(), height.toFloat()),
                block,
            )

    private fun assertEqualsWithTolerance(expected: Color, actual: Color, tolerance: Float = 0.0f) {
        kotlin.test.assertEquals(expected.red, actual.red, tolerance, "Red channel mismatch")
        kotlin.test.assertEquals(expected.green, actual.green, tolerance, "Green channel mismatch")
        kotlin.test.assertEquals(expected.blue, actual.blue, tolerance, "Blue channel mismatch")
        kotlin.test.assertEquals(expected.alpha, actual.alpha, tolerance, "Alpha channel mismatch")
    }

    @Composable
    private fun MeshGradientTestContent(
        rows: Int,
        columns: Int,
        hasBicubicColor: Boolean = false,
        size: IntSize,
        block: MeshGradientScope.() -> Unit,
    ) {
        val gradientPainter = remember {
            MeshGradientPainter(rows, columns, hasBicubicColor, block)
        }
        // Paint an explicit white background so the assertions do not depend on the platform
        // default window background color (Android draws on a white window).
        Layout(
            Modifier.drawBehind {
                drawRect(Color.White)
                with(gradientPainter) { draw(size.toSize()) }
            }
        ) { _, _ ->
            layout(size.width, size.height) {}
        }
    }
}
