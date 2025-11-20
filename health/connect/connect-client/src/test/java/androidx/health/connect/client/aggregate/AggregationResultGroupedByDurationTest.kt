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

package androidx.health.connect.client.aggregate

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AggregationResultGroupedByDurationTest {
    @Test
    fun constructor_endTimeNotAfterStartTime_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AggregationResultGroupedByDuration(
                result = AggregationResult(mapOf(), mapOf(), setOf()),
                startTime = Instant.parse("2025-02-22T20:22:02.00Z"),
                endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                zoneOffset = ZoneOffset.UTC,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            AggregationResultGroupedByDuration(
                result = AggregationResult(mapOf(), mapOf(), setOf()),
                startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                zoneOffset = ZoneOffset.UTC,
            )
        }

        AggregationResultGroupedByDuration(
            result = AggregationResult(mapOf(), mapOf(), setOf()),
            startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            endTime = Instant.parse("2025-02-22T20:22:02.00Z"),
            zoneOffset = ZoneOffset.UTC,
        )
    }

    @Test
    fun constructor_endTimeNotAfterStartTimeWithValidationEnabled_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AggregationResultGroupedByDuration(
                result = AggregationResult(mapOf(), mapOf(), setOf()),
                startTime = Instant.parse("2025-02-22T20:22:02.00Z"),
                endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                zoneOffset = ZoneOffset.UTC,
                shouldSkipValidation = false,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            AggregationResultGroupedByDuration(
                result = AggregationResult(mapOf(), mapOf(), setOf()),
                startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
                zoneOffset = ZoneOffset.UTC,
                shouldSkipValidation = false,
            )
        }

        AggregationResultGroupedByDuration(
            result = AggregationResult(mapOf(), mapOf(), setOf()),
            startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            endTime = Instant.parse("2025-02-22T20:22:02.00Z"),
            zoneOffset = ZoneOffset.UTC,
            shouldSkipValidation = false,
        )
    }

    @Test
    fun constructor_endTimeNotAfterStartTimeWithSkipValidation_doesNotThrow() {
        AggregationResultGroupedByDuration(
            result = AggregationResult(mapOf(), mapOf(), setOf()),
            startTime = Instant.parse("2025-02-22T20:22:02.00Z"),
            endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            zoneOffset = ZoneOffset.UTC,
            shouldSkipValidation = true,
        )

        AggregationResultGroupedByDuration(
            result = AggregationResult(mapOf(), mapOf(), setOf()),
            startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            endTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            zoneOffset = ZoneOffset.UTC,
            shouldSkipValidation = true,
        )

        AggregationResultGroupedByDuration(
            result = AggregationResult(mapOf(), mapOf(), setOf()),
            startTime = Instant.parse("2025-02-11T20:22:02.00Z"),
            endTime = Instant.parse("2025-02-22T20:22:02.00Z"),
            zoneOffset = ZoneOffset.UTC,
            shouldSkipValidation = true,
        )
    }
}
