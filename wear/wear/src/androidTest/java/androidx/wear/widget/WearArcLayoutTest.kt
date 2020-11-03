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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
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
                    sweepDegrees = 45f
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
                    sweepDegrees = 20f
                    clockwise = true
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "Two"
                    setBackgroundColor(Color.rgb(150, 150, 150))
                    sweepDegrees = 20f
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
                    sweepDegrees = 20f
                    clockwise = true
                }
            )
            addView(
                WearCurvedTextView(ApplicationProvider.getApplicationContext()).apply {
                    text = "Four"
                    setBackgroundColor(Color.rgb(150, 150, 150))
                    sweepDegrees = 20f
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

    companion object {
        private const val SCREEN_WIDTH = 390
        private const val SCREEN_HEIGHT = 390
        private const val TIMEOUT_MS = 1000L
    }
}
