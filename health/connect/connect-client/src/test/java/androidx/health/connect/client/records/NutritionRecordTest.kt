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

package androidx.health.connect.client.records

import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.calories
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                NutritionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    energy = Energy.calories(5.0)
                )
            )
            .isEqualTo(
                NutritionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    energy = Energy.calories(5.0)
                )
            )
    }

    @Test
    fun invalidNutritionValue_throws() {
        assertFailsWith<IllegalArgumentException> {
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                energy = Energy.calories(-1.0)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                energy = Energy.calories(100000001.0)
            )
        }
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                NutritionRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        energy = 240.calories
                    )
                    .toString()
            )
            .isEqualTo(
                "NutritionRecord(startTime=1970-01-01T00:00:01.234Z, startZoneOffset=null, endTime=1970-01-01T00:00:01.236Z, endZoneOffset=null, biotin=null, caffeine=null, calcium=null, energy=240.0 cal, energyFromFat=null, chloride=null, cholesterol=null, chromium=null, copper=null, dietaryFiber=null, folate=null, folicAcid=null, iodine=null, iron=null, magnesium=null, manganese=null, molybdenum=null, monounsaturatedFat=null, niacin=null, pantothenicAcid=null, phosphorus=null, polyunsaturatedFat=null, potassium=null, protein=null, riboflavin=null, saturatedFat=null, selenium=null, sodium=null, sugar=null, thiamin=null, totalCarbohydrate=null, totalFat=null, transFat=null, unsaturatedFat=null, vitaminA=null, vitaminB12=null, vitaminB6=null, vitaminC=null, vitaminD=null, vitaminE=null, vitaminK=null, zinc=null, name=null, mealType=0, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
