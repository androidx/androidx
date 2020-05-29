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

@file:Suppress("Deprecation")

package androidx.ui.core.test

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.util.SparseArray
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.Stable
import androidx.compose.State
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Constraints
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawLayerModifier
import androidx.ui.core.DrawModifier
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.IntrinsicMeasurable
import androidx.ui.core.IntrinsicMeasureScope
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Owner
import androidx.ui.core.ParentDataModifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.Ref
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.drawBehind
import androidx.ui.core.drawLayer
import androidx.ui.core.drawWithContent
import androidx.ui.core.offset
import androidx.ui.core.setContent
import androidx.ui.core.tag
import androidx.ui.framework.test.TestActivity
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.graphics.Shape
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.graphics.drawscope.translate
import androidx.ui.layout.ltr
import androidx.ui.layout.offset
import androidx.ui.layout.padding
import androidx.ui.layout.rtl
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.Math.abs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Corresponds to ContainingViewTest, but tests single composition measure, layout and draw.
 * It also tests that layouts with both Layout and MeasureBox work.
 */
@SmallTest
@RunWith(JUnit4::class)
class AndroidLayoutDrawTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
        density = Density(activity)
    }

    // Tests that simple drawing works with layered squares
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10.ipx)
        composeSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    // Tests that simple drawing works with draw with nested children
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun nestedDrawTest() {
        val yellow = Color(0xFFFFFF00)
        val red = Color(0xFF800000)
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10.ipx)
        composeNestedSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    // Tests that recomposition works with models used within Draw components
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeDrawTest() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        val red = Color(0xFF800000)
        val yellow = Color(0xFFFFFF00)
        activityTestRule.runOnUiThreadIR {
            model.outerColor = red
            model.innerColor = yellow
        }

        validateSquareColors(outerColor = red, innerColor = yellow, size = 10)
    }

    // Tests that recomposition of nested repaint boundaries work
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeNestedRepaintBoundariesColorChange() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        val yellow = Color(0xFFFFFF00)
        activityTestRule.runOnUiThreadIR {
            model.innerColor = yellow
        }

        validateSquareColors(outerColor = blue, innerColor = yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeNestedRepaintBoundariesSizeChange() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)
        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    // When there is a repaint boundary around a moving child, the child move
    // should be reflected in the repainted bitmap
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeRepaintBoundariesMove() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        val offset = mutableStateOf(10.ipx)
        composeMovingSquaresWithRepaintBoundary(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        positionLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            offset.value = 20.ipx
        }

        assertTrue(positionLatch!!.await(1, TimeUnit.SECONDS))
        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    // When there is no repaint boundary around a moving child, the child move
    // should be reflected in the repainted bitmap
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeMove() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        val offset = mutableStateOf(10.ipx)
        composeMovingSquares(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            // there isn't going to be a normal draw because we are just moving the repaint
            // boundary, but we should have a draw cycle
            activityTestRule.findAndroidComposeView().viewTreeObserver.addOnDrawListener(object :
                ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    drawLatch.countDown()
                }
            })
            offset.value = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    // Tests that recomposition works with models used within Layout components
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeSizeTest() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { model.size = 20.ipx }
        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    // The size and color are both changed in a simpler single-color square.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleSquareColorAndSizeTest() {
        val green = Color(0xFF00FF00)
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = green)

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(size = (model.size * 3), modifier = fillColor(model, isInner = false)) {
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 30.ipx
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 30)

        drawLatch = CountDownLatch(1)
        val blue = Color(0xFF0000FF)

        activityTestRule.runOnUiThreadIR {
            model.innerColor = blue
            model.outerColor = blue
        }
        validateSquareColors(outerColor = blue, innerColor = blue, size = 30)
    }

    // Components that aren't placed shouldn't be drawn.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun noPlaceNoDraw() {
        val green = Color(0xFF00FF00)
        val white = Color(0xFFFFFFFF)
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = white)

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = {
                    Padding(size = (model.size * 3), modifier = fillColor(model, isInner = false)) {
                    }
                    Padding(size = model.size, modifier = fillColor(model, isInner = true)) {
                    }
                }, measureBlock = { measurables, constraints, _ ->
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(placeables[0].width, placeables[0].height) {
                        placeables[0].place(0.ipx, 0.ipx)
                    }
                })
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)
    }

    // Make sure that draws intersperse properly with sub-layouts
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawOrderWithChildren() {
        val green = Color(0xFF00FF00)
        val white = Color(0xFFFFFFFF)
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = white)

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val contentDrawing = object : DrawModifier {
                    override fun ContentDrawScope.draw() {
                        // Fill the space with the outerColor
                        drawRect(model.outerColor)
                        val offset = size.width / 3
                        // clip drawing to the inner rectangle
                        clipRect(offset, offset, offset * 2, offset * 2) {
                            this@draw.drawContent()

                            // Fill bottom half with innerColor -- should be clipped
                            drawRect(model.innerColor,
                                topLeft = Offset(0f, size.height / 2f),
                                size = Size(size.width, size.height / 2f)
                            )
                        }
                    }
                }

                val paddingContent = Modifier.drawBehind {
                    // Fill top half with innerColor -- should be clipped
                    drawLatch.countDown()
                    drawRect(
                        model.innerColor,
                        size = Size(size.width, size.height / 2f)
                    )
                }
                Padding(size = (model.size * 3), modifier = contentDrawing + paddingContent) {
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = white, size = 20)
    }

    // Tests that calling measure multiple times on the same Measurable causes an exception
    @Test
    fun multipleMeasureCall() {
        val latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                TwoMeasureLayout(50.ipx, latch) {
                    AtLeastSize(50.ipx) {
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun multiChildLayoutTest() {
        val childrenCount = 3
        val childConstraints = arrayOf(
            Constraints(),
            Constraints.fixedWidth(50.ipx),
            Constraints.fixedHeight(50.ipx)
        )
        val headerChildrenCount = 1
        val footerChildrenCount = 2

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val header = @Composable {
                    Layout(measureBlock = { _, constraints, _ ->
                        assertEquals(childConstraints[0], constraints)
                        layout(0.ipx, 0.ipx) {}
                    }, children = emptyContent(), modifier = Modifier.tag("header"))
                }
                val footer = @Composable {
                    Layout(measureBlock = { _, constraints, _ ->
                        assertEquals(childConstraints[1], constraints)
                        layout(0.ipx, 0.ipx) {}
                    }, children = emptyContent(), modifier = Modifier.tag("footer"))
                    Layout(measureBlock = { _, constraints, _ ->
                        assertEquals(childConstraints[2], constraints)
                        layout(0.ipx, 0.ipx) {}
                    }, children = emptyContent(), modifier = Modifier.tag("footer"))
                }

                Layout({ header(); footer() }) { measurables, _, _ ->
                    assertEquals(childrenCount, measurables.size)
                    measurables.forEachIndexed { index, measurable ->
                        measurable.measure(childConstraints[index])
                    }
                    val measurablesHeader = measurables.filter { it.tag == "header" }
                    val measurablesFooter = measurables.filter { it.tag == "footer" }
                    assertEquals(headerChildrenCount, measurablesHeader.size)
                    assertSame(measurables[0], measurablesHeader[0])
                    assertEquals(footerChildrenCount, measurablesFooter.size)
                    assertSame(measurables[1], measurablesFooter[0])
                    assertSame(measurables[2], measurablesFooter[1])
                    layout(0.ipx, 0.ipx) {}
                }
            }
        }
    }

    // When a child's measure() is done within the layout, it should not affect the parent's
    // size. The parent's layout shouldn't be called when the child's size changes
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun measureInLayoutDoesNotAffectParentSize() {
        val white = Color(0xFFFFFFFF)
        val blue = Color(0xFF000080)
        val model = SquareModel(outerColor = blue, innerColor = white)
        var measureCalls = 0
        var layoutCalls = 0

        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    modifier = Modifier.drawBehind {
                        drawRect(model.outerColor)
                    },
                    children = {
                        AtLeastSize(
                            size = model.size,
                            modifier = Modifier.drawBehind {
                                drawLatch.countDown()
                                drawRect(model.innerColor)
                            }
                        )
                    }, measureBlock = { measurables, constraints, _ ->
                        measureCalls++
                        layout(30.ipx, 30.ipx) {
                            layoutCalls++
                            layoutLatch.countDown()
                            val placeable = measurables[0].measure(constraints)
                            placeable.place(
                                (30.ipx - placeable.width) / 2,
                                (30.ipx - placeable.height) / 2
                            )
                        }
                    })
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        layoutCalls = 0
        measureCalls = 0
        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, size = 20, totalSize = 30)
        assertEquals(0, measureCalls)
        assertEquals(1, layoutCalls)
    }

    @Test
    fun testLayout_whenMeasuringIsDoneDuringPlacing() {
        @Composable
        fun FixedSizeRow(
            width: IntPx,
            height: IntPx,
            children: @Composable () -> Unit
        ) {
            Layout(children = children, measureBlock = { measurables, constraints, _ ->
                val resolvedWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
                val resolvedHeight = height.coerceIn(constraints.minHeight, constraints.maxHeight)
                layout(resolvedWidth, resolvedHeight) {
                    val childConstraints = Constraints(
                        IntPx.Zero,
                        IntPx.Infinity,
                        resolvedHeight,
                        resolvedHeight
                    )
                    var left = IntPx.Zero
                    for (measurable in measurables) {
                        val placeable = measurable.measure(childConstraints)
                        if (left + placeable.width > width) {
                            break
                        }
                        placeable.place(left, IntPx.Zero)
                        left += placeable.width
                    }
                }
            })
        }

        @Composable
        fun FixedWidthBox(
            width: IntPx,
            measured: Ref<Boolean?>,
            laidOut: Ref<Boolean?>,
            drawn: Ref<Boolean?>,
            latch: CountDownLatch
        ) {
            Layout(
                children = {},
                modifier = Modifier.drawBehind {
                    drawn.value = true
                    latch.countDown()
                },
                measureBlock = { _, constraints, _ ->
                    measured.value = true
                    val resolvedWidth = width.coerceIn(constraints.minWidth, constraints.maxWidth)
                    val resolvedHeight = constraints.minHeight
                    layout(resolvedWidth, resolvedHeight) { laidOut.value = true }
                })
        }

        val childrenCount = 5
        val measured = Array(childrenCount) { Ref<Boolean?>() }
        val laidOut = Array(childrenCount) { Ref<Boolean?>() }
        val drawn = Array(childrenCount) { Ref<Boolean?>() }
        val latch = CountDownLatch(3)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Align {
                    FixedSizeRow(width = 90.ipx, height = 40.ipx) {
                        for (i in 0 until childrenCount) {
                            FixedWidthBox(
                                width = 30.ipx,
                                measured = measured[i],
                                laidOut = laidOut[i],
                                drawn = drawn[i],
                                latch = latch
                            )
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        for (i in 0 until childrenCount) {
            assertEquals(i <= 3, measured[i].value ?: false)
            assertEquals(i <= 2, laidOut[i].value ?: false)
            assertEquals(i <= 2, drawn[i].value ?: false)
        }
    }

    // When a new child is added, the parent must be remeasured because we don't know
    // if it affects the size and the child's measure() must be called as well.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRelayoutOnNewChild() {
        val drawChild = mutableStateOf(false)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 30.ipx, modifier = fillColor(outerColor)) {
                    if (drawChild.value) {
                        Padding(size = 20.ipx) {
                            AtLeastSize(size = 20.ipx, modifier = fillColor(innerColor)) {
                            }
                        }
                    }
                }
            }
        }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { drawChild.value = true }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 20)
    }

    // When we change a position of one LayoutNode up the tree it automatically
    // changes the position of all the children. RepaintBoundary with few intermediate
    // LayoutNode parents should be drawn on a correct position
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun moveRootLayoutRedrawsLeafRepaintBoundary() {
        val offset = mutableStateOf(0.ipx)
        drawLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    modifier = fillColor(Color.Green),
                    children = {
                        AtLeastSize(size = 10.ipx) {
                            AtLeastSize(
                                size = 10.ipx,
                                modifier = Modifier.drawLayer() + fillColor(Color.Cyan)
                            ) {
                            }
                        }
                    }
                ) { measurables, constraints, _ ->
                    layout(width = 20.ipx, height = 20.ipx) {
                        measurables.first().measure(constraints)
                            .place(offset.value, offset.value)
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        activityTestRule.waitAndScreenShot().apply {
            assertRect(Color.Cyan, size = 10, centerX = 5, centerY = 5)
            assertRect(Color.Green, size = 10, centerX = 15, centerY = 15)
        }

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 10.ipx }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        activityTestRule.waitAndScreenShot().apply {
            assertRect(Color.Green, size = 10, centerX = 5, centerY = 5)
            assertRect(Color.Cyan, size = 10, centerX = 15, centerY = 15)
        }
    }

    // When a child is removed, the parent must be remeasured and redrawn.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRedrawOnRemovedChild() {
        val drawChild = mutableStateOf(true)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 30.ipx, modifier = Modifier.drawBehind {
                    drawLatch.countDown()
                    drawRect(outerColor)
                }) {
                    AtLeastSize(size = 30.ipx) {
                        if (drawChild.value) {
                            Padding(size = 10.ipx) {
                                AtLeastSize(
                                    size = 10.ipx,
                                    modifier = Modifier.drawBehind {
                                        drawLatch.countDown()
                                        drawRect(innerColor)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { drawChild.value = false }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)
    }

    // When a child is removed, the parent must be remeasured.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testRelayoutOnRemovedChild() {
        val drawChild = mutableStateOf(true)

        val outerColor = Color(0xFF000080)
        val innerColor = Color(0xFFFFFFFF)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 30.ipx, modifier = Modifier.drawBehind {
                    drawLatch.countDown()
                    drawRect(outerColor)
                }) {
                    Padding(size = 20.ipx) {
                        if (drawChild.value) {
                            AtLeastSize(
                                size = 20.ipx,
                                modifier = Modifier.drawBehind {
                                    drawLatch.countDown()
                                    drawRect(innerColor)
                                }
                            )
                        }
                    }
                }
            }
        }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 20)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { drawChild.value = false }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)
    }

    @Test
    fun testAlignmentLines() {
        val TestVerticalLine = VerticalAlignmentLine(::min)
        val TestHorizontalLine = HorizontalAlignmentLine(::max)
        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child1 = @Composable {
                    Wrap {
                        Layout(children = {}) { _, _, _ ->
                            layout(
                                0.ipx,
                                0.ipx,
                                mapOf(
                                    TestVerticalLine to 10.ipx,
                                    TestHorizontalLine to 20.ipx
                                )
                            ) { }
                        }
                    }
                }
                val child2 = @Composable {
                    Wrap {
                        Layout(children = {}) { _, _, _ ->
                            layout(
                                0.ipx,
                                0.ipx,
                                mapOf(
                                    TestVerticalLine to 20.ipx,
                                    TestHorizontalLine to 10.ipx
                                )
                            ) { }
                        }
                    }
                }
                val inner = @Composable {
                    Layout({ child1(); child2() }) { measurables, constraints, _ ->
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        assertEquals(10.ipx, placeable1[TestVerticalLine])
                        assertEquals(20.ipx, placeable1[TestHorizontalLine])
                        assertEquals(20.ipx, placeable2[TestVerticalLine])
                        assertEquals(10.ipx, placeable2[TestHorizontalLine])
                        layout(0.ipx, 0.ipx) {
                            placeable1.place(0.ipx, 0.ipx)
                            placeable2.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    assertEquals(10.ipx, placeable[TestVerticalLine])
                    assertEquals(20.ipx, placeable[TestHorizontalLine])
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testAlignmentLines_areNotInheritedFromInvisibleChildren() {
        val TestLine1 = VerticalAlignmentLine(::min)
        val TestLine2 = VerticalAlignmentLine(::min)
        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child1 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine1 to 10.ipx)) {}
                    }
                }
                val child2 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine2 to 20.ipx)) { }
                    }
                }
                val inner = @Composable {
                    Layout({ child1(); child2() }) { measurables, constraints, _ ->
                        val placeable1 = measurables[0].measure(constraints)
                        measurables[1].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            // Only place the first child.
                            placeable1.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    assertEquals(10.ipx, placeable[TestLine1])
                    assertNull(placeable[TestLine2])
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testAlignmentLines_doNotCauseMultipleMeasuresOrLayouts() {
        val TestLine1 = VerticalAlignmentLine(::min)
        val TestLine2 = VerticalAlignmentLine(::min)
        var child1Measures = 0
        var child2Measures = 0
        var child1Layouts = 0
        var child2Layouts = 0
        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child1 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        ++child1Measures
                        layout(0.ipx, 0.ipx, mapOf(TestLine1 to 10.ipx)) {
                            ++child1Layouts
                        }
                    }
                }
                val child2 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        ++child2Measures
                        layout(0.ipx, 0.ipx, mapOf(TestLine2 to 20.ipx)) {
                            ++child2Layouts
                        }
                    }
                }
                val inner = @Composable {
                    Layout({ child1(); child2() }) { measurables, constraints, _ ->
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            placeable1.place(0.ipx, 0.ipx)
                            placeable2.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    assertEquals(10.ipx, placeable[TestLine1])
                    assertEquals(20.ipx, placeable[TestLine2])
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, child1Measures)
        assertEquals(1, child2Measures)
        assertEquals(1, child1Layouts)
        assertEquals(1, child2Layouts)
    }

    @Test
    fun testAlignmentLines_onlyLayoutEarlyWhenNeeded() {
        val TestLine1 = VerticalAlignmentLine(::min)
        val TestLine2 = VerticalAlignmentLine(::min)
        var child1Measures = 0
        var child2Measures = 0
        var child1Layouts = 0
        var child2Layouts = 0
        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child1 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        ++child1Measures
                        layout(0.ipx, 0.ipx, mapOf(TestLine1 to 10.ipx)) {
                            ++child1Layouts
                        }
                    }
                }
                val child2 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        ++child2Measures
                        layout(0.ipx, 0.ipx, mapOf(TestLine2 to 20.ipx)) {
                            ++child2Layouts
                        }
                    }
                }
                val inner = @Composable {
                    Layout({ child1(); child2() }) { measurables, constraints, _ ->
                        val placeable1 = measurables[0].measure(constraints)
                        assertEquals(10.ipx, placeable1[TestLine1])
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            placeable1.place(0.ipx, 0.ipx)
                            placeable2.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, child1Measures)
        assertEquals(1, child2Measures)
        assertEquals(1, child1Layouts)
        assertEquals(0, child2Layouts)
    }

    @Test
    fun testAlignmentLines_canBeQueriedInThePositioningBlock() {
        val TestLine = VerticalAlignmentLine(::min)
        val layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child1 = @Composable {
                    Layout(children = { }) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 10.ipx)) { }
                    }
                }
                val child2 = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(
                            0.ipx,
                            0.ipx,
                            mapOf(TestLine to 20.ipx)
                        ) { }
                    }
                }
                val inner = @Composable {
                    Layout({ child1(); child2() }) { measurables, constraints, _ ->
                        val placeable1 = measurables[0].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            assertEquals(10.ipx, placeable1[TestLine])
                            val placeable2 = measurables[1].measure(constraints)
                            assertEquals(20.ipx, placeable2[TestLine])
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testAlignmentLines_doNotCauseExtraLayout_whenQueriedAfterPositioning() {
        val TestLine = VerticalAlignmentLine(::min)
        val layoutLatch = CountDownLatch(1)
        var childLayouts = 0
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = { }) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 10.ipx)) {
                            ++childLayouts
                        }
                    }
                }
                val inner = @Composable {
                    Layout({ child() }) { measurables, constraints, _ ->
                        val placeable = measurables[0].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            assertEquals(10.ipx, placeable[TestLine])
                            placeable.place(0.ipx, 0.ipx)
                            assertEquals(10.ipx, placeable[TestLine])
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, childLayouts)
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly() {
        val TestLine = VerticalAlignmentLine(::min)
        var layoutLatch = CountDownLatch(1)
        val offset = mutableStateOf(10.ipx)
        var measure = 0
        var layout = 0
        var linePosition: IntPx? = null
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to offset.value)) {}
                    }
                }
                Layout(child) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    linePosition = placeable[TestLine]
                    ++measure
                    layout(placeable.width, placeable.height) {
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
        assertEquals(10.ipx, linePosition)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            offset.value = 20.ipx
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, measure)
        assertEquals(2, layout)
        assertEquals(20.ipx, linePosition)
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly_whenQueriedInLayout() {
        val TestLine = VerticalAlignmentLine(::min)
        var layoutLatch = CountDownLatch(1)
        val offset = mutableStateOf(10.ipx)
        var measure = 0
        var layout = 0
        var linePosition: IntPx? = null
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(
                            0.ipx,
                            0.ipx,
                            mapOf(TestLine to offset.value)
                        ) {}
                    }
                }
                Layout(child) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    ++measure
                    layout(placeable.width, placeable.height) {
                        linePosition = placeable[TestLine]
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
        assertEquals(10.ipx, linePosition)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 20.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, measure)
        assertEquals(2, layout)
        assertEquals(20.ipx, linePosition)
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly_whenMeasuredAndQueriedInLayout() {
        val TestLine = VerticalAlignmentLine(::min)
        var layoutLatch = CountDownLatch(1)
        val offset = mutableStateOf(10.ipx)
        var measure = 0
        var layout = 0
        var linePosition: IntPx? = null
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to offset.value)) { }
                    }
                }
                Layout(child) { measurables, constraints, _ ->
                    ++measure
                    layout(1.ipx, 1.ipx) {
                        val placeable = measurables.first().measure(constraints)
                        linePosition = placeable[TestLine]
                        ++layout
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(1, layout)
        assertEquals(10.ipx, linePosition)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 20.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, measure)
        assertEquals(2, layout)
        assertEquals(20.ipx, linePosition)
    }

    @Test
    fun testAlignmentLines_onlyComputesAlignmentLinesWhenNeeded() {
        var layoutLatch = CountDownLatch(1)
        val offset = mutableStateOf(10.ipx)
        var alignmentLinesCalculations = 0
        val TestLine = VerticalAlignmentLine { _, _ ->
            ++alignmentLinesCalculations
            0.ipx
        }
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val innerChild = @Composable {
                    offset.value // Artificial remeasure.
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 10.ipx)) { }
                    }
                }
                val child = @Composable {
                    Layout({ innerChild(); innerChild() }) { measurables, constraints, _ ->
                        offset.value // Artificial remeasure.
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            placeable1.place(0.ipx, 0.ipx)
                            placeable2.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(child) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    if (offset.value < 15.ipx) {
                        placeable[TestLine]
                    }
                    layout(0.ipx, 0.ipx) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, alignmentLinesCalculations)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 20.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, alignmentLinesCalculations)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 10.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(2, alignmentLinesCalculations)
    }

    @Test
    fun testAlignmentLines_providedLinesOverrideInherited() {
        val layoutLatch = CountDownLatch(1)
        val TestLine = VerticalAlignmentLine(::min)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val innerChild = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 10.ipx)) { }
                    }
                }
                val child = @Composable {
                    Layout({ innerChild() }) { measurables, constraints, _ ->
                        val placeable = measurables.first().measure(constraints)
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 20.ipx)) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    }
                }
                Layout(child) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    assertEquals(20.ipx, placeable[TestLine])
                    layout(0.ipx, 0.ipx) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testAlignmentLines_areRecalculatedCorrectlyOnRelayout_withNoRemeasure() {
        val TestLine = VerticalAlignmentLine(::min)
        var layoutLatch = CountDownLatch(1)
        var innerChildMeasures = 0
        var innerChildLayouts = 0
        var outerChildMeasures = 0
        var outerChildLayouts = 0
        val offset = mutableStateOf(0.ipx)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, _, _ ->
                        ++innerChildMeasures
                        layout(0.ipx, 0.ipx, mapOf(TestLine to 10.ipx)) { ++innerChildLayouts }
                    }
                }
                val inner = @Composable {
                    Layout({ Wrap { Wrap { child() } } }) { measurables, constraints, _ ->
                        ++outerChildMeasures
                        val placeable = measurables[0].measure(constraints)
                        layout(0.ipx, 0.ipx) {
                            ++outerChildLayouts
                            placeable.place(offset.value, 0.ipx)
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    val width = placeable.width.coerceAtLeast(10.ipx)
                    val height = placeable.height.coerceAtLeast(10.ipx)
                    layout(width, height) {
                        assertEquals(offset.value + 10.ipx, placeable[TestLine])
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, innerChildMeasures)
        assertEquals(1, innerChildLayouts)
        assertEquals(1, outerChildMeasures)
        assertEquals(1, outerChildLayouts)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            offset.value = 10.ipx
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, innerChildMeasures)
        assertEquals(1, innerChildLayouts)
        assertEquals(1, outerChildMeasures)
        assertEquals(2, outerChildLayouts)
    }

    @Test
    fun testAlignmentLines_whenQueriedAfterPlacing() {
        val TestLine = VerticalAlignmentLine(::min)
        val layoutLatch = CountDownLatch(1)
        var childLayouts = 0
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, constraints, _ ->
                        layout(
                            constraints.minWidth,
                            constraints.minHeight,
                            mapOf(TestLine to 10.ipx)
                        ) { ++childLayouts }
                    }
                }
                val inner = @Composable {
                    Layout({ Wrap { Wrap { child() } } }) { measurables, constraints, _ ->
                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0.ipx, 0.ipx)
                            assertEquals(10.ipx, placeable[TestLine])
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        // Two layouts as the alignment line was only queried after the child was placed.
        assertEquals(2, childLayouts)
    }

    @Test
    fun testAlignmentLines_whenQueriedAfterPlacing_haveCorrectNumberOfLayouts() {
        val TestLine = VerticalAlignmentLine(::min)
        var layoutLatch = CountDownLatch(1)
        var childLayouts = 0
        val offset = mutableStateOf(10.ipx)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Layout(children = {}) { _, constraints, _ ->
                        layout(
                            constraints.minWidth,
                            constraints.minHeight,
                            mapOf(TestLine to 10.ipx)
                        ) {
                            offset.value // To ensure relayout.
                            ++childLayouts
                        }
                    }
                }
                val inner = @Composable {
                    Layout({
                        WrapForceRelayout(offset) { child() }
                    }) { measurables, constraints, _ ->
                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            if (offset.value > 15.ipx) assertEquals(10.ipx, placeable[TestLine])
                            placeable.place(0.ipx, 0.ipx)
                            if (offset.value > 5.ipx) assertEquals(10.ipx, placeable[TestLine])
                        }
                    }
                }
                Layout(inner) { measurables, constraints, _ ->
                    val placeable = measurables.first().measure(constraints)
                    val width = placeable.width.coerceAtLeast(10.ipx)
                    val height = placeable.height.coerceAtLeast(10.ipx)
                    layout(width, height) {
                        offset.value // To ensure relayout.
                        placeable.place(0.ipx, 0.ipx)
                        layoutLatch.countDown()
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        // Two layouts as the alignment line was only queried after the child was placed.
        assertEquals(2, childLayouts)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 12.ipx }
        assertTrue(layoutLatch.await(5, TimeUnit.SECONDS))
        // Just one more layout as the alignment lines were speculatively calculated this time.
        assertEquals(3, childLayouts)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 17.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        // One layout as the alignment lines are queried before.
        assertEquals(4, childLayouts)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 12.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        // One layout as the alignment lines are still calculated speculatively.
        assertEquals(5, childLayouts)

        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 1.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(6, childLayouts)
        layoutLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR { offset.value = 10.ipx }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        // Two layouts again, since alignment lines were not queried during last layout,
        // so we did not calculate them speculatively anymore.
        assertEquals(8, childLayouts)
    }

    @Test
    fun testAlignmentLines_readFromModifier_duringMeasurement() = with(density) {
        val testVerticalLine = VerticalAlignmentLine(::min)
        val testHorizontalLine = HorizontalAlignmentLine(::max)

        val assertLines: Modifier.(IntPx, IntPx) -> Modifier = { vertical, horizontal ->
            this + object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                    layoutDirection: LayoutDirection
                ): MeasureScope.MeasureResult {
                    val placeable = measurable.measure(constraints)
                    assertEquals(vertical, placeable[testVerticalLine])
                    assertEquals(horizontal, placeable[testHorizontalLine])
                    return layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }

        testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
    }

    @Test
    fun testAlignmentLines_readFromModifier_duringPositioning_before() = with(density) {
        val testVerticalLine = VerticalAlignmentLine(::min)
        val testHorizontalLine = HorizontalAlignmentLine(::max)

        val assertLines: Modifier.(IntPx, IntPx) -> Modifier = { vertical, horizontal ->
            this + object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                    layoutDirection: LayoutDirection
                ): MeasureScope.MeasureResult {
                    val placeable = measurable.measure(constraints)
                    return layout(placeable.width, placeable.height) {
                        assertEquals(vertical, placeable[testVerticalLine])
                        assertEquals(horizontal, placeable[testHorizontalLine])
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }

        testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
    }

    @Test
    fun testAlignmentLines_readFromModifier_duringPositioning_after() = with(density) {
        val testVerticalLine = VerticalAlignmentLine(::min)
        val testHorizontalLine = HorizontalAlignmentLine(::max)

        val assertLines: Modifier.(IntPx, IntPx) -> Modifier = { vertical, horizontal ->
            this + object : LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                    layoutDirection: LayoutDirection
                ): MeasureScope.MeasureResult {
                    val placeable = measurable.measure(constraints)
                    return layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                        assertEquals(vertical, placeable[testVerticalLine])
                        assertEquals(horizontal, placeable[testHorizontalLine])
                    }
                }
            }
        }

        testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
    }

    private fun Density.testAlignmentLinesReads(
        testVerticalLine: VerticalAlignmentLine,
        testHorizontalLine: HorizontalAlignmentLine,
        assertLines: Modifier.(IntPx, IntPx) -> Modifier
    ) {
        val layoutLatch = CountDownLatch(7)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val layout = @Composable { modifier: Modifier ->
                    Layout(modifier = modifier, children = {}) { _, _, _ ->
                        layout(
                            0.ipx,
                            0.ipx,
                            mapOf(
                                testVerticalLine to 10.ipx,
                                testHorizontalLine to 20.ipx
                            )
                        ) {
                            layoutLatch.countDown()
                        }
                    }
                }

                layout(Modifier.assertLines(10.ipx, 20.ipx))
                layout(Modifier.assertLines(30.ipx, 30.ipx).offset(20.ipx.toDp(), 10.ipx.toDp()))
                layout(Modifier
                    .assertLines(30.ipx, 30.ipx)
                    .drawLayer()
                    .offset(20.ipx.toDp(), 10.ipx.toDp())
                )
                layout(Modifier
                    .assertLines(30.ipx, 30.ipx)
                    .background(Color.Blue)
                    .drawLayer()
                    .offset(20.ipx.toDp(), 10.ipx.toDp())
                    .drawLayer()
                    .background(Color.Blue)
                )
                layout(Modifier
                    .background(Color.Blue)
                    .assertLines(30.ipx, 30.ipx)
                    .background(Color.Blue)
                    .drawLayer()
                    .offset(20.ipx.toDp(), 10.ipx.toDp())
                    .drawLayer()
                    .background(Color.Blue)
                )
                Wrap(
                    Modifier
                        .background(Color.Blue)
                        .assertLines(30.ipx, 30.ipx)
                        .background(Color.Blue)
                        .drawLayer()
                        .offset(20.ipx.toDp(), 10.ipx.toDp())
                        .drawLayer()
                        .background(Color.Blue)
                ) {
                    layout(Modifier)
                }
                Wrap(
                    Modifier
                        .background(Color.Blue)
                        .assertLines(40.ipx, 50.ipx)
                        .background(Color.Blue)
                        .drawLayer()
                        .offset(20.ipx.toDp(), 10.ipx.toDp())
                        .drawLayer()
                        .background(Color.Blue)
                ) {
                    layout(Modifier.offset(10.ipx.toDp(), 20.ipx.toDp()))
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testLayoutBeforeDraw_forRecomposingNodesNotAffectingRootSize() {
        val offset = mutableStateOf(0.ipx)
        var latch = CountDownLatch(1)
        var laidOut = false
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val container = @Composable { children: @Composable () -> Unit ->
                    // This simulates a Container optimisation, when the child does not
                    // affect parent size.
                    Layout(children) { measurables, constraints, _ ->
                        layout(30.ipx, 30.ipx) {
                            measurables[0].measure(constraints).place(0.ipx, 0.ipx)
                        }
                    }
                }
                val recomposingChild = @Composable { children: @Composable (IntPx) -> Unit ->
                    // This simulates a child that recomposes, for example due to a transition.
                    children(offset.value)
                }
                val assumeLayoutBeforeDraw = @Composable { _: IntPx ->
                    // This assumes a layout was done before the draw pass.
                    Layout(
                        children = {},
                        modifier = Modifier.drawBehind {
                            assertTrue(laidOut)
                            latch.countDown()
                        }
                    ) { _, _, _ ->
                        laidOut = true
                        layout(0.ipx, 0.ipx) {}
                    }
                }

                container {
                    recomposingChild {
                        assumeLayoutBeforeDraw(it)
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            offset.value = 10.ipx
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testDrawWithLayoutNotPlaced() {
        val latch = CountDownLatch(1)
        var drawn = false
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = {
                    AtLeastSize(30.ipx, modifier = Modifier.drawBehind { drawn = true })
                }, modifier = Modifier.drawLatchModifier()) { _, _, _ ->
                    // don't measure or place the AtLeastSize
                    latch.countDown()
                    layout(20.ipx, 20.ipx) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        activityTestRule.runOnUiThreadIR {
            assertFalse(drawn)
        }
    }

    /**
     * Because we use invalidate() to cause relayout when children
     * are laid out, we want to ensure that when the View is 0-sized
     * that it gets a relayout when it needs to change to non-0
     */
    @Test
    fun testZeroSizeCanRelayout() {
        var latch = CountDownLatch(1)
        val model = SquareModel(size = 0.ipx)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = { }) { _, _, _ ->
                    latch.countDown()
                    layout(model.size, model.size) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 10.ipx
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testZeroSizeCanRelayout_child() {
        var latch = CountDownLatch(1)
        val model = SquareModel(size = 0.ipx)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = {
                    Layout(children = emptyContent()) { _, _, _ ->
                        latch.countDown()
                        layout(model.size, model.size) {}
                    }
                }) { measurables, constraints, _ ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 10.ipx
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testZeroSizeCanRelayout_childRepaintBoundary() {
        var latch = CountDownLatch(1)
        val model = SquareModel(size = 0.ipx)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = {
                    Layout(modifier = Modifier.drawLayer(), children = emptyContent()) { _, _, _ ->
                        latch.countDown()
                        layout(model.size, model.size) {}
                    }
                }) { measurables, constraints, _ ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.size = 10.ipx
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun parentSizeForDrawIsProvidedWithoutPadding() {
        val latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val drawnContent = Modifier.drawBehind {
                    assertEquals(100.0f, size.width)
                    assertEquals(100.0f, size.height)
                    latch.countDown()
                }
                AtLeastSize(100.ipx, PaddingModifier(10.ipx) + drawnContent) {
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun parentSizeForDrawInsideRepaintBoundaryIsProvidedWithoutPadding() {
        val latch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(
                    100.ipx,
                    PaddingModifier(10.ipx).drawLayer()
                        .drawBehind {
                            assertEquals(100.0f, size.width)
                            assertEquals(100.0f, size.height)
                            latch.countDown()
                        }
                ) {
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun alignmentLinesInheritedCorrectlyByParents_withModifiedPosition() {
        val testLine = HorizontalAlignmentLine(::min)
        val latch = CountDownLatch(1)
        val alignmentLinePosition = 10.ipx
        val padding = 20.ipx
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val child = @Composable {
                    Wrap {
                        Layout(children = {}, modifier = PaddingModifier(padding)) { _, _, _ ->
                            layout(0.ipx, 0.ipx, mapOf(testLine to alignmentLinePosition)) { }
                        }
                    }
                }

                Layout(child) { measurables, constraints, _ ->
                    assertEquals(
                        padding + alignmentLinePosition,
                        measurables[0].measure(constraints)[testLine]
                    )
                    latch.countDown()
                    layout(0.ipx, 0.ipx) { }
                }
            }
        }
    }

    @Test
    fun modifiers_validateCorrectSizes() {
        val layoutModifier = object : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
                layoutDirection: LayoutDirection
            ): MeasureScope.MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.place(0.ipx, 0.ipx)
                }
            }
        }
        val parentDataModifier = object : ParentDataModifier {
            override fun Density.modifyParentData(parentData: Any?) = parentData
        }
        val size = 50.ipx

        val latch = CountDownLatch(2)
        val childSizes = arrayOfNulls<IntPxSize>(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    children = {
                        FixedSize(size, layoutModifier)
                        FixedSize(size, parentDataModifier)
                    },
                    measureBlock = { measurables, constraints, _ ->
                        for (i in 0 until measurables.size) {
                            val child = measurables[i]
                            val placeable = child.measure(constraints)
                            childSizes[i] = IntPxSize(placeable.width, placeable.height)
                            latch.countDown()
                        }
                        layout(0.ipx, 0.ipx) { }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntPxSize(size, size), childSizes[0])
        assertEquals(IntPxSize(size, size), childSizes[1])
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_drawPositioning() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(outerColor)) {
                    FixedSize(
                        10.ipx,
                        PaddingModifier(10.ipx).background(innerColor).drawLatchModifier()
                    )
                }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @Test
    fun drawModifier_afterRtlModifier_testLayoutDirection() {
        val drawLatch = CountDownLatch(1)
        val layoutDirection = Ref<LayoutDirection>()
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(
                    50.ipx,
                    Modifier.rtl.drawBehind {
                        layoutDirection.value = this.layoutDirection
                        drawLatch.countDown()
                    }
                )
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, layoutDirection.value)
    }

    @Test
    fun drawModifier_beforeRtlModifiers_testLayoutDirection() {
        val drawLatch = CountDownLatch(1)
        val layoutDirection = Ref<LayoutDirection>()

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(
                    50.ipx,
                    Modifier.drawBehind {
                        layoutDirection.value = this.layoutDirection
                        drawLatch.countDown()
                    }.ltr.rtl
                )
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, layoutDirection.value)
    }

    @Test
    fun drawModifier_betweenRtlModifiers_testLayoutDirection() {
        val drawLatch = CountDownLatch(1)
        val layoutDirection = Ref<LayoutDirection>()

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(
                    50.ipx,
                    Modifier.rtl.ltr.drawBehind {
                        layoutDirection.value = this.layoutDirection
                        drawLatch.countDown()
                    }.padding(15.dp).rtl
                )
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, layoutDirection.value)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_modelChangesOnRoot() {
        val model = SquareModel(innerColor = Color.White, outerColor = Color.Green)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(model, false)) {
                    FixedSize(
                        10.ipx,
                        PaddingModifier(10.ipx).background(model, true).drawLatchModifier()
                    )
                }
            }
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.White, size = 10)
        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.innerColor = Color.Yellow
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_modelChangesOnRepaintBoundary() {
        val model = SquareModel(innerColor = Color.White, outerColor = Color.Green)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(Color.Green)) {
                    FixedSize(
                        10.ipx,
                        Modifier.drawLayer()
                            .plus(PaddingModifier(10.ipx))
                            .background(model, true)
                            .drawLatchModifier()
                    )
                }
            }
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.White, size = 10)
        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            model.innerColor = Color.Yellow
        }
        validateSquareColors(outerColor = Color.Green, innerColor = Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_oneModifier() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val colorModifier = Modifier.drawBehind {
                    drawRect(outerColor)
                    drawRect(
                        innerColor,
                        topLeft = Offset(10f, 10f),
                        size = Size(10f, 10f)
                    )
                    drawLatch.countDown()
                }
                FixedSize(30.ipx, colorModifier)
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_nestedModifiers() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val countDownModifier = Modifier.drawBehind {
                    drawLatch.countDown()
                }
                FixedSize(30.ipx, countDownModifier.background(color = outerColor)) {
                    Padding(10.ipx) {
                        FixedSize(10.ipx, Modifier.background(color = innerColor))
                    }
                }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_withLayoutModifier() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(color = outerColor)) {
                    FixedSize(
                        size = 10.ipx,
                        modifier = PaddingModifier(10.ipx)
                            .background(color = innerColor)
                            .drawLatchModifier()
                    )
                }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawModifier_withLayout() {
        val outerColor = Color.Blue
        val innerColor = Color.White
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val drawAndOffset = Modifier.drawWithContent {
                    drawRect(outerColor)
                    translate(10f, 10f) {
                        this@drawWithContent.drawContent()
                    }
                }
                FixedSize(30.ipx, drawAndOffset) {
                    FixedSize(
                        size = 10.ipx,
                        modifier = AlignTopLeft.background(innerColor).drawLatchModifier()
                    )
                }
            }
        }
        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layoutModifier_redrawsCorrectlyWhenOnlyNonModifiedSizeChanges() {
        val blue = Color(0xFF000080)
        val green = Color(0xFF00FF00)
        val offset = mutableStateOf(10.ipx)

        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, modifier = Modifier.drawBehind {
                    drawRect(green)
                }) {
                    FixedSize(
                        offset.value,
                        modifier = AlignTopLeft.drawLayer()
                            .drawBehind {
                                drawLatch.countDown()
                                drawRect(blue)
                            }
                    ) {
                    }
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = blue, size = 10, offset = -10)

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            offset.value = 20.ipx
        }
        validateSquareColors(
            outerColor = green,
            innerColor = blue,
            size = 20,
            offset = -5,
            totalSize = 30
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun modifier_combinedModifiers() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(Color.Blue).drawLatchModifier()) {
                    JustConstraints(CombinedModifier(Color.White)) {
                    }
                }
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }

    // Tests that show layout bounds draws outlines around content and modifiers
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun showLayoutBounds_content() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(size = 30.ipx, modifier = Modifier.background(Color.White)) {
                    FixedSize(
                        size = 10.ipx,
                        modifier = PaddingModifier(5.ipx)
                            .plus(PaddingModifier(5.ipx))
                            .drawLatchModifier()
                    )
                }
            }
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val owner = content.getChildAt(0) as Owner
            owner.showLayoutBounds = true
        }
        activityTestRule.waitAndScreenShot().apply {
            assertRect(Color.White, size = 8)
            assertRect(Color.Red, size = 10, holeSize = 8)
            assertRect(Color.White, size = 18, holeSize = 10)
            assertRect(Color.Blue, size = 20, holeSize = 18)
            assertRect(Color.White, size = 28, holeSize = 20)
            assertRect(Color.Red, size = 30, holeSize = 28)
        }
    }

    @Test
    fun requestRemeasureForAlreadyMeasuredChildWhileTheParentIsStillMeasuring() {
        val drawlatch = CountDownLatch(1)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Layout(children = {
                    val state = state { false }
                    var lastLayoutValue: Boolean = false
                    Layout(children = {}, modifier = Modifier.drawBehind {
                        // this verifies the layout was remeasured before being drawn
                        assertTrue(lastLayoutValue)
                        drawlatch.countDown()
                    }) { _, _, _ ->
                        lastLayoutValue = state.value
                        // this registers the value read
                        if (!state.value) {
                            // change the value right inside the measure block
                            // it will cause one more remeasure pass as we also read this value
                            state.value = true
                        }
                        layout(100.ipx, 100.ipx) {}
                    }
                    FixedSize(30.ipx, children = emptyContent())
                }) { measurables, constraints, _ ->
                    val (first, second) = measurables
                    val firstPlaceable = first.measure(constraints)
                    // switch frame, as inside the measure block we changed the model value
                    // this will trigger requestRemeasure on this first layout
                    FrameManager.nextFrame()
                    val secondPlaceable = second.measure(constraints)
                    layout(30.ipx, 30.ipx) {
                        firstPlaceable.place(0.ipx, 0.ipx)
                        secondPlaceable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }
        assertTrue(drawlatch.await(1, TimeUnit.SECONDS))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_scaleDraw() {
        activityTestRule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30.ipx,
                    modifier = Modifier.background(Color.Blue)
                ) {
                    FixedSize(
                        size = 20.ipx,
                        modifier = AlignTopLeft
                            .plus(PaddingModifier(5.ipx))
                            .scale(0.5f)
                            .background(Color.Red)
                            .latch(drawLatch)
                    ) {}
                }
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_scaleChange() {
        val scale = mutableStateOf(1f)
        val layerModifier = object : DrawLayerModifier {
            override val scaleX: Float get() = scale.value
            override val scaleY: Float get() = scale.value
        }
        activityTestRule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30.ipx,
                    modifier = Modifier.background(Color.Blue)
                ) {
                    FixedSize(
                        size = 10.ipx,
                        modifier = PaddingModifier(10.ipx)
                            .plus(layerModifier)
                            .background(Color.Red)
                            .latch(drawLatch)
                    ) {}
                }
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)

        activityTestRule.runOnUiThread {
            scale.value = 2f
        }

        activityTestRule.waitAndScreenShot().apply {
            assertRect(Color.Red, size = 20, centerX = 15, centerY = 15)
        }
    }

    // Test that when no clip to outline is set that it still draws properly.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun layerModifier_noClip() {
        val triangleShape = object : Shape {
            override fun createOutline(size: Size, density: Density): Outline =
                Outline.Generic(
                    Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                )
        }
        activityTestRule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30.ipx
                ) {
                    FixedSize(
                        size = 10.ipx,
                        modifier = PaddingModifier(10.ipx)
                            .drawLayer(shape = triangleShape)
                            .drawBehind {
                                drawRect(
                                    Color.Blue,
                                    topLeft = Offset(-10f, -10f),
                                    size = Size(30.0f, 30.0f)
                                )
                            }
                            .background(Color.Red)
                            .latch(drawLatch)
                    ) {}
                }
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testInvalidationMultipleLayers() {
        val innerColor = mutableStateOf(Color.Red)
        activityTestRule.runOnUiThread {
            activity.setContent {
                val children: @Composable () -> Unit = remember {
                    @Composable {
                        FixedSize(
                            size = 10.ipx,
                            modifier = Modifier.drawLayer()
                                .plus(PaddingModifier(10.ipx))
                                .background(innerColor.value)
                                .latch(drawLatch)
                        ) {}
                    }
                }
                FixedSize(
                    size = 30.ipx,
                    modifier = Modifier.drawLayer().background(Color.Blue)
                ) {
                    FixedSize(
                        size = 30.ipx,
                        modifier = Modifier.drawLayer(),
                        children = children
                    )
                }
            }
        }
        validateSquareColors(outerColor = Color.Blue, innerColor = Color.Red, size = 10)

        drawLatch = CountDownLatch(1)

        activityTestRule.runOnUiThread {
            innerColor.value = Color.White
        }

        validateSquareColors(outerColor = Color.Blue, innerColor = Color.White, size = 10)
    }

    @Test
    fun doubleDraw() {
        val offset = mutableStateOf(0.ipx)
        var outerLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    30.ipx,
                    Modifier.drawBehind { outerLatch.countDown() }.drawLayer()
                ) {
                    FixedSize(10.ipx, Modifier.drawBehind {
                        drawLine(
                            Color.Blue,
                            Offset(offset.value.value.toFloat(), 0f),
                            Offset(0f, offset.value.value.toFloat()),
                            stroke = Stroke(width = 0.0f) // 0.0f represents hairline stroke
                        )
                        drawLatch.countDown()
                    })
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(outerLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            drawLatch = CountDownLatch(1)
            outerLatch = CountDownLatch(1)
            offset.value = 10.ipx
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertFalse(outerLatch.await(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun passThroughLayout_passesThroughParentData() {
        val latch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            activity.setContent {
                Layout({
                    PassThroughLayout {
                        FixedSize(50.ipx, Modifier.tag("1"))
                    }
                }) { measurables, constraints, _ ->
                    assertEquals("1", measurables[0].tag)
                    val placeable = measurables[0].measure(constraints)
                    assertEquals(50.ipx, placeable.width)
                    latch.countDown()
                    layout(0.ipx, 0.ipx) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    // When a child with a layer is removed with its children, it shouldn't crash.
    @Test
    fun detachChildWithLayer() {
        activityTestRule.runOnUiThread {
            val composition = activity.setContent {
                FixedSize(10.ipx, Modifier.drawLayer()) {
                    FixedSize(8.ipx)
                }
            }

            val composeView = activityTestRule.findAndroidComposeView()
            composeView.restoreHierarchyState(SparseArray())
            composition.dispose()
        }
    }

    // When a layer moves, it should redraw properly
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawOnLayerMove() {
        val offset = mutableStateOf(10.ipx)
        var placeLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            activity.setContent {
                val yellowSquare = @Composable {
                    FixedSize(
                        10.ipx, Modifier.drawLayer().background(Color.Yellow).drawLatchModifier()
                    ) {
                    }
                }
                Layout(
                    modifier = Modifier.background(Color.Red),
                    children = yellowSquare
                ) { measurables, _, _ ->
                    val childConstraints = Constraints.fixed(10.ipx, 10.ipx)
                    val p = measurables[0].measure(childConstraints)
                    layout(30.ipx, 30.ipx) {
                        p.place(offset.value, offset.value)
                        placeLatch.countDown()
                    }
                }
            }
        }

        validateSquareColors(outerColor = Color.Red, innerColor = Color.Yellow, size = 10)

        placeLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            offset.value = 5.ipx
        }

        // Wait for layout to complete
        assertTrue(placeLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
        }

        activityTestRule.waitAndScreenShot(forceInvalidate = false).apply {
            // just test that it is red around the Yellow
            assertRect(Color.Red, size = 20, centerX = 10, centerY = 10, holeSize = 10)
            // now test that it is red in the lower-right
            assertRect(Color.Red, size = 10, centerX = 25, centerY = 25)
            assertRect(Color.Yellow, size = 10, centerX = 10, centerY = 10)
        }
    }

    // When a layer property changes, it should redraw properly
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawOnLayerPropertyChange() {
        val offset = mutableStateOf(0f)
        var translationLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            activity.setContent {
                FixedSize(30.ipx, Modifier.background(Color.Red).drawLatchModifier()) {
                    FixedSize(
                        10.ipx,
                        PaddingModifier(10.ipx).plus(
                            object : DrawLayerModifier {
                                override val translationX: Float
                                    get() {
                                        translationLatch.countDown()
                                        return offset.value
                                    }
                                override val translationY: Float
                                        get() = offset.value
                            }
                        ).background(Color.Yellow)
                    ) {
                    }
                }
            }
        }

        validateSquareColors(outerColor = Color.Red, innerColor = Color.Yellow, size = 10)

        translationLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            offset.value = -5f
        }
        // Wait for translation to complete
        assertTrue(translationLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
        }

        activityTestRule.waitAndScreenShot(forceInvalidate = false).apply {
            // just test that it is red around the Yellow
            assertRect(Color.Red, size = 20, centerX = 10, centerY = 10, holeSize = 10)
            // now test that it is red in the lower-right
            assertRect(Color.Red, size = 10, centerX = 25, centerY = 25)
            assertRect(Color.Yellow, size = 10, centerX = 10, centerY = 10)
        }
    }

    private fun composeSquares(model: SquareModel) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(size = model.size, modifier = Modifier.drawBehind {
                    drawRect(model.outerColor)
                }) {
                    AtLeastSize(size = model.size, modifier = Modifier.drawBehind {
                        drawLatch.countDown()
                        drawRect(model.innerColor)
                    })
                }
            }
        }
    }

    private fun composeSquaresWithNestedRepaintBoundaries(model: SquareModel) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(
                    size = model.size,
                    modifier = fillColor(model, isInner = false, doCountDown = false).drawLayer()
                ) {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.drawLayer() + fillColor(model, isInner = true)
                    ) {
                    }
                }
            }
        }
    }

    private fun composeMovingSquaresWithRepaintBoundary(model: SquareModel, offset: State<IntPx>) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Position(
                    size = model.size * 3,
                    offset = offset,
                    modifier = fillColor(model, isInner = false, doCountDown = false)
                ) {
                    AtLeastSize(
                        size = model.size,
                        modifier = Modifier.drawLayer() + fillColor(model, isInner = true)
                    ) {
                    }
                }
            }
        }
    }

    private fun composeMovingSquares(model: SquareModel, offset: State<IntPx>) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Position(
                    size = model.size * 3,
                    offset = offset,
                    modifier = fillColor(model, isInner = false, doCountDown = false)
                ) {
                    AtLeastSize(size = model.size, modifier = fillColor(model, isInner = true)) {
                    }
                }
            }
        }
    }

    private fun composeNestedSquares(model: SquareModel) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                val fillColorModifier = Modifier.drawBehind {
                    drawRect(model.innerColor)
                    drawLatch.countDown()
                }
                val innerDrawWithContentModifier = drawWithContent {
                    drawRect(model.outerColor)
                    val start = model.size.value.toFloat()
                    val end = start * 2
                    clipRect(start, start, end, end) {
                        this@drawWithContent.drawContent()
                    }
                }
                AtLeastSize(size = (model.size * 3), modifier = innerDrawWithContentModifier) {
                    AtLeastSize(size = (model.size * 3), modifier = fillColorModifier)
                }
            }
        }
    }

    private fun validateSquareColors(
        outerColor: Color,
        innerColor: Color,
        size: Int,
        offset: Int = 0,
        totalSize: Int = size * 3
    ) {
        activityTestRule.validateSquareColors(
            drawLatch,
            outerColor,
            innerColor,
            size,
            offset,
            totalSize
        )
    }

    @Composable
    private fun fillColor(color: Color, doCountDown: Boolean = true): Modifier =
        Modifier.drawBehind {
            drawRect(color)
            if (doCountDown) {
                drawLatch.countDown()
            }
        }

    @Composable
    private fun fillColor(
        squareModel: SquareModel,
        isInner: Boolean,
        doCountDown: Boolean = true
    ): Modifier = Modifier.drawBehind {
        drawRect(if (isInner) squareModel.innerColor else squareModel.outerColor)
        if (doCountDown) {
            drawLatch.countDown()
        }
    }

    private var positionLatch: CountDownLatch? = null

    @Composable
    fun Position(
        size: IntPx,
        offset: State<IntPx>,
        modifier: Modifier = Modifier,
        children: @Composable () -> Unit
    ) {
        Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
            val placeables = measurables.map { m ->
                m.measure(constraints)
            }
            layout(size, size) {
                placeables.forEach { child ->
                    child.place(offset.value, offset.value)
                }
                positionLatch?.countDown()
            }
        }
    }

    fun Modifier.drawLatchModifier() = drawBehind { drawLatch.countDown() }
}

fun Bitmap.assertRect(
    color: Color,
    holeSize: Int = 0,
    size: Int = width,
    centerX: Int = width / 2,
    centerY: Int = height / 2
) {
    assertTrue(centerX + size / 2 <= width)
    assertTrue(centerX - size / 2 >= 0)
    assertTrue(centerY + size / 2 <= height)
    assertTrue(centerY - size / 2 >= 0)
    val halfHoleSize = holeSize / 2
    for (x in centerX - size / 2 until centerX + size / 2) {
        for (y in centerY - size / 2 until centerY + size / 2) {
            if (abs(x - centerX) > halfHoleSize &&
                abs(y - centerY) > halfHoleSize
            ) {
                val currentColor = Color(getPixel(x, y))
                assertColorsEqual(color, currentColor)
            }
        }
    }
}

fun ActivityTestRule<*>.validateSquareColors(
    drawLatch: CountDownLatch,
    outerColor: Color,
    innerColor: Color,
    size: Int,
    offset: Int = 0,
    totalSize: Int = size * 3
) {
    assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    val bitmap = waitAndScreenShot()
    assertEquals(totalSize, bitmap.width)
    assertEquals(totalSize, bitmap.height)
    val squareStart = (totalSize - size) / 2 + offset
    val squareEnd = totalSize - ((totalSize - size) / 2) + offset
    for (x in 0 until totalSize) {
        for (y in 0 until totalSize) {
            val pixel = Color(bitmap.getPixel(x, y))
            val expected =
                if (!(x < squareStart || x >= squareEnd || y < squareStart || y >= squareEnd)) {
                    innerColor
                } else {
                    outerColor
                }
            assertColorsEqual(expected, pixel) {
                "Pixel within drawn rect[$x, $y] is $expected, but was $pixel"
            }
        }
    }
}

fun assertColorsEqual(
    expected: Color,
    color: Color,
    error: () -> String = { "$expected and $color are not similar!" }
) {
    val errorString = error()
    assertEquals(errorString, expected.red, color.red, 0.01f)
    assertEquals(errorString, expected.green, color.green, 0.01f)
    assertEquals(errorString, expected.blue, color.blue, 0.01f)
    assertEquals(errorString, expected.alpha, color.alpha, 0.01f)
}

@Composable
fun AtLeastSize(
    size: IntPx,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit = emptyContent()
) {
    Layout(
        measureBlock = { measurables, constraints, _ ->
            val newConstraints = Constraints(
                minWidth = max(size, constraints.minWidth),
                maxWidth = max(size, constraints.maxWidth),
                minHeight = max(size, constraints.minHeight),
                maxHeight = max(size, constraints.maxHeight)
            )
            val placeables = measurables.map { m ->
                m.measure(newConstraints)
            }
            var maxWidth = size
            var maxHeight = size
            placeables.forEach { child ->
                maxHeight = max(child.height, maxHeight)
                maxWidth = max(child.width, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child ->
                    child.place(0.ipx, 0.ipx)
                }
            }
        },
        modifier = modifier,
        children = children
    )
}

@Composable
fun FixedSize(
    size: IntPx,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit = emptyContent()
) {
    Layout(children = children, modifier = modifier) { measurables, _, _ ->
        val newConstraints = Constraints.fixed(size, size)
        val placeables = measurables.map { m ->
            m.measure(newConstraints)
        }
        layout(size, size) {
            placeables.forEach { child ->
                child.place(0.ipx, 0.ipx)
            }
        }
    }
}

@Composable
fun Align(modifier: Modifier = Modifier.None, children: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        measureBlock = { measurables, constraints, _ ->
            val newConstraints = Constraints(
                minWidth = IntPx.Zero,
                maxWidth = constraints.maxWidth,
                minHeight = IntPx.Zero,
                maxHeight = constraints.maxHeight
            )
            val placeables = measurables.map { m ->
                m.measure(newConstraints)
            }
            var maxWidth = constraints.minWidth
            var maxHeight = constraints.minHeight
            placeables.forEach { child ->
                maxHeight = max(child.height, maxHeight)
                maxWidth = max(child.width, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child ->
                    child.place(0.ipx, 0.ipx)
                }
            }
        }, children = children
    )
}

@Composable
internal fun Padding(
    size: IntPx,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        measureBlock = { measurables, constraints, _ ->
            val totalDiff = size * 2
            val newConstraints = Constraints(
                minWidth = (constraints.minWidth - totalDiff).coerceAtLeast(0.ipx),
                maxWidth = (constraints.maxWidth - totalDiff).coerceAtLeast(0.ipx),
                minHeight = (constraints.minHeight - totalDiff).coerceAtLeast(0.ipx),
                maxHeight = (constraints.maxHeight - totalDiff).coerceAtLeast(0.ipx)
            )
            val placeables = measurables.map { m ->
                m.measure(newConstraints)
            }
            var maxWidth = size
            var maxHeight = size
            placeables.forEach { child ->
                maxHeight = max(child.height + totalDiff, maxHeight)
                maxWidth = max(child.width + totalDiff, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child ->
                    child.place(size, size)
                }
            }
        }, children = children
    )
}

@Composable
fun TwoMeasureLayout(
    size: IntPx,
    latch: CountDownLatch,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    Layout(modifier = modifier, children = children) { measurables, _, _ ->
        val testConstraints = Constraints()
        measurables.forEach { it.measure(testConstraints) }
        val childConstraints = Constraints.fixed(size, size)
        try {
            val placeables2 = measurables.map { it.measure(childConstraints) }
            fail("Measuring twice on the same Measurable should throw an exception")
            layout(size, size) {
                placeables2.forEach { child ->
                    child.place(0.ipx, 0.ipx)
                }
            }
        } catch (_: IllegalStateException) {
            // expected
            latch.countDown()
        }
        layout(0.ipx, 0.ipx) { }
    }
}

@Composable
fun Wrap(
    modifier: Modifier = Modifier,
    minWidth: IntPx = 0.ipx,
    minHeight: IntPx = 0.ipx,
    children: @Composable () -> Unit = {}
) {
    Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = max(placeables.maxBy { it.width.value }?.width ?: 0.ipx, minWidth)
        val height = max(placeables.maxBy { it.height.value }?.height ?: 0.ipx, minHeight)
        layout(width, height) {
            placeables.forEach { it.place(0.ipx, 0.ipx) }
        }
    }
}

@Composable
fun Scroller(
    modifier: Modifier = Modifier,
    onScrollPositionChanged: (position: IntPx, maxPosition: IntPx) -> Unit,
    offset: State<IntPx>,
    child: @Composable () -> Unit
) {
    val maxPosition = state { IntPx.Infinity }
    ScrollerLayout(
        modifier = modifier,
        maxPosition = maxPosition.value,
        onMaxPositionChanged = {
            maxPosition.value = 0.ipx
            onScrollPositionChanged(offset.value, 0.ipx)
        },
        child = child
    )
}

@Composable
private fun ScrollerLayout(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") maxPosition: IntPx,
    onMaxPositionChanged: () -> Unit,
    child: @Composable () -> Unit
) {
    Layout(modifier = modifier, children = child) { measurables, constraints, _ ->
        val childConstraints = constraints.copy(
            maxHeight = constraints.maxHeight,
            maxWidth = IntPx.Infinity
        )
        val childMeasurable = measurables.first()
        val placeable = childMeasurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)
        layout(width, placeable.height) {
            onMaxPositionChanged()
            placeable.place(0.ipx, 0.ipx)
        }
    }
}

@Composable
fun WrapForceRelayout(
    model: State<IntPx>,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxBy { it.width.value }?.width ?: 0.ipx
        val height = placeables.maxBy { it.height.value }?.height ?: 0.ipx
        layout(width, height) {
            model.value
            placeables.forEach { it.place(0.ipx, 0.ipx) }
        }
    }
}

@Composable
fun SimpleRow(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
        var width = 0.ipx
        var height = 0.ipx
        val placeables = measurables.map {
            it.measure(constraints.copy(maxWidth = constraints.maxWidth - width)).also {
                width += it.width
                height = max(height, it.height)
            }
        }
        layout(width, height) {
            var currentWidth = 0.ipx
            placeables.forEach {
                it.place(currentWidth, 0.ipx)
                currentWidth += it.width
            }
        }
    }
}

@Composable
fun JustConstraints(modifier: Modifier, children: @Composable () -> Unit) {
    Layout(children, modifier) { _, constraints, _ ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

class DrawCounterListener(private val view: View) :
    ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
    }
}

fun PaddingModifier(padding: IntPx) = PaddingModifier(padding, padding, padding, padding)

data class PaddingModifier(
    val left: IntPx = 0.ipx,
    val top: IntPx = 0.ipx,
    val right: IntPx = 0.ipx,
    val bottom: IntPx = 0.ipx
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(
            constraints.offset(
                horizontal = -left - right,
                vertical = -top - bottom
            )
        )
        return layout(
            (left + placeable.width + right).coerceIn(constraints.minWidth, constraints.maxWidth),
            (top + placeable.height + bottom).coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            placeable.place(left, top)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = measurable.minIntrinsicWidth((height - (top + bottom)).coerceAtLeast(0.ipx)) +
            (left + right)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = measurable.maxIntrinsicWidth((height - (top + bottom)).coerceAtLeast(0.ipx)) +
            (left + right)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = measurable.minIntrinsicHeight((width - (left + right)).coerceAtLeast(0.ipx)) +
            (top + bottom)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: IntPx,
        layoutDirection: LayoutDirection
    ): IntPx = measurable.maxIntrinsicHeight((width - (left + right)).coerceAtLeast(0.ipx)) +
            (top + bottom)
}

internal val AlignTopLeft = object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx))
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0.ipx, 0.ipx)
        }
    }
}

@Stable
class SquareModel(
    size: IntPx = 10.ipx,
    outerColor: Color = Color(0xFF000080),
    innerColor: Color = Color(0xFFFFFFFF)
) {
    var size: IntPx by mutableStateOf(size)
    var outerColor: Color by mutableStateOf(outerColor)
    var innerColor: Color by mutableStateOf(innerColor)
}

// We only need this because IR compiler doesn't like converting lambdas to Runnables
fun ActivityTestRule<*>.runOnUiThreadIR(block: () -> Unit) {
    val runnable: Runnable = object : Runnable {
        override fun run() {
            block()
        }
    }
    runOnUiThread(runnable)
}

fun ActivityTestRule<*>.findAndroidComposeView(): ViewGroup {
    val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
    return findAndroidComposeView(contentViewGroup)!!
}

fun findAndroidComposeView(parent: ViewGroup): ViewGroup? {
    for (index in 0 until parent.childCount) {
        val child = parent.getChildAt(index)
        if (child is ViewGroup) {
            if (child is Owner)
                return child
            else {
                val composeView = findAndroidComposeView(child)
                if (composeView != null) {
                    return composeView
                }
            }
        }
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.O)
fun ActivityTestRule<*>.waitAndScreenShot(forceInvalidate: Boolean = true): Bitmap {
    val view = findAndroidComposeView()
    val flushListener = DrawCounterListener(view)
    val offset = intArrayOf(0, 0)
    var handler: Handler? = null
    runOnUiThread {
        view.getLocationInWindow(offset)
        if (forceInvalidate) {
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }
        handler = Handler()
    }

    if (forceInvalidate) {
        assertTrue(flushListener.latch.await(1, TimeUnit.SECONDS))
    }
    val width = view.width
    val height = view.height

    val dest =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val srcRect = android.graphics.Rect(0, 0, width, height)
    srcRect.offset(offset[0], offset[1])
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = object : PixelCopy.OnPixelCopyFinishedListener {
        override fun onPixelCopyFinished(result: Int) {
            copyResult = result
            latch.countDown()
        }
    }
    PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler!!)
    assertTrue(latch.await(1, TimeUnit.SECONDS))
    assertEquals(PixelCopy.SUCCESS, copyResult)
    return dest
}

fun Modifier.background(color: Color) = drawBehind {
    drawRect(color)
}

fun Modifier.background(model: SquareModel, isInner: Boolean) = drawBehind {
    drawRect(if (isInner) model.innerColor else model.outerColor)
}

class CombinedModifier(val color: Color) : LayoutModifier, DrawModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(Constraints.fixed(10.ipx, 10.ipx))
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                (constraints.maxWidth - placeable.width) / 2,
                (constraints.maxHeight - placeable.height) / 2
            )
        }
    }

    override fun ContentDrawScope.draw() {
        drawRect(color)
    }
}

fun Modifier.scale(scale: Float) = plus(LayoutScale(scale))
    .drawLayer(scaleX = scale, scaleY = scale)

class LayoutScale(val scale: Float) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minWidth / scale,
                minHeight = constraints.minHeight / scale,
                maxWidth = constraints.maxWidth / scale,
                maxHeight = constraints.maxHeight / scale
            )
        )
        return layout(placeable.width * scale, placeable.height * scale) {
            placeable.place(0.ipx, 0.ipx)
        }
    }
}

fun Modifier.latch(countDownLatch: CountDownLatch) = drawBehind {
    countDownLatch.countDown()
}
