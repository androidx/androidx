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
import android.os.Bundle
import android.os.ParcelUuid
import android.util.SparseArray
import androidx.annotation.RequiresApi

/**
 * TODO: Add docs
 * TODO: Add functions in SDK 33
 *
 * Advertise data packet container for Bluetooth LE advertising. This represents the data to be
 * advertised as well as the scan response data for active scans.
 * @hide
 */
class AdvertiseData internal constructor(
    internal val impl: AdvertiseDataImpl
) : AdvertiseDataImpl by impl {
    companion object {
        val CREATOR: Bundleable.Creator<AdvertiseData> =
            if (Build.VERSION.SDK_INT < 31) {
                AdvertiseDataImplApi21.CREATOR
            } else {
                AdvertiseDataImplApi31.CREATOR
            }

        internal fun createAdvertiseDataImpl(args: AdvertiseDataArgs): AdvertiseDataImpl {
            return if (Build.VERSION.SDK_INT < 31) {
                AdvertiseDataImplApi21(
                    AdvertiseDataImplApi21.getFwkAdvertiseDataBuilder(args).build(),
                    args.serviceSolicitationUuids
                )
            } else {
                AdvertiseDataImplApi31(
                    AdvertiseDataImplApi31.getFwkAdvertiseDataBuilder(args).build()
                )
            }
        }
    }

    internal constructor(fwkAdvertiseData: FwkAdvertiseData) : this(
        if (Build.VERSION.SDK_INT < 31) {
            AdvertiseDataImplApi21(fwkAdvertiseData)
        } else {
            AdvertiseDataImplApi31(fwkAdvertiseData)
        }
    )

    constructor(
        serviceUuids: MutableList<ParcelUuid>? = null,
        serviceSolicitationUuids: MutableList<ParcelUuid>? = null,
        manufacturerSpecificData: SparseArray<ByteArray>? = null,
        serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
        includeTxPowerLevel: Boolean = false,
        includeDeviceName: Boolean = false
    ) : this(
        createAdvertiseDataImpl(AdvertiseDataArgs(
            serviceUuids,
            serviceSolicitationUuids,
            manufacturerSpecificData,
            serviceData,
            includeTxPowerLevel,
            includeDeviceName
        )))
}

internal data class AdvertiseDataArgs(
    val serviceUuids: MutableList<ParcelUuid>? = null,
    val serviceSolicitationUuids: MutableList<ParcelUuid>? = null,
    val manufacturerSpecificData: SparseArray<ByteArray>? = null,
    val serviceData: MutableMap<ParcelUuid, ByteArray>? = null,
    var includeTxPowerLevel: Boolean = false,
    var includeDeviceName: Boolean = false,
)

internal interface AdvertiseDataImpl : Bundleable {
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
    companion object {
        internal const val FIELD_FWK_ADVERTISE_DATA = 0
        internal const val FIELD_SERVICE_SOLICITATION_UUIDS = 1

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }
    }

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
    companion object {
        val CREATOR: Bundleable.Creator<AdvertiseData> =
            object : Bundleable.Creator<AdvertiseData> {
                override fun fromBundle(bundle: Bundle): AdvertiseData {
                    val fwkAdvertiseData =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_ADVERTISE_DATA),
                            FwkAdvertiseData::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework advertise data"
                        )
                    val serviceSolicitationUuids =
                        Utils.getParcelableArrayListFromBundle(
                            bundle,
                            keyForField(FIELD_SERVICE_SOLICITATION_UUIDS),
                            ParcelUuid::class.java
                        )

                    return AdvertiseData(AdvertiseDataImplApi21(
                        fwkAdvertiseData,
                        serviceSolicitationUuids.toMutableList())
                    )
                }
            }

        internal fun getFwkAdvertiseDataBuilder(args: AdvertiseDataArgs): FwkAdvertiseData.Builder {
            val builder = FwkAdvertiseData.Builder()
                .setIncludeTxPowerLevel(args.includeTxPowerLevel)
                .setIncludeDeviceName(args.includeDeviceName)

            args.serviceUuids?.forEach { builder.addServiceUuid(it) }

            if (args.manufacturerSpecificData != null) {
                with(args.manufacturerSpecificData) {
                    for (index in 0 until size()) {
                        builder.addManufacturerData(keyAt(index), get(keyAt(index)))
                    }
                }
            }

            args.serviceData?.forEach { builder.addServiceData(it.key, it.value) }

            return builder
        }
    }

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_ADVERTISE_DATA), fwkInstance)
        if (serviceSolicitationUuids != null) {
            bundle.putParcelableArrayList(
                keyForField(FIELD_SERVICE_SOLICITATION_UUIDS),
                ArrayList(serviceSolicitationUuids)
            )
        }
        return bundle
    }
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
    fwkInstance: FwkAdvertiseData
) : AdvertiseDataFwkImplApi31(fwkInstance) {
    companion object {
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

        internal fun getFwkAdvertiseDataBuilder(args: AdvertiseDataArgs): FwkAdvertiseData.Builder {
            val builder = AdvertiseDataImplApi21.getFwkAdvertiseDataBuilder(args)

            args.serviceSolicitationUuids?.forEach { builder.addServiceSolicitationUuid(it) }
            return builder
        }
    }

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_ADVERTISE_DATA), fwkInstance)
        return bundle
    }
}