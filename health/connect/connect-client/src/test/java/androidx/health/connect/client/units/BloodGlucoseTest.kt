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

package androidx.health.connect.client.units

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloodGlucoseTest {

    @Rule @JvmField val expect = Expect.create()

    private val conversions: List<Triple<Double, Double, String>> =
        listOf(
            Triple(
                BloodGlucose.millimolesPerLiter(5.0).inMilligramsPerDeciliter,
                5 * 18.0,
                "mmol/L -> mg/dL",
            ),
            Triple(
                BloodGlucose.millimolesPerLiter(5.0).inMillimolesPerLiter,
                5.0,
                "mmol/L -> mmol/L",
            ),
            Triple(
                BloodGlucose.milligramsPerDeciliter(90.0).inMillimolesPerLiter,
                90 / 18.0,
                "mg/dL -> mmol/L",
            ),
            Triple(
                BloodGlucose.milligramsPerDeciliter(90.0).inMilligramsPerDeciliter,
                90.0,
                "mg/dL -> mg/dL",
            ),
        )

    @Test
    fun conversion() {
        for (conversion in conversions) {
            expect
                .withMessage(conversion.third)
                .that(conversion.first)
                .isWithin(0.00001)
                .of(conversion.second)
        }
    }

    @Test
    fun equals_sameValues_areEqual() {
        expect
            .that(BloodGlucose.millimolesPerLiter(5.0))
            .isEqualTo(BloodGlucose.millimolesPerLiter(5.0))
        expect
            .that(BloodGlucose.millimolesPerLiter(5.0))
            .isEqualTo(BloodGlucose.milligramsPerDeciliter(5.0 * 18.0))
    }

    @Test
    fun equals_differentValues_areNotEqual() {
        expect
            .that(BloodGlucose.millimolesPerLiter(5.01))
            .isNotEqualTo(BloodGlucose.millimolesPerLiter(5.0))
        expect
            .that(BloodGlucose.millimolesPerLiter(5.01))
            .isNotEqualTo(BloodGlucose.milligramsPerDeciliter(5.0 * 18.0))
    }

    @Test
    fun hashCode_sameValues_areEqual() {
        expect
            .that(BloodGlucose.millimolesPerLiter(5.0).hashCode())
            .isEqualTo(BloodGlucose.milligramsPerDeciliter(5.0 * 18.0).hashCode())
    }

    @Test
    fun hashCode_differentValues_areNotEqual() {
        expect
            .that(BloodGlucose.millimolesPerLiter(5.01).hashCode())
            .isNotEqualTo(BloodGlucose.milligramsPerDeciliter(5.0 * 18.0).hashCode())
    }
}
