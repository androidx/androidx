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

package androidx.health.services.client.data

import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN
import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.INCLINE_DURATION
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DataTypeConditionTest {

    @Test
    fun protoRoundTripDelta() {
        val proto = DataTypeCondition(INCLINE_DURATION, 10, GREATER_THAN).proto

        val observed = DataTypeCondition.deltaFromProto(proto)

        assertThat(observed.dataType).isEqualTo(INCLINE_DURATION)
        assertThat(observed.threshold).isEqualTo(10)
        assertThat(observed.comparisonType).isEqualTo(GREATER_THAN)
    }

    @Test
    fun protoRoundTripAggregate() {
        val proto = DataTypeCondition(CALORIES_TOTAL, 10.5, GREATER_THAN).proto

        val observed = DataTypeCondition.aggregateFromProto(proto)

        assertThat(observed.dataType).isEqualTo(CALORIES_TOTAL)
        assertThat(observed.threshold).isEqualTo(10.5)
        assertThat(observed.comparisonType).isEqualTo(GREATER_THAN)
    }
}
