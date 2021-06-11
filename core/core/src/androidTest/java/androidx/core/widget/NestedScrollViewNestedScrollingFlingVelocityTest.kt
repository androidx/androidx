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

package androidx.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingParent3
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.ArrayList
import java.util.Arrays

/**
 * This test verifies that the velocity that NestedScrollView flings with in response to finger
 * input is the same despite any nested scrolling scenario.
 */
@RunWith(Parameterized::class)
@LargeTest
class NestedScrollViewNestedScrollingFlingVelocityTest(
    private val fingerDirectionUp: Boolean,
    private val parentIntercepts: Boolean,
    private val preScrollConsumption: Int,
    private val postScrollConsumption: Int
) {

    private lateinit var mNestedScrollingParent: NestedScrollingParent
    private lateinit var mNestedScrollView: NestedScrollViewUnderTest

    @Suppress("DEPRECATION")
    @Rule
    @JvmField
    var mActivityRule = androidx.test.rule.ActivityTestRule(TestContentViewActivity::class.java)

    @Before
    @Throws(Throwable::class)
    fun setup() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        val child = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(1000, 1000)
            isClickable = parentIntercepts
        }
        mNestedScrollView = NestedScrollViewUnderTest(context).apply {
            layoutParams = ViewGroup.LayoutParams(1000, 1000)
            addView(child)
        }
        mNestedScrollingParent = NestedScrollingParent(context).apply {
            layoutParams = ViewGroup.LayoutParams(1000, 1000)
            addView(mNestedScrollView)
        }

        val testContentView = mActivityRule.activity.findViewById<TestContentView>(
            androidx.core.test.R.id.testContentView
        )
        testContentView.expectLayouts(1)
        mActivityRule.runOnUiThread { testContentView.addView(mNestedScrollingParent) }
        testContentView.awaitLayouts(2)
    }

    @Test
    fun uiFingerFling_flingVelocityIsCorrect() {

        val directionalDistance = if (fingerDirectionUp) -200 else 200
        val elapsedTime = 20L
        val expectedVelocity = if (fingerDirectionUp) 10000 else -10000

        mNestedScrollingParent.preScrollY =
            preScrollConsumption * if (fingerDirectionUp) 1 else -1
        mNestedScrollingParent.postScrollY =
            postScrollConsumption * if (fingerDirectionUp) 1 else -1

        val halfDirectionalDistance = directionalDistance / 2
        val halfTime = elapsedTime / 2

        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        val move1 = MotionEvent.obtain(
            0, halfTime, MotionEvent.ACTION_MOVE, 500f, (500 + halfDirectionalDistance).toFloat(), 0
        )
        val move2 = MotionEvent.obtain(
            0, elapsedTime, MotionEvent.ACTION_MOVE, 500f, (500 + directionalDistance).toFloat(), 0
        )
        val up = MotionEvent.obtain(
            0, elapsedTime, MotionEvent.ACTION_UP, 500f, (500 + directionalDistance).toFloat(), 0
        )

        mActivityRule.runOnUiThread {
            mNestedScrollingParent.dispatchTouchEvent(down)
            mNestedScrollingParent.dispatchTouchEvent(move1)
            mNestedScrollingParent.dispatchTouchEvent(move2)
            mNestedScrollingParent.dispatchTouchEvent(up)
        }

        assertThat(
            mNestedScrollView.flungVelocity.toDouble(),
            closeTo(expectedVelocity.toDouble(), 1.0)
        )
    }

    inner class NestedScrollViewUnderTest : NestedScrollView {

        var flungVelocity = 0

        constructor(context: Context) : super(context)

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int
        ) : super(context, attrs, defStyleAttr)

        override fun fling(velocityY: Int) {
            flungVelocity = velocityY
        }
    }

    inner class NestedScrollingParent(context: Context) :
        FrameLayout(context),
        NestedScrollingParent3 {

        var preScrollY: Int = 0
        var postScrollY: Int = 0

        override fun onStartNestedScroll(
            child: View,
            target: View,
            axes: Int,
            type: Int
        ): Boolean {
            return true
        }

        override fun onNestedScrollAccepted(
            child: View,
            target: View,
            axes: Int,
            type: Int
        ) {
        }

        override fun onStopNestedScroll(target: View, type: Int) {}

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int
        ) {
        }

        override fun onNestedPreScroll(
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int
        ) {

            val toScrollY = amountOfScrollToConsume(dy, preScrollY)
            preScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(0, toScrollY)
        }

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
            consumed: IntArray
        ) {

            val toScrollY = amountOfScrollToConsume(dyUnconsumed, postScrollY)
            postScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(0, toScrollY)
        }

        override fun setNestedScrollingEnabled(enabled: Boolean) {}

        override fun isNestedScrollingEnabled(): Boolean {
            return false
        }

        override fun startNestedScroll(axes: Int): Boolean {
            return false
        }

        override fun stopNestedScroll() {}

        override fun hasNestedScrollingParent(): Boolean {
            return false
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?
        ): Boolean {
            return false
        }

        override fun dispatchNestedFling(
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
            return false
        }

        override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {}

        override fun onStopNestedScroll(target: View) {}

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int
        ) {
        }

        override fun onNestedPreScroll(
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray
        ) {
        }

        override fun onNestedFling(
            target: View,
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean
        ): Boolean {
            return false
        }

        override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        override fun getNestedScrollAxes(): Int {
            return 0
        }

        private fun amountOfScrollToConsume(d: Int, max: Int): Int {
            if (d < 0 && max < 0) {
                return Math.max(d, max)
            } else if (d > 0 && max > 0) {
                return Math.min(d, max)
            }
            return 0
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(
            name = "fingerDirectionUp:{0}, " +
                "parentIntercepts:{1}, " +
                "preScrollConsumption:{2}, " +
                "postScrollConsumption:{3}"
        )
        fun data(): Collection<Array<Any>> {
            val configurations = ArrayList<Array<Any>>()

            for (fingerDirectionUp in arrayOf(true, false)) {
                for (parentIntercepts in arrayOf(true, false)) {
                    configurations.addAll(
                        Arrays.asList(
                            arrayOf(fingerDirectionUp, parentIntercepts, 0, 0),
                            arrayOf(fingerDirectionUp, parentIntercepts, 25, 0),
                            arrayOf(fingerDirectionUp, parentIntercepts, 50, 0),
                            arrayOf(fingerDirectionUp, parentIntercepts, 100, 0),
                            arrayOf(fingerDirectionUp, parentIntercepts, 0, 25),
                            arrayOf(fingerDirectionUp, parentIntercepts, 0, 50),
                            arrayOf(fingerDirectionUp, parentIntercepts, 0, 100),
                            arrayOf(fingerDirectionUp, parentIntercepts, 12, 13),
                            arrayOf(fingerDirectionUp, parentIntercepts, 25, 25),
                            arrayOf(fingerDirectionUp, parentIntercepts, 50, 50)
                        )
                    )
                }
            }

            return configurations
        }
    }
}
