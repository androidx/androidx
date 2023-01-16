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

package androidx.health.connect.client.impl.platform.records

import android.annotation.TargetApi
import android.healthconnect.datatypes.DataOrigin as PlatformDataOrigin
import android.healthconnect.datatypes.Device as PlatformDevice
import android.healthconnect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import android.healthconnect.datatypes.Metadata as PlatformMetadata
import android.healthconnect.datatypes.NutritionRecord as PlatformNutritionRecord
import android.healthconnect.datatypes.StepsRecord as PlatformStepsRecord
import android.healthconnect.datatypes.units.Energy as PlatformEnergy
import android.healthconnect.datatypes.units.Mass as PlatformMass
import android.os.Build
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class RecordsConvertersTest {

    @Test
    fun stepsRecordClass_convertToPlatform() {
        val stepsSdkClass = StepsRecord::class
        val stepsPlatformClass = PlatformStepsRecord::class.java

        assertThat(stepsSdkClass.toPlatformRecordClass()).isEqualTo(stepsPlatformClass)
    }

    @Test
    fun stepsRecord_convertToPlatform() {
        val steps =
            StepsRecord(
                count = 100,
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(5678L),
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata(id = "someId"))

        val platformSteps = steps.toPlatformRecord() as PlatformStepsRecord

        with(platformSteps) {
            assertThat(count).isEqualTo(100)
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(1234L))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(5678L))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(metadata.id).isEqualTo("someId")
        }
    }

    @Test
    fun heartRateRecord_convertToPlatform() {
        val heartRate =
            HeartRateRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(5678L),
                endZoneOffset = ZoneOffset.UTC,
                samples =
                    listOf(
                        HeartRateRecord.Sample(Instant.ofEpochMilli(1234L), 55L),
                        HeartRateRecord.Sample(Instant.ofEpochMilli(5678L), 57L)),
                metadata = Metadata(id = "an id"))

        val platformHeartRate = heartRate.toPlatformRecord() as PlatformHeartRateRecord

        with(platformHeartRate) {
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(1234L))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(5678L))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(samples)
                .comparingElementsUsing(
                    Correspondence.from<
                        PlatformHeartRateRecord.HeartRateSample,
                        PlatformHeartRateRecord.HeartRateSample>(
                        { actual, expected ->
                            actual!!.beatsPerMinute == expected!!.beatsPerMinute &&
                                actual.time == expected.time
                        },
                        "has same BPM and same time as"))
                .containsExactly(
                    PlatformHeartRateRecord.HeartRateSample(55L, Instant.ofEpochMilli(1234L)),
                    PlatformHeartRateRecord.HeartRateSample(57L, Instant.ofEpochMilli(5678L)))
            assertThat(metadata.id).isEqualTo("an id")
        }
    }

    @Test
    fun nutritionRecord_convertToPlatform() {
        val nutrition =
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.ofEpochMilli(5678L),
                endZoneOffset = ZoneOffset.UTC,
                caffeine = Mass.grams(20.0),
                energy = Energy.joules(300.0),
                metadata = Metadata(id = "another id"))

        val platformNutrition = nutrition.toPlatformRecord() as PlatformNutritionRecord
        val tolerance = 1.0e-9

        with(platformNutrition) {
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(1234L))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(5678L))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(caffeine!!.inKilograms)
                .isWithin(tolerance)
                .of(PlatformMass.fromKilograms(0.02).inKilograms)
            assertThat(energy!!.inJoules)
                .isWithin(tolerance)
                .of(PlatformEnergy.fromJoules(300.0).inJoules)
            assertThat(metadata.id).isEqualTo("another id")
        }
    }

    @Test
    fun metadata_convertToPlatform() {
        val metadata =
            Metadata(
                id = "someId",
                dataOrigin = DataOrigin("origin package name"),
                lastModifiedTime = Instant.ofEpochMilli(6666L),
                clientRecordId = "clientId",
                clientRecordVersion = 2L,
                device =
                    Device(
                        manufacturer = "Awesome-watches",
                        model = "AwesomeOne",
                        type = Device.TYPE_WATCH))

        with(metadata.toPlatformMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin)
                .isEqualTo(
                    PlatformDataOrigin.Builder().setPackageName("origin package name").build())
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(device)
                .isEqualTo(
                    PlatformDevice.Builder()
                        .setManufacturer("Awesome-watches")
                        .setModel("AwesomeOne")
                        .setType(PlatformDevice.DEVICE_TYPE_WATCH)
                        .build())
        }
    }

    @Test
    fun metadata_convertToPlatform_noDevice() {
        val metadata =
            Metadata(
                id = "someId",
                dataOrigin = DataOrigin("origin package name"),
                lastModifiedTime = Instant.ofEpochMilli(6666L),
                clientRecordId = "clientId",
                clientRecordVersion = 2L)

        with(metadata.toPlatformMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin)
                .isEqualTo(
                    PlatformDataOrigin.Builder().setPackageName("origin package name").build())
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(device).isEqualTo(PlatformDevice.Builder().build())
        }
    }

    @Test
    fun metadata_convertToSdk() {
        val metadata =
            PlatformMetadata.Builder()
                .apply {
                    setId("someId")
                    setDataOrigin(
                        PlatformDataOrigin.Builder().setPackageName("origin package name").build())
                    setLastModifiedTime(Instant.ofEpochMilli(6666L))
                    setClientRecordId("clientId")
                    setClientRecordVersion(2L)
                    setDevice(
                        PlatformDevice.Builder()
                            .setManufacturer("AwesomeTech")
                            .setModel("AwesomeTwo")
                            .setType(PlatformDevice.DEVICE_TYPE_WATCH)
                            .build())
                }
                .build()

        with(metadata.toSdkMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin).isEqualTo(DataOrigin("origin package name"))
            assertThat(lastModifiedTime).isEqualTo(Instant.ofEpochMilli(6666L))
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(device)
                .isEqualTo(
                    Device(
                        manufacturer = "AwesomeTech",
                        model = "AwesomeTwo",
                        type = Device.TYPE_WATCH))
        }
    }

    @Test
    fun stepsRecord_convertToSdk() {
        val steps =
            PlatformStepsRecord.Builder(
                    PlatformMetadata.Builder()
                        .apply {
                            setDevice(
                                PlatformDevice.Builder()
                                    .setType(PlatformDevice.DEVICE_TYPE_WATCH)
                                    .build())
                            setClientRecordVersion(123L)
                            setDataOrigin(
                                PlatformDataOrigin.Builder()
                                    .setPackageName("com.packageName")
                                    .build())
                            setLastModifiedTime(Instant.ofEpochMilli(9999L))
                        }
                        .build(),
                    Instant.ofEpochMilli(5678L),
                    Instant.ofEpochMilli(9012L),
                    200)
                .build()

        val sdkSteps = steps.toSdkRecord() as StepsRecord

        with(sdkSteps) {
            assertThat(count).isEqualTo(200)
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(5678L))
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(9012L))

            with(metadata) {
                assertThat(device).isEqualTo(Device(type = Device.TYPE_WATCH))
                assertThat(clientRecordVersion).isEqualTo(123L)
                assertThat(dataOrigin).isEqualTo(DataOrigin("com.packageName"))
            }
        }
    }
}
