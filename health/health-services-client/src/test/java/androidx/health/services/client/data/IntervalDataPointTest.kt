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

import android.os.Bundle
import com.google.common.truth.Truth
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntervalDataPointTest {
    fun Int.duration() = Duration.ofSeconds(toLong())

    @Test
    fun protoRoundTrip() {
        val proto = IntervalDataPoint(
            DataType.CALORIES,
            value = 130.0,
            startDurationFromBoot = 10.duration(),
            endDurationFromBoot = 20.duration(),
            Bundle().apply {
                putInt("int", 5)
                putString("string", "value")
            },
            accuracy = null // No interval DataPoints have an accuracy component
        ).proto

        val dataPoint = IntervalDataPoint.fromProto(proto)

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.CALORIES)
        Truth.assertThat(dataPoint.value).isEqualTo(130.0)
        Truth.assertThat(dataPoint.startDurationFromBoot).isEqualTo(10.duration())
        Truth.assertThat(dataPoint.endDurationFromBoot).isEqualTo(20.duration())
        Truth.assertThat(dataPoint.metadata.getInt("int")).isEqualTo(5)
        Truth.assertThat(dataPoint.metadata.getString("string")).isEqualTo("value")
        Truth.assertThat(dataPoint.accuracy).isNull()
    }

    @Test
    fun protoRoundTrip_emptyBundle() {
        val proto = IntervalDataPoint(
            DataType.CALORIES,
            value = 130.0,
            startDurationFromBoot = 10.duration(),
            endDurationFromBoot = 20.duration(),
            metadata = Bundle(),
            accuracy = null // No interval DataPoints have an accuracy component
        ).proto

        val dataPoint = IntervalDataPoint.fromProto(proto)

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.CALORIES)
        Truth.assertThat(dataPoint.value).isEqualTo(130.0)
        Truth.assertThat(dataPoint.startDurationFromBoot).isEqualTo(10.duration())
        Truth.assertThat(dataPoint.endDurationFromBoot).isEqualTo(20.duration())
        Truth.assertThat(dataPoint.metadata.keySet()).isEmpty()
        Truth.assertThat(dataPoint.accuracy).isNull()
    }
}
