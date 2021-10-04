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

internal object StartupTimingQuery {
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
            (process.name LIKE "$testProcessName" AND slice.name LIKE "startActivityAndWait") OR
            (process.name LIKE "$targetProcessName" AND (
                (slice.name LIKE "Choreographer#doFrame%" AND process.pid LIKE thread.tid) OR
                (slice.name LIKE "reportFullyDrawn() for %" AND process.pid LIKE thread.tid) OR
                (slice.name LIKE "DrawFrame%" AND thread.name LIKE "RenderThread")
            ))
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
        UiThread,
        RenderThread
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

    fun getFrameSubMetrics(
        absoluteTracePath: String,
        captureApiLevel: Int,
        targetPackageName: String,
        testPackageName: String
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
                    it.name.startsWith("Choreographer#doFrame") -> StartupSliceType.UiThread
                    it.name.startsWith("DrawFrame") -> StartupSliceType.RenderThread
                    it.name.startsWith("launching") -> StartupSliceType.Launching
                    it.name.startsWith("reportFullyDrawn") -> StartupSliceType.ReportFullyDrawn
                    it.name == "startActivityAndWait" -> StartupSliceType.StartActivityAndWait
                    else -> throw IllegalStateException("Unexpected slice $it")
                }
            }

        val startActivityAndWaitSlice = groupedData[StartupSliceType.StartActivityAndWait]?.first()
            ?: return null
        val launchingSlice = groupedData[StartupSliceType.Launching]?.firstOrNull {
            // find first "launching" slice that starts within startActivityAndWait
            // verify full name only on API 23+, since before package name not specified
            startActivityAndWaitSlice.contains(it.ts) &&
                (captureApiLevel < 23 || it.name == "launching: $targetPackageName")
        } ?: return null

        val reportFullyDrawnSlice = groupedData[StartupSliceType.ReportFullyDrawn]?.firstOrNull()

        val reportFullyDrawnEndTs: Long? = reportFullyDrawnSlice?.let {
            val uiSlices = groupedData.getOrElse(StartupSliceType.UiThread) { listOf() }
            val rtSlices = groupedData.getOrElse(StartupSliceType.RenderThread) { listOf() }

            // find first uiSlice with end after reportFullyDrawn (reportFullyDrawn may happen
            // during or before a given frame)
            val uiSlice = uiSlices.first { uiSlice ->
                uiSlice.endTs > reportFullyDrawnSlice.ts
            }

            // And find corresponding rt slice
            val rtSlice = rtSlices.first { rtSlice ->
                rtSlice.ts > uiSlice.ts
            }
            rtSlice.endTs
        }

        return SubMetrics(
            startTs = launchingSlice.ts,
            initialDisplayTs = launchingSlice.endTs,
            fullDisplayTs = reportFullyDrawnEndTs,
        )
    }
}