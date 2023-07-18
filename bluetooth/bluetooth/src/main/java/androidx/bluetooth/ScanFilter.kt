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

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter as FwkScanFilter
import android.os.Build
import android.os.ParcelUuid
import java.util.UUID

/**
 * Criteria for filtering result from Bluetooth LE scans. A ScanFilter allows clients to restrict
 * scan results to only those that are of interest to them.
 */
class ScanFilter(
    /** The scan filter for the remote device address. `null` if filter is not set. */
    val deviceAddress: BluetoothAddress? = null,

    /** The scan filter for the remote device name. `null` if filter is not set. */
    val deviceName: String? = null,

    /** The scan filter for manufacturer id. [MANUFACTURER_FILTER_NONE] if filter is not set. */
    val manufacturerId: Int = MANUFACTURER_FILTER_NONE,

    /** The scan filter for manufacturer data. `null` if filter is not set. */
    val manufacturerData: ByteArray? = null,

    /** The partial filter on manufacturerData. `null` if filter is not set. */
    val manufacturerDataMask: ByteArray? = null,

    /** The scan filter for service data uuid. `null` if filter is not set. */
    val serviceDataUuid: UUID? = null,

    /** The scan filter for service data. `null` if filter is not set. */
    val serviceData: ByteArray? = null,

    /** The partial filter on service data. `null` if filter is not set. */
    val serviceDataMask: ByteArray? = null,

    /** The scan filter for service uuid. `null` if filter is not set. */
    val serviceUuid: UUID? = null,

    /** The partial filter on service uuid. `null` if filter is not set. */
    val serviceUuidMask: UUID? = null
) {
    companion object {
        const val MANUFACTURER_FILTER_NONE: Int = -1
    }

    init {
        if (manufacturerId < 0 && manufacturerId != MANUFACTURER_FILTER_NONE) {
            throw IllegalArgumentException("Invalid manufacturerId")
        }

        if (manufacturerDataMask != null) {
            if (manufacturerData == null) {
                throw IllegalArgumentException(
                    "ManufacturerData is null while manufacturerDataMask is not null")
            }

            if (manufacturerData.size != manufacturerDataMask.size) {
                throw IllegalArgumentException(
                    "Size mismatch for manufacturerData and manufacturerDataMask")
            }
        }

        if (serviceDataMask != null) {
            if (serviceData == null) {
                throw IllegalArgumentException(
                    "ServiceData is null while serviceDataMask is not null")
            }

            if (serviceData.size != serviceDataMask.size) {
                throw IllegalArgumentException(
                    "Size mismatch for service data and service data mask")
            }
        }

        if (serviceUuid == null && serviceUuidMask != null) {
            throw IllegalArgumentException("ServiceUuid is null while ServiceUuidMask is not null")
        }
    }

    @delegate:SuppressLint("ObsoleteSdkInt")
    internal val fwkScanFilter: FwkScanFilter by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FwkScanFilter.Builder().run {
            deviceAddress?.let { setDeviceAddress(it.address) }

            deviceName?.let { setDeviceName(it) }

            if (manufacturerId != MANUFACTURER_FILTER_NONE && manufacturerData != null) {
                if (Build.VERSION.SDK_INT >= 33) {
                    setManufacturerData(
                        manufacturerId,
                        manufacturerData,
                        manufacturerDataMask
                    )
                } else {
                    setManufacturerData(manufacturerId, manufacturerData)
                }
            }

            if (serviceDataUuid != null) {
                if (Build.VERSION.SDK_INT >= 33) {
                    setServiceData(ParcelUuid(
                        serviceDataUuid),
                        serviceData,
                        serviceDataMask
                    )
                } else {
                    setServiceData(ParcelUuid(serviceDataUuid), serviceData)
                }
            }

            serviceUuid?.let {
                if (Build.VERSION.SDK_INT >= 33) {
                    setServiceUuid(ParcelUuid(it), ParcelUuid(serviceUuidMask))
                } else {
                    setServiceUuid(ParcelUuid(it))
                }
            }
            build()
        }
    }
}
