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
import android.widget.TextView
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
internal class GridLayoutManagerSnappingTest(
    val mConfig: Config,
    private val mReverseScroll: Boolean,
    private val mApplyPadding: Boolean
) : BaseGridLayoutManagerTest() {
    @Throws(Throwable::class)
    override fun setupBasic(config: Config, testAdapter: GridTestAdapter): RecyclerView {
        val rv = super.setupBasic(config, testAdapter)
        if (mApplyPadding) {
            rv.setPadding(17, 23, 0, 0)
        }
        return rv
    }

    @Ignore // b/244324972
    @Test
    @Throws(Throwable::class)
    fun snapOnScrollSameView() {
        val config = mConfig.clone() as Config
        val recyclerView = setupBasic(config)
        waitForFirstLayout(recyclerView)
        setupSnapHelper()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)
        val scrollDistance = getViewDimension(view) / 2 - 1
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        mGlm.expectIdleState(2)
        smoothScrollBy(scrollDist)
        mGlm.waitForSnap(25)

        // Views have not changed
        val viewAfterFling = findCenterView()
        Assert.assertSame("The view should have scrolled", view, viewAfterFling)
        assertCenterAligned(viewAfterFling)
    }

    @SdkSuppress(minSdkVersion = 22) // b/271599012
    @Test
    @Throws(Throwable::class)
    fun snapOnScrollNextItem() {
        val config = mConfig.clone() as Config
        val recyclerView = setupBasic(config)
        waitForFirstLayout(recyclerView)
        setupSnapHelper()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)
        val viewText = (view as TextView?)!!.getText()
        val scrollDistance = getViewDimension(view) + 1
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        smoothScrollBy(scrollDist)
        waitForIdleScroll(mRecyclerView)
        waitForIdleScroll(mRecyclerView)
        val viewAfterScroll = findCenterView()
        val viewAfterFlingText = (viewAfterScroll as TextView?)!!.getText()
        Assert.assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText)
        assertCenterAligned(viewAfterScroll)
    }

    @SdkSuppress(minSdkVersion = 22) // b/271599012
    @Test
    @Throws(Throwable::class)
    fun snapOnFlingSameView() {
        val config = mConfig.clone() as Config
        val recyclerView = setupBasic(config)
        waitForFirstLayout(recyclerView)
        setupSnapHelper()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)

        // Velocity small enough to not scroll to the next view.
        val velocity = (1.000001 * mRecyclerView.minFlingVelocity).toInt()
        val velocityDir = if (mReverseScroll) -velocity else velocity
        mGlm.expectIdleState(2)
        Assert.assertTrue(fling(velocityDir, velocityDir))
        // Wait for two settling scrolls: the initial one and the corrective one.
        waitForIdleScroll(mRecyclerView)
        mGlm.waitForSnap(100)
        val viewAfterFling = findCenterView()
        Assert.assertSame("The view should NOT have scrolled", view, viewAfterFling)
        assertCenterAligned(viewAfterFling)
    }

    @SdkSuppress(minSdkVersion = 22) // b/271599012
    @Test
    @Throws(Throwable::class)
    fun snapOnFlingNextView() {
        val config = mConfig.clone() as Config
        val recyclerView = setupBasic(config)
        waitForFirstLayout(recyclerView)
        setupSnapHelper()

        // Record the current center view.
        val view = findCenterView()
        assertCenterAligned(view)
        val viewText = (view as TextView?)!!.getText()

        // Velocity high enough to scroll beyond the current view.
        val velocity = (0.25 * mRecyclerView.maxFlingVelocity).toInt()
        val velocityDir = if (mReverseScroll) -velocity else velocity
        mGlm.expectIdleState(1)
        Assert.assertTrue(fling(velocityDir, velocityDir))
        mGlm.waitForSnap(100)
        instrumentation.waitForIdleSync()
        val viewAfterFling = findCenterView()
        val viewAfterFlingText = (viewAfterFling as TextView?)!!.getText()
        Assert.assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText)
        assertCenterAligned(viewAfterFling)
    }

    @Throws(Throwable::class)
    private fun setupSnapHelper() {
        val snapHelper: SnapHelper = LinearSnapHelper()
        mGlm.expectIdleState(1)
        snapHelper.attachToRecyclerView(mRecyclerView)
        mGlm.waitForSnap(25)
        mGlm.expectLayout(1)
        scrollToPosition(mConfig.mItemCount / 2)
        mGlm.waitForLayout(2)
        val view = findCenterView()
        val scrollDistance = distFromCenter(view) / 2
        if (scrollDistance == 0) {
            return
        }
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        mGlm.expectIdleState(2)
        smoothScrollBy(scrollDist)
        mGlm.waitForSnap(25)
    }

    private fun findCenterView(): View? {
        return mRecyclerView.findChildViewUnder(rvCenterX.toFloat(), rvCenterY.toFloat())
    }

    private fun getViewDimension(view: View?): Int {
        val helper: OrientationHelper = if (mGlm.canScrollHorizontally()) {
            OrientationHelper.createHorizontalHelper(mGlm)
        } else {
            OrientationHelper.createVerticalHelper(mGlm)
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
        return mGlm.getViewBounds(view).centerX()
    }

    private fun getViewCenterY(view: View?): Int {
        return mGlm.getViewBounds(view).centerY()
    }

    private fun assertCenterAligned(view: View?) {
        if (mGlm.canScrollHorizontally()) {
            Assert.assertEquals(
                "The child should align with the center of the parent",
                rvCenterX.toFloat(), getViewCenterX(view).toFloat(), 1f
            )
        } else {
            Assert.assertEquals(
                "The child should align with the center of the parent",
                rvCenterY.toFloat(), getViewCenterY(view).toFloat(), 1f
            )
        }
    }

    private fun distFromCenter(view: View?): Int {
        return if (mGlm.canScrollHorizontally()) {
            abs((rvCenterX - getViewCenterX(view)).toDouble()).toInt()
        } else {
            abs((rvCenterY - getViewCenterY(view)).toDouble()).toInt()
        }
    }

    @Throws(Throwable::class)
    private fun fling(velocityX: Int, velocityY: Int): Boolean {
        val didStart = AtomicBoolean(false)
        mActivityRule.runOnUiThread(Runnable {
            val result = mRecyclerView.fling(velocityX, velocityY)
            didStart.set(result)
        })
        if (!didStart.get()) {
            return false
        }
        waitForIdleScroll(mRecyclerView)
        return true
    }

    companion object {
        @Parameterized.Parameters(name = "config:{0},reverseScroll:{1},applyPadding:{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            val result: MutableList<Array<Any>> = ArrayList()
            val configs = createBaseVariations()
            for (config in configs) {
                for (reverseScroll in booleanArrayOf(true, false)) {
                    for (applyPadding in booleanArrayOf(true, false)) {
                        result.add(arrayOf(config, reverseScroll, applyPadding))
                    }
                }
            }
            return result
        }
    }
}
