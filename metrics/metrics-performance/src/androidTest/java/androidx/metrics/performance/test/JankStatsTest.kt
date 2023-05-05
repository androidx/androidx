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

import android.os.Build
import android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.view.Choreographer
import androidx.annotation.RequiresApi
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import androidx.metrics.performance.PerformanceMetricsState
import androidx.metrics.performance.StateInfo
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.hamcrest.Matchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class JankStatsTest {

    private lateinit var jankStats: JankStats
    private lateinit var metricsState: PerformanceMetricsState
    private lateinit var delayedActivity: DelayedActivity
    private lateinit var delayedView: DelayedView
    private lateinit var latchedListener: LatchedListener

    private var frameInit: FrameInitCompat

    private val NUM_FRAMES = 10

    /**
     * On some older APIs and emulators, frames may occasionally take longer than predicted
     * jank. We check against this MIN duration to avoid flaky tests.
     */
    private val MIN_JANK_NS = 100000000

    init {
        if (Build.VERSION.SDK_INT >= 16) {
            frameInit = FrameInit16(this)
        } else {
            frameInit = FrameInitCompat(this)
        }
    }

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
            jankStats = JankStats.createAndTrack(delayedActivity.window, latchedListener)
            metricsState = PerformanceMetricsState.getHolderForHierarchy(delayedView).state!!
        }
    }

    @Test
    @UiThreadTest
    fun testGetInstance() {
        assert(PerformanceMetricsState.getHolderForHierarchy(delayedView).state == metricsState)
    }

    /**
     * Test adding/removing listeners while inside the listener callback (on the same thread)
     */
    @Test
    fun testConcurrentListenerModifications() {
        lateinit var jsSecond: JankStats
        lateinit var jsThird: JankStats

        // We add two more JankStats object with another listener which will add/remove
        // in the callback. One more listener will not necessarily trigger the issue, because if
        // that listener is at the end of the internal list of listeners, then the iterator will
        // not try to find another one after it has been removed. So we make the list large enough
        // that we will be removing a listener in the middle.
        val secondListener = OnFrameListener {
            jsSecond.isTrackingEnabled = !jsSecond.isTrackingEnabled
            jsThird.isTrackingEnabled = !jsThird.isTrackingEnabled
        }
        delayedActivityRule.scenario.onActivity {
            jsSecond = JankStats.createAndTrack(delayedActivity.window, secondListener)
            jsThird = JankStats.createAndTrack(delayedActivity.window, secondListener)
        }
        runDelayTest(frameDelay = 0, NUM_FRAMES, latchedListener)
    }

    @Test
    fun testEnable() {
        assertTrue(jankStats.isTrackingEnabled)
        jankStats.isTrackingEnabled = false
        assertFalse(jankStats.isTrackingEnabled)
        jankStats.isTrackingEnabled = true
        assertTrue(jankStats.isTrackingEnabled)
    }

    @Test
    fun testEquality() {
        val states1 = listOf(StateInfo("1", "a"))
        val states2 = listOf(StateInfo("1", "a"), StateInfo("2", "b"))
        val frameDataBase = FrameData(0, 0, true, states1)
        val frameDataBaseCopy = FrameData(0, 0, true, states1)
        val frameDataBaseA = FrameData(0, 0, true, states2)
        val frameDataBaseB = FrameData(0, 0, false, states1)
        val frameDataBaseC = FrameData(0, 1, true, states1)
        val frameDataBaseD = FrameData(1, 0, true, states1)

        val frameData24 = FrameDataApi24(0, 0, 0, true, states1)
        val frameData24Copy = FrameDataApi24(0, 0, 0, true, states1)
        val frameData24A = FrameDataApi24(0, 0, 1, true, states1)

        val frameData31 = FrameDataApi31(0, 0, 0, 0, 0, true, states1)
        val frameData31Copy = FrameDataApi31(0, 0, 0, 0, 0, true, states1)
        val frameData31A = FrameDataApi31(0, 0, 0, 1, 0, true, states1)

        assertEquals(frameDataBase, frameDataBase)
        assertEquals(frameDataBase, frameDataBaseCopy)
        assertEquals(frameData24, frameData24)
        assertEquals(frameData24, frameData24Copy)
        assertEquals(frameData31, frameData31)
        assertEquals(frameData31, frameData31Copy)

        assertNotEquals(frameDataBase, frameDataBaseA)
        assertNotEquals(frameDataBase, frameDataBaseB)
        assertNotEquals(frameDataBase, frameDataBaseC)
        assertNotEquals(frameDataBase, frameDataBaseD)
        assertNotEquals(frameDataBase, frameData24)
        assertNotEquals(frameData24, frameDataBase)
        assertNotEquals(frameDataBase, frameData31)
        assertNotEquals(frameData31, frameDataBase)
        assertNotEquals(frameData24, frameData31)
        assertNotEquals(frameData31, frameData24)
        assertNotEquals(frameData24, frameData24A)
        assertNotEquals(frameData31, frameData31A)
    }

    @SdkSuppress(minSdkVersion = JELLY_BEAN)
    @Test
    @Ignore("b/272347202")
    fun testNoJank() {
        val frameDelay = 0

        frameInit.initFramePipeline()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals("numJankFrames should equal 0", 0, latchedListener.numJankFrames)
        latchedListener.reset()

        jankStats.jankHeuristicMultiplier = 0f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        // FrameMetrics sometimes drops a frame, so the total number of
        // jankData items might be less than NUM_FRAMES
        assertEquals(
            "jank frames != NUMFRAMES",
            NUM_FRAMES, latchedListener.numJankFrames
        )
        assertTrue(
            "With heuristicMultiplier 0, should be at least ${NUM_FRAMES - 1} " +
                "frames with jank data, not ${latchedListener.numJankFrames}",
            latchedListener.numJankFrames >= (NUM_FRAMES - 1)
        )
    }

    @SdkSuppress(minSdkVersion = JELLY_BEAN)
    @Test
    fun testMultipleListeners() {
        var secondListenerLatch = CountDownLatch(0)
        val frameDelay = 0

        frameInit.initFramePipeline()

        var numSecondListenerCalls = 0
        val secondListenerStates = mutableListOf<StateInfo>()
        val secondListener = OnFrameListener { volatileFrameData ->
            secondListenerStates.addAll(volatileFrameData.states)
            numSecondListenerCalls++
            if (numSecondListenerCalls >= NUM_FRAMES) {
                secondListenerLatch.countDown()
            }
        }
        lateinit var jankStats2: JankStats
        val scenario = delayedActivityRule.scenario
        scenario.onActivity { _ ->
            jankStats2 = JankStats.createAndTrack(delayedActivity.window, secondListener)
        }
        val testState = StateInfo("Testing State", "sampleState")
        metricsState.putSingleFrameState(testState.key, testState.value)

        // in case earlier frames arrive before our test begins
        secondListenerStates.clear()
        secondListenerLatch = CountDownLatch(1)
        latchedListener.reset()
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        secondListenerLatch.await(frameDelay * NUM_FRAMES + 1000L, TimeUnit.MILLISECONDS)
        val jankData: FrameData = latchedListener.jankData[0]
        assertTrue("No calls to second listener", numSecondListenerCalls > 0)
        assertEquals(listOf(testState), jankData.states)
        assertEquals(listOf(testState), secondListenerStates)

        jankStats2.isTrackingEnabled = false
        numSecondListenerCalls = 0
        latchedListener.reset()
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(0, numSecondListenerCalls)
        assertTrue("Removal of second listener should not have removed first",
            latchedListener.jankData.size > 0)

        // Now make sure that extra listeners can be added concurrently from other threads
        latchedListener.reset()
        val listenerPostingThread = Thread()
        var numNewListeners = 0
        lateinit var poster: Runnable
        poster = Runnable {
            JankStats.createAndTrack(
                delayedActivity.window,
                secondListener
            )
            ++numNewListeners
            if (numNewListeners < 100) {
                delayedView.postDelayed(poster, 10)
            }
        }
        scenario.onActivity { _ ->
            listenerPostingThread.run {
                poster.run()
            }
        }
        listenerPostingThread.start()
        // add listeners concurrently - no asserts here, just testing whether we
        // avoid any concurrency issues with adding and using multiple listeners
        runDelayTest(frameDelay, NUM_FRAMES * 100, latchedListener)
    }

    @SdkSuppress(minSdkVersion = JELLY_BEAN)
    @Test
    fun testRegularJank() {
        val frameDelay = 100

        frameInit.initFramePipeline()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        // FrameMetrics sometimes drops a frame, so the total number of
        // jankData items might be less than NUM_FRAMES
        assertTrue(
            "There should be at least ${NUM_FRAMES - 1} frames with jank data, " +
                "not ${latchedListener.jankData.size}",
            latchedListener.jankData.size >= (NUM_FRAMES - 1)
        )
        latchedListener.reset()

        jankStats.jankHeuristicMultiplier = 20f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(
            "multiplier 20, extremeMs 0: numJankFrames should equal 0",
            0, latchedListener.numJankFrames
        )
    }

    @SdkSuppress(minSdkVersion = JELLY_BEAN)
    @Test
    @Ignore("b/272347202")
    fun testFrameStates() {
        val frameDelay = 0

        frameInit.initFramePipeline()

        val state0 = StateInfo("Testing State 0", "sampleStateA")
        val state1 = StateInfo("Testing State 1", "sampleStateB")
        val state2 = StateInfo("Testing State 2", "sampleStateC")
        metricsState.putState(state0.key, state0.value)
        metricsState.putState(state1.key, state1.value)
        metricsState.putSingleFrameState(state2.key, state2.value)
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(
            "frameDelay 100: There should be $NUM_FRAMES frames with jank data", NUM_FRAMES,
            latchedListener.jankData.size
        )
        var item0: FrameData = latchedListener.jankData[0]
        assertEquals("There should be 3 states at frame 0", 3,
            item0.states.size)
        for (state in item0.states) {
            // Test that every state is in the states set above
            assertThat(state, Matchers.isIn(listOf(state0, state1, state2)))
        }
        // Test that all states set above are in the states for the first frame
        assertThat(state0, Matchers.isIn(item0.states))
        assertThat(state1, Matchers.isIn(item0.states))
        assertThat(state2, Matchers.isIn(item0.states))

        // Now test the rest of the frames, which should not include singleFrameState state2
        for (i in 1 until NUM_FRAMES) {
            val item = latchedListener.jankData[i]
            assertEquals("There should be 2 states at frame $i", 2,
                item.states.size)
            for (state in item.states) {
                assertThat(
                    state,
                    Matchers.either(Matchers.`is`(state0)).or(Matchers.`is`(state1))
                )
            }
        }

        // reset and clear states
        latchedListener.reset()
        metricsState.removeState(state0.key)
        metricsState.removeState(state1.key)

        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        assertEquals(
            "States should be empty after being cleared",
            0,
            item0.states.size
        )
        latchedListener.reset()
        val state3 = Pair("Testing State 3", "sampleStateD")
        val state4 = Pair("Testing State 4", "sampleStateE")
        metricsState.putState(state3.first, state3.second)
        metricsState.putState(state4.first, state4.second)
        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        assertEquals(2, item0.states.size)
        latchedListener.reset()

        // Test removal of state3 and replacement of state4
        metricsState.removeState(state3.first)
        metricsState.putState(state4.first, "sampleStateF")
        runDelayTest(frameDelay, 1, latchedListener)
        item0 = latchedListener.jankData[0]
        assertEquals(1, item0.states.size)
        assertEquals(state4.first, item0.states[0].key)
        assertEquals("sampleStateF", item0.states[0].value)
        latchedListener.reset()
    }

    /**
     * Data structure to hold per-frame state data to be injected during the test
     */
    data class FrameStateInputData(
        val addSFStates: List<Pair<String, String>> = emptyList(),
        val addStates: List<Pair<String, String>> = emptyList(),
        val removeStates: List<String> = emptyList()
    )

    /**
     * Utility function (embedded in a class because it uses version-specific APIs) which
     * is used by tests which require the frame pipeline to be empty when they
     * start. When the activity first starts, there are usually a couple of frames drawn.
     * Depending on when those frames are drawn relative to when the JankStats object and
     * OnFrameListener are set up, there can be old frame data still being set to JankStats
     * after the test has started, which causes problems with a test not getting the result
     * that it should. The workaround is to force these initial frames to draw before the test
     * begins, so that any data used by the test will only land on frames after the test begins
     * instead of these old activity-creation frames.
     */
    open class FrameInitCompat(val jankStatsTest: JankStatsTest) {
        open fun initFramePipeline() {}
    }

    @RequiresApi(16)
    class FrameInit16(jankStatsTest: JankStatsTest) : FrameInitCompat(jankStatsTest) {
        override fun initFramePipeline() {
            val latch = CountDownLatch(10)
            var numFrames = 10
            val callback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    --numFrames
                    latch.countDown()
                    if (numFrames > 0) {
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
            }
            jankStatsTest.delayedActivityRule.getScenario().onActivity {
                Choreographer.getInstance().postFrameCallback(callback)
            }
            latch.await(5, TimeUnit.SECONDS)

            jankStatsTest.latchedListener.reset()
        }
    }

    /**
     * JankStats doesn't do anything pre API 16. But it would be nice to not crash running
     * code that calls JankStats functionality on that version. This test just calls basic APIs
     * to make sure they don't crash.
     */
    @SdkSuppress(maxSdkVersion = ICE_CREAM_SANDWICH_MR1)
    @Test
    fun testPreAPI16() {
        delayedActivityRule.getScenario().onActivity {
            val state0 = StateInfo("Testing State 0", "sampleStateA")
            val state1 = StateInfo("Testing State 1", "sampleStateB")
            metricsState.putState(state0.key, state0.value)
            metricsState.putSingleFrameState(state1.key, state1.value)
        }
        runDelayTest(0, NUM_FRAMES, latchedListener)
    }

    @SdkSuppress(minSdkVersion = JELLY_BEAN)
    @Test
    @Ignore("b/272347202")
    fun testComplexFrameStateData() {
        frameInit.initFramePipeline()

        // perFrameStateData is a structure for testing which holds information about the
        // states that should be added or removed on every frame. This functionality is
        // handled inside DelayedView. //-Comments above each item indicate what the resulting
        // state should be in that frame, which is checked in the asserts below
        // TODO: make immutable, copy to mutable list for delayedView
        var perFrameStateData = mutableListOf(
            // 0: A:0
            JankStatsTest.FrameStateInputData(
                addStates = listOf("stateNameA" to "0"),
            ),
            // 1: A:0
            JankStatsTest.FrameStateInputData(),
            // 2: A:1
            JankStatsTest.FrameStateInputData(
                addStates = listOf("stateNameA" to "1"),
            ),
            // 3: A:2
            JankStatsTest.FrameStateInputData(
                addStates = listOf("stateNameA" to "2"),
            ),
            // 4: A:2
            JankStatsTest.FrameStateInputData(
                removeStates = listOf("stateNameA"),
            ),
            // 5: [nothing]
            JankStatsTest.FrameStateInputData(),
            // 6: A:0, B:10
            JankStatsTest.FrameStateInputData(
                addStates = listOf("stateNameA" to "0", "stateNameB" to "10"),
            ),
            // 7: A:0, B:10, C:100
            JankStatsTest.FrameStateInputData(
                addSFStates = listOf("stateNameC" to "100"),
            ),
            // 8: A:0, B:10
            JankStatsTest.FrameStateInputData(),
            // 9: A:0, B:10
            JankStatsTest.FrameStateInputData(
                removeStates = listOf("stateNameA", "stateNameB"),
            ),
            // 10: empty
            JankStatsTest.FrameStateInputData(),
            // 11: A:1
            JankStatsTest.FrameStateInputData(
                addStates = listOf("stateNameA" to "0", "stateNameA" to "1"),
            ),
        )
        // testData will hold input (above) plus expected results
        val expectedResults = listOf(
            mapOf("stateNameA" to "0"),
            mapOf("stateNameA" to "0"),
            mapOf("stateNameA" to "1"),
            mapOf("stateNameA" to "2"),
            mapOf("stateNameA" to "2"),
            emptyMap(),
            mapOf("stateNameA" to "0", "stateNameB" to "10"),
            mapOf("stateNameA" to "0", "stateNameB" to "10", "stateNameC" to "100"),
            mapOf("stateNameA" to "0", "stateNameB" to "10"),
            mapOf("stateNameA" to "0", "stateNameB" to "10"),
            emptyMap(),
            mapOf("stateNameA" to "1"),
        )

        runDelayTest(frameDelay = 0, numFrames = perFrameStateData.size,
            latchedListener, perFrameStateData)

        assertEquals("There should be ${expectedResults.size} frames of data",
            expectedResults.size, latchedListener.jankData.size)
        for (i in 0 until expectedResults.size) {
            val testResultStates = latchedListener.jankData[i].states
            val expectedResult = expectedResults[i]
            assertEquals("There should be ${expectedResult.size} states",
                expectedResult.size, testResultStates.size)
            for (state in testResultStates) {
                assertEquals("State value not correct",
                    state.value, expectedResult.get(state.key))
            }
        }
    }

    private fun runDelayTest(
        frameDelay: Int,
        numFrames: Int,
        listener: LatchedListener,
        perFrameStateData: List<FrameStateInputData>? = null
    ) {
        val latch = CountDownLatch(1)
        listener.latch = latch
        listener.minFrames = numFrames
        delayedActivityRule.getScenario().onActivity {
            if (perFrameStateData != null) delayedView.perFrameStateData = perFrameStateData
            delayedActivity.repetitions = 0
            delayedActivity.maxReps = numFrames
            delayedActivity.delayMs = frameDelay.toLong()
            delayedActivity.invalidate()
            listener.numFrames = 0
        }
        try {
            latch.await(max(frameDelay, 1) * numFrames + 1000L, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            assert(false)
        }
    }

    inner class LatchedListener : JankStats.OnFrameListener {
        var numJankFrames = 0
        var jankData = mutableListOf<FrameData>()
        var latch: CountDownLatch? = null
        var minFrames = 0
        var numFrames = 0

        fun reset() {
            jankData.clear()
            numJankFrames = 0
            numFrames = 0
        }

        override fun onFrame(volatileFrameData: FrameData) {
            if (latch == null) {
                throw Exception("latch not set in LatchedListener")
            } else {
                if (volatileFrameData.isJank && volatileFrameData.frameDurationUiNanos >
                        (MIN_JANK_NS * jankStats.jankHeuristicMultiplier)) {
                    this.numJankFrames++
                }
                this.jankData.add(volatileFrameData.copy())
                numFrames++
                if (numFrames >= minFrames) {
                    latch!!.countDown()
                }
            }
        }
    }
}