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
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.SparseArray
import androidx.annotation.RequiresApi

/**
 * TODO: Add docs
 * TODO: Add core's TransportDiscoveryData and use it to support SDK 33
 *
 * Advertise data packet container for Bluetooth LE advertising. This represents the data to be
 * advertised as well as the scan response data for active scans.
 * @hide
 */

class AdvertiseData internal constructor(
    internal val impl: AdvertiseDataImpl
) : Bundleable {

    companion object {
        internal const val FIELD_FWK_ADVERTISE_DATA = 0
        internal const val FIELD_SERVICE_SOLICITATION_UUIDS = 1

        val CREATOR: Bundleable.Creator<AdvertiseData> =
            object : Bundleable.Creator<AdvertiseData> {
                override fun fromBundle(bundle: Bundle): AdvertiseData {
                    return bundle.getAdvertiseData()
                }
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        internal fun Bundle.putAdvertiseData(data: AdvertiseData) {
            this.putParcelable(
                keyForField(FIELD_FWK_ADVERTISE_DATA), data.impl.fwkInstance
            )
            if (Build.VERSION.SDK_INT < 31) {
                if (data.serviceSolicitationUuids != null) {
                    this.putParcelableArrayList(
                        keyForField(FIELD_SERVICE_SOLICITATION_UUIDS),
                        ArrayList(data.serviceSolicitationUuids!!)
                    )
                }
            }
        }

        internal fun Bundle.getAdvertiseData(): AdvertiseData {
            val fwkAdvertiseData =
                Utils.getParcelableFromBundle(
                    this,
                    keyForField(FIELD_FWK_ADVERTISE_DATA),
                    android.bluetooth.le.AdvertiseData::class.java
                ) ?: throw IllegalArgumentException(
                    "Bundle doesn't include a framework advertise data"
                )

            val args = AdvertiseDataArgs()

            if (Build.VERSION.SDK_INT < 31) {
                args.serviceSolicitationUuids =
                    Utils.getParcelableArrayListFromBundle(
                        this,
                        keyForField(FIELD_SERVICE_SOLICITATION_UUIDS),
                        ParcelUuid::class.java
                    ).toMutableList()
            }
            return AdvertiseData(fwkAdvertiseData, args)
        }
    }

    val serviceUuids: MutableList<ParcelUuid>?
        get() = impl.serviceUuids
    val serviceSolicitationUuids: MutableList<ParcelUuid>?
        get() = impl.serviceSolicitationUuids
    val manufacturerSpecificData: SparseArray<ByteArray>?
        get() = impl.manufacturerSpecificData
    val serviceData: MutableMap<ParcelUuid, ByteArray>?
        get() = impl.serviceData
    val includeTxPowerLevel: Boolean
        get() = impl.includeTxPowerLevel
    val includeDeviceName: Boolean
        get() = impl.includeDeviceName

    internal constructor(fwkInstance: FwkAdvertiseData) : this(
        if (Build.VERSION.SDK_INT >= 31) {
            AdvertiseDataImplApi31(fwkInstance)
        } else {
            AdvertiseDataImplApi21(fwkInstance)
        }
    )

    constructor(
        serviceUuids: MutableList<ParcelUuid>? = null,
        serviceSolicitationUuids: MutableList<ParcelUuid>? = null,
        manufacturerSpecificData: SparseArray<ByteArray>? = null,
        serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
        includeTxPowerLevel: Boolean = false,
        includeDeviceName: Boolean = false
    ) : this(AdvertiseDataArgs(
            serviceUuids,
            serviceSolicitationUuids,
            manufacturerSpecificData,
            serviceData,
            includeTxPowerLevel,
            includeDeviceName
        ))

    internal constructor(args: AdvertiseDataArgs) : this(args.toFwkAdvertiseData(), args)

    internal constructor(fwkInstance: FwkAdvertiseData, args: AdvertiseDataArgs) : this(
        if (Build.VERSION.SDK_INT >= 31) {
            AdvertiseDataImplApi31(fwkInstance)
        } else {
            AdvertiseDataImplApi21(fwkInstance, args)
        }
    )

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putAdvertiseData(this)
        return bundle
    }
}

internal data class AdvertiseDataArgs(
    val serviceUuids: MutableList<ParcelUuid>? = null,
    var serviceSolicitationUuids: MutableList<ParcelUuid>? = null,
    val manufacturerSpecificData: SparseArray<ByteArray>? = null,
    val serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
    var includeTxPowerLevel: Boolean = false,
    var includeDeviceName: Boolean = false,
) {
    // "ClassVerificationFailure for FwkAdvertiseData.Builder
    @SuppressLint("ClassVerificationFailure")
    internal fun toFwkAdvertiseData(): FwkAdvertiseData {
        val builder = FwkAdvertiseData.Builder()
            .setIncludeTxPowerLevel(includeTxPowerLevel)
            .setIncludeDeviceName(includeDeviceName)

        serviceUuids?.forEach { builder.addServiceUuid(it) }

        if (manufacturerSpecificData != null) {
            with(manufacturerSpecificData) {
                for (index in 0 until size()) {
                    builder.addManufacturerData(keyAt(index), get(keyAt(index)))
                }
            }
        }

        serviceData?.forEach { builder.addServiceData(it.key, it.value) }

        if (Build.VERSION.SDK_INT >= 31 && serviceSolicitationUuids != null) {
            serviceSolicitationUuids?.forEach { builder.addServiceSolicitationUuid(it) }
        }

        return builder.build()
    }
}

internal interface AdvertiseDataImpl {
    val serviceUuids: MutableList<ParcelUuid>?
    val serviceSolicitationUuids: MutableList<ParcelUuid>?
    val manufacturerSpecificData: SparseArray<ByteArray>?
    val serviceData: MutableMap<ParcelUuid, ByteArray>?
    val includeTxPowerLevel: Boolean
    val includeDeviceName: Boolean

    val fwkInstance: FwkAdvertiseData
}

internal abstract class AdvertiseDataFwkImplApi21 internal constructor(
    override val fwkInstance: FwkAdvertiseData
) : AdvertiseDataImpl {
    override val serviceUuids: MutableList<ParcelUuid>?
        get() = fwkInstance.serviceUuids
    override val manufacturerSpecificData: SparseArray<ByteArray>?
        get() = fwkInstance.manufacturerSpecificData
    override val serviceData: MutableMap<ParcelUuid, ByteArray>?
        get() = fwkInstance.serviceData
    override val includeTxPowerLevel: Boolean
        get() = fwkInstance.includeTxPowerLevel
    override val includeDeviceName: Boolean
        get() = fwkInstance.includeDeviceName
}

internal class AdvertiseDataImplApi21 internal constructor(
    fwkInstance: FwkAdvertiseData,
    override val serviceSolicitationUuids: MutableList<ParcelUuid>? = null
) : AdvertiseDataFwkImplApi21(fwkInstance) {
    internal constructor(fwkInstance: FwkAdvertiseData, args: AdvertiseDataArgs) : this(
        fwkInstance, args.serviceSolicitationUuids)
}

@RequiresApi(Build.VERSION_CODES.S)
internal abstract class AdvertiseDataFwkImplApi31(
    fwkInstance: FwkAdvertiseData
) : AdvertiseDataFwkImplApi21(fwkInstance) {
    override val serviceSolicitationUuids: MutableList<ParcelUuid>?
        get() = fwkInstance.serviceSolicitationUuids
}

@RequiresApi(Build.VERSION_CODES.S)
internal class AdvertiseDataImplApi31 internal constructor(
    fwkInstance: FwkAdvertiseData,
) : AdvertiseDataFwkImplApi31(fwkInstance)