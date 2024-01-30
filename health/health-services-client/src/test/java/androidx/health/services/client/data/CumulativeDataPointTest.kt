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

import com.google.common.truth.Truth
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CumulativeDataPointTest {
    fun Int.instant() = Instant.ofEpochMilli(toLong())

    @Test
    fun protoRoundTrip() {
        val proto = CumulativeDataPoint(
            dataType = DataType.CALORIES_TOTAL,
            total = 100.0,
            start = 10.instant(),
            end = 99.instant(),
        ).proto

        val dataPoint = CumulativeDataPoint.fromProto(proto.cumulativeDataPoint)

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.CALORIES_TOTAL)
        Truth.assertThat(dataPoint.total).isEqualTo(100.0)
        Truth.assertThat(dataPoint.start).isEqualTo(10.instant())
        Truth.assertThat(dataPoint.end).isEqualTo(99.instant())
    }
}
