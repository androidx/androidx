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

import android.bluetooth.le.ScanFilter as FwkScanFilter
import android.os.Bundle
import android.os.ParcelUuid
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils

/**
 * TODO: Copy docs
 * TODO: Implement ScanFilterImplXxx
 * TODO: Implement APIs added in API 29
 *       getters: getServiceSolicitationUuid, getServiceSolicitationUuidMask
 *       setters: setServiceSolicitationUuid
 * TODO: Implement APIs added in API 33
 *       getters: getAdvertisingData, getAdvertisingDataMask, getAdvertisingDataType
 *       setters: setAdvertisingDataTypeWithData, setAdvertisingDataType,
 * @hide
 */
class ScanFilter internal constructor(internal val fwkInstance: FwkScanFilter) : Bundleable {

    companion object {
        internal const val FIELD_FWK_SCAN_FILTER = 0

        val CREATOR: Bundleable.Creator<ScanFilter> =
            object : Bundleable.Creator<ScanFilter> {
                override fun fromBundle(bundle: Bundle): ScanFilter {
                    val fwkScanFilter =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_SCAN_FILTER),
                            android.bluetooth.le.ScanFilter::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework scan filter"
                        )
                    return ScanFilter(fwkScanFilter)
                }
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        internal fun buildFwkScanFilter(
            deviceName: String? = null,
            deviceAddress: String? = null,
            serviceUuid: ParcelUuid? = null,
            serviceDataUuid: ParcelUuid? = null,
            serviceData: ByteArray? = null,
            serviceDataMask: ByteArray? = null,
            manufacturerId: Int = -1,
            manufacturerData: ByteArray? = null,
            manufacturerDataMask: ByteArray? = null
        ): FwkScanFilter {
            var builder = FwkScanFilter.Builder()
                .setDeviceName(deviceName)
                .setDeviceAddress(deviceAddress)
                .setServiceUuid(serviceUuid)

            if (serviceDataUuid != null) {
                if (serviceDataMask == null) {
                    builder.setServiceData(serviceDataUuid, serviceData)
                } else {
                    builder.setServiceData(serviceDataUuid, serviceData, serviceDataMask)
                }
            }
            if (manufacturerId >= 0) {
                if (manufacturerDataMask == null) {
                    builder.setManufacturerData(manufacturerId, manufacturerData)
                } else {
                    builder.setManufacturerData(
                        manufacturerId, manufacturerData, manufacturerDataMask)
                }
            }

            return builder.build()
        }
    }

    val deviceName: String?
        get() = fwkInstance.deviceName
    val deviceAddress: String?
        get() = fwkInstance.deviceAddress
    val serviceUuid: ParcelUuid?
        get() = fwkInstance.serviceUuid
    val serviceUuidMask: ParcelUuid?
        get() = fwkInstance.serviceUuidMask
    val serviceDataUuid: ParcelUuid?
        get() = fwkInstance.serviceDataUuid
    val serviceData: ByteArray?
        get() = fwkInstance.serviceData
    val serviceDataMask: ByteArray?
        get() = fwkInstance.serviceDataMask
    val manufacturerId: Int
        get() = fwkInstance.manufacturerId
    val manufacturerData: ByteArray?
        get() = fwkInstance.manufacturerData
    val manufacturerDataMask: ByteArray?
        get() = fwkInstance.manufacturerDataMask

    constructor(
        deviceName: String? = null,
        deviceAddress: String? = null,
        serviceUuid: ParcelUuid? = null,
        serviceDataUuid: ParcelUuid? = null,
        serviceData: ByteArray? = null,
        serviceDataMask: ByteArray? = null,
        manufacturerId: Int = -1,
        manufacturerData: ByteArray? = null,
        manufacturerDataMask: ByteArray? = null
    ) : this(buildFwkScanFilter(
        deviceName,
        deviceAddress,
        serviceUuid,
        serviceDataUuid,
        serviceData,
        serviceDataMask,
        manufacturerId,
        manufacturerData,
        manufacturerDataMask
    ))
    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_SCAN_FILTER), fwkInstance)
        return bundle
    }
}
