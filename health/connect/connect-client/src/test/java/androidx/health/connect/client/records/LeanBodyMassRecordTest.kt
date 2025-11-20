/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.health.connect.client.records

import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.kilograms
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(minSdk = 28)
@RunWith(AndroidJUnit4::class)
class LeanBodyMassRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                LeanBodyMassRecord(
                    time = Instant.ofEpochMilli(1678900000L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    mass = 70.5.kilograms,
                )
            )
            .isEqualTo(
                LeanBodyMassRecord(
                    time = Instant.ofEpochMilli(1678900000L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    mass = 70.5.kilograms,
                )
            )
    }

    @Test
    fun invalidMass_throws() {
        assertFailsWith<IllegalArgumentException> {
            LeanBodyMassRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                mass = (-1.0).kilograms,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LeanBodyMassRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                mass = 1001.0.kilograms,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                LeanBodyMassRecord(
                        time = Instant.ofEpochMilli(123456789L),
                        zoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.unknownRecordingMethod(),
                        mass = 65.0.kilograms,
                    )
                    .toString()
            )
            .isEqualTo(
                "LeanBodyMassRecord(time=1970-01-02T10:17:36.789Z, zoneOffset=Z, mass=65.0 kilograms, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
