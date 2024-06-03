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
import androidx.recyclerview.test.awaitScrollIdle
import androidx.test.filters.LargeTest
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert
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

    @Test
    @Throws(Throwable::class)
    fun snapOnScrollSameView() =
        runBlocking(Dispatchers.Main) {
            val config = mConfig.clone() as Config
            val recyclerView = setupBasic(config)
            awaitFirstLayout(recyclerView)
            setupSnapHelper()

            // Record the current center view.
            val view = findCenterView()
            assertCenterAligned(view)

            val scrollDistance = getViewDimension(view) / 2 - 1
            val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
            mRecyclerView.smoothScrollByOnMainAxis(scrollDist)
            awaitScrollAndSnapIdle(25)

            // Views have not changed
            val viewAfterFling = findCenterView()
            Assert.assertSame("The view should NOT have scrolled", view, viewAfterFling)
            assertCenterAligned(viewAfterFling)
        }

    @Test
    @Throws(Throwable::class)
    fun snapOnScrollNextItem() =
        runBlocking(Dispatchers.Main) {
            val config = mConfig.clone() as Config
            val recyclerView = setupBasic(config)
            awaitFirstLayout(recyclerView)
            setupSnapHelper()

            // Record the current center view.
            val view = findCenterView()
            assertCenterAligned(view)
            val viewText = (view as TextView?)!!.getText()
            val scrollDistance = getViewDimension(view) + 1
            val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
            mRecyclerView.smoothScrollByOnMainAxis(scrollDist)
            awaitScrollAndSnapIdle(25)

            val viewAfterScroll = findCenterView()
            val viewAfterFlingText = (viewAfterScroll as TextView?)!!.getText()
            Assert.assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText)
            assertCenterAligned(viewAfterScroll)
        }

    @Test
    @Throws(Throwable::class)
    fun snapOnFlingSameView() =
        runBlocking(Dispatchers.Main) {
            val config = mConfig.clone() as Config
            val recyclerView = setupBasic(config)
            awaitFirstLayout(recyclerView)
            setupSnapHelper()

            // Record the current center view.
            val view = findCenterView()
            assertCenterAligned(view)

            // Velocity small enough to not scroll to the next view.
            val velocity = (1.000001 * mRecyclerView.minFlingVelocity).toInt()
            val velocityDir = if (mReverseScroll) -velocity else velocity
            mGlm.expectIdleState(2)
            Assert.assertTrue(fling(velocityDir, velocityDir))
            awaitScrollAndSnapIdle(25)
            val viewAfterFling = findCenterView()
            Assert.assertSame("The view should NOT have scrolled", view, viewAfterFling)
            assertCenterAligned(viewAfterFling)
        }

    @Test
    @Throws(Throwable::class)
    fun snapOnFlingNextView() =
        runBlocking(Dispatchers.Main) {
            val config = mConfig.clone() as Config
            val recyclerView = setupBasic(config)
            awaitFirstLayout(recyclerView)
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
            awaitScrollAndSnapIdle(25)

            val viewAfterFling = findCenterView()
            val viewAfterFlingText = (viewAfterFling as TextView?)!!.getText()
            Assert.assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText)
            assertCenterAligned(viewAfterFling)
        }

    private suspend fun setupSnapHelper() {
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(mRecyclerView)
        awaitScrollAndSnapIdle(25)
        mGlm.expectLayout(1)
        scrollToPosition(mConfig.mItemCount / 2)
        mGlm.awaitLayout(2)
        val view = findCenterView()
        val scrollDistance = distFromCenter(view) / 2
        if (scrollDistance == 0) {
            return
        }
        val scrollDist = if (mReverseScroll) -scrollDistance else scrollDistance
        mRecyclerView.smoothScrollByOnMainAxis(scrollDist)
        awaitScrollAndSnapIdle(25)
    }

    private fun findCenterView(): View? {
        return mRecyclerView.findChildViewUnder(rvCenterX.toFloat(), rvCenterY.toFloat())
    }

    private fun getViewDimension(view: View?): Int {
        val helper: OrientationHelper =
            if (mGlm.canScrollHorizontally()) {
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
                rvCenterX.toFloat(),
                getViewCenterX(view).toFloat(),
                1f
            )
        } else {
            Assert.assertEquals(
                "The child should align with the center of the parent",
                rvCenterY.toFloat(),
                getViewCenterY(view).toFloat(),
                1f
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

    private suspend fun fling(velocityX: Int, velocityY: Int): Boolean {
        var didStart: Boolean
        withContext(Dispatchers.Main.immediate) {
            didStart = mRecyclerView.fling(velocityX, velocityY)
        }
        return didStart
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

    private suspend fun awaitFirstLayout(recyclerView: RecyclerView) {
        mGlm.expectLayout(1)
        setRecyclerView(recyclerView)
        mGlm.awaitLayout(2)
    }

    private suspend fun WrappedGridLayoutManager.awaitLayout(seconds: Int) {
        runInterruptible(Dispatchers.IO) {
            mLayoutLatch.await((seconds * if (DEBUG) 1000 else 1).toLong(), TimeUnit.SECONDS)
        }
        awaitFrame()
    }

    private suspend fun awaitScrollAndSnapIdle(seconds: Int) {
        val secondsToWait = seconds * (if (DEBUG) 100 else 1)
        withTimeout(secondsToWait.seconds) {
            withContext(Dispatchers.Main) {
                // SnapHelper uses RecyclerView's scroll idle as a signal to start the snap
                // This means that we get an idle signal when the scroll is not *really* idle,
                // but instead actually about to be restarted for the recovery scroll.
                // Trying to guess the exact number of scroll idle calls is very fragile
                // (consider: what if you happen to land in exactly the right spot?),
                // so we just wait for the scroll to go idle _and then stay idle_
                // for the rest of the frame.
                // If it doesn't stay idle, then we're not done scrolling and should wait again.
                while (true) {
                    mRecyclerView.awaitScrollIdle()
                    awaitFrame()
                    if (mRecyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        break
                    }
                }
            }
        }
    }
}

private fun RecyclerView.smoothScrollByOnMainAxis(dt: Int) {
    if (layoutManager!!.canScrollHorizontally()) {
        smoothScrollBy(dt, 0)
    } else {
        smoothScrollBy(0, dt)
    }
}
