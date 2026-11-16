/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BatteryStateTest {

    @Test
    fun equals_sameValues_isEqual() {
        val state = BatteryState(isCharging = true, batteryLevel = 80)
        val other = BatteryState(isCharging = true, batteryLevel = 80)

        assertThat(state).isEqualTo(other)
        assertThat(state.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_differentValues_isNotEqual() {
        val state = BatteryState(isCharging = true, batteryLevel = 80)

        assertThat(state).isNotEqualTo(BatteryState(isCharging = false, batteryLevel = 80))
        assertThat(state).isNotEqualTo(BatteryState(isCharging = true, batteryLevel = 79))
    }

    @Test
    fun toString_containsValues() {
        assertThat(BatteryState(isCharging = true, batteryLevel = 80).toString())
            .isEqualTo("BatteryState(isCharging=true, batteryLevel=80)")
    }
}
