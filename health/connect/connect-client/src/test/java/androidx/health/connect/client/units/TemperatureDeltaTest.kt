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

package androidx.health.connect.client.units

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemperatureDeltaTest {
    @Rule @JvmField val expect = Expect.create()

    @Test
    fun equals_sameValues_areEqual() {
        expect.that(TemperatureDelta.celsius(-1.0)).isEqualTo(TemperatureDelta.celsius(-1.0))
        expect.that(TemperatureDelta.celsius(-1.0)).isEqualTo(TemperatureDelta.fahrenheit(-1.8))
    }

    @Test
    fun equals_differentValues_areEqual() {
        expect.that(TemperatureDelta.celsius(-1.0)).isNotEqualTo(TemperatureDelta.celsius(-1.1))
        expect.that(TemperatureDelta.celsius(-1.0)).isNotEqualTo(TemperatureDelta.fahrenheit(-1.9))
    }

    @Test
    fun hashCode_sameValues_areEqual() {
        expect
            .that(TemperatureDelta.celsius(1.0).hashCode())
            .isEqualTo(TemperatureDelta.fahrenheit(1.8).hashCode())
    }

    @Test
    fun hashCode_differentValues_areNotEqual() {
        expect
            .that(TemperatureDelta.celsius(1.0).hashCode())
            .isNotEqualTo(TemperatureDelta.fahrenheit(1.9).hashCode())
    }

    @Test
    fun inCelsius_valueInFahrenheit_convertsValue() {
        expect.that(TemperatureDelta.fahrenheit(1.8).inCelsius).isEqualTo(1.0)
    }

    @Test
    fun inCelsius_valueInCelsius_returnsValue() {
        expect.that(TemperatureDelta.celsius(1.0).inCelsius).isEqualTo(1.0)
    }

    @Test
    fun inFahrenheit_valueInFahrenheit_returnsValue() {
        expect.that(TemperatureDelta.fahrenheit(1.8).inFahrenheit).isEqualTo(1.8)
    }

    @Test
    fun inFahrenheit_valueInCelsius_convertsValue() {
        expect.that(TemperatureDelta.celsius(1.0).inFahrenheit).isEqualTo(1.8)
    }
}
