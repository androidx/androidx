/*
 * Copyright 2023 The Android Open Source Project
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

import android.util.Log
import androidx.benchmark.macro.TAG
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.processNameLikePkg
import org.intellij.lang.annotations.Language

internal object MemoryCountersQuery {
    // https://perfetto.dev/docs/data-sources/memory-counters
    @Language("sql")
    internal fun getFullQuery(targetPackageName: String) = """
        SELECT
            track.name as counter_name,
            process.name as process_name,
            ts,
            value
        FROM counter
            LEFT JOIN process_counter_track as track on counter.track_id = track.id
            LEFT JOIN process using (upid)
        WHERE
            ${processNameLikePkg(targetPackageName)} AND
            track.name LIKE 'mem.%.count'
    """.trimIndent()

    private const val MINOR_PAGE_FAULTS_COUNT = "mem.mm.min_flt.count"
    private const val MAJOR_PAGE_FAULTS_COUNT = "mem.mm.maj_flt.count"
    private const val PAGE_FAULTS_BACKED_BY_SWAP_CACHE_COUNT = "mem.mm.swp_flt.count"
    private const val PAGE_FAULTS_BACKED_BY_READ_IO_COUNT = "mem.mm.read_io.count"
    private const val MEMORY_COMPACTION_EVENTS_COUNT = "mem.mm.compaction.count"
    private const val MEMORY_RECLAIM_EVENTS_COUNT = "mem.mm.reclaim.count"

    data class SubMetrics(
        // Minor Page Faults
        val minorPageFaults: Double,
        // Major Page Faults
        val majorPageFaults: Double,
        // Page Faults Served by Swap Cache
        val pageFaultsBackedBySwapCache: Double,
        // Read Page Faults backed by I/O
        val pageFaultsBackedByReadIO: Double,
        // Memory Compaction Events
        val memoryCompactionEvents: Double,
        // Memory Reclaim Events
        val memoryReclaimEvents: Double
    )

    fun getMemoryCounters(
        session: PerfettoTraceProcessor.Session,
        targetPackageName: String
    ): SubMetrics? {
        val queryResultIterator = session.query(
            query = getFullQuery(targetPackageName = targetPackageName)
        )

        var minorPageFaults = 0.0
        var majorPageFaults = 0.0
        var faultsBackedBySwapCache = 0.0
        var faultsBackedByReadIO = 0.0
        var memoryCompactionEvents = 0.0
        var memoryReclaimEvents = 0.0

        val rows = queryResultIterator.toList()
        if (rows.isEmpty()) {
            return null
        } else {
            rows.forEach { row ->
                when (row.string("counter_name")) {

                    MINOR_PAGE_FAULTS_COUNT -> {
                        minorPageFaults += row.double("value")
                    }

                    MAJOR_PAGE_FAULTS_COUNT -> {
                        majorPageFaults += row.double("value")
                    }

                    PAGE_FAULTS_BACKED_BY_SWAP_CACHE_COUNT -> {
                        faultsBackedBySwapCache += row.double("value")
                    }

                    PAGE_FAULTS_BACKED_BY_READ_IO_COUNT -> {
                        faultsBackedByReadIO += row.double("value")
                    }

                    MEMORY_COMPACTION_EVENTS_COUNT -> {
                        memoryCompactionEvents += row.double("value")
                    }

                    MEMORY_RECLAIM_EVENTS_COUNT -> {
                        memoryReclaimEvents += row.double("value")
                    }

                    else -> Log.d(TAG, "Unknown counter: $row")
                }
            }

            return SubMetrics(
                minorPageFaults = minorPageFaults,
                majorPageFaults = majorPageFaults,
                pageFaultsBackedBySwapCache = faultsBackedBySwapCache,
                pageFaultsBackedByReadIO = faultsBackedByReadIO,
                memoryCompactionEvents = memoryCompactionEvents,
                memoryReclaimEvents = memoryReclaimEvents
            )
        }
    }
}
