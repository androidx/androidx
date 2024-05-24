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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test cases for [ScanFilter] */
@RunWith(JUnit4::class)
class ScanFilterTest {

    @Test
    fun constructorWithDefaultParams() {
        val scanFilter = ScanFilter()

        assertThat(scanFilter.deviceAddress).isNull()
        assertThat(scanFilter.manufacturerId).isEqualTo(ScanFilter.MANUFACTURER_FILTER_NONE)
        assertThat(scanFilter.manufacturerData).isNull()
        assertThat(scanFilter.manufacturerDataMask).isNull()
        assertThat(scanFilter.serviceDataUuid).isNull()
        assertThat(scanFilter.serviceData).isNull()
        assertThat(scanFilter.serviceDataMask).isNull()
        assertThat(scanFilter.serviceUuid).isNull()
        assertThat(scanFilter.serviceUuidMask).isNull()
        assertThat(scanFilter.serviceSolicitationUuid).isNull()
        assertThat(scanFilter.serviceSolicitationUuidMask).isNull()
    }

    @Test
    fun constructor() {
        val deviceAddress =
            BluetoothAddress("00:01:02:03:04:05", BluetoothAddress.ADDRESS_TYPE_PUBLIC)
        val manufacturerId = 1
        val manufacturerData = "AA".toByteArray()
        val manufacturerDataMask = "AB".toByteArray()
        val serviceDataUuid = UUID.randomUUID()
        val serviceData = "BA".toByteArray()
        val serviceDataMask = "BB".toByteArray()
        val serviceUuid = UUID.randomUUID()
        val serviceUuidMask = UUID.randomUUID()
        val serviceSolicitationUuid = UUID.randomUUID()
        val serviceSolicitationUuidMask = UUID.randomUUID()

        val scanFilter =
            ScanFilter(
                deviceAddress = deviceAddress,
                manufacturerId = manufacturerId,
                manufacturerData = manufacturerData,
                manufacturerDataMask = manufacturerDataMask,
                serviceDataUuid = serviceDataUuid,
                serviceData = serviceData,
                serviceDataMask = serviceDataMask,
                serviceUuid = serviceUuid,
                serviceUuidMask = serviceUuidMask,
                serviceSolicitationUuid = serviceSolicitationUuid,
                serviceSolicitationUuidMask = serviceSolicitationUuidMask
            )

        assertThat(scanFilter.deviceAddress).isEqualTo(deviceAddress)
        assertThat(scanFilter.manufacturerId).isEqualTo(manufacturerId)
        assertThat(scanFilter.manufacturerDataMask).isEqualTo(manufacturerDataMask)
        assertThat(scanFilter.serviceDataUuid).isEqualTo(serviceDataUuid)
        assertThat(scanFilter.serviceData).isEqualTo(serviceData)
        assertThat(scanFilter.serviceDataMask).isEqualTo(serviceDataMask)
        assertThat(scanFilter.serviceUuid).isEqualTo(serviceUuid)
        assertThat(scanFilter.serviceUuidMask).isEqualTo(serviceUuidMask)
        assertThat(scanFilter.serviceSolicitationUuid).isEqualTo(serviceSolicitationUuid)
        assertThat(scanFilter.serviceSolicitationUuidMask).isEqualTo(serviceSolicitationUuidMask)

        assertThat(scanFilter.fwkScanFilter).isNotNull()
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
            ScanFilter(
                manufacturerData = manufacturerData,
                manufacturerDataMask = manufacturerDataMask
            )
        }
    }

    @Test
    fun constructorWithNullServiceData_andNonNullMask() {
        val serviceDataMask = "nonNullMask".toByteArray()

        assertFailsWith<IllegalArgumentException> { ScanFilter(serviceDataMask = serviceDataMask) }
    }

    @Test
    fun constructorWithInvalidServiceDataMaskSize() {
        val serviceData = "array".toByteArray()
        val serviceDataMask = "arrayOfDifferentSize".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(serviceData = serviceData, serviceDataMask = serviceDataMask)
        }
    }

    @Test
    fun constructorWithNullServiceUuid_andNonNullMask() {
        val serviceUuidMask = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> { ScanFilter(serviceUuidMask = serviceUuidMask) }
    }

    @Test
    fun constructorWithNullServiceSolicitationUuid_andNonNullMask() {
        val serviceSolicitationUuidMask = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> {
            ScanFilter(serviceSolicitationUuidMask = serviceSolicitationUuidMask)
        }
    }
}
