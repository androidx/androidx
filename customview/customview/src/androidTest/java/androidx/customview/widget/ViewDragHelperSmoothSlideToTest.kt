/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.customview.widget

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.customview.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ViewDragHelperSmoothSlideToTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    @Test
    fun testSmoothSlideTo_doesSlideTheView() {
        val callBack = TestCallback()
        val (testView, childView) = createTestView(callBack)

        activityRule.runOnUiThread {
            testView.viewDragHelper.smoothSlideViewTo(childView, 50, 50)
            testView.postInvalidateOnAnimation()
        }

        callBack.assertAnimationPlayed(2, TimeUnit.SECONDS)
        assertThat(childView.left).isEqualTo(50)
        assertThat(childView.top).isEqualTo(50)
    }

    @Test
    fun testSmoothSlideTo_withDurationAndInterpolator_doesSlideTheView() {
        val callBack = TestCallback()
        val (testView, childView) = createTestView(callBack)

        activityRule.runOnUiThread {
            testView.viewDragHelper.smoothSlideViewTo(childView, 50, 50, 500, testInterpolator)
            testView.postInvalidateOnAnimation()
        }

        callBack.assertAnimationPlayed(2, TimeUnit.SECONDS)
        assertThat(childView.left).isEqualTo(50)
        assertThat(childView.top).isEqualTo(50)
    }

    @Test
    fun testSmoothSlideTo_withDurationOnly_doesSlideTheView() {
        val callBack = TestCallback()
        val (testView, childView) = createTestView(callBack)

        activityRule.runOnUiThread {
            testView.viewDragHelper.smoothSlideViewTo(childView, 50, 50, 500, null)
            testView.postInvalidateOnAnimation()
        }

        callBack.assertAnimationPlayed(2, TimeUnit.SECONDS)
        assertThat(childView.left).isEqualTo(50)
        assertThat(childView.top).isEqualTo(50)
    }

    private fun createTestView(callback: TestCallback): Pair<TestView, View> {
        lateinit var testView: TestView
        lateinit var childView: View
        activityRule.runOnUiThread {
            val activity = activityRule.activity
            val root = activity.findViewById<FrameLayout>(R.id.content) as ViewGroup
            testView = TestView(root.context, callback).apply { setBackgroundColor(Color.YELLOW) }
            childView = View(root.context).apply { setBackgroundColor(Color.RED) }
            testView.addView(childView, ViewGroup.LayoutParams(100, 100))
            root.addView(testView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        return Pair(testView, childView)
    }
}

private class TestView(context: Context, callBack: ViewDragHelper.Callback) : FrameLayout(context) {
    val viewDragHelper: ViewDragHelper = ViewDragHelper.create(this, callBack)

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            postInvalidateOnAnimation()
        }
    }
}

private class TestCallback() : ViewDragHelper.Callback() {
    var animationStarted = false
    var countDownLatch: CountDownLatch? = null

    /** Assert that animation is played and finished in the given timeout. */
    fun assertAnimationPlayed(timeout: Long, timeUnit: TimeUnit) {
        val countDownLatch = CountDownLatch(1).also { this.countDownLatch = it }
        countDownLatch.await(timeout, timeUnit)
        assertThat(animationStarted).isTrue()
    }

    override fun tryCaptureView(child: View, pointerId: Int): Boolean = true

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int = left

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int = top

    override fun onViewDragStateChanged(state: Int) {
        when (state) {
            ViewDragHelper.STATE_SETTLING -> animationStarted = true
            ViewDragHelper.STATE_IDLE -> countDownLatch?.countDown()
        }
    }
}

private val testInterpolator = Interpolator {
    val t = it - 1f
    t * t * t * t * t + 1f
}
