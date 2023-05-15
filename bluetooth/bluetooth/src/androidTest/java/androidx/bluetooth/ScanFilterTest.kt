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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScanFilterTest {

    @Test
    fun constructorWithDefaultParams() {
        val scanFilter = ScanFilter()

        assertNull(scanFilter.deviceAddress)
        assertEquals(ScanFilter.MANUFACTURER_FILTER_NONE, scanFilter.manufacturerId)
        assertNull(scanFilter.manufacturerData)
        assertNull(scanFilter.manufacturerDataMask)
        assertNull(scanFilter.serviceDataUuid)
        assertNull(scanFilter.serviceData)
        assertNull(scanFilter.serviceDataMask)
        assertNull(scanFilter.serviceUuid)
        assertNull(scanFilter.serviceUuidMask)
    }

    @Test
    fun constructor() {
        val deviceAddress = BluetoothAddress("00:01:02:03:04:05", AddressType.ADDRESS_TYPE_PUBLIC)
        val manufacturerId = 1
        val manufacturerData = "AA".toByteArray()
        val manufacturerDataMask = "AB".toByteArray()
        val serviceDataUuid = UUID.randomUUID()
        val serviceData = "BA".toByteArray()
        val serviceDataMask = "BB".toByteArray()
        val serviceUuid = UUID.randomUUID()
        val serviceUuidMask = UUID.randomUUID()

        val scanFilter = ScanFilter(
            deviceAddress = deviceAddress,
            manufacturerId = manufacturerId,
            manufacturerData = manufacturerData,
            manufacturerDataMask = manufacturerDataMask,
            serviceDataUuid = serviceDataUuid,
            serviceData = serviceData,
            serviceDataMask = serviceDataMask,
            serviceUuid = serviceUuid,
            serviceUuidMask = serviceUuidMask
        )

        assertEquals(deviceAddress, scanFilter.deviceAddress)
        assertEquals(manufacturerId, scanFilter.manufacturerId)
        assertEquals(manufacturerDataMask, scanFilter.manufacturerDataMask)
        assertEquals(serviceDataUuid, scanFilter.serviceDataUuid)
        assertEquals(serviceData, scanFilter.serviceData)
        assertEquals(serviceDataMask, scanFilter.serviceDataMask)
        assertEquals(serviceUuid, scanFilter.serviceUuid)
        assertEquals(serviceUuidMask, scanFilter.serviceUuidMask)
    }

    @Test
    fun constructorWithInvalidManufacturerId() {
        val invalidManufacturerId = -2

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(manufacturerId = invalidManufacturerId)
        }
    }

    @Test
    fun constructorWithNullManufacturerData_andNonNullMask() {
        val manufacturerDataMask = "nonNullMask".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(manufacturerDataMask = manufacturerDataMask)
        }
    }

    @Test
    fun constructorWithInvalidManufacturerDataMaskSize() {
        val manufacturerData = "array".toByteArray()
        val manufacturerDataMask = "arrayOfDifferentSize".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(manufacturerData = manufacturerData,
                manufacturerDataMask = manufacturerDataMask)
        }
    }

    @Test
    fun constructorWithNullServiceData_andNonNullMask() {
        val serviceDataMask = "nonNullMask".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(serviceDataMask = serviceDataMask)
        }
    }

    @Test
    fun constructorWithInvalidServiceDataMaskSize() {
        val serviceData = "array".toByteArray()
        val serviceDataMask = "arrayOfDifferentSize".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(serviceData = serviceData,
                serviceDataMask = serviceDataMask)
        }
    }

    @Test
    fun constructorWithNullServiceUuid_andNonNullMask() {
        val serviceUuidMask = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(serviceUuidMask = serviceUuidMask)
        }
    }
}