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
import androidx.health.connect.client.units.Percentage
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
class OxygenSaturationRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                OxygenSaturationRecord(
                    time = Instant.ofEpochMilli(1678900000L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    percentage = Percentage(98.5),
                )
            )
            .isEqualTo(
                OxygenSaturationRecord(
                    time = Instant.ofEpochMilli(1678900000L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    percentage = Percentage(98.5),
                )
            )
    }

    @Test
    fun invalidPercentage_throws() {
        assertFailsWith<IllegalArgumentException> {
            OxygenSaturationRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                percentage = Percentage(-1.0),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OxygenSaturationRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                percentage = Percentage(101.0),
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                OxygenSaturationRecord(
                        time = Instant.ofEpochMilli(123456789L),
                        zoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.unknownRecordingMethod(),
                        percentage = Percentage(97.0),
                    )
                    .toString()
            )
            .isEqualTo(
                "OxygenSaturationRecord(time=1970-01-02T10:17:36.789Z, zoneOffset=Z, percentage=97.0%, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }
}
