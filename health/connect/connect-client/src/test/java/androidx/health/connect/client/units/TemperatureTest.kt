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
class TemperatureTest {

    @Rule @JvmField val expect = Expect.create()

    @Test
    fun equals_sameValues_areEqual() {
        expect.that(Temperature.celsius(37.6)).isEqualTo(Temperature.celsius(37.6))
        expect.that(Temperature.celsius(37.6)).isEqualTo(Temperature.fahrenheit(99.68))
    }

    @Test
    fun equals_differentValues_areNotEqual() {
        expect.that(Temperature.celsius(37.61)).isNotEqualTo(Temperature.celsius(37.6))
        expect.that(Temperature.celsius(37.61)).isNotEqualTo(Temperature.fahrenheit(99.68))
    }

    @Test
    fun hashCode_sameValues_areEqual() {
        expect
            .that(Temperature.celsius(37.6).hashCode())
            .isEqualTo(Temperature.fahrenheit(99.68).hashCode())
    }

    @Test
    fun hashCode_differentValues_areNotEqual() {
        expect
            .that(Temperature.celsius(37.61).hashCode())
            .isNotEqualTo(Temperature.fahrenheit(99.68).hashCode())
    }
}
