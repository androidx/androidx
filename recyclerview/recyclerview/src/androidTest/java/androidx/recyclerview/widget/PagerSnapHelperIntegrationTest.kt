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
package androidx.recyclerview.widget

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.testutils.SwipeToLocation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PagerSnapHelperIntegrationTest(
    val mConfig: Config,
    private val mReverseScroll: Boolean,
    private val mChildSize: ChildSize,
    private val mApplyPadding: Boolean
) : BaseLinearLayoutManagerTest() {
    enum class ChildSize(val mSizeParam: Int) {
        SMALLER((0.6 * RECYCLERVIEW_SIZE).toInt()),
        SAME(ViewGroup.LayoutParams.MATCH_PARENT),
        LARGER((1.4 * RECYCLERVIEW_SIZE).toInt())
    }

    private fun setUpTest() {
        setupByConfig(mConfig, false, childLayoutParams, parentLayoutParams)
        if (mApplyPadding) {
            // Use even numbers for padding, as we use int division by 2 in the tests
            mRecyclerView.setPadding(10, 22, 0, 0)
        }
        waitForFirstLayout()
        setupSnapHelper()
    }

    @Test
    fun snapOnScrollSameView() {
        setUpTest()

        // Record the current center view.
        val view = findCenterView() as TextView?
        assertCenterAligned(view)
        val scrollDistance = getViewDimension(view) / 2 - 1
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        mLayoutManager.expectIdleState(2)
        smoothScrollBy(scrollDist)
        mLayoutManager.waitForSnap(10)

        // Views have not changed
        val viewAfterFling = findCenterView()
        Assert.assertSame("The view should NOT have scrolled", view, viewAfterFling)
        assertCenterAligned(viewAfterFling)
    }

    @Test
    fun snapOnScrollNextView() {
        setUpTest()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)
        val scrollDistance = getViewDimension(view) / 2 + 1
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        mLayoutManager.expectIdleState(2)
        smoothScrollBy(scrollDist)
        mLayoutManager.waitForSnap(10)

        // Views have not changed
        val viewAfterFling = findCenterView()
        Assert.assertNotSame("The view should have scrolled", view, viewAfterFling)
        val offset =
            if (mConfig.mReverseLayout) {
                if (mReverseScroll) 1 else -1
            } else {
                if (mReverseScroll) -1 else 1
            }
        val expectedPosition = mConfig.mItemCount / 2 + offset
        Assert.assertEquals(
            expectedPosition.toLong(),
            mLayoutManager.getPosition(viewAfterFling!!).toLong()
        )
        assertCenterAligned(viewAfterFling)
    }

    @Test
    fun snapOnFlingSameView() {
        setUpTest()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)

        // Velocity small enough to not scroll to the next view.
        val velocity = (1.000001 * mRecyclerView.minFlingVelocity).toInt()
        val velocityDir = if (mReverseScroll) -velocity else velocity
        mLayoutManager.expectIdleState(2)
        // Scroll at one pixel in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(
            Runnable {
                mRecyclerView.scrollBy(if (mReverseScroll) -1 else 1, if (mReverseScroll) -1 else 1)
            }
        )
        waitForIdleScroll(mRecyclerView)
        Assert.assertTrue(fling(velocityDir, velocityDir))
        // Wait for two settling scrolls: the initial one and the corrective one.
        waitForIdleScroll(mRecyclerView)
        mLayoutManager.waitForSnap(100)
        val viewAfterFling = findCenterView()
        Assert.assertSame("The view should NOT have scrolled", view, viewAfterFling)
        assertCenterAligned(viewAfterFling)
    }

    @Test
    fun snapOnFlingNextView() {
        setUpTest()
        runSnapOnMaxFlingNextView((0.2 * mRecyclerView.maxFlingVelocity).toInt())
    }

    @Test
    fun snapOnMaxFlingNextView() {
        setUpTest()
        runSnapOnMaxFlingNextView(mRecyclerView.maxFlingVelocity)
    }

    @Ignore // b/269644618
    @Test
    fun snapWhenFlingToSnapPosition() {
        setUpTest()
        runSnapOnFlingExactlyToNextView()
    }

    private val parentLayoutParams: RecyclerView.LayoutParams
        get() = RecyclerView.LayoutParams(RECYCLERVIEW_SIZE, RECYCLERVIEW_SIZE)

    private val childLayoutParams: RecyclerView.LayoutParams
        get() =
            RecyclerView.LayoutParams(
                if (mConfig.mOrientation == RecyclerView.HORIZONTAL) {
                    mChildSize.mSizeParam
                } else {
                    ViewGroup.LayoutParams.MATCH_PARENT
                },
                if (mConfig.mOrientation == RecyclerView.VERTICAL) {
                    mChildSize.mSizeParam
                } else {
                    ViewGroup.LayoutParams.MATCH_PARENT
                }
            )

    private fun runSnapOnMaxFlingNextView(velocity: Int) {
        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)
        val velocityDir = if (mReverseScroll) -velocity else velocity
        mLayoutManager.expectIdleState(1)

        // Scroll at one pixel in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(
            Runnable {
                mRecyclerView.scrollBy(if (mReverseScroll) -1 else 1, if (mReverseScroll) -1 else 1)
            }
        )
        waitForIdleScroll(mRecyclerView)
        Assert.assertTrue(fling(velocityDir, velocityDir))
        mLayoutManager.waitForSnap(100)
        val viewAfterFling = findCenterView()
        Assert.assertNotSame("The view should have scrolled", view, viewAfterFling)
        val offset =
            if (mConfig.mReverseLayout) {
                if (mReverseScroll) 1 else -1
            } else {
                if (mReverseScroll) -1 else 1
            }
        val expectedPosition = mConfig.mItemCount / 2 + offset
        Assert.assertEquals(
            expectedPosition.toLong(),
            mLayoutManager.getPosition(viewAfterFling!!).toLong()
        )
        assertCenterAligned(viewAfterFling)
    }

    private fun runSnapOnFlingExactlyToNextView() {
        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)

        // Determine the target item to scroll to
        val offset =
            if (mConfig.mReverseLayout) {
                if (mReverseScroll) 1 else -1
            } else {
                if (mReverseScroll) -1 else 1
            }
        val expectedPosition = mConfig.mItemCount / 2 + offset

        // Smooth scroll in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(
            Runnable { mRecyclerView.smoothScrollToPosition(expectedPosition) }
        )
        waitForDistanceToTarget(expectedPosition, .5f)

        // Interrupt scroll and fling to target view, ending exactly when the view is snapped
        mLayoutManager.expectIdleState(1)
        Espresso.onView(
                CoreMatchers.allOf(
                    ViewMatchers.isDescendantOfA(
                        ViewMatchers.isAssignableFrom(RecyclerView::class.java)
                    ),
                    ViewMatchers.withText(mTestAdapter.getItemAt(expectedPosition).displayText)
                )
            )
            .perform(SwipeToLocation.flingToCenter())
        waitForIdleScroll(mRecyclerView)

        // Wait until the RecyclerView comes to a rest
        mLayoutManager.waitForSnap(100)

        // Check the result
        val viewAfterFling = findCenterView()
        Assert.assertNotSame("The view should have scrolled", view, viewAfterFling)
        Assert.assertEquals(
            expectedPosition.toLong(),
            mLayoutManager.getPosition(viewAfterFling!!).toLong()
        )
        assertCenterAligned(viewAfterFling)
    }

    private fun setupSnapHelper() {
        val snapHelper: SnapHelper = PagerSnapHelper()

        // Do we expect a snap when attaching the SnapHelper?
        val centerView = findCenterView()
        val expectSnap = distFromCenter(centerView) != 0
        mLayoutManager.expectIdleState(1)
        snapHelper.attachToRecyclerView(mRecyclerView)
        if (expectSnap) {
            mLayoutManager.waitForSnap(2)
        }
        mLayoutManager.expectLayouts(1)
        scrollToPositionWithOffset(mConfig.mItemCount / 2, scrollOffset)
        mLayoutManager.waitForLayout(2)
    }

    private val scrollOffset: Int
        get() {
            val params = mTestAdapter.mLayoutParams ?: return 0
            if (
                mConfig.mOrientation == RecyclerView.HORIZONTAL &&
                    params.width == ViewGroup.LayoutParams.MATCH_PARENT ||
                    mConfig.mOrientation == RecyclerView.VERTICAL &&
                        params.height == ViewGroup.LayoutParams.MATCH_PARENT
            ) {
                return 0
            }
            // In reverse layouts, the rounding error of x/2 ends up on the other side of the center
            // Instead of fixing all asserts, just move the rounding error to the same side as
            // without
            // reverse layout.
            val reverseAdjustment =
                ((if (mConfig.mReverseLayout) 1 else 0) *
                    // For larger children, the offset becomes negative, so
                    // we need to subtract the adjustment rather than add it
                    if (mChildSize == ChildSize.LARGER) -1 else 1)
            return if (mConfig.mOrientation == RecyclerView.HORIZONTAL) {
                (getWidthMinusPadding(mRecyclerView) - params.width + reverseAdjustment) / 2
            } else {
                (getHeightMinusPadding(mRecyclerView) - params.height + reverseAdjustment) / 2
            }
        }

    private fun findCenterView(): View? {
        return mRecyclerView.findChildViewUnder(rvCenterX.toFloat(), rvCenterY.toFloat())
    }

    private fun getViewDimension(view: View?): Int {
        val helper: OrientationHelper =
            if (mLayoutManager.canScrollHorizontally()) {
                OrientationHelper.createHorizontalHelper(mLayoutManager)
            } else {
                OrientationHelper.createVerticalHelper(mLayoutManager)
            }
        return helper.getDecoratedMeasurement(view)
    }

    private fun getWidthMinusPadding(view: View): Int {
        return view.width - view.getPaddingLeft() - view.getPaddingRight()
    }

    private fun getHeightMinusPadding(view: View): Int {
        return view.height - view.paddingTop - view.paddingBottom
    }

    private val rvCenterX: Int
        get() = getWidthMinusPadding(mRecyclerView) / 2 + mRecyclerView.getPaddingLeft()

    private val rvCenterY: Int
        get() = getHeightMinusPadding(mRecyclerView) / 2 + mRecyclerView.paddingTop

    private fun getViewCenterX(view: View?): Int {
        return mLayoutManager.getViewBounds(view).centerX()
    }

    private fun getViewCenterY(view: View?): Int {
        return mLayoutManager.getViewBounds(view).centerY()
    }

    private fun assertCenterAligned(view: View?) {
        if (mLayoutManager.canScrollHorizontally()) {
            Assert.assertEquals(rvCenterX.toLong(), getViewCenterX(view).toLong())
        } else {
            Assert.assertEquals(rvCenterY.toLong(), getViewCenterY(view).toLong())
        }
    }

    private fun distFromCenter(view: View?): Int {
        return if (mLayoutManager.canScrollHorizontally()) {
            abs((rvCenterX - getViewCenterX(view)).toDouble()).toInt()
        } else {
            abs((rvCenterY - getViewCenterY(view)).toDouble()).toInt()
        }
    }

    private fun fling(velocityX: Int, velocityY: Int): Boolean {
        val didStart = AtomicBoolean(false)
        mActivityRule.runOnUiThread(
            Runnable {
                val result = mRecyclerView.fling(velocityX, velocityY)
                didStart.set(result)
            }
        )
        if (!didStart.get()) {
            return false
        }
        waitForIdleScroll(mRecyclerView)
        return true
    }

    /**
     * Waits until the RecyclerView has smooth scrolled till within the given margin from the target
     * item. The percentage is relative to the size of the target view.
     *
     * @param targetPosition The adapter position of the view we want to scroll to
     * @param distancePercent The distance from the view when we stop waiting, relative to the
     *   target view
     */
    @Throws(InterruptedException::class)
    private fun waitForDistanceToTarget(
        targetPosition: Int,
        @Suppress("SameParameterValue") distancePercent: Float
    ) {
        val latch = CountDownLatch(1)
        mRecyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val target = mLayoutManager.findViewByPosition(targetPosition) ?: return
                    val distancePx = distFromCenter(target)
                    val size =
                        if (mConfig.mOrientation == RecyclerView.HORIZONTAL) {
                            target.width
                        } else {
                            target.height
                        }
                    if (distancePx.toFloat() / size <= distancePercent) {
                        latch.countDown()
                    }
                }
            }
        )
        Assert.assertTrue(
            "should be close enough to the target view within 10 seconds",
            latch.await(10, TimeUnit.SECONDS)
        )
    }

    companion object {
        private const val RECYCLERVIEW_SIZE = 1000

        @Parameterized.Parameters(
            name = "config:{0},reverseScroll:{1},mChildSize:{2},applyPadding:{3}"
        )
        @JvmStatic
        fun params(): List<Array<Any>> {
            val result: MutableList<Array<Any>> = ArrayList()
            val configs = createBaseVariations()
            for (config in configs) {
                for (reverseScroll in booleanArrayOf(false, true)) {
                    for (childSize in ChildSize.values()) {
                        for (applyPadding in booleanArrayOf(false, true)) {
                            if (!config.mWrap) {
                                result.add(arrayOf(config, reverseScroll, childSize, applyPadding))
                            }
                        }
                    }
                }
            }
            return result
        }
    }
}
