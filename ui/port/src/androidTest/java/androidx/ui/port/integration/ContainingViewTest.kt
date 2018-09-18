/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.port.integration

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.PollingCheck
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.ComponentNode
import androidx.ui.foundation.ContainingView
import androidx.ui.foundation.DrawNode
import androidx.ui.foundation.LayoutNode
import androidx.ui.foundation.LayoutNode.Companion.measure
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

@SmallTest
@RunWith(JUnit4::class)
class ContainingViewTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<ImageDrawTest.Companion.TestActivity>(
        ImageDrawTest.Companion.TestActivity::class.java
    )
    private lateinit var activity: Activity
    private lateinit var instrumentation: Instrumentation
    private lateinit var handler: Handler

    @Before
    fun setup() {
        activity = activityTestRule.activity
        PollingCheck.waitFor { activity.hasWindowFocus() }
        instrumentation = InstrumentationRegistry.getInstrumentation()
        activityTestRule.runOnUiThread {
            handler = Handler()
        }
    }

    /**
     * Ensure that when there is no content that layout still happens
     */
    @Test
    fun noContentNode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }
        setContainingView(null)
    }

    /**
     * Tests that layout is called properly on LayoutNode when added to a View hierarchy
     */
    @Test
    fun singleLayoutNode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }
        var didLayout = false
        val layout = LayoutNode { _, _ ->
            didLayout = true
            Size(9.0, 10.0)
        }
        val view = setContainingView(layout)

        activityTestRule.runOnUiThread {
            assertEquals(9, view.measuredWidth)
            assertEquals(10, view.measuredHeight)
            assertEquals(9, view.width)
            assertEquals(10, view.height)
            assertTrue(didLayout)
        }
    }

    /**
     * Layouts that nest should all get layout events
     */
    @Test
    fun nestedLayoutNode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }
        val outerLayout = LayoutNode { constraints, _ ->
            // give the same constraints to all children
            var width = 0
            var height = 0
            layoutChildren.values.forEach { child ->
                assertNotNull(child)
                if (child != null) {
                    measure(child, constraints, true)
                    width = max(child.width, width)
                    height = max(child.height, height)
                }
            }
            layoutChildren.values.forEach { child ->
                if (child != null) {
                    position(child, (width - child.width) / 2, (height - child.height) / 2)
                }
            }
            Size(width.toDouble(), height.toDouble())
        }
        val squareLayout = LayoutNode { constraints, _ ->
            Size(10.0, 10.0)
        }
        val rectLayout = LayoutNode { constraints, _ ->
            Size(20.0, 8.0)
        }

        outerLayout.add(0, squareLayout)
        outerLayout.add(1, rectLayout)
        val view = setContainingView(outerLayout)

        assertEquals(1, outerLayout.depth)
        assertEquals(2, squareLayout.depth)
        assertEquals(2, rectLayout.depth)

        activityTestRule.runOnUiThread {
            assertEquals(20, view.width)
            assertEquals(10, view.height)
            val rootView = view.getChildAt(0) as ViewGroup
            System.out.println("root view = $rootView")
            assertEquals(20, rootView.width)
            assertEquals(10, rootView.height)

            assertEquals(20, outerLayout.width)
            assertEquals(10, outerLayout.height)
            val outerLayoutView = rootView.getChildAt(0) as ViewGroup
            assertEquals(20, outerLayoutView.width)
            assertEquals(10, outerLayoutView.height)

            assertEquals(10, squareLayout.width)
            assertEquals(10, squareLayout.height)
            val squareLayoutView = outerLayoutView.getChildAt(0)!!
            assertEquals(10, squareLayoutView.width)
            assertEquals(10, squareLayoutView.height)
            assertEquals(5, squareLayoutView.left)
            assertEquals(0, squareLayoutView.top)
            assertEquals(5, squareLayout.x)
            assertEquals(0, squareLayout.y)

            assertEquals(20, rectLayout.width)
            assertEquals(8, rectLayout.height)
            val rectLayoutView = outerLayoutView.getChildAt(1)!!
            assertEquals(20, rectLayoutView.width)
            assertEquals(8, rectLayoutView.height)
            assertEquals(0, rectLayoutView.left)
            assertEquals(1, rectLayoutView.top)
            assertEquals(0, rectLayout.x)
            assertEquals(1, rectLayout.y)
        }
    }

    /**
     * Make sure that a simple configuration draws to the screen correctly.
     */
    @Test
    fun simpleDraw() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val layout = LayoutNode { _, _ -> Size(10.0, 10.0) }
        val drawLatch = CountDownLatch(1)
        val draw = DrawNode { canvas ->
            drawLatch.countDown()
            drawFill(canvas, 0xFF0000FF.toInt()) { c, paint ->
                c.drawRect(Rect(0.0, 0.0, 10.0, 10.0), paint)
            }
        }
        draw.invalidate()
        layout.add(0, draw)
        val view = setContainingView(layout)

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = waitAndScreenShot(Rect(0.0, 0.0, 10.0, 10.0), view)
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 0))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(9, 9))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 9))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(9, 0))
    }

    /**
     * Two DrawNodes in the same layout should have the correct draw order.
     */
    @Test
    fun overlappingDraw() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val layout = LayoutNode { _, _ -> Size(10.0, 10.0) }
        val drawLatch = CountDownLatch(1)
        val square = DrawNode { canvas ->
            drawLatch.countDown()
            drawFill(canvas, 0xFF0000FF.toInt()) { c, paint ->
                c.drawRect(Rect(0.0, 0.0, 10.0, 10.0), paint)
            }
        }
        layout.add(0, square)
        val circle = DrawNode { canvas ->
            drawFill(canvas, 0xFF00FF00.toInt()) { c, paint ->
                c.drawOval(Rect(0.0, 0.0, 10.0, 10.0), paint)
            }
        }
        layout.add(1, circle)

        val view = setContainingView(layout)

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = waitAndScreenShot(Rect(0.0, 0.0, 10.0, 10.0), view)
        // Square is drawn to the corners
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 0))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(9, 9))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(0, 9))
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(9, 0))
        // Circle draws in the middle
        assertEquals(0xFF00FF00.toInt(), bitmap.getPixel(5, 5))
        assertEquals(0xFF00FF00.toInt(), bitmap.getPixel(5, 1))
        assertEquals(0xFF00FF00.toInt(), bitmap.getPixel(5, 9))
        assertEquals(0xFF00FF00.toInt(), bitmap.getPixel(1, 5))
        assertEquals(0xFF00FF00.toInt(), bitmap.getPixel(9, 5))
    }

    /**
     * DrawNodes in different layouts should be placed with respect to their LayoutNode parent
     */
    @Test
    fun complexLayoutDraw() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        // Draw rects in the upper left and lower right, but not in the upper right and lower left
        val layout = LayoutNode { c, _ ->
            val first = layoutChildren[children[0]]!!
            val second = layoutChildren[children[1]]!!
            measure(first, c, false)
            measure(second, c, false)
            position(first, 0, 0)
            position(second, 10, 10)
            Size(20.0, 20.0)
        }
        val subLayout1 = LayoutNode { _, _ -> Size(10.0, 10.0) }
        val subLayout2 = LayoutNode { _, _ -> Size(10.0, 10.0) }
        val redRect = DrawNode { canvas ->
            drawFill(canvas, 0xFFFF0000.toInt()) { c, paint ->
                c.drawRect(Rect(0.0, 0.0, 10.0, 10.0), paint)
            }
        }
        val drawLatch = CountDownLatch(1)
        val blueRect = DrawNode { canvas ->
            drawLatch.countDown()
            drawFill(canvas, 0xFF0000FF.toInt()) { c, paint ->
                c.drawRect(Rect(0.0, 0.0, 10.0, 10.0), paint)
            }
        }
        subLayout1.add(0, redRect)
        subLayout2.add(0, blueRect)
        layout.add(0, subLayout1)
        layout.add(1, subLayout2)

        val view = setContainingView(layout)

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = waitAndScreenShot(Rect(0.0, 0.0, 20.0, 20.0), view)
        // top-left square is red
        assertEquals(0xFFFF0000.toInt(), bitmap.getPixel(5, 5))

        // bottom-right square is blue
        assertEquals(0xFF0000FF.toInt(), bitmap.getPixel(15, 15))
    }

    /**
     * Ensure that when LayoutNodes and DrawNodes are interspersed that they are drawn in the right
     * order
     */
    @Test
    fun interspersedDraw() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val layout = LayoutNode { c, _ ->
            val first = layoutChildren[children[1]]!!
            measure(first, c, false)
            position(first, 0, 0)
            Size(20.0, 20.0)
        }
        val drawLatch = CountDownLatch(1)
        val draw1 = DrawNode { canvas ->
            drawFill(canvas, android.graphics.Color.BLUE) { c, paint ->
                c.drawRect(Rect(0.0, 0.0, 20.0, 20.0), paint)
            }
            drawLatch.countDown()
        }
        val draw2 = DrawNode { canvas ->
            drawFill(canvas, android.graphics.Color.BLACK) { c, paint ->
                c.drawOval(Rect(0.0, 0.0, 20.0, 20.0), paint)
            }
        }
        val draw3 = DrawNode { canvas ->
            drawFill(canvas, android.graphics.Color.WHITE) { c, paint ->
                c.drawRect(Rect(5.0, 5.0, 15.0, 15.0), paint)
            }
        }
        layout.add(0, draw1)
        val child1 = LayoutNode { _, _ -> Size(20.0, 20.0) }
        layout.add(1, child1)
        child1.add(0, draw2)
        layout.add(2, draw3)

        val view = setContainingView(layout)

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = waitAndScreenShot(Rect(0.0, 0.0, 20.0, 20.0), view)

        // outer corners should be draw1's blue
        assertEquals(android.graphics.Color.BLUE, bitmap.getPixel(0, 0))

        // inner part should be draw2 circle's black
        assertEquals(android.graphics.Color.BLACK, bitmap.getPixel(10, 1))

        // center should be draw3's white
        assertEquals(android.graphics.Color.WHITE, bitmap.getPixel(10, 10))
    }

    /** When a change requires a layout to change, only the changed layout should change. */
    @Test
    fun dirtyLayout() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val layoutLatch1 = CountDownLatch(1)
        val layoutLatch2 = CountDownLatch(2)
        val outer = LayoutNode { c, _ ->
            val first = layoutChildren[children[0]]!!
            val second = layoutChildren[children[1]]!!
            val firstSize = measure(first, c, true)
            val secondSize = measure(second, c, true)
            position(first, 0, 0)
            position(second, 0, 0)
            layoutLatch1.countDown()
            layoutLatch2.countDown()

            System.out.println("max width = ${max(firstSize.width, secondSize.width)}")
            Size(
                max(firstSize.width, secondSize.width),
                max(firstSize.height, secondSize.height)
            )
        }
        var size1 = 10.0
        val layout1 = LayoutNode { _, _ -> Size(size1, size1) }
        outer.add(0, layout1)

        val layout2 = LayoutNode { _, _ -> Size(10.0, 10.0) }
        outer.add(1, layout2)

        val view = setContainingView(outer)

        assertTrue(layoutLatch1.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            assertEquals(10, view.width)
            assertEquals(10, view.height)
        }

        size1 = 20.0
        layout1.dirtyLayout()

        assertTrue(layoutLatch2.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            assertEquals(10, layout2.width)
            assertEquals(10, layout2.height)

            assertEquals(20, layout1.width)
            assertEquals(20, layout1.width)

            assertEquals(20, outer.width)
            assertEquals(20, outer.width)
            assertEquals(20, view.width)
            assertEquals(20, view.height)
        }
    }

    /**
     * Call a canvas command with a solid color paint
     */
    private fun drawFill(canvas: Canvas, color: Int, block: (Canvas, Paint) -> Unit) {
        val paint = Paint()
        paint.color = Color.fromARGB(
            android.graphics.Color.alpha(color),
            android.graphics.Color.red(color), android.graphics.Color.green(color),
            android.graphics.Color.blue(color)
        )
        paint.style = PaintingStyle.fill
        block(canvas, paint)
    }

    private fun setContainingView(node: ComponentNode?): ContainingView {
        var view: ContainingView? = null
        val onPreDraw = mock<ViewTreeObserver.OnPreDrawListener> {
            on { onPreDraw() } doReturn true
        }
        activityTestRule.runOnUiThread {
            val frameLayout = FrameLayout(activity)
            activity.setContentView(frameLayout)
            val containingView = ContainingView(activity)
            view = containingView
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER
            frameLayout.addView(containingView, layoutParams)
            if (node != null) {
                containingView.root.add(0, node)
            }
            view!!.viewTreeObserver.addOnPreDrawListener(onPreDraw)
        }
        verify(onPreDraw, timeout(2000)).onPreDraw()
        view!!.viewTreeObserver.removeOnPreDrawListener(onPreDraw)

        return view!!
    }

    private fun waitAndScreenShot(rect: Rect, view: View): Bitmap {
        val flushListener = DrawCounterListener(view)
        val offset = intArrayOf(0, 0)
        activityTestRule.runOnUiThread {
            view.getLocationInWindow(offset)
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }

        assertTrue(flushListener.latch.await(1, TimeUnit.SECONDS))

        val dest =
            Bitmap.createBitmap(rect.width.toInt(), rect.height.toInt(), Bitmap.Config.ARGB_8888)
        val srcRect = android.graphics.Rect(0, 0, rect.width.toInt(), rect.height.toInt())
        srcRect.offset(rect.left.toInt() + offset[0], rect.top.toInt() + offset[1])
        val latch = CountDownLatch(1)
        var copyResult = 0
        val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result: Int ->
            copyResult = result
            latch.countDown()
        }
        PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler)
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, copyResult)
        return dest
    }
}

private class DrawCounterListener(private val view: View) :
    ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.getViewTreeObserver().removeOnPreDrawListener(this)
        }
        return true
    }
}
