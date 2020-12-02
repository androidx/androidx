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

package androidx.wear.widget

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.test.R
import androidx.wear.widget.util.AsyncViewActions.waitForMatchingView
import androidx.wear.widget.WearArcLayout.LayoutParams.VALIGN_CENTER
import androidx.wear.widget.WearArcLayout.LayoutParams.VALIGN_OUTER
import androidx.wear.widget.WearArcLayout.LayoutParams.VALIGN_INNER
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class WearArcLayoutTest {

    private val bitmap = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val renderDoneLatch = CountDownLatch(1)

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear")

    private fun doOneTest(key: String, views: List<View>) {
        // Set the main frame.
        val mainFrame = FrameLayout(ApplicationProvider.getApplicationContext())
        mainFrame.setBackgroundColor(Color.GRAY)

        for (view in views) {
            mainFrame.addView(view)
        }
        val screenWidth = MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY)
        val screenHeight = MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY)
        mainFrame.measure(screenWidth, screenHeight)
        mainFrame.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)
        mainFrame.draw(canvas)
        renderDoneLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        bitmap.assertAgainstGolden(screenshotRule, key)
    }

    private fun createArc(text1: String = "SWEEP", text2: String = "Default") =
        WearArcLayout(ApplicationProvider.getApplicationContext()).apply {
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = text1
                    textColor = Color.BLUE
                    setBackgroundColor(Color.rgb(100, 100, 0))
                    minSweepDegrees = 45f
                }
            )
            addView(
                TextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "TXT"
                    setTextColor(Color.GREEN)
                    layoutParams =
                        WearArcLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = text2
                    textColor = Color.RED
                    setBackgroundColor(Color.rgb(0, 100, 100))
                }
            )
        }

    private fun createMixedArc() =
        WearArcLayout(ApplicationProvider.getApplicationContext()).apply {
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "One"
                    setBackgroundColor(Color.rgb(100, 100, 100))
                    maxSweepDegrees = 20f
                    clockwise = true
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "Two"
                    setBackgroundColor(Color.rgb(150, 150, 150))
                    maxSweepDegrees = 20f
                    clockwise = false
                }
            )
            addView(
                TextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "TXT"
                    setTextColor(Color.GREEN)
                    layoutParams =
                        WearArcLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { rotate = false }
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "Three"
                    setBackgroundColor(Color.rgb(100, 100, 100))
                    maxSweepDegrees = 20f
                    clockwise = true
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "Four"
                    setBackgroundColor(Color.rgb(150, 150, 150))
                    maxSweepDegrees = 20f
                    clockwise = false
                }
            )
        }

    @Test
    @Throws(Exception::class)
    fun testArcs() {
        doOneTest(
            "basic_arcs_screenshot",
            listOf(
                createArc(),
                createArc("SWEEP", "Start").apply {
                    anchorAngleDegrees = 90f
                    anchorType = WearArcLayout.ANCHOR_START
                },
                createArc("SWEEP", "End").apply {
                    anchorAngleDegrees = 270f
                    anchorType = WearArcLayout.ANCHOR_END
                },
                createArc("SWEEP", "Center").apply {
                    anchorAngleDegrees = 315f
                    anchorType = WearArcLayout.ANCHOR_CENTER
                }
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testArcsCcw() {
        doOneTest(
            "basic_arcs_ccw_screenshot",
            listOf(
                createArc(),
                createArc("SWEEP", "Start").apply {
                    anchorAngleDegrees = 270f
                    anchorType = WearArcLayout.ANCHOR_START
                },
                createArc("SWEEP", "End").apply {
                    anchorAngleDegrees = 90f
                    anchorType = WearArcLayout.ANCHOR_END
                },
                createArc("SWEEP", "Center").apply {
                    anchorAngleDegrees = 45f
                    anchorType = WearArcLayout.ANCHOR_CENTER
                }
            ).apply { forEach { it.clockwise = false } }
        )
    }

    @Test
    @Throws(Exception::class)
    fun testArcsMixed() {
        doOneTest(
            "basic_arcs_mix_screenshot",
            listOf(
                createMixedArc(),
                createMixedArc().apply {
                    clockwise = false
                }
            )
        )
    }

    // Extension functions to make the margin test more readable.
    fun WearArcLayout.addSeparator() {
        addView(
            WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                text = " "
                minSweepDegrees = 10f
                setBackgroundColor(Color.rgb(100, 100, 100))
                clockwise = true
                textSize = 40f
            }
        )
    }

    fun WearArcLayout.addText(
        text0: String,
        color: Int,
        marginLeft: Int = 0,
        marginTop: Int = 0,
        marginRight: Int = 0,
        marginBottom: Int = 0,
        vAlign: Int = VALIGN_CENTER
    ) {
        addView(
            WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                text = text0
                setBackgroundColor(color)
                clockwise = true
                textSize = 14f
                layoutParams = WearArcLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(marginLeft, marginTop, marginRight, marginBottom)
                    verticalAlignment = vAlign
                }
            }
        )
    }

    private fun createArcWithMargin() =
        WearArcLayout(ApplicationProvider.getApplicationContext()).apply {
            anchorType = WearArcLayout.ANCHOR_CENTER
            addSeparator()
            addText("RI", Color.RED, marginTop = 16, vAlign = VALIGN_INNER)
            addText("GI", Color.GREEN, marginTop = 8, marginBottom = 8, vAlign = VALIGN_INNER)
            addText("BI", Color.BLUE, marginBottom = 16, vAlign = VALIGN_INNER)
            addSeparator()
            addText("Red", Color.RED, marginTop = 16)
            addText("Green", Color.GREEN, marginTop = 8, marginBottom = 8)
            addText("Blue", Color.BLUE, marginBottom = 16)
            addSeparator()
            addText("RO", Color.RED, marginTop = 16, vAlign = VALIGN_OUTER)
            addText("GO", Color.GREEN, marginTop = 8, marginBottom = 8, vAlign = VALIGN_OUTER)
            addText("BO", Color.BLUE, marginBottom = 16, vAlign = VALIGN_OUTER)
            addSeparator()
            addText("L", Color.WHITE, marginRight = 20)
            addSeparator()
            addText("C", Color.WHITE, marginRight = 10, marginLeft = 10)
            addSeparator()
            addText("R", Color.WHITE, marginLeft = 20)
            addSeparator()
        }

    private fun createTwoArcsWithMargin() = listOf(
        // First arc goes on top
        createArcWithMargin(),

        // Second arc in the bottom, and we change al children to go counterclockwise.
        createArcWithMargin().apply {
            anchorAngleDegrees = 180f
            children.forEach {
                (it as? WearCurvedTextView) ?.clockwise = false
            }
        }
    )

    @Test
    fun testMargins() {
        doOneTest(
            "margin_test",
            createTwoArcsWithMargin()
        )
    }

    @Test
    fun testMarginsCcw() {
        doOneTest(
            "margin_ccw_test",
            createTwoArcsWithMargin().map {
                it.apply { clockwise = false }
            }
        )
    }

    // Generates a click in the x,y coordinates in the view's coordinate system.
    fun customClick(x: Float, y: Float) = ViewActions.actionWithAssertions(
        GeneralClickAction(
            Tap.SINGLE,
            { view ->
                val xy = IntArray(2)
                view.getLocationOnScreen(xy)
                floatArrayOf(x + xy[0], y + xy[1])
            },
            Press.PINPOINT,
            InputDevice.SOURCE_UNKNOWN,
            MotionEvent.BUTTON_PRIMARY
        )
    )

    // Sending clicks is slow, around a quarter of a second each, on a desktop emulator.
    @Test(timeout = 100000)
    fun testTouchEvents() {
        val scenario = ActivityScenario.launch(TouchTestActivity::class.java)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val widgetWidth = context.resources.getDimension(R.dimen.touch_test_widget_width)
        val widgetHeight = context.resources.getDimension(R.dimen.touch_test_widget_height)

        val STEP = 30

        // Find the main FrameLayout that contains all widgets under test.
        val theView = Espresso.onView(withId(R.id.curved_frame))
            .perform(
                waitForMatchingView(
                    allOf(
                        withId(R.id.curved_frame),
                        isDisplayed()
                    ),
                    2000
                )
            )

        // Setup on-click handlers for each view so we can get the index of the clicked view.
        var clicked: Int
        scenario.onActivity {
            listOf(
                R.id.curved_text1, R.id.curved_text2, R.id.curved_text3,
                R.id.curved_text4, R.id.text5, R.id.curved_text6
            ).mapIndexed { ix, viewId ->
                it.findViewById<View>(viewId)?.setOnClickListener {
                    clicked = ix
                }
            }
        }

        // Simulate clicks in a grid all over the screen and draw a circle centered in the
        // position of the click and which color indicates the view that got clicked.
        // Black means no view got the click event, white means a out of range value.
        for (y in STEP / 2 until widgetHeight.toInt() step STEP) {
            val points = mutableListOf<ColoredPoint>()
            for (x in STEP / 2 until widgetWidth.toInt() step STEP) {
                // Perform a click, and record a point colored according to which view was clicked.
                clicked = -1
                theView.perform(customClick(x.toFloat(), y.toFloat()))
                points.add(
                    ColoredPoint(
                        x.toFloat(), y.toFloat(),
                        // Color the circle.
                        listOf(
                            Color.BLACK, // no view got the event.
                            Color.RED, Color.GREEN, Color.BLUE,
                            Color.YELLOW, Color.MAGENTA, Color.CYAN
                        ).elementAtOrNull(clicked + 1) ?: Color.WHITE
                    )
                )
            }

            // Add all circles on the current line to the DrawableSurface.
            // Points are batched to improve performance a bit.
            Espresso.onView(withId(R.id.drawable_surface)).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> = any(View::class.java)
                override fun getDescription(): String = "Add Points"
                override fun perform(uiController: UiController?, view: View?) {
                    (view as? DrawableSurface)?.addPoints(points)
                }
            })
        }

        // At the end, get a screenshot to compare against the golden
        scenario.onActivity {
            it.findViewById<View>(R.id.curved_frame).draw(canvas)
        }
        bitmap.assertAgainstGolden(screenshotRule, "touch_screenshot")
    }

    companion object {
        private const val SCREEN_WIDTH = 390
        private const val SCREEN_HEIGHT = 390
        private const val TIMEOUT_MS = 1000L
    }
}

// Helper activity for testing touch.
class TouchTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.wear_arc_layout_touch_test)
    }
}

data class ColoredPoint(val x: Float, val y: Float, val c: Int)

// Helper class to draw some point/circles of different colors. Used by the touch test.
open class DrawableSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var points = mutableListOf<ColoredPoint>()

    override fun onDraw(canvas: Canvas) {
        val paint = Paint().apply {
            strokeWidth = radius / 2f
            style = Paint.Style.STROKE
            alpha = 0
        }
        points.forEach { p ->
            paint.color = p.c
            canvas.drawCircle(p.x, p.y, radius, paint)
        }
    }

    fun addPoints(newPoints: Collection<ColoredPoint>) {
        points.addAll(newPoints)
        invalidate()
    }

    companion object {
        var radius = 6f
    }
}