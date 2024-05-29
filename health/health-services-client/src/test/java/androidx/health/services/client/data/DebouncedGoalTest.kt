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

package androidx.health.services.client.data

import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataType.Companion.PACE_STATS
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebouncedGoalTest {
    @Test
    fun sampleDataType_protoRoundTrip() {
        val debouncedDataTypeCondition =
            DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                HEART_RATE_BPM,
                120.0,
                GREATER_THAN,
                /* initialDelay= */ 60,
                /* durationAtThreshold= */ 5
            )
        val proto = (DebouncedGoal.createSampleDebouncedGoal(debouncedDataTypeCondition)).proto
        val debouncedGoal = DebouncedGoal.fromProto(proto)

        assertThat(debouncedGoal.proto).isEqualTo(proto)
    }

    @Test
    fun aggregateDataType_protoRoundTrip() {
        val debouncedDataTypeCondition =
            DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                PACE_STATS,
                4.0,
                GREATER_THAN,
                /* initialDelay= */ 60,
                /* durationAtThreshold= */ 5
            )
        val proto = (DebouncedGoal.createAggregateDebouncedGoal(debouncedDataTypeCondition)).proto
        val debouncedGoal = DebouncedGoal.fromProto(proto)

        assertThat(debouncedGoal.proto).isEqualTo(proto)
    }
}
