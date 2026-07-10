/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.records.isAtLeastSdkExtension16
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = 36)
class AggregationMappingsTest {

    private val allAggregationMaps =
        listOf(
            DOUBLE_AGGREGATION_METRIC_TYPE_MAP,
            DURATION_AGGREGATION_METRIC_TYPE_MAP,
            DURATION_TO_LONG_AGGREGATION_METRIC_TYPE_MAP,
            ENERGY_AGGREGATION_METRIC_TYPE_MAP,
            GRAMS_AGGREGATION_METRIC_TYPE_MAP,
            KILOGRAMS_AGGREGATION_METRIC_TYPE_MAP,
            LENGTH_AGGREGATION_METRIC_TYPE_MAP,
            LONG_AGGREGATION_METRIC_TYPE_MAP,
            POWER_AGGREGATION_METRIC_TYPE_MAP,
            PRESSURE_AGGREGATION_METRIC_TYPE_MAP,
            TEMPERATURE_DELTA_METRIC_TYPE_MAP,
            VELOCITY_AGGREGATION_METRIC_TYPE_MAP,
            VOLUME_AGGREGATION_METRIC_TYPE_MAP,
        )

    @Before
    fun setUp() {
        // Update this when adding new aggregate metrics.
        Assume.assumeTrue(isAtLeastSdkExtension16())
    }

    @Test
    fun allAggregateMetrics_areAddedToAggregationMaps() {
        val recordClasses = RECORDS_CLASS_NAME_MAP.keys
        val aggregateMetrics =
            recordClasses.flatMap { recordClass ->
                recordClass.java.fields
                    .filter { it.isAggregateMetric() }
                    .map { it.get(null) as AggregateMetric<*> }
            }

        val allMappedMetrics = allAggregationMaps.flatMap { it.keys }
        val missingMetrics = aggregateMetrics.filter { !allMappedMetrics.contains(it) }
        val presentMetrics = aggregateMetrics.filter { allMappedMetrics.contains(it) }
        assertWithMessage(
                "Missing metrics: ${missingMetrics.map { it.metricKey }}, Present Metrics: ${presentMetrics.map { it.metricKey }}"
            )
            .that(allMappedMetrics)
            .containsAtLeastElementsIn(aggregateMetrics)
    }

    private fun Field.isAggregateMetric(): Boolean {
        return Modifier.isStatic(modifiers) &&
            Modifier.isPublic(modifiers) &&
            AggregateMetric::class.java.isAssignableFrom(type)
    }
}
