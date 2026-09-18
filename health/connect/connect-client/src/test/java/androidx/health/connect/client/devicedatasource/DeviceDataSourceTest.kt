/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client.devicedatasource

import android.os.Build
import androidx.health.connect.client.ExperimentalDeviceDataSourceApi
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalDeviceDataSourceApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DeviceDataSourceTest {

    @Test
    fun deviceDataTypeSource_equalsAndHashCode() {
        val source1 =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = true,
                isUserEnabled = true,
            )
        val source2 =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = true,
                isUserEnabled = true,
            )
        val diffDataType =
            DeviceDataTypeSource(
                dataType = HeartRateRecord::class,
                isAvailable = true,
                isUserEnabled = true,
            )
        val diffAvailable =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = false,
                isUserEnabled = true,
            )
        val diffEnabled =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = true,
                isUserEnabled = false,
            )

        assertThat(source1).isEqualTo(source2)
        assertThat(source1.hashCode()).isEqualTo(source2.hashCode())

        assertThat(source1).isNotEqualTo(diffDataType)
        assertThat(source1).isNotEqualTo(diffAvailable)
        assertThat(source1).isNotEqualTo(diffEnabled)
        assertThat(source1).isNotEqualTo("other")
    }

    @Test
    fun deviceDataTypeSource_toString() {
        val source =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = true,
                isUserEnabled = false,
            )
        assertThat(source.toString()).contains("StepsRecord")
        assertThat(source.toString()).contains("isAvailable=true")
        assertThat(source.toString()).contains("isUserEnabled=false")
    }

    @Test
    fun deviceDataSource_equalsAndHashCode() {
        val typeSource =
            DeviceDataTypeSource(
                dataType = StepsRecord::class,
                isAvailable = true,
                isUserEnabled = true,
            )
        val device1 =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = setOf(typeSource),
            )
        val device2 =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = setOf(typeSource),
            )
        val diffOrigin =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.other"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = setOf(typeSource),
            )
        val diffDevice =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(type = Device.TYPE_WATCH, manufacturer = "Fitbit", model = "Charge"),
                deviceDataTypeSources = setOf(typeSource),
            )
        val diffTypeSources =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = emptySet(),
            )

        assertThat(device1).isEqualTo(device2)
        assertThat(device1.hashCode()).isEqualTo(device2.hashCode())

        assertThat(device1).isNotEqualTo(diffOrigin)
        assertThat(device1).isNotEqualTo(diffDevice)
        assertThat(device1).isNotEqualTo(diffTypeSources)
        assertThat(device1).isNotEqualTo("other")
    }

    @Test
    fun deviceDataSource_toString() {
        val device =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = emptySet(),
            )
        assertThat(device.toString()).contains("com.example.device")
        assertThat(device.toString()).contains("Pixel Watch")
    }

    @Test
    fun deviceDataSourceCapabilities_equalsAndHashCode() {
        val cap1 = DeviceDataSourceCapabilities(recordTypes = setOf(StepsRecord::class))
        val cap2 = DeviceDataSourceCapabilities(recordTypes = setOf(StepsRecord::class))
        val diffCap = DeviceDataSourceCapabilities(recordTypes = setOf(HeartRateRecord::class))

        assertThat(cap1).isEqualTo(cap2)
        assertThat(cap1.hashCode()).isEqualTo(cap2.hashCode())

        assertThat(cap1).isNotEqualTo(diffCap)
        assertThat(cap1).isNotEqualTo("other")
    }

    @Test
    fun deviceDataSourceCapabilities_toString() {
        val cap = DeviceDataSourceCapabilities(recordTypes = setOf(StepsRecord::class))
        assertThat(cap.toString()).contains("StepsRecord")
    }

    @Test
    fun getDeviceDataSourcesResponse_equalsAndHashCode() {
        val device =
            DeviceDataSource(
                deviceDataOrigin = DataOrigin("com.example.device"),
                device =
                    Device(
                        type = Device.TYPE_WATCH,
                        manufacturer = "Google",
                        model = "Pixel Watch",
                    ),
                deviceDataTypeSources = emptySet(),
            )
        val resp1 = GetDeviceDataSourcesResponse(deviceDataSources = listOf(device))
        val resp2 = GetDeviceDataSourcesResponse(deviceDataSources = listOf(device))
        val diffResp = GetDeviceDataSourcesResponse(deviceDataSources = emptyList())

        assertThat(resp1).isEqualTo(resp2)
        assertThat(resp1.hashCode()).isEqualTo(resp2.hashCode())

        assertThat(resp1).isNotEqualTo(diffResp)
        assertThat(resp1).isNotEqualTo("other")
    }

    @Test
    fun getDeviceDataSourcesResponse_toString() {
        val resp = GetDeviceDataSourcesResponse(deviceDataSources = emptyList())
        assertThat(resp.toString()).contains("GetDeviceDataSourcesResponse")
    }
}
