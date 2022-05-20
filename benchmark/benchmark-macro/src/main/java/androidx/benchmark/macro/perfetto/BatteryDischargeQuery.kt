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

internal object BatteryDischargeQuery {
    private fun getFullQuery(slice: Slice) = """
        SELECT
            max(c.value)/1000 AS startMah,
            min(c.value)/1000 AS endMah,
            max(c.value)/1000 - min(c.value)/1000 AS diffMah
        FROM counter c
        JOIN counter_track t ON c.track_id = t.id
        WHERE t.name = 'batt.charge_uah'
        AND c.ts >= ${slice.ts} AND c.ts <= ${slice.endTs}
    """.trimIndent()

    data class BatteryDischargeMeasurement(
        var name: String,
        var chargeMah: Double
    )

    public fun getBatteryDischargeMetrics(
        absoluteTracePath: String,
        slice: Slice
    ): List<BatteryDischargeMeasurement> {
        val queryResult = PerfettoTraceProcessor.rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = getFullQuery(slice)
        )

        val resultLines = queryResult.split("\n")

        if (resultLines.first() != """"startMah","endMah","diffMah"""") {
            throw IllegalStateException("query failed!\n${getFullQuery(slice)}")
        }

        // results are in CSV with a header row, with 1 row of results
        val columns = resultLines
            .filter { it.isNotBlank() } // drop blank lines
            .drop(1) // drop the header row
            .joinToString().split(",")

        if (columns.isEmpty()) {
            return emptyList()
        }

        return listOf(
            BatteryDischargeMeasurement(
                name = "Start",
                chargeMah = columns[0].toDouble(),
            ),
            BatteryDischargeMeasurement(
                name = "End",
                chargeMah = columns[1].toDouble()
            ),
            BatteryDischargeMeasurement(
                name = "Diff",
                chargeMah = columns[2].toDouble()
            )
        )
    }
}