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

package androidx.bluetooth.core

import android.bluetooth.le.AdvertiseData as FwkAdvertiseData
import android.os.Build
import android.os.ParcelUuid
import android.util.SparseArray
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdvertiseDataTest {

    companion object {
        val TEST_SERVICE_UUID = ParcelUuid.fromString("FFFFFFF0-FFFF-FFFF-FFFF-FFFFFFFFFFFF")
        val TEST_SERVICE_UUIDS = mutableListOf<ParcelUuid>(TEST_SERVICE_UUID)
        val TEST_SERVICE_SOLICITATION_UUID =
            ParcelUuid.fromString("CCCCCCC0-CCCC-CCCC-CCCC-CCCCCCCCCCCC")
        val TEST_SERVICE_SOLICITATION_UUIDS = mutableListOf<ParcelUuid>(
            TEST_SERVICE_SOLICITATION_UUID)
        val TEST_MANUFACTURER_ID = 1000
        val TEST_MANUFACTURER_SPECIFIC_DATUM = "MANUFACTURER-DATA".toByteArray()
        val TEST_MANUFACTURER_SPECIFIC_DATA = SparseArray<ByteArray>().also {
            it.put(TEST_MANUFACTURER_ID, TEST_MANUFACTURER_SPECIFIC_DATUM)
        }
        val TEST_SERVICE_DATA_UUID = ParcelUuid.fromString(
            "DDDDDDD0-DDDD-DDDD-DDDD-DDDDDDDDDDDD")
        val TEST_SERVICE_DATUM = "SERVICE-DATA".toByteArray()
        val TEST_SERVICE_DATA = mutableMapOf<ParcelUuid, ByteArray>(
            TEST_SERVICE_DATA_UUID to TEST_SERVICE_DATUM)
        val TEST_INCLUDE_TX_POWER_LEVEL = false
        val TEST_INCLUDE_DEVICE_NAME = false

        internal fun <E> SparseArray<E>.compareContent(other: SparseArray<E>?) {
            Truth.assertThat(other).isNotNull()
            Truth.assertThat(size()).isEqualTo(other?.size())
            if (other != null && size() == other.size()) {
                for (index in 0 until size()) {
                    val key = keyAt(index)
                    val value = get(key)
                    Truth.assertThat(key).isEqualTo(other.keyAt(index))
                    Truth.assertThat(value).isEqualTo(other.get(key))
                }
            }
        }
    }

    @Test
    fun constructorWithValues_createsFrameworkInstanceCorrectly() {
        val advertiseData = AdvertiseData(
            serviceUuids = TEST_SERVICE_UUIDS,
            serviceSolicitationUuids = TEST_SERVICE_SOLICITATION_UUIDS,
            manufacturerSpecificData = TEST_MANUFACTURER_SPECIFIC_DATA,
            serviceData = TEST_SERVICE_DATA,
            includeTxPowerLevel = TEST_INCLUDE_TX_POWER_LEVEL,
            includeDeviceName = TEST_INCLUDE_DEVICE_NAME
        )
        val fwkAdvertiseData = advertiseData.impl.fwkInstance
        Truth.assertThat(fwkAdvertiseData.serviceUuids).isEqualTo(TEST_SERVICE_UUIDS)
        TEST_MANUFACTURER_SPECIFIC_DATA.compareContent(fwkAdvertiseData.manufacturerSpecificData)
        Truth.assertThat(fwkAdvertiseData.serviceData).isEqualTo(TEST_SERVICE_DATA)
        Truth.assertThat(fwkAdvertiseData.includeTxPowerLevel).isEqualTo(
            TEST_INCLUDE_TX_POWER_LEVEL)
        Truth.assertThat(fwkAdvertiseData.includeDeviceName).isEqualTo(TEST_INCLUDE_DEVICE_NAME)
        if (Build.VERSION.SDK_INT >= 31) {
            Truth.assertThat(fwkAdvertiseData.serviceSolicitationUuids).isEqualTo(
                TEST_SERVICE_SOLICITATION_UUIDS)
        }
    }

    @Test
    fun constructorWithFwkInstance_createsAdvertiseDataCorrectly() {
        val fwkAdvertiseDataBuilder = FwkAdvertiseData.Builder()
            .addServiceUuid(TEST_SERVICE_UUID)
            .addManufacturerData(TEST_MANUFACTURER_ID, TEST_MANUFACTURER_SPECIFIC_DATUM)
            .addServiceData(TEST_SERVICE_DATA_UUID, TEST_SERVICE_DATUM)
            .setIncludeTxPowerLevel(TEST_INCLUDE_TX_POWER_LEVEL)
            .setIncludeDeviceName(TEST_INCLUDE_DEVICE_NAME)
        if (Build.VERSION.SDK_INT >= 31) {
            fwkAdvertiseDataBuilder.addServiceSolicitationUuid(TEST_SERVICE_SOLICITATION_UUID)
        }

        val advertiseData = AdvertiseData(fwkAdvertiseDataBuilder.build())

        Truth.assertThat(advertiseData.serviceUuids).isEqualTo(TEST_SERVICE_UUIDS)
        TEST_MANUFACTURER_SPECIFIC_DATA.compareContent(advertiseData.manufacturerSpecificData)
        Truth.assertThat(advertiseData.serviceData).isEqualTo(TEST_SERVICE_DATA)
        Truth.assertThat(advertiseData.includeTxPowerLevel).isEqualTo(TEST_INCLUDE_TX_POWER_LEVEL)
        Truth.assertThat(advertiseData.includeDeviceName).isEqualTo(TEST_INCLUDE_DEVICE_NAME)
        if (Build.VERSION.SDK_INT >= 31) {
            Truth.assertThat(advertiseData.serviceSolicitationUuids)
                .isEqualTo(TEST_SERVICE_SOLICITATION_UUIDS)
        }
    }

    @Test
    fun advertiseDataBundleable() {
        val advertiseData = AdvertiseData(
            serviceUuids = TEST_SERVICE_UUIDS,
            serviceSolicitationUuids = TEST_SERVICE_SOLICITATION_UUIDS,
            manufacturerSpecificData = TEST_MANUFACTURER_SPECIFIC_DATA,
            serviceData = TEST_SERVICE_DATA,
            includeTxPowerLevel = TEST_INCLUDE_TX_POWER_LEVEL,
            includeDeviceName = TEST_INCLUDE_DEVICE_NAME
        )
        val bundle = advertiseData.toBundle()

        val advertiseDataFromBundle = AdvertiseData.CREATOR.fromBundle(bundle)
        Truth.assertThat(advertiseDataFromBundle.serviceUuids).isEqualTo(TEST_SERVICE_UUIDS)
        TEST_MANUFACTURER_SPECIFIC_DATA.compareContent(
            advertiseDataFromBundle.manufacturerSpecificData)
        Truth.assertThat(advertiseDataFromBundle.serviceData).isEqualTo(TEST_SERVICE_DATA)
        Truth.assertThat(advertiseDataFromBundle.includeTxPowerLevel).isEqualTo(
            TEST_INCLUDE_TX_POWER_LEVEL)
        Truth.assertThat(advertiseDataFromBundle.includeDeviceName).isEqualTo(
            TEST_INCLUDE_DEVICE_NAME)
        Truth.assertThat(advertiseDataFromBundle.serviceSolicitationUuids).isEqualTo(
            TEST_SERVICE_SOLICITATION_UUIDS)
    }
}