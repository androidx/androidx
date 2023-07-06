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

package androidx.metrics.performance.janktest

import android.view.Window
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats

/**
 * This utility class can be used to provide a simple data aggregation mechanism for JankStats.
 * Instead of receiving a callback on every frame and caching that data, JankStats users can
 * create JankStats indirectly through this Aggregator class, which will compile the data
 * and issue it upon request.
 *
 * @param window The Window for which stats will be tracked. A JankStatsAggregator
 * instance is specific to each window in an application, since the timing metrics are
 * tracked on a per-window basis internally.
 * @throws IllegalStateException This function will throw an exception if `window` has
 * a null DecorView.
*/
class JankStatsAggregator(window: Window, private val onJankReportListener: OnJankReportListener) {

    private val listener = object : JankStats.OnFrameListener {
        override fun onFrame(volatileFrameData: FrameData) {
            ++numFrames
            if (volatileFrameData.isJank) {
                // Store copy of frameData because it will be reused by JankStats before any report
                // is issued
                jankReport.add(volatileFrameData.copy())
                if (jankReport.size >= REPORT_BUFFER_LIMIT) {
                    issueJankReport("Max buffer size reached")
                }
            }
        }
    }

    val jankStats = JankStats.createAndTrack(window, listener)

    private var jankReport = ArrayList<FrameData>()

    private var numFrames: Int = 0

    /**
     * Issue a report on current jank data. The data includes FrameData for every frame
     * experiencing jank since the listener was set, or since the last time a report
     * was issued for this JankStats object. Calling this function will cause the jankData
     * to be reset and cleared. Note that this function may be called externally, from application
     * code, but it may also be called internally for various reasons (to reduce memory size
     * by clearing the buffer, or because there was an important lifecycle event). The
     * [reason] parameter explains why the report was issued if it was not called externally.
     *
     * @param reason An optional parameter specifying the reason that the report was issued.
     * This parameter may be used if JankStats issues a report for some internal reason.
     */
    fun issueJankReport(reason: String = "") {
        val jankReportCopy = jankReport
        val numFramesCopy = numFrames
        onJankReportListener.onJankReport(reason, numFramesCopy, jankReportCopy)
        jankReport = ArrayList()
        numFrames = 0
    }

    /**
     * This listener is called whenever there is a call to [issueJankReport].
     */
    fun interface OnJankReportListener {
        /**
         * The implementation of this method will be called whenever there is a call
         * to [issueJankReport].
         *
         * @param reason Optional reason that this report was issued
         * @param totalFrames The total number of frames (jank and not) since collection
         * began (or since the last time the report was issued and reset)
         * @param jankFrameData The FrameData for every frame experiencing jank during
         * the collection period
         */
        fun onJankReport(reason: String, totalFrames: Int, jankFrameData: List<FrameData>)
    }

    companion object {
        /**
         * The number of frames for which data can be accumulated is limited to avoid
         * memory problems. When the limit is reached, a report is automatically issued
         * and the buffer is cleared.
         */
        private const val REPORT_BUFFER_LIMIT = 1000
    }
}
