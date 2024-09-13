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

import androidx.health.connect.client.records.ExercisePerformanceTarget.AmrapTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.CadenceTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.HeartRateTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.PowerTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.RateOfPerceivedExertionTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.SpeedTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.UnknownTarget
import androidx.health.connect.client.records.ExercisePerformanceTarget.WeightTarget
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.metersPerSecond
import androidx.health.connect.client.units.watts
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisePerformanceTargetTest {
    @Test
    fun powerTarget_hashCodeAndEquals() {
        val target1 = PowerTarget(minPower = 10.watts, maxPower = 20.watts)
        val target1Copy = PowerTarget(minPower = 10.watts, maxPower = 20.watts)
        val target2 = PowerTarget(minPower = 15.watts, maxPower = 25.watts)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun speedTarget_hashCodeAndEquals() {
        val target1 = SpeedTarget(minSpeed = 10.metersPerSecond, maxSpeed = 20.metersPerSecond)
        val target1Copy = SpeedTarget(minSpeed = 10.metersPerSecond, maxSpeed = 20.metersPerSecond)
        val target2 = SpeedTarget(minSpeed = 15.metersPerSecond, maxSpeed = 25.metersPerSecond)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun cadenceTarget_hashCodeAndEquals() {
        val target1 = CadenceTarget(minCadence = 10.0, maxCadence = 20.0)
        val target1Copy = CadenceTarget(minCadence = 10.0, maxCadence = 20.0)
        val target2 = CadenceTarget(minCadence = 15.0, maxCadence = 25.0)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun heartRateTarget_hashCodeAndEquals() {
        val target1 = HeartRateTarget(minHeartRate = 100.0, maxHeartRate = 120.0)
        val target1Copy = HeartRateTarget(minHeartRate = 100.0, maxHeartRate = 120.0)
        val target2 = HeartRateTarget(minHeartRate = 110.0, maxHeartRate = 130.0)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun weightTarget_hashCodeAndEquals() {
        val target1 = WeightTarget(mass = 100.kilograms)
        val target1Copy = WeightTarget(mass = 100.kilograms)
        val target2 = WeightTarget(mass = 120.kilograms)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun rateOfPerceivedExertionTarget_hashCodeAndEquals() {
        val target1 = RateOfPerceivedExertionTarget(rpe = 5)
        val target1Copy = RateOfPerceivedExertionTarget(rpe = 5)
        val target2 = RateOfPerceivedExertionTarget(rpe = 7)

        assertThat(target1.hashCode()).isNotEqualTo(target2.hashCode())
        assertThat(target1).isNotEqualTo(target2)
        assertThat(target1.hashCode()).isEqualTo(target1Copy.hashCode())
        assertThat(target1).isEqualTo(target1Copy)
    }

    @Test
    fun amrapTarget_hashCodeAndEquals() {
        assertThat(AmrapTarget.hashCode()).isEqualTo(AmrapTarget.hashCode())
        assertThat(AmrapTarget).isEqualTo(AmrapTarget)
    }

    @Test
    fun unknownTarget_hashCodeAndEquals() {
        assertThat(UnknownTarget.hashCode()).isEqualTo(UnknownTarget.hashCode())
        assertThat(UnknownTarget).isEqualTo(UnknownTarget)
    }
}
