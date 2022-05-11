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

import androidx.benchmark.macro.PowerCategory
import androidx.benchmark.macro.PowerMetric

// We want to use android_powrails.sql, but cannot as they do not split into sections with slice

internal object PowerQuery {
    private fun getFullQuery(slice: Slice) = """
        SELECT
            t.name,
            max(c.value) - min(c.value) AS energyUws,
            (max(c.value) - min(c.value))/((max(c.ts) - min(c.ts)) / 1000000) AS powerUs
        FROM counter c
        JOIN counter_track t ON c.track_id = t.id
        WHERE t.name GLOB 'power.*'
        AND c.ts >= ${slice.ts} AND c.ts <= ${slice.endTs}
        GROUP BY t.name
    """.trimIndent()

    private val categoryToSubsystem = mapOf(
        PowerCategory.CPU to listOf("Cpu"),
        PowerCategory.DISPLAY to listOf("Display"),
        PowerCategory.GPU to listOf("Gpu"),
        PowerCategory.GPS to listOf("Gps"),
        PowerCategory.MEMORY to listOf("Ddr", "MemoryInterface"),
        PowerCategory.MACHINE_LEARNING to listOf("Tpu"),
        PowerCategory.NETWORK to listOf(
            "Aoc",
            "Radio",
            "VsysPwrMmwave",
            "Wifi",
            "Modem"
        ),
        PowerCategory.UNCATEGORIZED to emptyList()
    )

    /**
     * The ComponentMeasurement object are built with attributes:
     *
     * @param `name` - The name of the subsystem associated with the power usage in camel case.
     *
     * @param `energyUws` - The energy used during the trace, measured in uWs.
     *
     * @param `powerUw` - The energy used divided by the elapsed time, measured in uW.
     */
    data class ComponentMeasurement(
        var name: String,
        var energyUws: Double,
        var powerUw: Double,
    ) {
        fun getValue(type: PowerMetric.Type): Double {
            return if (type is PowerMetric.Type.Power) powerUw else energyUws
        }
    }

    /**
     * The CategoryMeasurement object are built with attributes:
     *
     * @param `energyUws` - The sum total energy used during the trace of all components in the
     * category, measured in uWs.
     *
     * @param `powerUw` - The sum total energy used divided by the elapsed time of all components in
     * the category, measured in uW.
     *
     * @param `components` - A list of all ComponentMeasurements under the same `PowerCategory`.
     */
    data class CategoryMeasurement(
        var energyUws: Double,
        var powerUw: Double,
        var components: List<ComponentMeasurement>
    ) {
        fun getValue(type: PowerMetric.Type): Double {
            return if (type is PowerMetric.Type.Power) powerUw else energyUws
        }
    }

    public fun getPowerMetrics(
        absoluteTracePath: String,
        slice: Slice
    ): Map<PowerCategory, CategoryMeasurement> {
        // gather all recorded rails
        val railMetrics: List<ComponentMeasurement> = getRailMetrics(absoluteTracePath, slice)
        railMetrics.ifEmpty { return emptyMap() }

        // sort ComponentMeasurements into CategoryMeasurements
        return sortComponentsByCategories(railMetrics)
    }

    private fun sortComponentsByCategories(
        railMetrics: List<ComponentMeasurement>
    ): Map<PowerCategory, CategoryMeasurement> {
        // sort all ComponentMeasurements into CategoryMeasurements
        return PowerCategory.values().associateWith { category ->
            // combine components under same category
            val rails: List<ComponentMeasurement> = railMetrics.filter { rail ->
                railInCategory(category, rail.name)
            }

            // combine components into category
            rails.fold(
                CategoryMeasurement(
                    energyUws = 0.0,
                    powerUw = 0.0,
                    components = rails
                )
            ) { total, next ->
                CategoryMeasurement(
                    energyUws = total.energyUws + next.energyUws,
                    powerUw = total.powerUw + next.powerUw,
                    components = total.components
                )
            }
        }.filter { (_, measurement) ->
            measurement.components.isNotEmpty()
        }
    }

    private fun getRailMetrics(
        absoluteTracePath: String,
        slice: Slice
    ): List<ComponentMeasurement> {
        val queryResult = PerfettoTraceProcessor.rawQuery(
            absoluteTracePath = absoluteTracePath,
            query = getFullQuery(slice)
        )

        val resultLines = queryResult.split("\n")

        if (resultLines.first() != """"name","energyUws","powerUs"""") {
            throw IllegalStateException("query failed!\n${getFullQuery(slice)}")
        }

        // results are in CSV with a header row, and strings wrapped with quotes
        return resultLines
            .filter { it.isNotBlank() } // drop blank lines
            .drop(1) // drop the header row
            .map {
                val columns = it.split(",")
                ComponentMeasurement(
                    name = columns[0].unquote().camelCase(),
                    energyUws = columns[1].toDouble(),
                    powerUw = columns[2].toDouble(),
                )
            }
    }

    /**
     * Checks if category contains rail, or is uncategorized.
     *
     * @param `category` - A [PowerCategory] which maps to a list of subsystems.
     *
     * @param `railName` - The name of a rail.
     *
     */
    private fun railInCategory(
        category: PowerCategory,
        railName: String
    ): Boolean {
        if (category == PowerCategory.UNCATEGORIZED) {
            return !filterRails(categoryToSubsystem.values.flatten(), railName)
        }
        return filterRails(categoryToSubsystem[category] ?: emptyList(), railName)
    }

    /**
     * Checks if rail name contains subsystem.
     *
     * @param `subsystems` - A list of subsystems to check against rail name.  If the rail is a
     * part of the subsystem, the subsystem will be a substring of the rail name.
     *
     * @param `railName` - The name of a rail.
     */
    private fun filterRails(
        subsystems: List<String>,
        railName: String
    ): Boolean {
        for (subsystem in subsystems) {
            if (railName.contains(subsystem)) {
                return true
            }
        }
        return false
    }

    init {
        check(categoryToSubsystem.keys.size == PowerCategory.values().size) {
            "Missing power categories in categoryToSubsystem"
        }
    }
}