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

import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.processNameLikePkg
import org.intellij.lang.annotations.Language

internal object MemoryCountersQuery {
    // https://perfetto.dev/docs/data-sources/memory-counters
    @Language("sql")
    internal fun getQuery(targetPackageName: String) =
        """
        SELECT
            track.name as counter_name,
            SUM(value)
        FROM counter
            LEFT JOIN process_counter_track as track on counter.track_id = track.id
            LEFT JOIN process using (upid)
        WHERE
            ${processNameLikePkg(targetPackageName)} AND
            track.name LIKE 'mem.%.count'
        GROUP BY counter_name
    """
            .trimIndent()

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
        val queryResultIterator =
            session.query(query = getQuery(targetPackageName = targetPackageName))

        val rows = queryResultIterator.toList()
        return if (rows.isEmpty()) {
            null
        } else {
            val summations: Map<String, Double> =
                rows.associate { it.string("counter_name") to it.double("SUM(value)") }
            SubMetrics(
                minorPageFaults = summations[MINOR_PAGE_FAULTS_COUNT] ?: 0.0,
                majorPageFaults = summations[MAJOR_PAGE_FAULTS_COUNT] ?: 0.0,
                pageFaultsBackedBySwapCache =
                    summations[PAGE_FAULTS_BACKED_BY_SWAP_CACHE_COUNT] ?: 0.0,
                pageFaultsBackedByReadIO = summations[PAGE_FAULTS_BACKED_BY_READ_IO_COUNT] ?: 0.0,
                memoryCompactionEvents = summations[MEMORY_COMPACTION_EVENTS_COUNT] ?: 0.0,
                memoryReclaimEvents = summations[MEMORY_RECLAIM_EVENTS_COUNT] ?: 0.0
            )
        }
    }
}
