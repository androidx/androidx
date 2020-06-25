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

package androidx.ui.core

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.test.AlignTopLeft
import androidx.ui.core.test.AtLeastSize
import androidx.ui.core.test.FixedSize
import androidx.ui.core.test.Padding
import androidx.ui.core.test.background
import androidx.ui.foundation.Box
import androidx.ui.geometry.Size
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.compositeOver
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.toArgb
import androidx.ui.layout.height
import androidx.ui.layout.heightIn
import androidx.ui.layout.ltr
import androidx.ui.layout.rtl
import androidx.ui.layout.width
import androidx.ui.layout.widthIn
import androidx.ui.layout.wrapContentHeight
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findRoot
import androidx.ui.unit.dp
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class PainterModifierTest {

    val containerWidth = 100.0f
    val containerHeight = 100.0f

    @get:Rule
    val rule = createComposeRule()
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        drawLatch = CountDownLatch(1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierColorFilter() {
        val paintLatch = CountDownLatch(1)
        rule.setContent {
            testPainter(
                colorFilter = ColorFilter(Color.Cyan, BlendMode.srcIn),
                latch = paintLatch
            )
        }

        paintLatch.await(5, TimeUnit.SECONDS)

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Cyan.toArgb(), getPixel(50, 50))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierAlpha() {
        val paintLatch = CountDownLatch(1)
        rule.setContent {
            testPainter(
                alpha = 0.5f,
                latch = paintLatch
            )
        }

        paintLatch.await(5, TimeUnit.SECONDS)

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            val expected = Color(
                alpha = 0.5f,
                red = Color.Red.red,
                green = Color.Red.green,
                blue = Color.Red.blue
            ).compositeOver(Color.White)

            val result = Color(getPixel(50, 50))
            assertEquals(expected.red, result.red, 0.01f)
            assertEquals(expected.green, result.green, 0.01f)
            assertEquals(expected.blue, result.blue, 0.01f)
            assertEquals(expected.alpha, result.alpha, 0.01f)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierRtl() {
        val paintLatch = CountDownLatch(1)
        rule.setContent {
            testPainter(
                rtl = true,
                latch = paintLatch
            )
        }

        paintLatch.await(5, TimeUnit.SECONDS)

        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Blue.toArgb(), getPixel(50, 50))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterAspectRatioMaintainedInSmallerParent() {
        val paintLatch = CountDownLatch(1)
        val containerSizePx = containerWidth.roundToInt() * 3
        rule.setContent {
            FixedSize(size = containerSizePx, modifier = Modifier.background(Color.White)) {
                // Verify that the contents are scaled down appropriately even though
                // the Painter's intrinsic width and height is twice that of the component
                // it is to be drawn into
                Padding(containerWidth.roundToInt()) {
                    AtLeastSize(size = containerWidth.roundToInt(),
                        modifier = Modifier.paint(
                            LatchPainter(
                                containerWidth * 2,
                                containerHeight * 2,
                                paintLatch
                            ),
                            alignment = Alignment.Center,
                            contentScale = ContentScale.Inside
                        )
                    ) {
                    }
                }
            }
        }

        paintLatch.await(5, TimeUnit.SECONDS)

        obtainScreenshotBitmap(
            containerSizePx,
            containerSizePx
        ).apply {
            assertEquals(Color.White.toArgb(), getPixel(containerWidth.roundToInt() - 1,
                containerHeight.roundToInt() - 1))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() + 1,
                containerWidth.roundToInt() + 1))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() * 2 - 1,
                containerWidth.roundToInt() * 2 - 1))
            assertEquals(Color.White.toArgb(), getPixel(containerWidth.roundToInt() * 2 + 1,
                containerHeight.roundToInt() * 2 + 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterAlignedBottomRightIfSmallerThanParent() {
        val paintLatch = CountDownLatch(1)
        val containerSizePx = containerWidth.roundToInt() * 2
        rule.setContent {
            AtLeastSize(size = containerWidth.roundToInt() * 2,
                modifier = Modifier.background(Color.White).paint(
                    LatchPainter(
                        containerWidth,
                        containerHeight,
                        paintLatch
                    ),
                    alignment = Alignment.BottomEnd,
                    contentScale = ContentScale.Inside
                )
            ) {
                // Intentionally empty
            }
        }

        paintLatch.await(5, TimeUnit.SECONDS)

        val bottom = containerSizePx - 1
        val right = containerSizePx - 1
        val innerBoxTop = containerSizePx - containerWidth.roundToInt()
        val innerBoxLeft = containerSizePx - containerWidth.roundToInt()
        obtainScreenshotBitmap(
            containerSizePx,
            containerSizePx
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(right, bottom))
            assertEquals(Color.Red.toArgb(), getPixel(innerBoxLeft, bottom))
            assertEquals(Color.Red.toArgb(), getPixel(innerBoxLeft, innerBoxTop + 1))
            assertEquals(Color.Red.toArgb(), getPixel(right, innerBoxTop + 1))

            assertEquals(Color.White.toArgb(), getPixel(innerBoxLeft - 1, bottom))
            assertEquals(Color.White.toArgb(), getPixel(innerBoxLeft - 1, innerBoxTop - 1))
            assertEquals(Color.White.toArgb(), getPixel(right, innerBoxTop - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterModifierIntrinsicSize() {
        val paintLatch = CountDownLatch(1)
        rule.setContent {
            NoMinSizeContainer {
                NoIntrinsicSizeContainer(
                    Modifier.paint(LatchPainter(containerWidth, containerHeight, paintLatch))
                ) {
                    // Intentionally empty
                }
            }
        }

        paintLatch.await()
        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() - 1, 0))
            assertEquals(
                Color.Red.toArgb(), getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() - 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterIntrinsicSizeDoesNotExceedMax() {
        val paintLatch = CountDownLatch(1)
        val containerSize = containerWidth.roundToInt() / 2
        rule.setContent {
            NoIntrinsicSizeContainer(
                Modifier.background(Color.White) +
                        FixedSizeModifier(containerWidth.roundToInt())
            ) {
                NoIntrinsicSizeContainer(
                    AlignTopLeft + FixedSizeModifier(containerSize).paint(
                        LatchPainter(
                            containerWidth,
                            containerHeight,
                            paintLatch
                        ),
                        alignment = Alignment.TopStart
                    )
                ) {
                    // Intentionally empty
                }
            }
        }

        paintLatch.await()
        obtainScreenshotBitmap(
            containerWidth.roundToInt(),
            containerHeight.roundToInt()
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerWidth.roundToInt() / 2 - 1, 0))
            assertEquals(
                Color.White.toArgb(), getPixel(
                    containerWidth.roundToInt() - 1,
                    containerHeight.roundToInt() - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerHeight.roundToInt() / 2 - 1))

            assertEquals(Color.White.toArgb(), getPixel(containerWidth.roundToInt() / 2 + 1, 0))
            assertEquals(Color.White.toArgb(), getPixel(containerWidth.roundToInt() / 2 + 1,
                containerHeight.roundToInt() / 2 + 1))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testPainterNotSizedToIntrinsics() {
        val paintLatch = CountDownLatch(1)
        val containerSize = containerWidth.roundToInt() / 2
        rule.setContent {
            NoIntrinsicSizeContainer(
                Modifier.background(Color.White) +
                        FixedSizeModifier(containerSize)
            ) {
                NoIntrinsicSizeContainer(
                    FixedSizeModifier(containerSize).paint(
                        LatchPainter(
                            containerWidth,
                            containerHeight,
                            paintLatch
                        ),
                        sizeToIntrinsics = false, alignment = Alignment.TopStart)
                ) {
                    // Intentionally empty
                }
            }
        }

        paintLatch.await()
        obtainScreenshotBitmap(
            containerSize,
            containerSize
        ).apply {
            assertEquals(Color.Red.toArgb(), getPixel(0, 0))
            assertEquals(Color.Red.toArgb(), getPixel(containerSize - 1, 0))
            assertEquals(
                Color.Red.toArgb(), getPixel(
                    containerSize - 1,
                    containerSize - 1
                )
            )
            assertEquals(Color.Red.toArgb(), getPixel(0, containerSize - 1))
        }
    }

    @Test
    fun testPainterFixedHeightScalesDownWidth() {
        val composableHeightPx = 100f
        val composableMaxWidthPx = 300f
        val painterWidth = 400f
        val painterHeight = 200f

        val density = rule.density.density
        // The resultant composable should match the height provided in the height modifier
        // however, despite the width being a maximum of 300 pixels, the composable
        // should be 200 pixels wide as the painter is scaled down to ensure the height constraint
        // is satisfied. Because the Painter is twice as tall as the composable, the composable
        // width should be half that of the painter
        testPainterScaleMatchesSize(
            Modifier.height(((composableHeightPx) / density).dp)
                .widthIn(0.dp, (composableMaxWidthPx / density).dp),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            painterWidth / 2,
            composableHeightPx
        )
    }

    @Test
    fun testPainterFixedWidthScalesDownHeight() {
        val composableWidthPx = 100f
        val composableMaxHeightPx = 300f
        val painterWidth = 400f
        val painterHeight = 200f

        val density = rule.density.density
        // The resultant composable should match the height provided in the height modifier
        // however, despite the width being a maximum of 300 pixels, the resultant composable
        // should be 200 pixels wide as the painter is scaled down to ensure the height constraint
        // is satisfied. Because the Painter is twice as tall as the composable, the composable
        // width should be half that of the painter
        testPainterScaleMatchesSize(
            Modifier.width(((composableWidthPx) / density).dp)
                .heightIn(0.dp, (composableMaxHeightPx / density).dp),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight / 4
        )
    }

    @Test
    fun testPainterFixedDimensionUnchanged() {
        val painterWidth = 1000f
        val painterHeight = 375f
        val density = rule.density.density
        val composableWidth = 500f
        val composableHeight = 800f
        // Because the constraints are tight here, do not attempt to resize the composable
        // based on the intrinsic dimensions of the Painter
        testPainterScaleMatchesSize(
            Modifier.width((composableWidth / density).dp).height((composableHeight / density).dp),
            ContentScale.Fit,
            Size(painterWidth, painterHeight),
            composableWidth,
            composableHeight
        )
    }

    @Test
    fun testPainterComposableHeightScaledUpWithFixedWidth() {
        val composableWidthPx = 200f
        val painterWidth = 100f
        val painterHeight = 200f
        // A Painter with ContentScale.FillWidth will scale its content to ensure that the
        // composable width fills its width constraint. This also scales the height by the
        // same scale factor. Because the intrinsic width is twice that of the width constraint,
        // the height should be double that of the intrinsic height of the painter
        testPainterScaleMatchesSize(
            Modifier.width((composableWidthPx / rule.density.density).dp).wrapContentHeight(),
            ContentScale.FillWidth,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight * 2
        )
    }

    @Test
    fun testPainterWidthScaledDownWithSmallerHeight() {
        val composableWidthPx = 200f
        val painterWidth = 100f
        val painterHeight = 200f
        // A Painter with ContentScale.Inside should scale its content down to fit within the
        // constraints of the composable
        // In this case a fixed width that is larger than the painter with undefined height
        // should have the composable width match that of its input and the height match
        // that of the painter
        testPainterScaleMatchesSize(
            Modifier.width((composableWidthPx / rule.density.density).dp).wrapContentHeight(),
            ContentScale.Inside,
            Size(painterWidth, painterHeight),
            composableWidthPx,
            painterHeight
        )
    }

    private fun testPainterScaleMatchesSize(
        modifier: Modifier,
        contentScale: ContentScale,
        painterSize: Size,
        composableWidthPx: Float,
        composableHeightPx: Float
    ) {
        val paintLatch = CountDownLatch(1)

        var composableWidth = 0f
        var composableHeight = 0f
        rule.setContent {
            composableWidth = composableWidthPx / DensityAmbient.current.density
            composableHeight = composableHeightPx / DensityAmbient.current.density
            // Because the painter is told to fit inside the constraints, the width should
            // match that of the provided fixed width and the height should match that of the
            // composable as no scaling is being done
            val painter = object : Painter() {
                override val intrinsicSize: Size
                    get() = painterSize

                override fun DrawScope.onDraw() { /* no-op */ }
            }
            Box(modifier =
                Modifier.drawWithContent {
                    drawContent()
                    paintLatch.countDown()
                }.plus(modifier)
                .paint(painter, contentScale = contentScale)
            )
        }

        paintLatch.await()
        findRoot()
            .assertWidthIsEqualTo(composableWidth.dp)
            .assertHeightIsEqualTo(composableHeight.dp)
    }

    @Composable
    private fun testPainter(
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        rtl: Boolean = false,
        latch: CountDownLatch
    ) {
        val p = LatchPainter(containerWidth, containerHeight, latch)
        AtLeastSize(
            modifier = Modifier.background(Color.White)
                .plus(if (rtl) Modifier.rtl else Modifier.ltr)
                .paint(p, alpha = alpha, colorFilter = colorFilter),
            size = containerWidth.roundToInt()
        ) {
            // Intentionally empty
        }
    }

    private fun obtainScreenshotBitmap(width: Int, height: Int = width): Bitmap {
        val bitmap = findRoot().captureToBitmap()
        Assert.assertEquals(width, bitmap.width)
        Assert.assertEquals(height, bitmap.height)
        return bitmap
    }

    private class LatchPainter(
        val width: Float,
        val height: Float,
        val latch: CountDownLatch
    ) : Painter() {

        var color = Color.Red

        override val intrinsicSize: Size
            get() = Size(width, height)

        override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
            color = if (layoutDirection == LayoutDirection.Rtl) Color.Blue else Color.Red
            return true
        }

        override fun DrawScope.onDraw() {
            drawRect(color = color)
            latch.countDown()
        }
    }

    /**
     * Container composable that relaxes the minimum width and height constraints
     * before giving them to their child
     */
    @Composable
    fun NoMinSizeContainer(children: @Composable () -> Unit) {
        Layout(children) { measurables, constraints ->
            val loosenedConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.map { it.measure(loosenedConstraints) }
            val maxPlaceableWidth = placeables.maxBy { it.width }?.width ?: 0
            val maxPlaceableHeight = placeables.maxBy { it.height }?.width ?: 0
            val width = max(maxPlaceableWidth, loosenedConstraints.minWidth)
            val height = max(maxPlaceableHeight, loosenedConstraints.minHeight)
            layout(width, height) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    }

    /**
     * Composable that is sized purely by the constraints given by its modifiers
     */
    @Composable
    fun NoIntrinsicSizeContainer(
        modifier: Modifier = Modifier,
        children: @Composable () -> Unit
    ) {
        Layout(children, modifier) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = max(
                placeables.maxBy { it.width }?.width ?: 0, constraints
                    .minWidth
            )
            val height = max(
                placeables.maxBy { it.height }?.height ?: 0, constraints
                    .minHeight
            )
            layout(width, height) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    }

    class FixedSizeModifier(val width: Int, val height: Int = width) : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            val placeable = measurable.measure(
                Constraints(
                    minWidth = width,
                    minHeight = height,
                    maxWidth = width,
                    maxHeight = height
                )
            )
            return layout(width, height) {
                placeable.place(0, 0)
            }
        }
    }
}