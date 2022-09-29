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

package androidx.health.connect.client.impl.converters.records

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.kilocaloriesPerDay
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordAggregationsTest {

    @Test
    fun totalCaloriesBurned_energyTotalAggregates() {
        val total =
            aggregate(
                metric = TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                records = listOf(
                    TotalCaloriesBurnedRecord(
                        energy = 1.kilocalories,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = END_TIME,
                        endZoneOffset = null,
                    )
                )
            )

        assertEquals(1.kilocalories, total)
    }

    @Test
    fun basalMetabolicRate_basalCaloriesTotalAggregates() {
        val total =
            aggregate(
                metric = BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL,
                records = listOf(
                    BasalMetabolicRateRecord(
                        basalMetabolicRate = 1.kilocaloriesPerDay,
                        time = START_TIME,
                        zoneOffset = null,
                    )
                ),
                fieldName = "bmr",
            )

        assertEquals(1.kilocalories, total)
    }

    @Test
    fun nutrition_energyTotalAggregates() {
        val total =
            aggregate(
                metric = NutritionRecord.ENERGY_TOTAL,
                records = listOf(
                    NutritionRecord(
                        energy = 1.kilocalories,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = END_TIME,
                        endZoneOffset = null,
                    )
                ),
            )

        assertEquals(1.kilocalories, total)
    }

    @Test
    fun nutrition_energyFromFatTotalAggregates() {
        val total =
            aggregate(
                metric = NutritionRecord.ENERGY_FROM_FAT_TOTAL,
                records = listOf(
                    NutritionRecord(
                        energyFromFat = 1.kilocalories,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = END_TIME,
                        endZoneOffset = null,
                    )
                ),
            )

        assertEquals(1.kilocalories, total)
    }

    @Test
    fun activeCaloriesBurned_activeCaloriesTotalAggregates() {
        val total =
            aggregate(
                metric = ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                records = listOf(
                    ActiveCaloriesBurnedRecord(
                        energy = 1.kilocalories,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = END_TIME,
                        endZoneOffset = null,
                    )
                ),
            )

        assertEquals(1.kilocalories, total)
    }

    private fun <T : Any> aggregate(
        metric: AggregateMetric<T>,
        records: Iterable<Record>,
        fieldName: String = requireNotNull(metric.aggregationField),
    ): T? {
        val metricKey = metric.metricKey
        val doubleValues = HashMap<String, Double>()
        val longValues = HashMap<String, Long>()

        for (record in records) {
            val value = record.toProto().valuesMap.getValue(fieldName)
            when {
                value.hasDoubleVal() -> doubleValues.merge(metricKey, value.doubleVal, Double::plus)
                value.hasLongVal() -> longValues.merge(metricKey, value.longVal, Long::plus)
            }
        }

        return AggregationResult(
            longValues = longValues,
            doubleValues = doubleValues,
            dataOrigins = emptySet(),
        )[metric]
    }

    private companion object {
        @SuppressWarnings("GoodTime") // Safe to use in test
        private val START_TIME = Instant.ofEpochMilli(1234L)

        @SuppressWarnings("GoodTime") // Safe to use in test
        private val END_TIME = Instant.ofEpochMilli(5678L)
    }
}