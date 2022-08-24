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

import android.os.ParcelUuid
import android.util.SparseArray
import android.bluetooth.le.AdvertiseData as FwkAdvertiseData
import android.os.Bundle
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils

/**
 * TODO: Implement AdvertiseDataImplXxx
 * TODO: Implement addXxx functions
 * TODO: Add docs
 * TODO: Add functions in SDK 31
 * TODO: Add functions in SDK 33
 *
 * Advertise data packet container for Bluetooth LE advertising. This represents the data to be
 * advertised as well as the scan response data for active scans.
 *
 * Add @see BluetoothLeAdvertiser, @see ScanRecord when they are ready
 * @hide
 */
class AdvertiseData internal constructor(private val fwkInstance: FwkAdvertiseData) : Bundleable {
    companion object {
        internal const val FIELD_FWK_ADVERTISE_DATA = 0

        val CREATOR: Bundleable.Creator<AdvertiseData> =
            object : Bundleable.Creator<AdvertiseData> {
                override fun fromBundle(bundle: Bundle): AdvertiseData {
                    val fwkAdvertiseData =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_ADVERTISE_DATA),
                            android.bluetooth.le.AdvertiseData::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework advertise data"
                        )
                    return AdvertiseData(fwkAdvertiseData)
                }
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        private fun <T> SparseArray<T>.entriesIterator(): Iterator<Pair<Int, T>> {
            val size = this.size()
            return object : Iterator<Pair<Int, T>> {
                var currIndex = 0
                override fun hasNext(): Boolean = currIndex < size
                override fun next(): Pair<Int, T> = Pair(keyAt(currIndex), valueAt(currIndex++))
            }
        }

        internal fun buildFwkAdvertiseData(
            serviceUuids: MutableList<ParcelUuid>? = null,
            manufacturerSpecificData: SparseArray<ByteArray>? = null,
            serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
            includeTxPowerLevel: Boolean = false,
            includeDeviceName: Boolean = false
        ): FwkAdvertiseData {
            val builder = FwkAdvertiseData.Builder()
                .setIncludeTxPowerLevel(includeTxPowerLevel)
                .setIncludeDeviceName(includeDeviceName)

            serviceUuids?.forEach { builder.addServiceUuid(it) }

            manufacturerSpecificData?.entriesIterator()?.forEach {
                builder.addManufacturerData(it.first, it.second)
            }

            serviceData?.forEach { builder.addServiceData(it.key, it.value) }

            return builder.build()
        }
    }

    val serviceUuids: MutableList<ParcelUuid>?
        get() = fwkInstance.serviceUuids
    val manufacturerSpecificData: SparseArray<ByteArray>?
        get() = fwkInstance.manufacturerSpecificData
    val serviceData: MutableMap<ParcelUuid, ByteArray>?
        get() = fwkInstance.serviceData
    val includeTxPowerLevel: Boolean
        get() = fwkInstance.includeTxPowerLevel
    val includeDeviceName: Boolean
        get() = fwkInstance.includeDeviceName

    constructor(
        serviceUuids: MutableList<ParcelUuid>? = null,
        manufacturerSpecificData: SparseArray<ByteArray>? = null,
        serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
        includeTxPowerLevel: Boolean = false,
        includeDeviceName: Boolean = false
    ) : this(buildFwkAdvertiseData(
        serviceUuids,
        manufacturerSpecificData,
        serviceData,
        includeTxPowerLevel,
        includeDeviceName
    ))

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_ADVERTISE_DATA), fwkInstance)
        return bundle
    }
}