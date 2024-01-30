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

import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.Slice
import org.intellij.lang.annotations.Language

internal object BatteryDischargeQuery {
    @Language("sql")
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

    fun getBatteryDischargeMetrics(
    session: PerfettoTraceProcessor.Session,
        slice: Slice
    ): List<BatteryDischargeMeasurement> {
        val queryResult = session.query(
            query = getFullQuery(slice)
        ).toList()

        if (queryResult.isEmpty()) {
            return emptyList()
        }

        if (queryResult.size != 1) {
            throw IllegalStateException("Unexpected query result size for battery discharge.")
        }

        val row = queryResult.single()
        return listOf(
            BatteryDischargeMeasurement(
                name = "Start",
                chargeMah = row["startMah"] as Double,
            ),
            BatteryDischargeMeasurement(
                name = "End",
                chargeMah = row["endMah"] as Double
            ),
            BatteryDischargeMeasurement(
                name = "Diff",
                chargeMah = row["diffMah"] as Double
            )
        )
    }
}
