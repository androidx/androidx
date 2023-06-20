/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.core.widget

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.app.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.PollingCheck
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** On S and higher, a large fling back should remove the stretch and start flinging the content. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@MediumTest
class NestedScrollViewStretchFlingTest {
    lateinit var nestedScrollView: NestedScrollView

    @Rule
    @JvmField
    @Suppress("DEPRECATION")
    val mRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Before
    fun setup() {
        val drawLatch = CountDownLatch(1)
        mRule.scenario.onActivity { activity ->
            nestedScrollView = NestedScrollView(activity)
            val layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            activity.setContentView(nestedScrollView, layoutParams)

            val linearLayout = LinearLayout(activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            val linearLayoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            nestedScrollView.addView(linearLayout, linearLayoutParams)

            repeat(1000) {
                val child = FrameLayout(activity)
                child.setBackgroundColor(Color.HSVToColor(floatArrayOf(it * 10f, 1f, 1f)))
                val childLayoutParams = ViewGroup.LayoutParams(50, 50)
                linearLayout.addView(child, childLayoutParams)
            }
            nestedScrollView.viewTreeObserver.addOnPreDrawListener {
                drawLatch.countDown()
                true
            }

            // Disabled animations will cause EdgeEffects to not do anything.
            // This will enable animations for our Activity.
            val setDurationScale =
                ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java)

            setDurationScale.invoke(null, 1f)
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun flingContentAfterStretchOnTop() {
        stretchThenFling(stretchMotionUp = false) {
            assertTrue(nestedScrollView.mEdgeGlowTop.distance > 0f)
            assertEquals(0, nestedScrollView.scrollY)
        }

        // Wait for the stretch to release
        PollingCheck.waitFor(1000L) { nestedScrollView.mEdgeGlowTop.isFinished }

        var lastScroll = 0
        PollingCheck.waitFor(1000L) {
            var nextScroll = 0
            mRule.scenario.onActivity { nextScroll = nestedScrollView.scrollY }
            val changed = nextScroll == lastScroll
            lastScroll = nextScroll
            !changed
        }

        assertTrue(lastScroll > 0)
    }

    @Test
    @FlakyTest(bugId = 240290945)
    fun flingContentAfterStretchOnBottom() {
        mRule.scenario.onActivity {
            nestedScrollView.scrollTo(0, nestedScrollView.scrollRange)
            assertEquals(nestedScrollView.scrollRange, nestedScrollView.scrollY)
        }
        stretchThenFling(stretchMotionUp = true) {
            assertTrue(nestedScrollView.mEdgeGlowBottom.distance > 0f)
            assertEquals(nestedScrollView.scrollRange, nestedScrollView.scrollY)
        }

        // Wait for the stretch to release
        PollingCheck.waitFor(1000L) { nestedScrollView.mEdgeGlowBottom.isFinished }

        var lastScroll = 0
        PollingCheck.waitFor(1000L) {
            var nextScroll = 0
            mRule.scenario.onActivity { nextScroll = nestedScrollView.scrollY }
            val changed = nextScroll == lastScroll
            lastScroll = nextScroll
            !changed
        }

        assertTrue(lastScroll < nestedScrollView.scrollRange)
    }

    private fun stretchThenFling(stretchMotionUp: Boolean, onFlingStart: () -> Unit) {
        val x = nestedScrollView.width / 2f
        val yStart = nestedScrollView.height / 2f

        val yStretch = if (stretchMotionUp) 0f else nestedScrollView.height.toFloat()

        val stretchTime = 20L
        val endStretchTime = 500L

        val events = mutableListOf<MotionEvent>()
        // down
        events += MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, yStart, 0)
        // stretch
        events += MotionEvent.obtain(0, stretchTime, MotionEvent.ACTION_MOVE, x, yStretch, 0)
        // hold
        events += MotionEvent.obtain(0, endStretchTime, MotionEvent.ACTION_MOVE, x, yStretch, 0)

        val yFling = (yStretch + yStart) / 2f
        val yFlingHalf = (yStretch + yFling) / 2f
        val flingHalfTime = endStretchTime + 10L
        val flingTime = flingHalfTime + 10L

        // fling
        events += MotionEvent.obtain(0, flingHalfTime, MotionEvent.ACTION_MOVE, x, yFlingHalf, 0)
        events += MotionEvent.obtain(0, flingTime, MotionEvent.ACTION_MOVE, x, yFling, 0)
        events += MotionEvent.obtain(0, flingTime, MotionEvent.ACTION_UP, x, yFling, 0)

        events.forEachIndexed { index, event ->
            mRule.scenario.onActivity {
                nestedScrollView.dispatchTouchEvent(event)
                if (index == events.lastIndex) {
                    onFlingStart()
                }
            }
        }
    }
}
