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
import org.intellij.lang.annotations.Language

internal object AudioUnderrunQuery {
    @Language("sql")
    private fun getFullQuery() = """
        SELECT counter.value, counter.ts
        FROM track
        JOIN counter ON track.id = counter.track_id
        WHERE track.type = 'process_counter_track' AND track.name LIKE 'nRdy%'
    """.trimIndent()

    data class SubMetrics(
        val totalMs: Int,
        val zeroMs: Int
    )

    fun getSubMetrics(
        session: PerfettoTraceProcessor.Session
    ): SubMetrics {
        val queryResult = session.query(getFullQuery())

        var lastTs: Long? = null
        var totalNs: Long = 0
        var zeroNs: Long = 0

        queryResult
            .asSequence()
            .forEach { lineVals ->
                if (lineVals.size != EXPECTED_COLUMN_COUNT)
                    throw IllegalStateException("query failed")

                if (lastTs == null) {
                    lastTs = lineVals[VAL_TS] as Long
                } else {
                    val frameNs = lineVals[VAL_TS] as Long - lastTs!!
                    lastTs = lineVals[VAL_TS] as Long

                    totalNs += frameNs

                    val frameCounter = (lineVals[VAL_VALUE] as Double).toInt()

                    if (frameCounter == 0)
                        zeroNs += frameNs
                }
            }

        return SubMetrics((totalNs / 1_000_000).toInt(), (zeroNs / 1_000_000).toInt())
    }

    private const val VAL_VALUE = "value"
    private const val VAL_TS = "ts"
    private const val EXPECTED_COLUMN_COUNT = 2
}
