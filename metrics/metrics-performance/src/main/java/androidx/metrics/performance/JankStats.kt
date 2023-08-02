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
package androidx.metrics.performance

import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import java.lang.IllegalStateException

/**
 * This class is used to both accumulate and report information about UI "jank" (runtime
 * performance problems) in an application.
 *
 * There are three major components at work in JankStats:
 *
 * **Identifying Jank**: This library uses internal heuristics to determine when
 * jank has occurred, and uses that information to know when to issue jank reports so
 * that developers have information on those problems to help analyze and fix the issues.
 *
 * **Providing UI Context**: To make the jank reports more useful and actionable,
 * the system provides a mechanism to help track the current state of the UI and user.
 * This information is provided whenever reports are logged, so that developers can
 * understand not only when problems occurred, but what the user was doing at the time,
 * to help identify problem areas in the application that can then be addressed. Some
 * of this state is provided automatically, and internally, by various AndroidX libraries.
 * But developers are encouraged to provide their own app-specific state as well. See
 * [PerformanceMetricsState] for more information on logging this state information.
 *
 * **Reporting Results**: On every frame, the JankStats client is notified via a listener
 * with information about that frame, including how long the frame took to
 * complete, whether it was considered jank, and what the UI context was during that frame.
 * Clients are encouraged to aggregate and upload the data as they see fit for analysis that
 * can help debug overall performance problems.
 *
 * Note that the behavior of JankStats varies according to API level, because it is dependent
 * upon underlying capabilities in the platform to determine frame timing information.
 * Below API level 16, JankStats does nothing, because there is no way to derive dependable
 * frame timing data. Starting at API level 16, JankStats uses rough frame timing information
 * that can at least provide estimates of how long frames should have taken, compared to how
 * long they actually took. Starting with API level 24, frame durations are more dependable,
 * using platform timing APIs that are available in that release. And starting in API level
 * 31, there is even more underlying platform information which helps provide more accurate
 * timing still. On all of these releases (starting with API level 16), the base functionality
 * of JankStats should at least provide useful information about performance problems, along
 * with the state of the application during those frames, but the timing data will be necessarily
 * more accurate for later releases, as described above.
 */
@Suppress("SingletonConstructor")
class JankStats private constructor(
    window: Window,
    private val frameListener: OnFrameListener
) {
    private val holder: PerformanceMetricsState.Holder

    /**
     * JankStats uses the platform FrameMetrics API internally when it is available to track frame
     * timings. It turns this data into "jank" metrics. Prior to API 24, it uses other mechanisms
     * to derive frame durations (not as dependable as FrameMetrics, but better than nothing).
     *
     * Because of this platform version limitation, most of the functionality of
     * JankStats is in the impl class, which is instantiated when necessary
     * based on the runtime OS version. The JankStats API is basically a think wrapper around
     * the implementations in these classes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal val implementation: JankStatsBaseImpl

    init {
        val decorView: View? = window.peekDecorView()
        if (decorView == null) {
            throw IllegalStateException(
                "window.peekDecorView() is null: " +
                    "JankStats can only be created with a Window that has a non-null DecorView"
            )
        }
        holder = PerformanceMetricsState.create(decorView)
        implementation =
            when {
                Build.VERSION.SDK_INT >= 31 -> {
                    JankStatsApi31Impl(this, decorView, window)
                }
                Build.VERSION.SDK_INT >= 26 -> {
                    JankStatsApi26Impl(this, decorView, window)
                }
                Build.VERSION.SDK_INT >= 24 -> {
                    JankStatsApi24Impl(this, decorView, window)
                }
                Build.VERSION.SDK_INT >= 22 -> {
                    JankStatsApi22Impl(this, decorView)
                }
                Build.VERSION.SDK_INT >= 16 -> {
                    JankStatsApi16Impl(this, decorView)
                }
                else -> {
                    JankStatsBaseImpl(this)
                }
            }
        implementation.setupFrameTimer(true)
    }
    /**
     * Whether this JankStats instance is enabled for tracking and reporting jank data.
     * Enabling tracking causes JankStats to listen to system frame-timing information and
     * record data on a per-frame basis that can later be reported to the JankStats listener.
     * Tracking is enabled by default at creation time.
     */
    var isTrackingEnabled: Boolean = true
        /**
         * Enabling tracking causes JankStats to listen to system frame-timing information and
         * record data on a per-frame basis that can later be reported to the JankStats listener.
         * Tracking is enabled by default at creation time.
         */
        @UiThread
        set(value) {
            implementation.setupFrameTimer(value)
            field = value
        }

    /**
     * This multiplier is used to determine when frames are exhibiting jank.
     *
     * The factor is multiplied by the current refresh rate to calculate a frame
     * duration beyond which frames are considered, and reported, as having jank.
     * For example, an app wishing to ignore smaller-duration jank events should
     * increase the multiplier. Setting the value to 0, while not recommended for
     * production usage, causes all frames to be regarded as jank, which can be
     * used in tests to verify correct instrumentation behavior.
     *
     * By default, the multiplier is 2.
     */
    var jankHeuristicMultiplier: Float = 2.0f
        set(value) {
            // reset calculated value to force recalculation based on new heuristic
            JankStatsBaseImpl.frameDuration = -1
            field = value
        }

    /**
     * Called internally (by Impl classes) with the frame data, which is passed onto the client.
     */
    internal fun logFrameData(volatileFrameData: FrameData) {
        frameListener.onFrame(volatileFrameData)
    }

    companion object {
        /**
         * Creates a new JankStats object and starts tracking jank metrics for the given
         * window.
         * @see isTrackingEnabled
         * @throws IllegalStateException `window` must be active, with a non-null DecorView. See
         * [Window.peekDecorView].
         */
        @JvmStatic
        @UiThread
        @Suppress("ExecutorRegistration")
        fun createAndTrack(window: Window, frameListener: OnFrameListener): JankStats {
            return JankStats(window, frameListener)
        }
    }

    /**
     * This interface should be implemented to receive per-frame callbacks with jank data.
     *
     * Internally, the [FrameData] objected passed to [OnFrameListener.onFrame] is
     * reused and populated with new data on every frame. This means that listeners
     * implementing [OnFrameListener] cannot depend on the data received in that
     * structure over time and should consider the [FrameData] object **obsolete when control
     * returns from the listener**. Clients wishing to retain data from this call should **copy
     * the data elsewhere before returning**.
     */
    fun interface OnFrameListener {

        /**
         * The implementation of this method will be called on every frame when an
         * OnFrameListener is set on this JankStats object.
         *
         * The FrameData object **will be modified internally after returning from the listener**;
         * any data that needs to be retained should be copied before returning.
         *
         * @param volatileFrameData The data for the most recent frame.
         */
        fun onFrame(
            volatileFrameData: FrameData
        )
    }
}
