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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloodGlucoseRecordTest {

    @Test
    fun relationToMealEnums_existInMapping() {
        val allEnums =
            getAllIntDefEnums<BloodGlucoseRecord>("""RELATION_TO_MEAL.*(?<!UNKNOWN)$""")

        assertThat(BloodGlucoseRecord.RELATION_TO_MEAL_STRING_TO_INT_MAP.values)
            .containsExactlyElementsIn(allEnums)
        assertThat(BloodGlucoseRecord.RELATION_TO_MEAL_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }

    @Test
    fun specimenSourceEnums_existInMapping() {
        val allEnums = getAllIntDefEnums<BloodGlucoseRecord>("""SPECIMEN_SOURCE.*(?<!UNKNOWN)$""")

        assertThat(BloodGlucoseRecord.SPECIMEN_SOURCE_STRING_TO_INT_MAP.values)
            .containsExactlyElementsIn(allEnums)
        assertThat(BloodGlucoseRecord.SPECIMEN_SOURCE_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }
}
