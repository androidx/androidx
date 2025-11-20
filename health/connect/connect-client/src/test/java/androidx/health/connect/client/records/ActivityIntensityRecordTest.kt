/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.records.ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_STRING_TO_INT_MAP
import androidx.health.connect.client.records.metadata.Metadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ActivityIntensityRecordTest {

    @Test
    fun equals_validRecord() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 16)
        EqualsTester()
            .addEqualityGroup(
                ActivityIntensityRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata.manualEntry(),
                    activityIntensityType =
                        ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
                ),
                ActivityIntensityRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata.manualEntry(),
                    activityIntensityType =
                        ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
                ),
            )
            .addEqualityGroup(
                ActivityIntensityRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    metadata = Metadata.manualEntry(),
                    activityIntensityType =
                        ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_VIGOROUS,
                )
            )
            .testEquals()
    }

    @Test
    fun hashCode_includesAllFields() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 16)
        val baseRecord =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
        val baseRecord2 =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
        val otherStartTime =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1235L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
        val otherZoneOffset =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.ofHours(1),
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.ofHours(1),
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
        val otherType =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_VIGOROUS,
            )
        val otherMetadata =
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata.unknownRecordingMethod(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )

        assertThat(baseRecord.hashCode()).isEqualTo(baseRecord2.hashCode())
        assertThat(baseRecord.hashCode()).isNotEqualTo(otherStartTime.hashCode())
        assertThat(baseRecord.hashCode()).isNotEqualTo(otherZoneOffset.hashCode())
        assertThat(baseRecord.hashCode()).isNotEqualTo(otherType.hashCode())
        assertThat(baseRecord.hashCode()).isNotEqualTo(otherMetadata.hashCode())
    }

    @Ignore // b/442020059
    @Test
    fun constructor_invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            ActivityIntensityRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
                activityIntensityType =
                    ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
        }
    }

    @Test
    fun toString_containsMembers() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 16)
        assertThat(
                ActivityIntensityRecord(
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(1236L),
                        endZoneOffset = null,
                        metadata = Metadata.unknownRecordingMethod(),
                        activityIntensityType =
                            ActivityIntensityRecord.Companion.ACTIVITY_INTENSITY_TYPE_MODERATE,
                    )
                    .toString()
            )
            .isEqualTo(
                "ActivityIntensityRecord(startTime=1970-01-01T00:00:01.234Z, startZoneOffset=null, endTime=1970-01-01T00:00:01.236Z, endZoneOffset=null, activityIntensityType=0, metadata=Metadata(id='', dataOrigin=DataOrigin(packageName=''), lastModifiedTime=1970-01-01T00:00:00Z, clientRecordId=null, clientRecordVersion=0, device=null, recordingMethod=0))"
            )
    }

    @Test
    fun allActivityIntensityTypeEnums_hasMapping() {
        val allEnums =
            ActivityIntensityRecord.Companion::class
                .members
                .asSequence()
                .filter { it -> it.name.startsWith("ACTIVITY_INTENSITY_TYPE") }
                .filter { it -> it.returnType == typeOf<Int>() }
                .map { it -> it.call(ActivityIntensityRecord.Companion) }
                .toHashSet()

        assertThat(ACTIVITY_INTENSITY_TYPE_STRING_TO_INT_MAP.values.toSet())
            .containsExactlyElementsIn(allEnums)
        assertThat(ACTIVITY_INTENSITY_TYPE_INT_TO_STRING_MAP.keys)
            .containsExactlyElementsIn(allEnums)
    }
}
