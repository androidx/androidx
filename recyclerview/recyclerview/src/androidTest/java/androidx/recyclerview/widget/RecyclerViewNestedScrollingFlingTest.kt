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

package androidx.recyclerview.widget

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingParent3
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.ActivityScenarioResetRule
import androidx.testutils.ResettableActivityScenarioRule
import java.util.Arrays
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This test verifies that the velocity that RecyclerView flings with in response to finger input is
 * the same despite any nested scrolling scenario.
 */
@RunWith(Parameterized::class)
@LargeTest
class RecyclerViewNestedScrollingFlingTest(
    private val orientationVertical: Boolean,
    private val scrollDirectionForward: Boolean,
    private val rvIntercepts: Boolean,
    private val preScrollConsumption: Int,
    private val postScrollConsumption: Int,
) {

    private lateinit var mNestedScrollingParent: NestedScrollingParent
    private lateinit var mRecyclerView: RecyclerView

    @Rule
    @JvmField
    val mActivityResetRule: ActivityScenarioResetRule<TestActivity> =
        TestActivity.ResetRule(mActivityRule.scenario)

    @Before
    @Throws(Throwable::class)
    fun setup() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        mRecyclerView =
            RecyclerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(1000, 1000)
                adapter = TestAdapter(context, 1000, rvIntercepts)
                val rvOrientation =
                    if (orientationVertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
                layoutManager = LinearLayoutManager(context, rvOrientation, false)
            }

        mNestedScrollingParent =
            NestedScrollingParent(context).apply {
                layoutParams = ViewGroup.LayoutParams(1000, 1000)
                addView(mRecyclerView)
            }

        val testedFrameLayout = mActivityRule.getActivity().container
        testedFrameLayout.expectLayouts(1)
        mActivityRule.runOnUiThread { testedFrameLayout.addView(mNestedScrollingParent) }
        testedFrameLayout.waitForLayout(2)
    }

    @Test
    fun uiFingerFling_flingVelocityIsCorrect() {

        val directionalDistance = if (scrollDirectionForward) -200 else 200
        val elapsedTime = 20L
        var expectedVelocity = if (scrollDirectionForward) 10000 else -10000

        // There is an off by 1 error in all Android SDK versions M (API 23) in
        // View#getLocationInWindow(int[]) where it calculates negative offsets to be 1 pixel larger
        // than they should be (unless that number is X.5 where X is a negative integer). This
        // causes flings in nested scrolling scenarios to be wrong when the view being flung
        // becomes positioned negative to the screen in either dimension.
        //
        // This only affects tests where we are scrolling forward (because that is the only time
        // an offset to the window could becoming negative), horizontally (because in the vertical,
        // there is an action bar that offsets the child such that it never ends up with negative
        // offset to the window), and when the RecyclerView becomes the target view without
        // needing to intercept (for other reasons which is likely a bug in RV).
        //
        // We are making up for the bug by expecting a slightly different velocity (one that is
        // accurate given that the bug exists).
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.M &&
                !orientationVertical &&
                scrollDirectionForward &&
                !rvIntercepts &&
                (preScrollConsumption != 0 || postScrollConsumption != 0)
        ) {
            expectedVelocity = 9850
        }

        if (orientationVertical) {
            mNestedScrollingParent.preScrollY =
                preScrollConsumption * if (scrollDirectionForward) 1 else -1
            mNestedScrollingParent.postScrollY =
                postScrollConsumption * if (scrollDirectionForward) 1 else -1
        } else {
            mNestedScrollingParent.preScrollX =
                preScrollConsumption * if (scrollDirectionForward) 1 else -1
            mNestedScrollingParent.postScrollX =
                postScrollConsumption * if (scrollDirectionForward) 1 else -1
        }

        val velocities = intArrayOf(1, 1)
        mRecyclerView.onFlingListener =
            object : RecyclerView.OnFlingListener() {
                override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                    velocities[0] = velocityX
                    velocities[1] = velocityY
                    return false
                }
            }

        val halfDirectionalDistance = directionalDistance / 2
        val halfTime = elapsedTime / 2

        val x2 = if (orientationVertical) 500f else 500f + halfDirectionalDistance
        val y2 = if (orientationVertical) 500f + halfDirectionalDistance else 500f
        val x3 = if (orientationVertical) 500f else 500f + directionalDistance
        val y3 = if (orientationVertical) 500f + directionalDistance else 500f

        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        val move1 = MotionEvent.obtain(0, halfTime, MotionEvent.ACTION_MOVE, x2, y2, 0)
        val move2 = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_MOVE, x3, y3, 0)
        val up = MotionEvent.obtain(0, elapsedTime, MotionEvent.ACTION_UP, x3, y3, 0)

        mActivityRule.runOnUiThread {
            mNestedScrollingParent.dispatchTouchEvent(down)
            mNestedScrollingParent.dispatchTouchEvent(move1)
            mNestedScrollingParent.dispatchTouchEvent(move2)
            mNestedScrollingParent.dispatchTouchEvent(up)
        }

        val (expected, errorRange) =
            if (orientationVertical) Pair(intArrayOf(0, expectedVelocity), intArrayOf(0, 1))
            else Pair(intArrayOf(expectedVelocity, 0), intArrayOf(1, 0))

        assertThat(
            velocities[0].toDouble(),
            closeTo(expected[0].toDouble(), errorRange[0].toDouble()),
        )
        assertThat(
            velocities[1].toDouble(),
            closeTo(expected[1].toDouble(), errorRange[1].toDouble()),
        )
    }

    inner class NestedScrollingParent(context: Context) :
        FrameLayout(context), NestedScrollingChild3, NestedScrollingParent3 {

        var preScrollX: Int = 0
        var postScrollX: Int = 0
        var preScrollY: Int = 0
        var postScrollY: Int = 0

        override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
            return true
        }

        override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {}

        override fun onStopNestedScroll(target: View, type: Int) {}

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
        ) {}

        override fun onNestedPreScroll(
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int,
        ) {
            val toScrollX = amountOfScrollToConsume(dx, preScrollX)
            preScrollX -= toScrollX
            consumed[0] += toScrollX

            val toScrollY = amountOfScrollToConsume(dy, preScrollY)
            preScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(toScrollX, toScrollY)
        }

        override fun startNestedScroll(axes: Int, type: Int): Boolean {
            return false
        }

        override fun stopNestedScroll(type: Int) {}

        override fun hasNestedScrollingParent(type: Int): Boolean {
            return false
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?,
            type: Int,
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?,
            type: Int,
        ): Boolean {
            return false
        }

        override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
            consumed: IntArray,
        ) {
            val toScrollX = amountOfScrollToConsume(dxUnconsumed, postScrollX)
            postScrollX -= toScrollX
            consumed[0] += toScrollX

            val toScrollY = amountOfScrollToConsume(dyUnconsumed, postScrollY)
            postScrollY -= toScrollY
            consumed[1] += toScrollY

            scrollBy(toScrollX, toScrollY)
        }

        override fun dispatchNestedScroll(
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            offsetInWindow: IntArray?,
            type: Int,
            consumed: IntArray,
        ) {}

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
            offsetInWindow: IntArray?,
        ): Boolean {
            return false
        }

        override fun dispatchNestedPreScroll(
            dx: Int,
            dy: Int,
            consumed: IntArray?,
            offsetInWindow: IntArray?,
        ): Boolean {
            return false
        }

        override fun dispatchNestedFling(
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean,
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
            dyUnconsumed: Int,
        ) {}

        override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {}

        override fun onNestedFling(
            target: View,
            velocityX: Float,
            velocityY: Float,
            consumed: Boolean,
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

    private inner class TestAdapter
    internal constructor(
        private val mContext: Context,
        private val itemSize: Int,
        private val rvIntercepts: Boolean,
    ) : RecyclerView.Adapter<TestViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
            val view = View(mContext)
            view.layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            view.isClickable = rvIntercepts
            return TestViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {}

        override fun getItemCount(): Int {
            return 1
        }
    }

    private inner class TestViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView)

    companion object {

        @ClassRule @JvmField val mActivityRule = ResettableActivityScenarioRule<TestActivity>()

        @JvmStatic
        @Parameterized.Parameters(
            name =
                "orientationVertical:{0}, " +
                    "scrollDirectionForward:{1}, " +
                    "rvIntercepts:{2}, " +
                    "preScrollConsumption:{3}, " +
                    "postScrollConsumption:{4}"
        )
        fun data(): Collection<Array<Any>> {
            val configurations = ArrayList<Array<Any>>()

            for (orientationVertical in arrayOf(true, false)) {
                for (scrollDirectionForward in arrayOf(true, false)) {
                    for (rvIntercepts in arrayOf(true, false)) {
                        configurations.addAll(
                            Arrays.asList(
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    0,
                                    0,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    25,
                                    0,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    50,
                                    0,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    100,
                                    0,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    0,
                                    25,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    0,
                                    50,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    0,
                                    100,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    12,
                                    13,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    25,
                                    25,
                                ),
                                arrayOf(
                                    orientationVertical,
                                    scrollDirectionForward,
                                    rvIntercepts,
                                    50,
                                    50,
                                ),
                            )
                        )
                    }
                }
            }

            return configurations
        }
    }
}
