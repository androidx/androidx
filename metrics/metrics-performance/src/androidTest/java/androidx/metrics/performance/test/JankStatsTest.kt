/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.metrics.performance.test

import androidx.core.util.Pair
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.Companion.jankHeuristicMultiplier
import androidx.metrics.performance.JankStats.OnFrameListener
import androidx.metrics.performance.StateInfo
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class JankStatsTest {

    private lateinit var jankStats: JankStats
    private lateinit var delayedActivity: DelayedActivity
    private lateinit var delayedView: DelayedView
    private lateinit var latchedListener: LatchedListener

    private val IDLE_PAUSE = 1000
    private val NUM_FRAMES = 10

    @Rule
    @JvmField
    var delayedActivityRule: ActivityScenarioRule<DelayedActivity> =
        ActivityScenarioRule(DelayedActivity::class.java)

    @Before
    fun setup() {
        val scenario = delayedActivityRule.scenario
        scenario.onActivity { activity: DelayedActivity? ->
            delayedActivity = activity!!
            delayedView = delayedActivity.findViewById(R.id.delayedView)
            latchedListener = LatchedListener()
            latchedListener.latch = CountDownLatch(1)
            jankStats = JankStats(delayedView, latchedListener)
            jankStats.setTrackingEnabled(true)
        }
    }

    @Test
    @UiThreadTest
    fun testGetInstance() {
        assert(JankStats.getInstance(delayedView) == jankStats)
    }

    @Test
    @UiThreadTest
    fun testEnable() {
        assertTrue(jankStats.isTrackingEnabled())
        jankStats.setTrackingEnabled(false)
        assertFalse(jankStats.isTrackingEnabled())
        jankStats.setTrackingEnabled(true)
        assertTrue(jankStats.isTrackingEnabled())
    }

    @Test
    fun testNoJank() {
        val frameDelay = 0

        // Prime the system first to flush out any frames happening before we start forcing jank
        runDelayTest(0, NUM_FRAMES, latchedListener)
        latchedListener.reset()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        Assert.assertEquals("numJankFrames should equal 0", 0, latchedListener.numJankFrames)
        latchedListener.reset()

        jankHeuristicMultiplier = 0f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        Assert.assertEquals(
            "multiplier 0, extremeMs 0: numJankFrames should equal NUM_FRAMES",
            NUM_FRAMES, latchedListener.numJankFrames
        )
    }

    @Test
    fun testRegularJank() {
        val frameDelay = 100

        // Prime the system first to flush out any frames happening before we start forcing jank
        runDelayTest(0, NUM_FRAMES, latchedListener)
        latchedListener.reset()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        Assert.assertEquals(
            "numJankFrames should equal NUM_FRAMES",
            NUM_FRAMES,
            latchedListener.numJankFrames
        )
        latchedListener.reset()

        jankHeuristicMultiplier = 20f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        Assert.assertEquals(
            "multiplier 20, extremeMs 0: numJankFrames should equal 0",
            0, latchedListener.numJankFrames
        )
    }

    @Test
    fun testFrameStates() {
        val frameDelay = 100
        var states: List<StateInfo>

        // Clear out any existing states first to make the testing below more deterministic
        jankStats.clearStates()

        // prime the system first -some platform versions start with historical data which may not
        // have the forced-jank frames up front that we're counting on. Running it twice ensures
        // that our assumptions will be true the second time around
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        latchedListener.reset()

        val state0 = StateInfo("Testing State 0", "sampleStateA")
        val state1 = StateInfo("Testing State 1", "sampleStateB")
        val state2 = StateInfo("Testing State 2", "sampleStateC")
        jankStats.addState(state0.stateName, state0.state)
        jankStats.addState(state1.stateName, state1.state)
        jankStats.addSingleFrameState(state2.stateName, state2.state)
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        Assert.assertEquals(
            "frameDelay 100: There should be 10 frames with jank data", NUM_FRAMES,
            latchedListener.jankData.size
        )
        var item0: FrameData = latchedListener.jankData[0]
        states = item0.states
        Assert.assertEquals("There should be 3 states at frame 0", 3, states.size)
        for (state in states) {
            // Test that every state is in the states set above
            Assert.assertThat(state, Matchers.isIn(listOf(state0, state1, state2)))
        }
        // Test that all states set above are in the states for the first frame
        Assert.assertThat(state0, Matchers.isIn(states))
        Assert.assertThat(state1, Matchers.isIn(states))
        Assert.assertThat(state2, Matchers.isIn(states))

        // Now test the rest of the frames, which should not include singleFrameState state2
        for (i in 1 until NUM_FRAMES) {
            val item = latchedListener.jankData[i]
            states = item.states
            Assert.assertEquals("There should be 2 states at frame 0", 2, states.size)
            for (state in states) {
                Assert.assertThat(
                    state,
                    Matchers.either(Matchers.`is`(state0)).or(Matchers.`is`(state1))
                )
            }
        }

        // reset by clearing states
        latchedListener.reset()
        jankStats.clearStates()

        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        Assert.assertEquals(
            "States should be empty after being cleared",
            0,
            item0.states.size
        )
        latchedListener.reset()
        val state3 = Pair("Testing State 3", "sampleStateD")
        val state4 = Pair("Testing State 4", "sampleStateE")
        jankStats.addState(state3.first!!, state3.second!!)
        jankStats.addState(state4.first!!, state4.second!!)
        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        states = item0.states
        Assert.assertEquals(2, states.size)
        latchedListener.reset()

        // Test removal of state3 and replacement of state4
        jankStats.removeState(state3.first!!)
        jankStats.addState(state4.first!!, "sampleStateF")
        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        states = item0.states
        Assert.assertEquals(1, states.size)
        Assert.assertEquals(state4.first, states[0].stateName)
        Assert.assertEquals("sampleStateF", states[0].state)
        latchedListener.reset()
    }

    private fun runDelayTest(frameDelay: Int, numFrames: Int, listener: LatchedListener) {
        val latch = CountDownLatch(numFrames)
        listener.latch = latch
        delayedActivity.repetions = numFrames
        delayedActivity.delayMs = frameDelay.toLong()
        delayedActivityRule.scenario.onActivity {
            delayedActivity.invalidate()
        }
        try {
            Thread.sleep((numFrames * frameDelay + IDLE_PAUSE).toLong())
        } catch (e: Exception) {
        }
        try {
            latch.await(20, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            assert(false)
        }
    }

    internal inner class LatchedListener : OnFrameListener {
        var numJankFrames = 0
        var jankData = mutableListOf<FrameData>()
        var latch: CountDownLatch? = null

        fun reset() {
            jankData.clear()
            numJankFrames = 0
        }

        override fun onJankStatsFrame(
            frameData: FrameData
        ) {
            if (latch == null) {
                throw Exception("latch not set in LatchedListener")
            } else {
                if (frameData.isJank) {
                    this.numJankFrames++
                }
                this.jankData.add(FrameData(frameData))
                latch!!.countDown()
            }
        }
    }
}