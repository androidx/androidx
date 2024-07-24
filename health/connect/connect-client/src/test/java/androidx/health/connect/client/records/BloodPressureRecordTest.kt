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

import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloodPressureRecordTest {
    @Test
    fun bodyPositionEnums_existInMapping() {
        val allEnums = getAllIntDefEnums<BloodPressureRecord>("""BODY_POSITION.*(?<!UNKNOWN)$""")

        assertThat(BloodPressureRecord.BODY_POSITION_STRING_TO_INT_MAP.values)
            .containsExactlyElementsIn(allEnums)
        assertThat(BloodPressureRecord.BODY_POSITION_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }

    @Test
    fun measurementLocationEnums_existInMapping() {
        val allEnums =
            getAllIntDefEnums<BloodPressureRecord>("""MEASUREMENT_LOCATION.*(?<!UNKNOWN)$""")

        assertThat(BloodPressureRecord.MEASUREMENT_LOCATION_STRING_TO_INT_MAP.values)
            .containsExactlyElementsIn(allEnums)
        assertThat(BloodPressureRecord.MEASUREMENT_LOCATION_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                BloodPressureRecord(
                        time = Instant.ofEpochMilli(1234L),
                        zoneOffset = null,
                        systolic = Pressure.millimetersOfMercury(120.0),
                        diastolic = Pressure.millimetersOfMercury(112.0),
                        bodyPosition = BloodPressureRecord.BODY_POSITION_RECLINING,
                        measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST,
                        metadata = Metadata.EMPTY,
                    )
                    .toString()
            )
            .isEqualTo(
                "BloodPressureRecord(time=1970-01-01T00:00:01.234Z, zoneOffset=null, systolic=120.0 mmHg, diastolic=112.0 mmHg, bodyPosition=4, measurementLocation=1, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
