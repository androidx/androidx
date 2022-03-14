/*
 * Copyright 2022 The Android Open Source Project
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

// We want to use android_powrails.sql, but cannot as they do not split into sections with slice

internal object PowerQuery {
    private fun getFullQuery(slice: Slice) = """
        SELECT
            t.name,
            (max(c.value) - min(c.value))/((max(c.ts) - min(c.ts)) / 1000000) AS powerUs
        FROM counter c
        JOIN counter_track t ON c.track_id = t.id
        WHERE t.name GLOB 'power.*'
        AND c.ts >= ${slice.ts} AND c.ts <= ${slice.endTs}
        GROUP BY t.name
    """.trimIndent()

    data class PowerMetrics(
        var name: String,
        var powerUs: Double,
    )

    fun getTotalPowerMetrics(
        absoluteTracePath: String,
        slice: Slice
    ): List<PowerMetrics> {
        val allMetrics = getPowerMetrics(absoluteTracePath, slice)
        allMetrics.ifEmpty { return emptyList() }

        val regex = "Rails(Cpu\\w+|SystemFabric|MemoryInterface|[A-Z][a-z]+)".toRegex()

        return allMetrics.map { original ->
            val subsystem = if (regex.containsMatchIn(original.name))
                regex.find(original.name)?.groups?.get(1)?.value.toString()
            else original.name
            PowerMetrics(
                subsystem,
                allMetrics.filter {
                    it.name.contains(subsystem)
                }.fold(0.0) { total, next ->
                    total + next.powerUs
                }
            )
        }.distinct()
    }

    fun getPowerMetrics(
        absoluteTracePath: String,
        slice: Slice
    ): List<PowerMetrics> {
        val queryResult = PerfettoTraceProcessor.rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = getFullQuery(slice)
        )

        val resultLines = queryResult.split("\n")

        if (resultLines.first() != """"name","powerUs"""") {
            throw IllegalStateException("query failed!\n${getFullQuery(slice)}")
        }

        // results are in CSV with a header row, and strings wrapped with quotes
        return resultLines
            .filter { it.isNotBlank() } // drop blank lines
            .drop(1) // drop the header row
            .map {
                val columns = it.split(",")
                PowerMetrics(
                    name = columns[0].unquote().camelCase(),
                    powerUs = columns[1].toDouble(),
                )
            }
    }
}
