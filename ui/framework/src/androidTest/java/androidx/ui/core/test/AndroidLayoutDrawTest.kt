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
package androidx.ui.core.test

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.ParentData
import androidx.ui.core.Ref
import androidx.ui.core.WithConstraints
import androidx.ui.core.coerceAtLeast
import androidx.ui.core.coerceIn
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.setContent
import androidx.test.filters.SdkSuppress
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Density
import androidx.ui.core.DensityAmbient
import androidx.ui.core.RepaintBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Corresponds to ContainingViewTest, but tests single composition measure, layout and draw.
 * It also tests that layouts with both Layout and MeasureBox work.
 * TODO(popam): remove this comment and ContainingViewTest when ComplexMeasureBox is removed
 */
@SmallTest
@RunWith(JUnit4::class)
class AndroidLayoutDrawTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity
    private lateinit var handler: Handler
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        runOnUiThread { handler = Handler() }
        drawLatch = CountDownLatch(1)
    }

    // Tests that simple drawing works with layered squares
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val yellow = Color(0xFFFFFF00.toInt())
        val red = Color(0xFF800000.toInt())
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10.ipx)
        composeSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    // Tests that simple drawing works with draw with nested children
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun nestedDrawTest() {
        val yellow = Color(0xFFFFFF00.toInt())
        val red = Color(0xFF800000.toInt())
        val model = SquareModel(outerColor = yellow, innerColor = red, size = 10.ipx)
        composeNestedSquares(model)

        validateSquareColors(outerColor = yellow, innerColor = red, size = 10)
    }

    // Tests that recomposition works with models used within Draw components
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeDrawTest() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        val red = Color(0xFF800000.toInt())
        val yellow = Color(0xFFFFFF00.toInt())
        runOnUiThread {
            model.outerColor = red
            model.innerColor = yellow
        }

        validateSquareColors(outerColor = red, innerColor = yellow, size = 10)
    }

    // Tests that recomposition of nested repaint boundaries work
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeNestedRepaintBoundariesColorChange() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        val yellow = Color(0xFFFFFF00.toInt())
        runOnUiThread {
            model.innerColor = yellow
        }

        validateSquareColors(outerColor = blue, innerColor = yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeNestedRepaintBoundariesSizeChange() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquaresWithNestedRepaintBoundaries(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        runOnUiThread {
            model.size = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    // When there is a repaint boundary around a moving child, the child move
    // should be reflected in the repainted bitmap
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeRepaintBoundariesMove() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        var offset = OffsetModel(10.ipx)
        composeMovingSquaresWithRepaintBoundary(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        runOnUiThread {
            // there isn't going to be a normal draw because we are just moving the repaint
            // boundary, but we should have a draw cycle
            findAndroidCraneView().viewTreeObserver.addOnDrawListener(object :
                ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    drawLatch.countDown()
                }
            })
            offset.offset = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    // When there is no repaint boundary around a moving child, the child move
    // should be reflected in the repainted bitmap
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeMove() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        var offset = OffsetModel(10.ipx)
        composeMovingSquares(model, offset)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        runOnUiThread {
            // there isn't going to be a normal draw because we are just moving the repaint
            // boundary, but we should have a draw cycle
            findAndroidCraneView().viewTreeObserver.addOnDrawListener(object :
                ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    drawLatch.countDown()
                }
            })
            offset.offset = 20.ipx
        }

        validateSquareColors(outerColor = blue, innerColor = white, offset = 10, size = 10)
    }

    // Tests that recomposition works with models used within Layout components
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun recomposeSizeTest() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        composeSquares(model)
        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        drawLatch = CountDownLatch(1)
        runOnUiThread { model.size = 20.ipx }
        validateSquareColors(outerColor = blue, innerColor = white, size = 20)
    }

    // The size and color are both changed in a simpler single-color square.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleSquareColorAndSizeTest() {
        val green = Color(0xFF00FF00.toInt())
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = green)

        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Padding(size = (model.size * 3)) {
                        Draw { canvas, parentSize ->
                            val paint = Paint().apply {
                                color = model.outerColor
                            }
                            canvas.drawRect(parentSize.toRect(), paint)
                            drawLatch.countDown()
                        }
                    }
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)

        drawLatch = CountDownLatch(1)
        runOnUiThread {
            model.size = 30.ipx
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 30)

        drawLatch = CountDownLatch(1)
        val blue = Color(0xFF0000FF.toInt())

        runOnUiThread {
            model.innerColor = blue
            model.outerColor = blue
        }
        validateSquareColors(outerColor = blue, innerColor = blue, size = 30)
    }

    // Components that aren't placed shouldn't be drawn.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun noPlaceNoDraw() {
        val green = Color(0xFF00FF00.toInt())
        val white = Color(0xFFFFFFFF.toInt())
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = white)

        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Layout(children = {
                        Padding(size = (model.size * 3)) {
                            Draw { canvas, parentSize ->
                                drawLatch.countDown()
                                val paint = Paint()
                                paint.color = model.outerColor
                                canvas.drawRect(parentSize.toRect(), paint)
                                drawLatch.countDown()
                            }
                        }
                        Padding(size = model.size) {
                            Draw { canvas, parentSize ->
                                drawLatch.countDown()
                                val paint = Paint()
                                paint.color = model.innerColor
                                canvas.drawRect(parentSize.toRect(), paint)
                            }
                        }
                    }, layoutBlock = { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }
                        layout(placeables[0].width, placeables[0].height) {
                            placeables[0].place(0.ipx, 0.ipx)
                        }
                    })
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = green, size = 20)
    }

    // Make sure that draws intersperse properly with sub-layouts
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawOrderWithChildren() {
        val green = Color(0xFF00FF00.toInt())
        val white = Color(0xFFFFFFFF.toInt())
        val model = SquareModel(size = 20.ipx, outerColor = green, innerColor = white)

        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw { canvas, parentSize ->
                        // Fill the space with the outerColor
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(parentSize.toRect(), paint)
                        canvas.nativeCanvas.save()
                        val offset = parentSize.width.value / 3
                        // clip drawing to the inner rectangle
                        canvas.clipRect(Rect(offset, offset, offset * 2, offset * 2))
                    }
                    Padding(size = (model.size * 3)) {
                        Draw { canvas, parentSize ->
                            // Fill top half with innerColor -- should be clipped
                            drawLatch.countDown()
                            val paint = Paint()
                            paint.color = model.innerColor
                            val paintRect = Rect(
                                0f, 0f, parentSize.width.value,
                                parentSize.height.value / 2f
                            )
                            canvas.drawRect(paintRect, paint)
                        }
                    }
                    Draw { canvas, parentSize ->
                        // Fill bottom half with innerColor -- should be clipped
                        val paint = Paint()
                        paint.color = model.innerColor
                        val paintRect = Rect(
                            0f, parentSize.height.value / 2f,
                            parentSize.width.value, parentSize.height.value
                        )
                        canvas.drawRect(paintRect, paint)
                        // restore the canvas
                        canvas.nativeCanvas.restore()
                    }
                }
            }
        }
        validateSquareColors(outerColor = green, innerColor = white, size = 20)
    }

    @Test
    fun withConstraintsTest() {
        val size = 20.ipx

        val countDownLatch = CountDownLatch(1)
        val topConstraints = Ref<Constraints>()
        val paddedConstraints = Ref<Constraints>()
        val firstChildConstraints = Ref<Constraints>()
        val secondChildConstraints = Ref<Constraints>()
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    WithConstraints { constraints ->
                        topConstraints.value = constraints
                        Padding(size = size) {
                            WithConstraints { constraints ->
                                paddedConstraints.value = constraints
                                Layout(layoutBlock = { _, childConstraints ->
                                    firstChildConstraints.value = childConstraints
                                    layout(size, size) { }
                                }, children = { })
                                Layout(layoutBlock = { _, chilConstraints ->
                                    secondChildConstraints.value = chilConstraints
                                    layout(size, size) { }
                                }, children = { })
                                Draw { _, _ ->
                                    countDownLatch.countDown()
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        val expectedPaddedConstraints = Constraints(
            topConstraints.value!!.minWidth - size * 2,
            topConstraints.value!!.maxWidth - size * 2,
            topConstraints.value!!.minHeight - size * 2,
            topConstraints.value!!.maxHeight - size * 2
        )
        assertEquals(expectedPaddedConstraints, paddedConstraints.value)
        assertEquals(paddedConstraints.value, firstChildConstraints.value)
        assertEquals(paddedConstraints.value, secondChildConstraints.value)
    }

    // Tests that calling measure multiple times on the same Measurable causes an exception
    @Test
    fun multipleMeasureCall() {
        val latch = CountDownLatch(1)
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    TwoMeasureLayout(50.ipx, latch) {
                        AtLeastSize(50.ipx) {
                        }
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
            Constraints.tightConstraintsForWidth(50.ipx),
            Constraints.tightConstraintsForHeight(50.ipx)
        )
        val headerChildrenCount = 1
        val footerChildrenCount = 2

        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    val header = @Composable {
                        Layout(layoutBlock = { _, constraints ->
                            assertEquals(childConstraints[0], constraints)
                        }, children = {})
                    }
                    val footer = @Composable {
                        Layout(layoutBlock = { _, constraints ->
                            assertEquals(childConstraints[1], constraints)
                        }, children = {})
                        Layout(layoutBlock = { _, constraints ->
                            assertEquals(childConstraints[2], constraints)
                        }, children = {})
                    }
                    @Suppress("USELESS_CAST")
                    Layout(childrenArray = arrayOf(header, footer)) { measurables, _ ->
                        assertEquals(childrenCount, measurables.size)
                        measurables.forEachIndexed { index, measurable ->
                            measurable.measure(childConstraints[index])
                        }
                        assertEquals(headerChildrenCount, measurables[header as () -> Unit].size)
                        assertSame(measurables[0], measurables[header as () -> Unit][0])
                        assertEquals(footerChildrenCount, measurables[footer as () -> Unit].size)
                        assertSame(measurables[1], measurables[footer as () -> Unit][0])
                        assertSame(measurables[2], measurables[footer as () -> Unit][1])
                    }
                }
            }
        }
    }

    @Test
    fun multiChildLayoutTest_doesNotOverrideChildrenParentData() {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    val header = @Composable {
                        ParentData(data = 0) {
                            Layout(layoutBlock = { _, _ -> }, children = {})
                        }
                    }
                    val footer = @Composable {
                        ParentData(data = 1) {
                            Layout(layoutBlock = { _, _ -> }, children = {})
                        }
                    }

                    Layout(childrenArray = arrayOf(header, footer)) { measurables, _ ->
                        assertEquals(0, measurables[0].parentData)
                        assertEquals(1, measurables[1].parentData)
                    }
                }
            }
        }
    }

    // TODO(lmr): refactor to use the globally provided one when it lands
    private fun Activity.compose(composable: @Composable() () -> Unit) {
        val root = AndroidCraneView(this)

        setContentView(root)
        Compose.composeInto(root.root, context = this) {
            ContextAmbient.Provider(value = this) {
                DensityAmbient.Provider(value = Density(this)) {
                    composable()
                }
            }
        }
    }

    // When a child's measure() is done within the layout, it should not affect the parent's
    // size. The parent's layout shouldn't be called when the child's size changes
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun measureInLayoutDoesNotAffectParentSize() {
        val white = Color(0xFFFFFFFF.toInt())
        val blue = Color(0xFF000080.toInt())
        val model = SquareModel(outerColor = blue, innerColor = white)
        var measureCalls = 0
        var layoutCalls = 0

        val layoutLatch = CountDownLatch(1)
        runOnUiThread {
            activity.compose {
                Draw { canvas, parentSize ->
                    val paint = Paint()
                    paint.color = model.outerColor
                    canvas.drawRect(parentSize.toRect(), paint)
                }
                Layout(children = {
                    AtLeastSize(size = model.size) {
                        Draw { canvas, parentSize ->
                            drawLatch.countDown()
                            val paint = Paint()
                            paint.color = model.innerColor
                            canvas.drawRect(parentSize.toRect(), paint)
                        }
                    }
                }, layoutBlock = { measurables, constraints ->
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
        layoutLatch.await(1, TimeUnit.SECONDS)

        validateSquareColors(outerColor = blue, innerColor = white, size = 10)

        layoutCalls = 0
        measureCalls = 0
        drawLatch = CountDownLatch(1)
        runOnUiThread {
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
            @Children children: @Composable() () -> Unit
        ) {
            Layout(children = children, layoutBlock = { measurables, constraints ->
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
            Layout(children = {
                Draw(children = { }, onPaint = { _, _ ->
                    drawn.value = true
                    latch.countDown()
                })
            }, layoutBlock = { _, constraints ->
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
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
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
        val drawChild = DoDraw()

        val outerColor = Color(0xFF000080.toInt())
        val innerColor = Color(0xFFFFFFFF.toInt())
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    AtLeastSize(size = 30.ipx) {
                        Draw { canvas, parentSize ->
                            drawLatch.countDown()
                            val paint = Paint()
                            paint.color = outerColor
                            canvas.drawRect(parentSize.toRect(), paint)
                        }
                        if (drawChild.value) {
                            Padding(size = 20.ipx) {
                                AtLeastSize(size = 20.ipx) {
                                    Draw { canvas, parentSize ->
                                        drawLatch.countDown()
                                        val paint = Paint()
                                        paint.color = innerColor
                                        canvas.drawRect(parentSize.toRect(), paint)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // The padded area doesn't draw
        validateSquareColors(outerColor = outerColor, innerColor = outerColor, size = 10)

        drawLatch = CountDownLatch(1)
        runOnUiThread { drawChild.value = true }

        validateSquareColors(outerColor = outerColor, innerColor = innerColor, size = 20)
    }

    // We only need this because IR compiler doesn't like converting lambdas to Runnables
    private fun runOnUiThread(block: () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                block()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    private fun composeSquares(model: SquareModel) {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw { canvas, parentSize ->
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(parentSize.toRect(), paint)
                    }
                    Padding(size = model.size) {
                        AtLeastSize(size = model.size) {
                            Draw { canvas, parentSize ->
                                drawLatch.countDown()
                                val paint = Paint()
                                paint.color = model.innerColor
                                canvas.drawRect(parentSize.toRect(), paint)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeSquaresWithNestedRepaintBoundaries(model: SquareModel) {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw { canvas, parentSize ->
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(parentSize.toRect(), paint)
                    }
                    Padding(size = model.size) {
                        RepaintBoundary {
                            RepaintBoundary {
                                AtLeastSize(size = model.size) {
                                    Draw { canvas, parentSize ->
                                        drawLatch.countDown()
                                        val paint = Paint()
                                        paint.color = model.innerColor
                                        canvas.drawRect(parentSize.toRect(), paint)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeMovingSquaresWithRepaintBoundary(model: SquareModel, offset: OffsetModel) {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw { canvas, parentSize ->
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(parentSize.toRect(), paint)
                    }
                    Position(size = model.size * 3, offset = offset) {
                        RepaintBoundary {
                            AtLeastSize(size = model.size) {
                                Draw { canvas, parentSize ->
                                    drawLatch.countDown()
                                    val paint = Paint()
                                    paint.color = model.innerColor
                                    canvas.drawRect(parentSize.toRect(), paint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeMovingSquares(model: SquareModel, offset: OffsetModel) {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw { canvas, parentSize ->
                        val paint = Paint()
                        paint.color = model.outerColor
                        canvas.drawRect(parentSize.toRect(), paint)
                    }
                    Position(size = model.size * 3, offset = offset) {
                        AtLeastSize(size = model.size) {
                            Draw { canvas, parentSize ->
                                drawLatch.countDown()
                                val paint = Paint()
                                paint.color = model.innerColor
                                canvas.drawRect(parentSize.toRect(), paint)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun composeNestedSquares(model: SquareModel) {
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Draw(children = {
                        AtLeastSize(size = (model.size * 3)) {
                            Draw(children = {
                                Draw { canvas, parentSize ->
                                    val paint = Paint()
                                    paint.color = model.innerColor
                                    canvas.drawRect(parentSize.toRect(), paint)
                                    drawLatch.countDown()
                                }
                            }, onPaint = { canvas, parentSize ->
                                val paint = Paint()
                                paint.color = model.outerColor
                                canvas.drawRect(parentSize.toRect(), paint)
                                val start = model.size.value.toFloat()
                                val end = start * 2
                                canvas.nativeCanvas.save()
                                canvas.clipRect(Rect(start, start, end, end))
                                drawChildren()
                                canvas.nativeCanvas.restore()
                            })
                        }
                    }, onPaint = { canvas, parentSize ->
                        val paint = Paint()
                        paint.color = Color(0xFF000000.toInt())
                        canvas.drawRect(parentSize.toRect(), paint)
                    })
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
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = waitAndScreenShot()
        assertEquals(totalSize, bitmap.width)
        assertEquals(totalSize, bitmap.height)
        val squareStart = (totalSize - size) / 2 + offset
        val squareEnd = totalSize - ((totalSize - size) / 2) + offset
        for (x in 0 until totalSize) {
            for (y in 0 until totalSize) {
                val pixel = bitmap.getPixel(x, y)
                val pixelString = Color(pixel).toString()
                if (x < squareStart || x >= squareEnd || y < squareStart || y >= squareEnd) {
                    assertEquals(
                        "Pixel within drawn rect[$x, $y] is $outerColor, " +
                                "but was $pixelString", outerColor.toArgb(), pixel
                    )
                } else {
                    assertEquals(
                        "Pixel within drawn rect[$x, $y] is $innerColor, " +
                                "but was $pixelString", innerColor.toArgb(), pixel
                    )
                }
            }
        }
    }

    private fun findAndroidCraneView(): AndroidCraneView {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findAndroidCraneView(contentViewGroup)!!
    }

    private fun findAndroidCraneView(parent: ViewGroup): AndroidCraneView? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidCraneView) {
                return child
            } else if (child is ViewGroup) {
                val craneView = findAndroidCraneView(child)
                if (craneView != null) {
                    return craneView
                }
            }
        }
        return null
    }

    private fun waitAndScreenShot(): Bitmap {
        val view = findAndroidCraneView()
        val flushListener = DrawCounterListener(view)
        val offset = intArrayOf(0, 0)
        runOnUiThread {
            view.getLocationInWindow(offset)
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }

        assertTrue(flushListener.latch.await(1, TimeUnit.SECONDS))
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
        PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler)
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, copyResult)
        return dest
    }
}

@Composable
fun AtLeastSize(size: IntPx, @Children children: @Composable() () -> Unit) {
    Layout(
        layoutBlock = { measurables, constraints ->
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
        }, children = children
    )
}

@Composable
fun Align(@Children children: @Composable() () -> Unit) {
    Layout(
        layoutBlock = { measurables, constraints ->
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
fun Padding(size: IntPx, @Children children: @Composable() () -> Unit) {
    Layout(
        layoutBlock = { measurables, constraints ->
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
    @Children children: @Composable() () -> Unit
) {
    Layout(children = children) { measurables, _ ->
        val testConstraints = Constraints()
        measurables.forEach { it.measure(testConstraints) }
        val childConstraints = Constraints.tightConstraints(size, size)
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
    }
}

@Composable
fun Position(size: IntPx, offset: OffsetModel, @Children children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val placeables = measurables.map { m ->
            m.measure(constraints)
        }
        layout(size, size) {
            placeables.forEach { child ->
                child.place(offset.offset, offset.offset)
            }
        }
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

@Model
class SquareModel(
    var size: IntPx = 10.ipx,
    var outerColor: Color = Color(0xFF000080.toInt()),
    var innerColor: Color = Color(0xFFFFFFFF.toInt())
)

@Model
class OffsetModel(var offset: IntPx)

@Model
class DoDraw(var value: Boolean = false)
