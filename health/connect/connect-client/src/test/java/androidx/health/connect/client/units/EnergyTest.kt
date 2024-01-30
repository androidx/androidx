/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.units

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnergyTest {

    @Rule @JvmField val expect = Expect.create()

    @Test
    fun equals_sameValues_areEqual() {
        expect.that(Energy.kilocalories(235.0)).isEqualTo(Energy.kilocalories(235.0))
        expect.that(Energy.kilocalories(235.0)).isEqualTo(Energy.calories(235_000.0))
    }

    @Test
    fun equals_differentValues_areNotEqual() {
        expect.that(Energy.kilocalories(235.001)).isNotEqualTo(Energy.kilocalories(235.0))
        expect.that(Energy.kilocalories(235.001)).isNotEqualTo(Energy.calories(235_000.0))
    }

    @Test
    fun hashCode_sameValues_areEqual() {
        expect
            .that(Energy.kilocalories(235.0).hashCode())
            .isEqualTo(Energy.calories(235_000.0).hashCode())
    }

    @Test
    fun hashCode_differentValues_areNotEqual() {
        expect
            .that(Energy.kilocalories(235.001).hashCode())
            .isNotEqualTo(Energy.calories(235_000.0).hashCode())
    }

    @Test
    fun calories_roundTrip() {
        expect
            .that(Energy.calories(1.0).inCalories)
            .isEqualTo(1.0)
    }

    @Test
    fun kilocalories_roundTrip() {
        expect
            .that(Energy.kilocalories(1.0).inKilocalories)
            .isEqualTo(1.0)
    }

    @Test
    fun joules_roundTrip() {
        expect
            .that(Energy.joules(1.0).inJoules)
            .isEqualTo(1.0)
    }

    @Test
    fun kilojoules_roundTrip() {
        expect
            .that(Energy.kilojoules(1.0).inKilojoules)
            .isEqualTo(1.0)
    }
}
