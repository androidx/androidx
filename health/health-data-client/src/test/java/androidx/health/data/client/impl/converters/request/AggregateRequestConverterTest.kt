/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.impl.converters.request

import androidx.health.data.client.impl.converters.time.toProto
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.data.client.records.Steps
import androidx.health.data.client.request.AggregateGroupByDurationRequest
import androidx.health.data.client.request.AggregateRequest
import androidx.health.data.client.time.TimeRangeFilter
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.RequestProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

private val METRIC = Steps.STEPS_COUNT_TOTAL
private val METRIC_PROTO =
    RequestProto.AggregateMetricSpec.newBuilder()
        .setDataTypeName("Steps")
        .setAggregationType("total")
        .setFieldName("count")
private val TIME_RANGE_FILTER =
    TimeRangeFilter.between(Instant.ofEpochMilli(123), Instant.ofEpochMilli(456))
private val DATA_ORIGIN_FILTER = listOf(DataOrigin("testAppName"))

@RunWith(AndroidJUnit4::class)
class AggregateRequestConverterTest {
    @Test
    fun aggregateRequestToProto() {
        val request =
            AggregateRequest(
                metrics = setOf(METRIC),
                timeRangeFilter = TIME_RANGE_FILTER,
                dataOriginFilter = DATA_ORIGIN_FILTER
            )

        assertThat(request.toProto())
            .isEqualTo(
                RequestProto.AggregateDataRequest.newBuilder()
                    .addMetricSpec(METRIC_PROTO)
                    .addAllDataOrigin(DATA_ORIGIN_FILTER.toProtoList())
                    .setTimeSpec(TIME_RANGE_FILTER.toProto())
                    .build()
            )
    }

    @Test
    fun aggregateGroupByDurationRequestToProto() {
        val request =
            AggregateGroupByDurationRequest(
                metrics = setOf(METRIC),
                timeRangeFilter = TIME_RANGE_FILTER,
                timeRangeSlicer = Duration.ofMillis(98765),
                dataOriginFilter = DATA_ORIGIN_FILTER
            )

        assertThat(request.toProto())
            .isEqualTo(
                RequestProto.AggregateDataRequest.newBuilder()
                    .addMetricSpec(METRIC_PROTO)
                    .addAllDataOrigin(DATA_ORIGIN_FILTER.toProtoList())
                    .setTimeSpec(TIME_RANGE_FILTER.toProto())
                    .setSliceDurationMillis(98765)
                    .build()
            )
    }

    private fun List<DataOrigin>.toProtoList() =
        this.map { DataProto.DataOrigin.newBuilder().setApplicationId(it.packageName).build() }
}
