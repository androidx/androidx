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

package androidx.metrics.performance.benchmark

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStatsInternalsForTesting
import androidx.metrics.performance.PerformanceMetricsState
import androidx.metrics.performance.benchmark.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Idea Want to test per-frame performance. This means need to test what happens when frame data is
 * sent with and without PerformanceMetricsState. Should also test setting state (regular and single
 * frame). Because frame data is received asynchronously, should instrument JankStats and
 * PerformanceMetrics to allow the key methods to be called from outside (@TestApi)
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class JankStatsBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(MainActivity::class.java)

    lateinit var metricsStateHolder: PerformanceMetricsState.Holder
    lateinit var jankStats: JankStats
    lateinit var jankStatsImpl: JankStatsInternalsForTesting
    lateinit var textview: TextView

    object frameListener : JankStats.OnFrameListener {
        override fun onFrame(volatileFrameData: FrameData) {}
    }

    @Before
    fun setup() {
        activityRule.runOnUiThread {
            textview =
                activityRule.activity.findViewById(
                    androidx.metrics.performance.benchmark.R.id.textview
                )
            metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(textview)
            jankStats = JankStats.createAndTrack(activityRule.activity.window, frameListener)
            jankStatsImpl = JankStatsInternalsForTesting(jankStats)
        }
    }

    @Test
    fun setNewState() {
        var iteration = 0
        benchmarkRule.measureRepeatedOnMainThread {
            iteration++
            metricsStateHolder.state?.putState("Activity$iteration", "activity")
        }
    }

    @Test
    fun setStateOverAndOver() {
        benchmarkRule.measureRepeatedOnMainThread {
            metricsStateHolder.state?.putState("Activity", "activity")
        }
    }

    @Test
    fun setAndRemoveState() {
        benchmarkRule.measureRepeatedOnMainThread {
            // Simply calling removeState() on the public API is not sufficient for benchmarking
            // allocations, because it will not actually be removed until later, when JankStats
            // issues data for a frame after the time the state was removed. Thus we call
            // our testing method here instead to forcibly remove it, which should test the
            // allocation behavior of the object pool used for states.
            jankStatsImpl.removeStateNow(metricsStateHolder.state!!, "Activity")
            metricsStateHolder.state?.putState("Activity", "activity")
        }
    }

    @Test
    fun getFrameData() {
        metricsStateHolder.state?.putState("Activity", "activity")
        benchmarkRule.measureRepeated { jankStatsImpl.getFrameData() }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    class Api24TestClass {
        @Suppress("DEPRECATION")
        fun frameMetricsTest(
            activityRule: androidx.test.rule.ActivityTestRule<MainActivity>,
            benchmarkRule: BenchmarkRule,
            textview: TextView,
            jankStatsImpl: JankStatsInternalsForTesting,
        ) {
            var frameMetrics: FrameMetrics? = null
            val frameMetricsLatch = CountDownLatch(1)
            val listener =
                Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
                    frameMetrics = metrics
                    frameMetricsLatch.countDown()
                }
            // First have to get a FrameMetrics object, which we cannot create ourselves.
            // Instead, we will enable FrameMetrics on the window and wait to receive a callback
            val thread = HandlerThread("FrameMetricsAggregator")
            thread.start()
            activityRule.runOnUiThread {
                activityRule.activity.window.addOnFrameMetricsAvailableListener(
                    listener,
                    Handler(thread.looper),
                )
                textview.invalidate()
            }
            frameMetricsLatch.await(2, TimeUnit.SECONDS)
            if (frameMetrics != null) {
                benchmarkRule.measureRepeated { jankStatsImpl.getFrameData(frameMetrics!!) }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun getFrameData24() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Api24TestClass().frameMetricsTest(activityRule, benchmarkRule, textview, jankStatsImpl)
        }
    }

    @Test
    fun logFrameData() {
        metricsStateHolder.state?.putState("Activity", "activity")
        val frameData = jankStatsImpl.getFrameData()
        if (frameData != null) {
            benchmarkRule.measureRepeated { jankStatsImpl.logFrameData(frameData) }
        }
    }
}
