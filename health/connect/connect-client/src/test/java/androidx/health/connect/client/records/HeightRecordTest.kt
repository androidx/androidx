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
import androidx.health.connect.client.units.meters
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
class HeightRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
                HeightRecord(
                    time = Instant.ofEpochMilli(456789L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    height = 1.75.meters,
                )
            )
            .isEqualTo(
                HeightRecord(
                    time = Instant.ofEpochMilli(456789L),
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    height = 1.75.meters,
                )
            )
    }

    @Test
    fun invalidHeight_throws() {
        assertFailsWith<IllegalArgumentException> {
            HeightRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                height = (-1.0).meters,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            HeightRecord(
                time = Instant.ofEpochMilli(1234L),
                zoneOffset = null,
                metadata = Metadata.manualEntry(),
                height = 3.1.meters,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assertThat(
                HeightRecord(
                        time = Instant.ofEpochMilli(1234L),
                        zoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.unknownRecordingMethod(),
                        height = 2.0.meters,
                    )
                    .toString()
            )
            .isEqualTo(
                "HeightRecord(time=1970-01-01T00:00:01.234Z, zoneOffset=Z, height=2.0 meters, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }

    @Test
    fun aggregateMetrics_areAvailable() {
        assertThat(HeightRecord.HEIGHT_AVG).isNotNull()
        assertThat(HeightRecord.HEIGHT_MIN).isNotNull()
        assertThat(HeightRecord.HEIGHT_MAX).isNotNull()
    }
}
