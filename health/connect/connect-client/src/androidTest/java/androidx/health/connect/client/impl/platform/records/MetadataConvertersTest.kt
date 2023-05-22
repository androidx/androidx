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
import android.os.Build
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class MetadataConvertersTest {

    fun metadata_convertToPlatform() {
        val metadata =
            Metadata(
                id = "someId",
                dataOrigin = DataOrigin("origin package name"),
                lastModifiedTime = Instant.ofEpochMilli(6666L),
                clientRecordId = "clientId",
                clientRecordVersion = 2L,
                recordingMethod = Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED,
                device =
                    Device(
                        manufacturer = "Awesome-watches",
                        model = "AwesomeOne",
                        type = Device.TYPE_WATCH
                    )
            )

        with(metadata.toPlatformMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin)
                .isEqualTo(
                    PlatformDataOriginBuilder().setPackageName("origin package name").build()
                )
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(recordingMethod)
                .isEqualTo(PlatformMetadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED)
            assertThat(device)
                .isEqualTo(
                    PlatformDeviceBuilder()
                        .setManufacturer("Awesome-watches")
                        .setModel("AwesomeOne")
                        .setType(PlatformDevice.DEVICE_TYPE_WATCH)
                        .build()
                )
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
                clientRecordVersion = 2L
            )

        with(metadata.toPlatformMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin)
                .isEqualTo(
                    PlatformDataOriginBuilder().setPackageName("origin package name").build()
                )
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(device).isEqualTo(PlatformDeviceBuilder().build())
        }
    }

    @Test
    fun metadata_convertToSdk() {
        val metadata =
            PlatformMetadataBuilder()
                .apply {
                    setId("someId")
                    setDataOrigin(
                        PlatformDataOriginBuilder().setPackageName("origin package name").build()
                    )
                    setLastModifiedTime(Instant.ofEpochMilli(6666L))
                    setClientRecordId("clientId")
                    setClientRecordVersion(2L)
                    setRecordingMethod(PlatformMetadata.RECORDING_METHOD_MANUAL_ENTRY)
                    setDevice(
                        PlatformDeviceBuilder()
                            .setManufacturer("AwesomeTech")
                            .setModel("AwesomeTwo")
                            .setType(PlatformDevice.DEVICE_TYPE_WATCH)
                            .build()
                    )
                }
                .build()

        with(metadata.toSdkMetadata()) {
            assertThat(id).isEqualTo("someId")
            assertThat(dataOrigin).isEqualTo(DataOrigin("origin package name"))
            assertThat(lastModifiedTime).isEqualTo(Instant.ofEpochMilli(6666L))
            assertThat(clientRecordId).isEqualTo("clientId")
            assertThat(clientRecordVersion).isEqualTo(2L)
            assertThat(recordingMethod).isEqualTo(Metadata.RECORDING_METHOD_MANUAL_ENTRY)
            assertThat(device)
                .isEqualTo(
                    Device(
                        manufacturer = "AwesomeTech",
                        model = "AwesomeTwo",
                        type = Device.TYPE_WATCH
                    )
                )
        }
    }
}
