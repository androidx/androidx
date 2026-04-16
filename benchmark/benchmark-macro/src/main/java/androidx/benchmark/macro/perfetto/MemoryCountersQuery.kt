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

import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.processNameLikePkg

internal object MemoryCountersQuery {
    // https://perfetto.dev/docs/data-sources/memory-counters
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
        /**
         * The count of page faults that were resolved without requiring disk I/O. This usually
         * happens when the page is already in memory but needs to be mapped into the process's page
         * table.
         */
        val minorPageFaults: Double,
        /**
         * The count of page faults that required reading a page from disk. High numbers here often
         * correlate with slow performance and "jank" because the CPU must wait for slow storage
         * I/O.
         */
        val majorPageFaults: Double,
        /** Counts faults where the required page was found in the swap cache. */
        val pageFaultsBackedBySwapCache: Double,
        /**
         * A specific subset of page faults that required a read operation from the underlying
         * storage.
         */
        val pageFaultsBackedByReadIO: Double,
        /**
         * Records how many times the kernel attempted to move memory pages to create larger
         * contiguous blocks of free memory. Frequent compaction can indicate high memory
         * fragmentation.
         */
        val memoryCompactionEvents: Double,
        /**
         * Records how many times the kernel attempted to "steal" or reclaim memory from the process
         * to satisfy other memory requests under pressure.
         */
        val memoryReclaimEvents: Double,
    )

    fun getMemoryCounters(session: TraceProcessor.Session, targetPackageName: String): SubMetrics? {
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
                memoryReclaimEvents = summations[MEMORY_RECLAIM_EVENTS_COUNT] ?: 0.0,
            )
        }
    }
}
