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

@file:SuppressLint("NewApi")

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.health.connect.client.ExperimentalDeviceDataSourceApi
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config

@OptIn(ExperimentalDeviceDataSourceApi::class)
@RunWith(AndroidJUnit4::class)
@Config(
    minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
)
class DeviceDataSourceConvertersTest {

    @Test
    fun toSdkDeviceDataTypeSource_mappedRecord_convertsSuccessfully() {
        val platformTypeSource = mock(PlatformDeviceDataTypeSource::class.java)
        `when`(platformTypeSource.dataType)
            .thenReturn(android.health.connect.datatypes.StepsRecord::class.java)
        `when`(platformTypeSource.isAvailable).thenReturn(true)
        `when`(platformTypeSource.isUserEnabled).thenReturn(false)

        val sdkTypeSource = platformTypeSource.toSdkDeviceDataTypeSource()

        assertThat(sdkTypeSource).isNotNull()
        assertThat(sdkTypeSource!!.dataType).isEqualTo(StepsRecord::class)
        assertThat(sdkTypeSource.isAvailable).isTrue()
        assertThat(sdkTypeSource.isUserEnabled).isFalse()
    }

    @Test
    fun toSdkDeviceDataTypeSource_unmappedRecord_returnsNull() {
        val platformTypeSource = mock(PlatformDeviceDataTypeSource::class.java)
        `when`(platformTypeSource.dataType)
            .thenReturn(android.health.connect.datatypes.Record::class.java)
        `when`(platformTypeSource.isAvailable).thenReturn(true)
        `when`(platformTypeSource.isUserEnabled).thenReturn(true)

        val sdkTypeSource = platformTypeSource.toSdkDeviceDataTypeSource()

        assertThat(sdkTypeSource).isNull()
    }

    @Test
    fun toSdkDeviceDataTypeSource_symptomType_returnsNull() {
        val platformTypeSource = mock(PlatformDeviceDataTypeSource::class.java)
        `when`(platformTypeSource.symptomType).thenReturn(13)
        `when`(platformTypeSource.isAvailable).thenReturn(true)
        `when`(platformTypeSource.isUserEnabled).thenReturn(false)

        val sdkTypeSource = platformTypeSource.toSdkDeviceDataTypeSource()

        assertThat(sdkTypeSource).isNull()
    }

    @Test
    fun toSdkDeviceDataSource_convertsOriginDeviceAndDataTypes() {
        val platformDataOrigin =
            android.health.connect.datatypes.DataOrigin.Builder()
                .setPackageName("com.example.device")
                .build()
        val platformDevice =
            android.health.connect.datatypes.Device.Builder()
                .setType(android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH)
                .setManufacturer("Google")
                .setModel("Pixel Watch")
                .build()
        val platformRecordTypeSource = mock(PlatformDeviceDataTypeSource::class.java)
        `when`(platformRecordTypeSource.dataType)
            .thenReturn(android.health.connect.datatypes.StepsRecord::class.java)
        `when`(platformRecordTypeSource.isAvailable).thenReturn(true)
        `when`(platformRecordTypeSource.isUserEnabled).thenReturn(true)

        val platformSymptomTypeSource = mock(PlatformDeviceDataTypeSource::class.java)
        `when`(platformSymptomTypeSource.symptomType).thenReturn(13)
        `when`(platformSymptomTypeSource.isAvailable).thenReturn(false)
        `when`(platformSymptomTypeSource.isUserEnabled).thenReturn(true)

        val platformDataSource = mock(PlatformDeviceDataSource::class.java)
        `when`(platformDataSource.deviceDataOrigin).thenReturn(platformDataOrigin)
        `when`(platformDataSource.device).thenReturn(platformDevice)
        `when`(platformDataSource.deviceDataTypeSources)
            .thenReturn(setOf(platformRecordTypeSource, platformSymptomTypeSource))

        val sdkDataSource = platformDataSource.toSdkDeviceDataSource()

        assertThat(sdkDataSource.deviceDataOrigin.packageName).isEqualTo("com.example.device")
        assertThat(sdkDataSource.device.type).isEqualTo(Device.TYPE_WATCH)
        assertThat(sdkDataSource.device.manufacturer).isEqualTo("Google")
        assertThat(sdkDataSource.device.model).isEqualTo("Pixel Watch")
        assertThat(sdkDataSource.deviceDataTypeSources).hasSize(1)

        val recordSource = sdkDataSource.deviceDataTypeSources.first()
        assertThat(recordSource.dataType).isEqualTo(StepsRecord::class)
        assertThat(recordSource.isAvailable).isTrue()
        assertThat(recordSource.isUserEnabled).isTrue()
    }

    @Test
    fun toSdkDeviceDataSourceCapabilities_convertsRecordTypes() {
        val mockPlatformCapabilities = mock(PlatformDeviceDataSourceCapabilities::class.java)
        `when`(mockPlatformCapabilities.recordTypes)
            .thenReturn(
                setOf(
                    android.health.connect.datatypes.StepsRecord::class.java,
                    android.health.connect.datatypes.HeartRateRecord::class.java,
                    android.health.connect.datatypes.Record::class.java,
                )
            )

        val sdkCapabilities = mockPlatformCapabilities.toSdkDeviceDataSourceCapabilities()

        assertThat(sdkCapabilities.recordTypes)
            .containsExactly(StepsRecord::class, HeartRateRecord::class)
    }

    @Test
    fun toSdkGetDeviceDataSourcesResponse_convertsDataSourcesList() {
        val platformDataOrigin =
            android.health.connect.datatypes.DataOrigin.Builder()
                .setPackageName("com.example.device")
                .build()
        val platformDevice =
            android.health.connect.datatypes.Device.Builder()
                .setType(android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH)
                .setManufacturer("Google")
                .setModel("Pixel Watch")
                .build()
        val platformDataSource = mock(PlatformDeviceDataSource::class.java)
        `when`(platformDataSource.deviceDataOrigin).thenReturn(platformDataOrigin)
        `when`(platformDataSource.device).thenReturn(platformDevice)
        `when`(platformDataSource.deviceDataTypeSources).thenReturn(emptySet())

        val platformResponse = mock(PlatformGetDeviceDataSourcesResponse::class.java)
        `when`(platformResponse.deviceDataSources).thenReturn(listOf(platformDataSource))

        val sdkResponse = platformResponse.toSdkGetDeviceDataSourcesResponse()

        assertThat(sdkResponse.deviceDataSources).hasSize(1)
        assertThat(sdkResponse.deviceDataSources[0].deviceDataOrigin.packageName)
            .isEqualTo("com.example.device")
    }
}
