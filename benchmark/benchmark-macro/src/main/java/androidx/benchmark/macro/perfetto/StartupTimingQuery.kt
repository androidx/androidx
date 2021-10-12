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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.macro.StartupMode

internal object StartupTimingQuery {

    /**
     * On older platforms, process name may be truncated, especially in cold startup traces,
     * when the process name dump at trace begin happens _before_ app process is created.
     *
     * @see perfetto.protos.ProcessStatsConfig.scan_all_processes_on_start
     */
    private fun String.truncatedProcessName() = takeLast(15)

    private fun getFullQuery(testProcessName: String, targetProcessName: String) = """
        ------ Select all startup-relevant slices from slice table
        SELECT
            slice.name as name,
            slice.ts as ts,
            slice.dur as dur
        FROM slice
            INNER JOIN thread_track on slice.track_id = thread_track.id
            INNER JOIN thread USING(utid)
            INNER JOIN process USING(upid)
        WHERE (
            -- Test process starts before tracing, so it shouldn't have truncation problem
            (process.name LIKE "$testProcessName" AND slice.name LIKE "startActivityAndWait") OR
            (
                (
                    -- check for full or truncated process name (can happen on older platforms)
                    process.name LIKE "$targetProcessName" OR
                    process.name LIKE "${targetProcessName.truncatedProcessName()}"
                ) AND (
                    (slice.name LIKE "activityResume" AND process.pid LIKE thread.tid) OR
                    (slice.name LIKE "Choreographer#doFrame%" AND process.pid LIKE thread.tid) OR
                    (slice.name LIKE "reportFullyDrawn() for %" AND process.pid LIKE thread.tid) OR
                    (slice.name LIKE "DrawFrame%" AND thread.name LIKE "RenderThread")
                )
            )
        )
        ------ Add in async slices
        UNION
        SELECT
            slice.name as name,
            slice.ts as ts,
            slice.dur as dur
        FROM slice
            INNER JOIN process_track on slice.track_id = process_track.id
            INNER JOIN process USING(upid)
        WHERE (
            -- API 23+:   "launching: <target>"
            -- API 19-22: "launching"
            slice.name LIKE "launching%" AND process.name LIKE "system_server"
        )
        ORDER BY ts ASC
    """.trimIndent()

    enum class StartupSliceType {
        StartActivityAndWait,
        Launching,
        ReportFullyDrawn,
        FrameUiThread,
        FrameRenderThread,
        ActivityResume
    }

    data class SubMetrics(
        val timeToInitialDisplayNs: Long,
        val timeToFullDisplayNs: Long?,
        val timelineRange: LongRange
    ) {
        constructor(
            startTs: Long,
            initialDisplayTs: Long,
            fullDisplayTs: Long?
        ) : this(
            timeToInitialDisplayNs = initialDisplayTs - startTs,
            timeToFullDisplayNs = fullDisplayTs?.let { it - startTs },
            timelineRange = startTs..(fullDisplayTs ?: initialDisplayTs),
        )
    }

    private fun findEndRenderTimeForUiFrame(
        uiSlices: List<Slice>,
        rtSlices: List<Slice>,
        predicate: (Slice) -> Boolean
    ): Long {
        // find first UI slice that corresponds with the predicate
        val uiSlice = uiSlices.first(predicate)

        // find corresponding rt slice
        val rtSlice = rtSlices.first { rtSlice ->
            rtSlice.ts > uiSlice.ts
        }
        return rtSlice.endTs
    }

    fun getFrameSubMetrics(
        absoluteTracePath: String,
        captureApiLevel: Int,
        targetPackageName: String,
        testPackageName: String,
        startupMode: StartupMode
    ): SubMetrics? {
        val queryResult = PerfettoTraceProcessor.rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = getFullQuery(
                testProcessName = testPackageName,
                targetProcessName = targetPackageName
            )
        )
        val slices = Slice.parseListFromQueryResult(queryResult)

        val groupedData = slices
            .filter { it.dur > 0 } // drop non-terminated slices
            .groupBy {
                when {
                    // note: we use "startsWith" as many of these have more details
                    // appended to the slice name in more recent platform versions
                    it.name.startsWith("Choreographer#doFrame") -> StartupSliceType.FrameUiThread
                    it.name.startsWith("DrawFrame") -> StartupSliceType.FrameRenderThread
                    it.name.startsWith("launching") -> StartupSliceType.Launching
                    it.name.startsWith("reportFullyDrawn") -> StartupSliceType.ReportFullyDrawn
                    it.name == "activityResume" -> StartupSliceType.ActivityResume
                    it.name == "startActivityAndWait" -> StartupSliceType.StartActivityAndWait
                    else -> throw IllegalStateException("Unexpected slice $it")
                }
            }

        val startActivityAndWaitSlice = groupedData[StartupSliceType.StartActivityAndWait]?.first()
            ?: return null

        val uiSlices = groupedData.getOrElse(StartupSliceType.FrameUiThread) { listOf() }
        val rtSlices = groupedData.getOrElse(StartupSliceType.FrameRenderThread) { listOf() }

        val startTs: Long
        val initialDisplayTs: Long
        if (captureApiLevel >= 29 || startupMode != StartupMode.HOT) {
            val launchingSlice = groupedData[StartupSliceType.Launching]?.firstOrNull {
                // find first "launching" slice that starts within startActivityAndWait
                // verify full name only on API 23+, since before package name not specified
                startActivityAndWaitSlice.contains(it.ts) &&
                    (captureApiLevel < 23 || it.name == "launching: $targetPackageName")
            } ?: return null
            startTs = launchingSlice.ts
            initialDisplayTs = launchingSlice.endTs
        } else {
            // Prior to API 29, hot starts weren't traced with the launching slice, so we do a best
            // guess - the time taken to Activity#onResume, and then produce the next frame.
            startTs = groupedData[StartupSliceType.ActivityResume]?.first()?.ts
                ?: return null
            initialDisplayTs = findEndRenderTimeForUiFrame(uiSlices, rtSlices) { uiSlice ->
                uiSlice.ts > startTs
            }
        }

        val reportFullyDrawnSlice = groupedData[StartupSliceType.ReportFullyDrawn]?.firstOrNull()

        val reportFullyDrawnEndTs: Long? = reportFullyDrawnSlice?.let {
            // find first uiSlice with end after reportFullyDrawn (reportFullyDrawn may happen
            // during or before a given frame)
            findEndRenderTimeForUiFrame(uiSlices, rtSlices) { uiSlice ->
                uiSlice.endTs > reportFullyDrawnSlice.ts
            }
        }

        return SubMetrics(
            startTs = startTs,
            initialDisplayTs = initialDisplayTs,
            fullDisplayTs = reportFullyDrawnEndTs,
        )
    }
}