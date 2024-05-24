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

import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.MemoryUsageMetric.Mode
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.processNameLikePkg
import org.intellij.lang.annotations.Language

internal object MemoryUsageQuery {
    // https://perfetto.dev/docs/data-sources/memory-counters
    @Language("sql")
    internal fun getQuery(targetPackageName: String, mode: Mode) =
        when (mode) {
            Mode.Last -> "SELECT track.name as counter_name, MAX(ts), value "
            Mode.Max -> "SELECT track.name as counter_name, MAX(value) as value "
        } +
            """
            FROM counter
                LEFT JOIN process_counter_track as track on counter.track_id = track.id
                LEFT JOIN process using (upid)
            WHERE
                ${processNameLikePkg(targetPackageName)} AND
                (
                    track.name LIKE 'mem.rss%' OR
                    track.name LIKE 'Heap size (KB)' OR
                    track.name LIKE 'GPU Memory'
                )
            GROUP BY counter_name
        """
                .trimIndent()

    fun getMemoryUsageKb(
        session: PerfettoTraceProcessor.Session,
        targetPackageName: String,
        mode: Mode
    ): Map<MemoryUsageMetric.SubMetric, Int>? {
        val queryResultIterator =
            session.query(query = getQuery(targetPackageName = targetPackageName, mode))

        val rows = queryResultIterator.toList()
        return if (rows.isEmpty()) {
            null
        } else {
            rows
                .mapNotNull { row ->
                    val counterName = row.string("counter_name")
                    val metric =
                        MemoryUsageMetric.SubMetric.values().firstOrNull {
                            it.counterName == counterName
                        }
                    if (metric == null) {
                        null
                    } else {
                        val measurement = row.double("value")
                        metric to
                            if (metric.alreadyInKb) {
                                measurement.toInt()
                            } else {
                                measurement.toInt() / 1024
                            }
                    }
                }
                .toMap()
        }
    }
}
