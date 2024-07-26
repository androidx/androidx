/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.health.connect.client.records

import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkinTemperatureRecordTest {

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord(
                startTime = Instant.ofEpochMilli(1235L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                deltas = emptyList()
            )
        }
    }

    @Test
    fun invalidBaseline_lessThanLimit_throws() {
        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                baseline = Temperature.celsius(-0.1),
                deltas = emptyList()
            )
        }
    }

    @Test
    fun invalidBaseline_moreThanLimit_throws() {
        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                baseline = Temperature.celsius(100.1),
                deltas = emptyList()
            )
        }
    }

    @Test
    fun invalidDeltaTime_beforeStartTime_throws() {
        val delta =
            SkinTemperatureRecord.Delta(
                time = Instant.ofEpochMilli(1231L),
                delta = TemperatureDelta.celsius(2.0)
            )

        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                deltas = listOf(delta)
            )
        }
    }

    @Test
    fun invalidDeltaTime_afterEndTime_throws() {
        val delta =
            SkinTemperatureRecord.Delta(
                time = Instant.ofEpochMilli(1237L),
                delta = TemperatureDelta.celsius(2.0)
            )

        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                deltas = listOf(delta)
            )
        }
    }

    @Test
    fun invalidDelta_lessThanLimit_throws() {
        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord.Delta(
                time = Instant.ofEpochMilli(1237L),
                delta = TemperatureDelta.celsius(-30.1)
            )
        }
    }

    @Test
    fun invalidDelta_moreThanLimit_throws() {
        assertFailsWith<IllegalArgumentException> {
            SkinTemperatureRecord.Delta(
                time = Instant.ofEpochMilli(1237L),
                delta = TemperatureDelta.celsius(30.1)
            )
        }
    }

    @Test
    fun validRecords_equals() {
        assertThat(
                SkinTemperatureRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    deltas =
                        listOf(
                            SkinTemperatureRecord.Delta(
                                time = Instant.ofEpochMilli(1234L),
                                delta = TemperatureDelta.celsius(2.0)
                            )
                        )
                )
            )
            .isEqualTo(
                SkinTemperatureRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    deltas =
                        listOf(
                            SkinTemperatureRecord.Delta(
                                time = Instant.ofEpochMilli(1234L),
                                delta = TemperatureDelta.celsius(2.0)
                            )
                        )
                )
            )
    }

    @Test
    fun validRecords_hash_equals() {
        assertThat(
                SkinTemperatureRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        deltas =
                            listOf(
                                SkinTemperatureRecord.Delta(
                                    time = Instant.ofEpochMilli(1234L),
                                    delta = TemperatureDelta.celsius(2.0)
                                )
                            )
                    )
                    .hashCode()
            )
            .isEqualTo(
                SkinTemperatureRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        deltas =
                            listOf(
                                SkinTemperatureRecord.Delta(
                                    time = Instant.ofEpochMilli(1234L),
                                    delta = TemperatureDelta.celsius(2.0)
                                )
                            )
                    )
                    .hashCode()
            )
    }
}
