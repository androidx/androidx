/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth

import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test cases for [AdvertiseParams] */
@RunWith(JUnit4::class)
class AdvertiseParamsTest {

    @Test
    fun defaultParams() {
        val advertiseParams = AdvertiseParams()

        assertThat(advertiseParams.shouldIncludeDeviceAddress).isFalse()
        assertThat(advertiseParams.shouldIncludeDeviceName).isFalse()
        assertThat(advertiseParams.isConnectable).isFalse()
        assertThat(advertiseParams.isDiscoverable).isFalse()
        assertThat(advertiseParams.durationMillis).isEqualTo(0)
        assertThat(advertiseParams.manufacturerData.size).isEqualTo(0)
        assertThat(advertiseParams.serviceData.size).isEqualTo(0)
        assertThat(advertiseParams.serviceUuids.size).isEqualTo(0)
        assertThat(advertiseParams.serviceSolicitationUuids.size).isEqualTo(0)
    }

    @Test
    fun basicParams() {
        val shouldIncludeDeviceAddress = true
        val shouldIncludeDeviceName = true
        val isConnectable = true
        val isDiscoverable = true

        val advertiseParams =
            AdvertiseParams(
                shouldIncludeDeviceAddress = shouldIncludeDeviceAddress,
                shouldIncludeDeviceName = shouldIncludeDeviceName,
                isConnectable = isConnectable,
                isDiscoverable = isDiscoverable
            )

        assertThat(advertiseParams.shouldIncludeDeviceAddress).isEqualTo(shouldIncludeDeviceAddress)
        assertThat(advertiseParams.shouldIncludeDeviceName).isEqualTo(shouldIncludeDeviceName)
        assertThat(advertiseParams.isConnectable).isEqualTo(isConnectable)
        assertThat(advertiseParams.isDiscoverable).isEqualTo(isDiscoverable)
    }

    @Test
    fun manufacturerDataParams() {
        val manuId1 = 1
        val manuId2 = 2
        val manuId3 = 3

        val manuData1 = "AA".toByteArray()
        val manuData2 = "BB".toByteArray()
        val manuData3 = "AB".toByteArray()

        val manufacturerData = mutableMapOf<Int, ByteArray>()
        manufacturerData[manuId1] = manuData1
        manufacturerData[manuId2] = manuData2
        manufacturerData[manuId3] = manuData3

        val advertiseParams = AdvertiseParams(manufacturerData = manufacturerData)

        assertThat(advertiseParams.manufacturerData[manuId1]).isEqualTo(manuData1)
        assertThat(advertiseParams.manufacturerData[manuId2]).isEqualTo(manuData2)
        assertThat(advertiseParams.manufacturerData[manuId3]).isEqualTo(manuData3)
    }

    @Test
    fun serviceDataParams() {
        val serviceUuid1 = UUID.randomUUID()
        val serviceUuid2 = UUID.randomUUID()
        val serviceUuid3 = UUID.randomUUID()

        val serviceData1 = "AA".toByteArray()
        val serviceData2 = "BB".toByteArray()
        val serviceData3 = "AB".toByteArray()

        val serviceData = mutableMapOf<UUID, ByteArray>()
        serviceData[serviceUuid1] = serviceData1
        serviceData[serviceUuid2] = serviceData2
        serviceData[serviceUuid3] = serviceData3

        val advertiseParams = AdvertiseParams(serviceData = serviceData)

        assertThat(advertiseParams.serviceData[serviceUuid1]).isEqualTo(serviceData1)
        assertThat(advertiseParams.serviceData[serviceUuid2]).isEqualTo(serviceData2)
        assertThat(advertiseParams.serviceData[serviceUuid3]).isEqualTo(serviceData3)
    }

    @Test
    fun serviceUuidsParams() {
        val serviceUuid1 = UUID.randomUUID()
        val serviceUuid2 = UUID.randomUUID()
        val serviceUuid3 = UUID.randomUUID()

        val serviceUuids = listOf(serviceUuid1, serviceUuid2, serviceUuid3)

        val advertiseParams = AdvertiseParams(serviceUuids = serviceUuids)

        assertThat(advertiseParams.serviceUuids[0]).isEqualTo(serviceUuid1)
        assertThat(advertiseParams.serviceUuids[1]).isEqualTo(serviceUuid2)
        assertThat(advertiseParams.serviceUuids[2]).isEqualTo(serviceUuid3)
    }

    @Test
    fun serviceSolicitationUuidsParams() {
        val serviceSolicitationUuid1 = UUID.randomUUID()
        val serviceSolicitationUuid2 = UUID.randomUUID()
        val serviceSolicitationUuid3 = UUID.randomUUID()

        val serviceSolicitationUuids =
            listOf(serviceSolicitationUuid1, serviceSolicitationUuid2, serviceSolicitationUuid3)

        val advertiseParams = AdvertiseParams(serviceSolicitationUuids = serviceSolicitationUuids)

        assertThat(advertiseParams.serviceSolicitationUuids[0]).isEqualTo(serviceSolicitationUuid1)
        assertThat(advertiseParams.serviceSolicitationUuids[1]).isEqualTo(serviceSolicitationUuid2)
        assertThat(advertiseParams.serviceSolicitationUuids[2]).isEqualTo(serviceSolicitationUuid3)
    }
}
