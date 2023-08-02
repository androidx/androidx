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

internal object AudioUnderrunQuery {
    private fun getFullQuery() = """
        SELECT track.name, counter.value, counter.ts
        FROM track
        JOIN counter ON track.id = counter.track_id
        WHERE track.type = 'process_counter_track' AND track.name LIKE 'nRdy%'
    """.trimIndent()

    data class SubMetrics(
        val totalMs: Int,
        val zeroMs: Int
    )

    fun getSubMetrics(
        absoluteTracePath: String
    ): SubMetrics {
        val queryResult = PerfettoTraceProcessor.rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = getFullQuery()
        )

        val resultLines = queryResult.split("\n")

        if (resultLines.first() != "\"name\",\"value\",\"ts\"") {
            throw IllegalStateException("query failed!")
        }

        // we can't measure duration when there is only one time stamp
        if (resultLines.size <= 3) {
            throw RuntimeException("No playing audio detected")
        }

        var trackName: String? = null
        var lastTs: Long? = null

        var totalNs: Long = 0
        var zeroNs: Long = 0

        resultLines
            .drop(1) // column names
            .dropLast(1) // empty line
            .forEach {
                val lineVals = it.split(",")
                if (lineVals.size != VAL_MAX)
                    throw IllegalStateException("query failed")

                if (trackName == null) {
                    trackName = lineVals[VAL_NAME]
                } else if (!trackName.equals(lineVals[VAL_NAME])) {
                    throw RuntimeException("There could be only one AudioTrack per measure")
                }

                if (lastTs == null) {
                    lastTs = lineVals[VAL_TS].toLong()
                } else {
                    val frameNs = lineVals[VAL_TS].toLong() - lastTs!!
                    lastTs = lineVals[VAL_TS].toLong()

                    totalNs += frameNs

                    val frameCounter = lineVals[VAL_VALUE].toDouble().toInt()

                    if (frameCounter == 0)
                        zeroNs += frameNs
                }
            }

        return SubMetrics((totalNs / 1_000_000).toInt(), (zeroNs / 1_000_000).toInt())
    }

    private const val VAL_NAME = 0
    private const val VAL_VALUE = 1
    private const val VAL_TS = 2
    private const val VAL_MAX = 3
}