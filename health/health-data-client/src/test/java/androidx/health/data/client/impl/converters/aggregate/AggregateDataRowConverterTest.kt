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
package androidx.health.data.client.impl.converters.aggregate

import androidx.health.data.client.aggregate.AggregateDataRow
import androidx.health.data.client.aggregate.AggregateDataRowGroupByDuration
import androidx.health.data.client.metadata.DataOrigin
import androidx.health.platform.client.proto.DataProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AggregateDataRowConverterTest {
    @Test
    fun retrieveAggregateDataRow() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .build()

        proto
            .retrieveAggregateDataRow()
            .assertEquals(
                AggregateDataRow(
                    longValues = mapOf(Pair("longKey", 567L)),
                    doubleValues = mapOf(Pair("doubleKey", 123.4)),
                    dataOrigins = listOf(DataOrigin("testApp")),
                )
            )
    }

    @Test
    // ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
    @SuppressWarnings("GoodTime")
    fun toAggregateDataRowGroupByDuration() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartTimeEpochMs(1111)
                .setEndTimeEpochMs(9999)
                .setZoneOffsetSeconds(123)
                .build()

        proto
            .toAggregateDataRowGroupByDuration()
            .assertEquals(
                AggregateDataRowGroupByDuration(
                    data =
                        AggregateDataRow(
                            longValues = mapOf(Pair("longKey", 567L)),
                            doubleValues = mapOf(Pair("doubleKey", 123.4)),
                            dataOrigins = listOf(DataOrigin("testApp")),
                        ),
                    startTime = Instant.ofEpochMilli(1111),
                    endTime = Instant.ofEpochMilli(9999),
                    zoneOffset = ZoneOffset.ofTotalSeconds(123),
                )
            )
    }

    @Test
    fun toAggregateDataRowGroupByDuration_startOrEndTimeNotSet_throws() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartTimeEpochMs(1111)
                .setEndTimeEpochMs(9999)
                .setZoneOffsetSeconds(123)
                .build()

        var thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto
                    .toBuilder()
                    .clearStartTimeEpochMs()
                    .build()
                    .toAggregateDataRowGroupByDuration()
            }
        assertThat(thrown.message).isEqualTo("start time must be set")
        thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto.toBuilder().clearEndTimeEpochMs().build().toAggregateDataRowGroupByDuration()
            }
        assertThat(thrown.message).isEqualTo("end time must be set")
    }

    private fun AggregateDataRow.assertEquals(expected: AggregateDataRow) {
        assertThat(longValues).isEqualTo(expected.longValues)
        assertThat(doubleValues).isEqualTo(expected.doubleValues)
        assertThat(dataOrigins).isEqualTo(expected.dataOrigins)
    }

    // ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
    @SuppressWarnings("GoodTime")
    private fun AggregateDataRowGroupByDuration.assertEquals(
        expected: AggregateDataRowGroupByDuration,
    ) {
        data.assertEquals(expected.data)
        assertThat(startTime.toEpochMilli()).isEqualTo(1111)
        assertThat(endTime.toEpochMilli()).isEqualTo(9999)
        assertThat(zoneOffset.totalSeconds).isEqualTo(123)
    }
}
