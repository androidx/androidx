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
package androidx.ui.core.test

import android.app.Activity
import androidx.ui.core.*
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.Handler
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.vectormath64.max
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composeInto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.Math.round
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

@SmallTest
@RunWith(JUnit4::class)
class ContainingViewTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity
    private lateinit var instrumentation: Instrumentation
    private lateinit var handler: Handler

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        instrumentation = InstrumentationRegistry.getInstrumentation()
        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    @Test
    @Suppress("PLUGIN_ERROR")
    fun simpleDrawTest() {
        val drawLatch = CountDownLatch(1)
        val totalSize = 30.dp.toPx(activity).toDouble()
        val rectPaintSize = 10.dp.toPx(activity).toDouble()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.composeInto {
                    <CraneWrapper>
                        val background : (Canvas) -> Unit = { canvas ->
                            val paint = Paint()
                            paint.color = Color(0xFFFFFF00.toInt())
                            canvas.drawRect(Rect(0.0, 0.0, totalSize, totalSize), paint)
                        }
                        // Component constructor parameters over-memoize, so use a property instead
                        <Draw onPaint=background/>
                        <Padding size=10.dp>
                            <AtLeastSize size=10.dp>
                                val foreground : (Canvas) -> Unit = { canvas ->
                                    drawLatch.countDown()
                                    val paint = Paint()
                                    paint.color = Color(0xFF0000FF.toInt())
                                    canvas.drawRect(
                                        Rect(0.0, 0.0, rectPaintSize, rectPaintSize),
                                        paint
                                    )
                                }
                                // Component constructor parameters over-memoize, so use a property instead
                                <Draw onPaint=foreground/>
                            </AtLeastSize>
                        </Padding>
                    </CraneWrapper>
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
        drawLatch.await(1, TimeUnit.SECONDS)
        val bitmap = waitAndScreenShot()
        assertEquals(ceil(totalSize).toInt(), bitmap.width)
        assertEquals(ceil(totalSize).toInt(), bitmap.height)

        val offset = round(10.dp.toPx(activity))
        val endRect = round(20.dp.toPx(activity))
        for (x in 0 until totalSize.toInt()) {
            for (y in 0 until totalSize.toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val pixelString = (pixel.toLong() and 0xFFFFFFFF).toString(16)
                if (x in offset + 1 until endRect - 1 && y in offset + 1 until endRect - 1) {
                    // This is clearly in the blue rect area
                    assertEquals(
                        "Pixel within drawn rect[$x, $y] is blue, but was ($pixelString)",
                        0xFF0000FF.toInt(),
                        pixel
                    )
                } else if (x == offset - 1 || x == offset || x == endRect - 1 || x == endRect - 2 ||
                    y == offset - 1 || y == offset || y == endRect - 1 || x == endRect - 2
                ) {
                    // This is likely to be antialiased. Don't bother checking it
                } else {
                    // This is in the yellow area
                    assertEquals(
                        "Pixel outside drawn rect[$x, $y] is yellow, but was ($pixelString)",
                        0xFFFFFF00.toInt(),
                        pixel
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
        val addPreDrawListener = object : Runnable {
            override fun run() {
                view.getLocationInWindow(offset)
                view.viewTreeObserver.addOnPreDrawListener(flushListener)
                view.invalidate()
            }
        }
        activityTestRule.runOnUiThread(addPreDrawListener)

        assertTrue(flushListener.latch.await(1, TimeUnit.SECONDS))
        val width = view.width
        val height = view.height

        val dest =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcRect = android.graphics.Rect(0, 0, width, width)
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

    companion object {
        class TestActivity : Activity() {
            var hasFocusLatch = CountDownLatch(1)

            override fun onWindowFocusChanged(hasFocus: Boolean) {
                super.onWindowFocusChanged(hasFocus)
                if (hasFocus) {
                    hasFocusLatch.countDown()
                }
            }
        }
    }
}

@Composable
fun AtLeastSize(size: Dimension, @Children children: @Composable() () -> Unit) {
    <MeasureBox bust=Math.random()> constraints, measureOperations ->
        val measurables = measureOperations.collect(children)
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
        measureOperations.layout(maxWidth, maxHeight) {
            placeables.forEach { child ->
                child.place(0.dp, 0.dp)
            }
        }
    </MeasureBox>
}

@Composable
fun Padding(size: Dimension, @Children children: @Composable() () -> Unit) {
    <MeasureBox bust=Math.random()> constraints, measureOperations ->
        val measurables = measureOperations.collect(children)
        val totalDiff = size * 2
        val newConstraints = Constraints(
            minWidth = (constraints.minWidth - totalDiff).coerceAtLeast(0.dp),
            maxWidth = (constraints.maxWidth - totalDiff).coerceAtLeast(0.dp),
            minHeight = (constraints.minHeight - totalDiff).coerceAtLeast(0.dp),
            maxHeight = (constraints.maxHeight - totalDiff).coerceAtLeast(0.dp)
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
        measureOperations.layout(maxWidth, maxHeight) {
            placeables.forEach { child ->
                child.place(size, size)
            }
        }
    </MeasureBox>
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
