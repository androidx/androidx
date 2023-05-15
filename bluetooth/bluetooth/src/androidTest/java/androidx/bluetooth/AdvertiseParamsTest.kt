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

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdvertiseParamsTest {

    @Test
    fun defaultParams() {
        val advertiseParams = AdvertiseParams()

        assertEquals(false, advertiseParams.shouldIncludeDeviceAddress)
        assertEquals(false, advertiseParams.shouldIncludeDeviceName)
        assertEquals(false, advertiseParams.isConnectable)
        assertEquals(false, advertiseParams.isDiscoverable)
        assertEquals(0, advertiseParams.timeoutMillis)
        assertEquals(0, advertiseParams.manufacturerData.size)
        assertEquals(0, advertiseParams.serviceData.size)
        assertEquals(0, advertiseParams.serviceUuids.size)
    }

    @Test
    fun basicParams() {
        val shouldIncludeDeviceAddress = true
        val shouldIncludeDeviceName = true
        val isConnectable = true
        val isDiscoverable = true

        val advertiseParams = AdvertiseParams(
            shouldIncludeDeviceAddress = shouldIncludeDeviceAddress,
            shouldIncludeDeviceName = shouldIncludeDeviceName,
            isConnectable = isConnectable,
            isDiscoverable = isDiscoverable
        )

        assertEquals(shouldIncludeDeviceAddress, advertiseParams.shouldIncludeDeviceAddress)
        assertEquals(shouldIncludeDeviceName, advertiseParams.shouldIncludeDeviceName)
        assertEquals(isConnectable, advertiseParams.isConnectable)
        assertEquals(isDiscoverable, advertiseParams.isDiscoverable)
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

        val advertiseParams = AdvertiseParams(
            manufacturerData = manufacturerData
        )

        assertEquals(manuData1, advertiseParams.manufacturerData[manuId1])
        assertEquals(manuData2, advertiseParams.manufacturerData[manuId2])
        assertEquals(manuData3, advertiseParams.manufacturerData[manuId3])
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

        val advertiseParams = AdvertiseParams(
            serviceData = serviceData
        )

        assertEquals(serviceData1, advertiseParams.serviceData[serviceUuid1])
        assertEquals(serviceData2, advertiseParams.serviceData[serviceUuid2])
        assertEquals(serviceData3, advertiseParams.serviceData[serviceUuid3])
    }

    @Test
    fun serviceUuidsParams() {
        val serviceUuid1 = UUID.randomUUID()
        val serviceUuid2 = UUID.randomUUID()
        val serviceUuid3 = UUID.randomUUID()

        val serviceUuids = listOf(serviceUuid1, serviceUuid2, serviceUuid3)

        val advertiseParams = AdvertiseParams(
            serviceUuids = serviceUuids
        )

        assertEquals(serviceUuid1, advertiseParams.serviceUuids[0])
        assertEquals(serviceUuid2, advertiseParams.serviceUuids[1])
        assertEquals(serviceUuid3, advertiseParams.serviceUuids[2])
    }
}
