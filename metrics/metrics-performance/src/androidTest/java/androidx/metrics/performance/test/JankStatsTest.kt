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

import android.os.Build.VERSION.SDK_INT
import android.view.Choreographer
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
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

    private val NUM_FRAMES = 10

    /**
     * On some older APIs and emulators, frames may occasionally take longer than predicted jank. We
     * check against this MIN duration to avoid flaky tests.
     */
    private val MIN_JANK_NS = 100000000

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

    /** Test adding/removing listeners while inside the listener callback (on the same thread) */
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
        assumeTrue("Skip running an API 26 as it is flaky b/361092826", SDK_INT != 26)
        assertTrue(jankStats.isTrackingEnabled)
        jankStats.isTrackingEnabled = false
        assertFalse(jankStats.isTrackingEnabled)
        jankStats.isTrackingEnabled = true
        assertTrue(jankStats.isTrackingEnabled)

        // Test to make sure duplicate enablement isn't adding listeners
        jankStats.isTrackingEnabled = true
        jankStats.isTrackingEnabled = true
        jankStats.isTrackingEnabled = true
        initFramePipeline()

        runDelayTest(0, NUM_FRAMES, latchedListener)

        // FrameMetrics sometimes drops a frame, so the total number of
        // jankData items might be less than NUM_FRAMES
        assertEquals(NUM_FRAMES, latchedListener.numFrames)
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

    @Test
    fun testNoJank() {
        val frameDelay = 0

        initFramePipeline()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals("numJankFrames should equal 0", 0, latchedListener.numJankFrames)
        latchedListener.reset()

        jankStats.jankHeuristicMultiplier = 0f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        // FrameMetrics sometimes drops a frame, so the total number of
        // jankData items might be less than NUM_FRAMES. Check against actual
        // number of frames received instead.
        assertEquals(
            "numJankFrames != numFrames",
            latchedListener.numFrames,
            latchedListener.numJankFrames,
        )
    }

    @Test
    fun testMultipleListeners() {
        var secondListenerLatch = CountDownLatch(0)
        val frameDelay = 0

        initFramePipeline()

        var numSecondListenerCalls = 0
        val secondListenerFrameData = mutableListOf<FrameData>()
        val secondListener = OnFrameListener { volatileFrameData ->
            // Sometimes we get a late frame arrival while we are checking the data.
            // This sync call prevents ConcurrentModException
            synchronized(secondListenerFrameData) {
                secondListenerFrameData.add(volatileFrameData.copy())
            }
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

        resetFrameStates()
        val testState = StateInfo("Testing State", "sampleState")
        val insertTime = System.nanoTime()
        metricsState.putSingleFrameState(testState.key, testState.value)
        // in case earlier frames arrive before our test begins
        secondListenerFrameData.clear()
        secondListenerLatch = CountDownLatch(1)

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        secondListenerLatch.await(frameDelay * NUM_FRAMES + 1000L, TimeUnit.MILLISECONDS)
        assertTrue("No calls to second listener", numSecondListenerCalls > 0)

        // Test in both jankData.states and secondListenerStates:
        // - Ensure that testState exists in the list of states
        // - Ensure that frameStart +  for that frameData is greater than insertTime
        // - Ensure that that state exists only once in the list

        assertEquals(
            "Should be exactly one occurrence of SingleFrameState",
            1,
            checkSingleStateExistence(testState, latchedListener.jankData, insertTime),
        )
        synchronized(secondListenerFrameData) {
            assertEquals(
                "Should be exactly one occurrence of SingleFrameState",
                1,
                checkSingleStateExistence(testState, secondListenerFrameData, insertTime),
            )
        }

        jankStats2.isTrackingEnabled = false
        // isTrackingEnabled=false is async, so we sync with
        // onActivity main thread before resetting 2nd listener count
        delayedActivityRule.scenario.onActivity { /* just block */ }
        numSecondListenerCalls = 0

        latchedListener.reset()
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(0, numSecondListenerCalls)
        assertTrue(
            "Removal of second listener should not have removed first",
            latchedListener.jankData.size > 0,
        )

        // Now make sure that extra listeners can be added concurrently from other threads
        latchedListener.reset()
        val listenerPostingThread = Thread()
        var numNewListeners = 0
        lateinit var poster: Runnable
        poster = Runnable {
            JankStats.createAndTrack(delayedActivity.window, secondListener)
            ++numNewListeners
            if (numNewListeners < 100) {
                delayedView.postDelayed(poster, 10)
            }
        }
        scenario.onActivity { _ -> listenerPostingThread.run { poster.run() } }
        listenerPostingThread.start()
        // add listeners concurrently - no asserts here, just testing whether we
        // avoid any concurrency issues with adding and using multiple listeners
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
    }

    /**
     * Ensure that there is only one occurrence of a given StateInfo entry. This is used to validate
     * that SingleFrameState does the right thing - inserts into the current frame and removes it
     * immediately.
     */
    fun checkSingleStateExistence(
        singleState: StateInfo,
        frameData: List<FrameData>,
        insertionTimeNanos: Long,
    ): Int {
        var numOccurrences = 0
        for (item in frameData) {
            for (state in item.states) {
                if (state.equals(singleState)) {
                    numOccurrences++
                    assertTrue(
                        "State be added before frame end time",
                        (item.frameStartNanos + item.frameDurationUiNanos) > insertionTimeNanos,
                    )
                }
            }
        }
        return numOccurrences
    }

    @Test
    fun testRegularJank() {
        val frameDelay = 100

        initFramePipeline()

        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)

        // FrameMetrics sometimes drops a frame, so the total number of
        // jankData items might be less than NUM_FRAMES
        assertEquals(
            "There should be ${latchedListener.numFrames} frames " +
                "with jank data, not ${latchedListener.jankData.size}",
            latchedListener.numFrames,
            latchedListener.jankData.size,
        )
        latchedListener.reset()

        jankStats.jankHeuristicMultiplier = 20f
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(
            "multiplier 20, extremeMs 0: numJankFrames should equal 0",
            0,
            latchedListener.numJankFrames,
        )
    }

    @Test
    fun testFrameStates() {
        val frameDelay = 0

        initFramePipeline()

        resetFrameStates()

        val state0 = StateInfo("Testing State 0", "sampleStateA")
        val state1 = StateInfo("Testing State 1", "sampleStateB")
        val state2 = StateInfo("Testing State 2", "sampleStateC")
        metricsState.putState(state0.key, state0.value)
        metricsState.putState(state1.key, state1.value)
        metricsState.putSingleFrameState(state2.key, state2.value)
        runDelayTest(frameDelay, NUM_FRAMES, latchedListener)
        assertEquals(
            "frameDelay 100: There should be ${latchedListener.numFrames} frames with" +
                "jank data",
            latchedListener.numFrames,
            latchedListener.jankData.size,
        )
        var item0: FrameData = latchedListener.jankData[0]
        assertEquals("There should be 3 states at frame 0", 3, item0.states.size)
        for (state in item0.states) {
            // Test that every state is in the states set above
            assertThat(state, Matchers.isIn(listOf(state0, state1, state2)))
        }
        // Test that all states set above are in the states for the first frame
        assertThat(state0, Matchers.isIn(item0.states))
        assertThat(state1, Matchers.isIn(item0.states))
        assertThat(state2, Matchers.isIn(item0.states))

        // Now test the rest of the frames, which should not include singleFrameState state2
        for (i in 1 until latchedListener.numFrames) {
            val item = latchedListener.jankData[i]
            assertEquals("There should be 2 states at frame $i", 2, item.states.size)
            for (state in item.states) {
                assertThat(state, Matchers.either(Matchers.`is`(state0)).or(Matchers.`is`(state1)))
            }
        }

        // reset and clear states
        resetFrameStates()
        latchedListener.reset()
        metricsState.removeState(state0.key)
        metricsState.removeState(state1.key)

        syncFrameStates()
        item0 = latchedListener.jankData[0]
        assertEquals(
            "States should be empty after being cleared, but got ${item0.states}",
            0,
            item0.states.size,
        )
        latchedListener.reset()
        val state3 = Pair("Testing State 3", "sampleStateD")
        val state4 = Pair("Testing State 4", "sampleStateE")
        metricsState.putState(state3.first, state3.second)
        metricsState.putState(state4.first, state4.second)
        syncFrameStates()
        item0 = latchedListener.jankData[0]
        assertEquals("states: ${item0.states}", 2, item0.states.size)
        latchedListener.reset()

        // Test removal of state3 and replacement of state4
        resetFrameStates()
        metricsState.removeState(state3.first)
        metricsState.putState(state4.first, "sampleStateF")
        syncFrameStates()
        item0 = latchedListener.jankData[0]
        assertEquals("states: ${item0.states}", 1, item0.states.size)
        assertEquals(state4.first, item0.states[0].key)
        assertEquals("sampleStateF", item0.states[0].value)
        latchedListener.reset()
    }

    /** Data structure to hold per-frame state data to be injected during the test */
    data class FrameStateInputData(
        val addSFStates: List<Pair<String, String>> = emptyList(),
        val addStates: List<Pair<String, String>> = emptyList(),
        val removeStates: List<String> = emptyList(),
    )

    /**
     * Utility function which is used by tests which require the frame pipeline to be empty when
     * they start. When the activity first starts, there are usually a couple of frames drawn.
     * Depending on when those frames are drawn relative to when the JankStats object and
     * OnFrameListener are set up, there can be old frame data still being set to JankStats after
     * the test has started, which causes problems with a test not getting the result that it
     * should. The workaround is to force these initial frames to draw before the test begins, so
     * that any data used by the test will only land on frames after the test begins instead of
     * these old activity-creation frames.
     */
    private fun initFramePipeline() {
        val latch = CountDownLatch(10)
        var numFrames = 10
        val callback: Choreographer.FrameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    --numFrames
                    latch.countDown()
                    if (numFrames > 0) {
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
            }
        delayedActivityRule.getScenario().onActivity {
            Choreographer.getInstance().postFrameCallback(callback)
        }
        latch.await(5, TimeUnit.SECONDS)

        latchedListener.reset()
    }

    @Test
    fun testComplexFrameStateData() {
        initFramePipeline()

        // perFrameStateData is a structure for testing which holds information about the
        // states that should be added or removed on every frame. This functionality is
        // handled inside DelayedView. //-Comments above each item indicate what the resulting
        // state should be in that frame, which is checked in the asserts below
        // TODO: make immutable, copy to mutable list for delayedView
        var perFrameStateData =
            mutableListOf(
                // 0: A:0
                JankStatsTest.FrameStateInputData(addStates = listOf("stateNameA" to "0")),
                // 1: A:0
                JankStatsTest.FrameStateInputData(),
                // 2: A:1
                JankStatsTest.FrameStateInputData(addStates = listOf("stateNameA" to "1")),
                // 3: A:2
                JankStatsTest.FrameStateInputData(addStates = listOf("stateNameA" to "2")),
                // 4: A:2
                JankStatsTest.FrameStateInputData(removeStates = listOf("stateNameA")),
                // 5: [nothing]
                JankStatsTest.FrameStateInputData(),
                // 6: A:0, B:10
                JankStatsTest.FrameStateInputData(
                    addStates = listOf("stateNameA" to "0", "stateNameB" to "10")
                ),
                // 7: A:0, B:10, C:100
                JankStatsTest.FrameStateInputData(addSFStates = listOf("stateNameC" to "100")),
                // 8: A:0, B:10
                JankStatsTest.FrameStateInputData(),
                // 9: A:0, B:10
                JankStatsTest.FrameStateInputData(
                    removeStates = listOf("stateNameA", "stateNameB")
                ),
                // 10: empty
                JankStatsTest.FrameStateInputData(),
                // 11: A:1
                JankStatsTest.FrameStateInputData(
                    addStates = listOf("stateNameA" to "0", "stateNameA" to "1")
                ),
                // 12: A:1, B:2
                JankStatsTest.FrameStateInputData(
                    addStates =
                        listOf(
                            "stateNameA" to "0",
                            "stateNameB" to "0",
                            "stateNameB" to "1",
                            "stateNameA" to "1",
                        )
                ),
                // 13-17: empty, just to allow extra frames to pulse
                // Run more than the exact number of frames we have states for. Sometimes the system
                // isn't done running all of the frames in which these states should go by the
                // time we've run that number of frames.
                JankStatsTest.FrameStateInputData(),
                JankStatsTest.FrameStateInputData(),
                JankStatsTest.FrameStateInputData(),
                JankStatsTest.FrameStateInputData(),
                JankStatsTest.FrameStateInputData(),
            )
        // expectedResults holds the values of the states which we would expect to see in
        // a normal test run.
        // This list is currently unused due to flaky test issues related to race conditions
        // between the test/UI thread and the FrameMetrics thread. It's difficult to
        // deterministically insert and then test against data landing in specific frames.
        // Leaving this here for future reference if we want to make the tests more robust
        // eventually.
        val expectedResults =
            listOf(
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
                mapOf("stateNameA" to "1", "stateNameB" to "1"),
            )

        resetFrameStates()
        runDelayTest(
            frameDelay = 0,
            numFrames = perFrameStateData.size,
            latchedListener,
            perFrameStateData,
        )

        // There might be one or two dropped frames, check that we have nearly the number
        // expected
        assertTrue(
            "There should be at least ${expectedResults.size - 2} frames of data" +
                "but there were ${latchedListener.jankData.size}",
            (latchedListener.jankData.size > expectedResults.size - 2),
        )

        // A more flexible way to check for the above, accounting for very minor frame boundary
        // collisions which could cause states to be off by a frame or so, is to check
        // the sequence of values that any state goes through in the results:
        // stateNameA: 0, 1, 2, none, 0, none, 1
        // stateNameB: none, 10, none
        // stateNameC: none, 100, none
        // Even this runs into problems, however, so disabling checks in this test for now.

        //        checkComplexFrameStates("stateNameA",
        //            arrayOf("0", "1", "2", null, "0", null, "1"))
        //        checkComplexFrameStates("stateNameB", arrayOf<String?>(null, "10", null))
        //        checkComplexFrameStates("stateNameC", arrayOf<String?>(null, "100", null))
    }

    /**
     * Currently unused - this function checks the given frame data against a set of known states,
     * in order. This works in general, but occasionally one of the states is wrong (due to
     * multi-threaded race conditions with the frameMetrics thread, I suspect), so not used for now.
     * Leaving it here in case we want more robust testing in the future.
     */
    private fun checkComplexFrameStates(stateName: String, stateValues: Array<String?>) {
        var stateValuesIndex = 0
        var currStateValue: String? = "placeholder"
        // Iterating on the frame data has potential ConcurrentModificationException issues since
        // the thread placing data in that array is running asynchronously. It should be done
        // by the time we check the data, but may still be running anyway
        for (frameData in latchedListener.jankData) {
            val nextStateValue = stateValues[stateValuesIndex]
            var matched = false
            for (state in frameData.states) {
                if (state.key == stateName) {
                    if (nextStateValue == state.value) {
                        matched = true
                        ++stateValuesIndex
                        currStateValue = state.value
                        break
                    } else {
                        assertEquals("Next state value not correct", currStateValue, state.value)
                        matched = true
                    }
                }
            }
            if (!matched) {
                if (currStateValue != null) {
                    assertEquals(nextStateValue, null)
                    currStateValue = null
                    ++stateValuesIndex
                }
            }
            if (stateValuesIndex >= stateValues.size) break
        }
        assertEquals(stateValuesIndex, stateValues.size)
    }

    private fun checkFrameStates(
        expectedResult: Map<String, String>,
        testResultStates: List<StateInfo>,
    ): Boolean {
        if (expectedResult.size != testResultStates.size) return false
        for (state in testResultStates) {
            if (state.value != expectedResult.get(state.key)) {
                return false
            }
        }
        return true
    }

    /**
     * We need to ensure that state data only gets set when the system is ready to send frameData
     * for future frames. It is possible for some of the initial frames to have start times that
     * pre-date the current time, which is when we might be setting/removing state.
     *
     * To ensure that the right thing happens, call this function prior to setting any frame state.
     * It will run frames through the system until the frameData start time is after the current
     * time when this function is called. Then it will reset [latchedListener] to clear it of any
     * state data just processed.
     */
    private fun resetFrameStates() {
        try {
            syncFrameStates()
        } finally {
            latchedListener.reset()
        }
    }

    /**
     * When we add or remove a frame state, it records the time for that request, then waits for a
     * later frame starting after that time to actually add/remove those states. This sometimes
     * breaks when there is an existing frame still to be processed and we only wait for that single
     * frame to pulse. Because the frame started before the state request(s), they are not
     * added/removed as expected and tests can fail.
     *
     * The solution is to pulse frames until we see a frame happen after the current time, which
     * should be sufficient (since this function should only be called after the state requests have
     * been made, thus before the current time, thus before the frame we are waiting for). This
     * function does just that; pulses frames until one has a start time after the time when this
     * function is called. Then we record the state settings for that frame and return.
     */
    private fun syncFrameStates() {
        val currentNanos = System.nanoTime()
        // failsafe - limit the iterations, don't want to loop forever. Typically we will
        // only run for one or two frames.
        var numAttempts = 0
        while (numAttempts < 100) {
            runDelayTest(0, 1, latchedListener)
            if (latchedListener.jankData.size > 0) {
                if (latchedListener.jankData[0].frameStartNanos > currentNanos) {
                    return
                }
            }
            latchedListener.reset()
            numAttempts++
        }
    }

    private fun runDelayTest(
        frameDelay: Int,
        numFrames: Int,
        listener: LatchedListener,
        perFrameStateData: List<FrameStateInputData>? = null,
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
                if (
                    volatileFrameData.isJank &&
                        volatileFrameData.frameDurationUiNanos >
                            (MIN_JANK_NS * jankStats.jankHeuristicMultiplier)
                ) {
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
