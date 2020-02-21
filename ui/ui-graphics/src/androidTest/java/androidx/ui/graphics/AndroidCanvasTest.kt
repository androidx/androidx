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

package androidx.ui.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import androidx.ui.test.captureToBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class AndroidCanvasTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testEnableDisableZ() {
        val activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        val drawLatch = CountDownLatch(1)
        var groupView: ViewGroup? = null

        activityTestRule.runOnUiThread {
            val group = EnableDisableZViewGroup(drawLatch, activity)
            groupView = group
            group.setBackgroundColor(Color.WHITE)
            group.layoutParams = ViewGroup.LayoutParams(12, 12)
            val child = View(activity)
            val childLayoutParams = FrameLayout.LayoutParams(10, 10)
            childLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
            child.layoutParams = childLayoutParams
            child.elevation = 4f
            child.setBackgroundColor(Color.WHITE)
            group.addView(child)
            activity.setContentView(group)
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = groupView!!.captureToBitmap()
        assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
        assertEquals(Color.WHITE, bitmap.getPixel(9, 9))
        assertNotEquals(Color.WHITE, bitmap.getPixel(10, 10))
    }

    class EnableDisableZViewGroup @JvmOverloads constructor(
        val drawLatch: CountDownLatch,
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {
        override fun dispatchDraw(canvas: Canvas) {
            val androidCanvas = Canvas(canvas)
            androidCanvas.enableZ()
            for (i in 0 until childCount) {
                drawChild(androidCanvas.nativeCanvas, getChildAt(i), drawingTime)
            }
            androidCanvas.disableZ()
            drawLatch.countDown()
        }
    }
}