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

import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.Slice
import androidx.benchmark.perfetto.processNameLikePkg
import androidx.benchmark.perfetto.toSlices
import org.intellij.lang.annotations.Language

internal object FrameTimingQuery {
    @Language("sql")
    private fun getFullQuery(packageName: String) = """
        ------ Select all frame-relevant slices from slice table
        SELECT
            slice.name as name,
            slice.ts as ts,
            slice.dur as dur
        FROM slice
            INNER JOIN thread_track on slice.track_id = thread_track.id
            INNER JOIN thread USING(utid)
            INNER JOIN process USING(upid)
        WHERE (
            ( slice.name LIKE "Choreographer#doFrame%" AND process.pid LIKE thread.tid ) OR
            ( slice.name LIKE "DrawFrame%" AND thread.name like "RenderThread" )
        ) AND ${processNameLikePkg(packageName)}
        ------ Add in actual frame slices (prepended with "actual " to differentiate)
        UNION
        SELECT
            "actual " || actual_frame_timeline_slice.name as name,
            actual_frame_timeline_slice.ts as ts,
            actual_frame_timeline_slice.dur as dur
        FROM actual_frame_timeline_slice
            INNER JOIN process USING(upid)
        WHERE
            ${processNameLikePkg(packageName)}
        ------ Add in expected time slices (prepended with "expected " to differentiate)
        UNION
        SELECT
            "expected " || expected_frame_timeline_slice.name as name,
            expected_frame_timeline_slice.ts as ts,
            expected_frame_timeline_slice.dur as dur
        FROM expected_frame_timeline_slice
            INNER JOIN process USING(upid)
        WHERE
            ${processNameLikePkg(packageName)}
        ORDER BY ts ASC
    """.trimIndent()

    enum class SubMetric {
        FrameDurationCpuNs,
        FrameDurationUiNs,
        FrameOverrunNs;

        fun supportedOnApiLevel(apiLevel: Int): Boolean {
            return apiLevel >= 31 || this != FrameOverrunNs
        }
    }

    enum class FrameSliceType {
        Expected,
        Actual,
        UiThread,
        RenderThread
    }

    /**
     * Container for frame data.
     *
     * Nullable slices are always present on API 31+
     */
    internal class FrameData(
        val uiSlice: Slice,
        val rtSlice: Slice,
        val expectedSlice: Slice?,
        val actualSlice: Slice?
    ) {
        fun get(subMetric: SubMetric): Long {
            return when (subMetric) {
                SubMetric.FrameDurationCpuNs -> rtSlice.endTs - uiSlice.ts
                SubMetric.FrameDurationUiNs -> uiSlice.dur
                SubMetric.FrameOverrunNs -> {
                    // workaround b/279088460, where actual slice ends too early
                    maxOf(actualSlice!!.endTs, rtSlice.endTs) - expectedSlice!!.endTs
                }
            }
        }
        companion object {
            fun tryCreateBasic(
                uiSlice: Slice?,
                rtSlice: Slice?
            ): FrameData? {
                return uiSlice?.let {
                    rtSlice?.let {
                        FrameData(uiSlice, rtSlice, null, null)
                    }
                }
            }

            fun tryCreate31(
                uiSlice: Slice?,
                rtSlice: Slice?,
                expectedSlice: Slice?,
                actualSlice: Slice?,
            ): FrameData? {
                return if (uiSlice != null &&
                    rtSlice != null &&
                    expectedSlice != null &&
                    actualSlice != null
                ) {
                    FrameData(uiSlice, rtSlice, expectedSlice, actualSlice)
                } else {
                    null
                }
            }
        }
    }

    /**
     * Binary search for a slice matching the specified frameId, or null if not found.
     */
    private fun List<Slice>.binarySearchFrameId(frameId: Int): Slice? {
        val targetIndex = binarySearch { potentialTarget ->
            potentialTarget.frameId!! - frameId
        }
        return if (targetIndex >= 0) {
            get(targetIndex)
        } else {
            null
        }
    }

    internal fun getFrameData(
        session: PerfettoTraceProcessor.Session,
        captureApiLevel: Int,
        packageName: String,
    ): List<FrameData> {
        val queryResultIterator = session.query(
            query = getFullQuery(packageName)
        )
        val slices = queryResultIterator.toSlices().let { list ->
            list.map { it.copy(ts = it.ts - list.first().ts) }
        }

        val groupedData = slices
            .filter { it.dur > 0 } // drop non-terminated slices
            .groupBy {
                when {
                    // note: we use "startsWith" as starting in S, all of these will end
                    // with frame ID (or GPU completion frame ID)
                    it.name.startsWith("Choreographer#doFrame") -> FrameSliceType.UiThread
                    it.name.startsWith("DrawFrame") -> FrameSliceType.RenderThread
                    it.name.startsWith("actual ") -> FrameSliceType.Actual
                    it.name.startsWith("expected ") -> FrameSliceType.Expected
                    else -> throw IllegalStateException("Unexpected slice $it")
                }
            }

        val uiSlices = groupedData.getOrElse(FrameSliceType.UiThread) { listOf() }
        val rtSlices = groupedData.getOrElse(FrameSliceType.RenderThread) { listOf() }
        val actualSlices = groupedData.getOrElse(FrameSliceType.Actual) { listOf() }
        val expectedSlices = groupedData.getOrElse(FrameSliceType.Expected) { listOf() }

        if (uiSlices.isEmpty()) {
            return emptyList()
        }

        // check data looks reasonable
        val newSlicesShouldBeEmpty = captureApiLevel < 31
        require(actualSlices.isEmpty() == newSlicesShouldBeEmpty)
        require(expectedSlices.isEmpty() == newSlicesShouldBeEmpty)

        return if (captureApiLevel >= 31) {
            // No slice should be missing a frameId
            require(slices.none { it.frameId == null })

            val actualSlicesPool = actualSlices.toMutableList()
            rtSlices.mapNotNull { rtSlice ->
                val frameId = rtSlice.frameId!!

                val uiSlice = uiSlices.binarySearchFrameId(frameId)

                // Ideally, we'd rely on frameIds, but these can fall out of sync due to b/279088460
                //     expectedSlice = expectedSlices.binarySearchFrameId(frameId),
                //     actualSlice = actualSlices.binarySearchFrameId(frameId)
                // A pool of actual slices is used to prevent incorrect duplicate mapping. At the
                //     end of the trace, the synthetic expect/actual slices may be missing even if
                //     the complete end of frame is present, and we want to discard those. This
                //     doesn't happen at front of trace, since we find actuals from the end.
                if (uiSlice != null) {
                    // Use fixed offset since synthetic tracepoint for actual may start after the
                    // actual UI slice (have observed 2us in practice)
                    val actualSlice = actualSlicesPool.lastOrNull { it.ts < uiSlice.ts + 50_000 }
                    actualSlicesPool.remove(actualSlice)
                    val expectedSlice = actualSlice?.frameId?.run {
                        expectedSlices.binarySearchFrameId(this)
                    }
                    FrameData.tryCreate31(
                        uiSlice = uiSlice,
                        rtSlice = rtSlice,
                        expectedSlice = expectedSlice,
                        actualSlice = actualSlice
                    )
                } else {
                    null
                }
            }
        } else {
            require(slices.none { it.frameId != null })
            rtSlices.mapNotNull { rtSlice ->
                FrameData.tryCreateBasic(
                    uiSlice = uiSlices.firstOrNull { it.contains(rtSlice.ts) },
                    rtSlice = rtSlice
                )
            }
        }
    }

    fun List<FrameData>.getFrameSubMetrics(captureApiLevel: Int): Map<SubMetric, List<Long>> {
        return SubMetric.values()
            .filter { it.supportedOnApiLevel(captureApiLevel) }
            .associateWith { subMetric ->
                map { frame -> frame.get(subMetric) }
            }
    }
}