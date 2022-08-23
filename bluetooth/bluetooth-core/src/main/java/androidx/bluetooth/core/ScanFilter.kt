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
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils

/**
 * TODO: Copy docs
 * TODO: Implement APIs added in API 33
 *       getters: getAdvertisingData, getAdvertisingDataMask, getAdvertisingDataType
 *       setters: setAdvertisingDataTypeWithData, setAdvertisingDataType,
 * @hide
 */
class ScanFilter internal constructor(
    internal val impl: ScanFilterInterface
) : ScanFilterInterface by impl {
    companion object {
        val CREATOR: Bundleable.Creator<ScanFilter> =
            if (Build.VERSION.SDK_INT < 29) {
                ScanFilterImplBase.CREATOR
            } else {
                ScanFilterImplApi29.CREATOR
            }

        internal fun createScanFilterImpl(args: ScanFilterArgs): ScanFilterInterface {
            return if (Build.VERSION.SDK_INT < 29) {
                ScanFilterImplBase(
                    ScanFilterImplBase.getFwkScanFilterBuilder(args).build(),
                    args.serviceSolicitationUuid,
                    args.serviceSolicitationUuidMask
                )
            } else {
                ScanFilterImplApi29(
                    ScanFilterImplApi29.getFwkScanFilterBuilder(args).build()
                )
            }
        }
    }
    internal constructor(fwkScanFilter: FwkScanFilter) : this(
        if (Build.VERSION.SDK_INT < 29) {
            ScanFilterImplBase(fwkScanFilter)
        } else {
            ScanFilterImplApi29(fwkScanFilter)
        }
    )

    constructor(
        deviceName: String? = null,
        deviceAddress: String? = null,
        serviceUuid: ParcelUuid? = null,
        serviceUuidMask: ParcelUuid? = null,
        serviceDataUuid: ParcelUuid? = null,
        serviceData: ByteArray? = null,
        serviceDataMask: ByteArray? = null,
        manufacturerId: Int = -1,
        manufacturerData: ByteArray? = null,
        manufacturerDataMask: ByteArray? = null,
        serviceSolicitationUuid: ParcelUuid? = null,
        serviceSolicitationUuidMask: ParcelUuid? = null
    ) : this(createScanFilterImpl(ScanFilterArgs(
        deviceName,
        deviceAddress,
        serviceUuid,
        serviceUuidMask,
        serviceDataUuid,
        serviceData,
        serviceDataMask,
        manufacturerId,
        manufacturerData,
        manufacturerDataMask,
        serviceSolicitationUuid,
        serviceSolicitationUuidMask
    )))
}

internal data class ScanFilterArgs(
    var deviceName: String? = null,
    var deviceAddress: String? = null,
    var serviceUuid: ParcelUuid? = null,
    var serviceUuidMask: ParcelUuid? = null,
    var serviceDataUuid: ParcelUuid? = null,
    var serviceData: ByteArray? = null,
    var serviceDataMask: ByteArray? = null,
    var manufacturerId: Int = -1,
    var manufacturerData: ByteArray? = null,
    var manufacturerDataMask: ByteArray? = null,
    var serviceSolicitationUuid: ParcelUuid? = null,
    var serviceSolicitationUuidMask: ParcelUuid? = null
)

internal interface ScanFilterInterface : Bundleable {
    val deviceName: String?
    val deviceAddress: String?
    val serviceUuid: ParcelUuid?
    val serviceUuidMask: ParcelUuid?
    val serviceDataUuid: ParcelUuid?
    val serviceData: ByteArray?
    val serviceDataMask: ByteArray?
    val manufacturerId: Int
    val manufacturerData: ByteArray?
    val manufacturerDataMask: ByteArray?
    val serviceSolicitationUuid: ParcelUuid?
    val serviceSolicitationUuidMask: ParcelUuid?
}

internal abstract class ScanFilterImpl internal constructor(
    internal val fwkInstance: FwkScanFilter
) : ScanFilterInterface {
    companion object {
        internal const val FIELD_FWK_SCAN_FILTER = 0
        internal const val FIELD_SERVICE_SOLICITATION_UUID = 1
        internal const val FIELD_SERVICE_SOLICITATION_UUID_MASK = 2

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }
    }

    override val deviceName: String?
        get() = fwkInstance.deviceName
    override val deviceAddress: String?
        get() = fwkInstance.deviceAddress
    override val serviceUuid: ParcelUuid?
        get() = fwkInstance.serviceUuid
    override val serviceUuidMask: ParcelUuid?
        get() = fwkInstance.serviceUuidMask
    override val serviceDataUuid: ParcelUuid?
        get() = fwkInstance.serviceDataUuid
    override val serviceData: ByteArray?
        get() = fwkInstance.serviceData
    override val serviceDataMask: ByteArray?
        get() = fwkInstance.serviceDataMask
    override val manufacturerId: Int
        get() = fwkInstance.manufacturerId
    override val manufacturerData: ByteArray?
        get() = fwkInstance.manufacturerData
    override val manufacturerDataMask: ByteArray?
        get() = fwkInstance.manufacturerDataMask
}

internal class ScanFilterImplBase internal constructor(
    fwkInstance: FwkScanFilter,
    override val serviceSolicitationUuid: ParcelUuid? = null,
    override val serviceSolicitationUuidMask: ParcelUuid? = null
) : ScanFilterImpl(fwkInstance) {
    companion object {
        val CREATOR: Bundleable.Creator<ScanFilter> =
            object : Bundleable.Creator<ScanFilter> {
                override fun fromBundle(bundle: Bundle): ScanFilter {
                    val fwkScanFilter =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_SCAN_FILTER),
                            FwkScanFilter::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework scan filter"
                        )
                    val serviceSolicitationUuid =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_SERVICE_SOLICITATION_UUID),
                            ParcelUuid::class.java
                        )
                    val serviceSolicitationUuidMask =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_SERVICE_SOLICITATION_UUID_MASK),
                            ParcelUuid::class.java
                        )
                    return ScanFilter(ScanFilterImplBase(
                        fwkScanFilter,
                        serviceSolicitationUuid,
                        serviceSolicitationUuidMask))
                }
            }

        internal fun getFwkScanFilterBuilder(args: ScanFilterArgs): FwkScanFilter.Builder {
            val builder = FwkScanFilter.Builder()
                .setDeviceName(args.deviceName)
                .setDeviceAddress(args.deviceAddress)
                .setServiceUuid(args.serviceUuid)

            if (args.serviceDataUuid != null) {
                if (args.serviceDataMask == null) {
                    builder.setServiceData(args.serviceDataUuid, args.serviceData)
                } else {
                    builder.setServiceData(
                        args.serviceDataUuid, args.serviceData, args.serviceDataMask)
                }
            }
            if (args.manufacturerId >= 0) {
                if (args.manufacturerDataMask == null) {
                    builder.setManufacturerData(args.manufacturerId, args.manufacturerData)
                } else {
                    builder.setManufacturerData(
                        args.manufacturerId, args.manufacturerData, args.manufacturerDataMask)
                }
            }
            return builder
        }
    }

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_SCAN_FILTER), fwkInstance)
        if (serviceSolicitationUuid != null) {
            bundle.putParcelable(
                keyForField(FIELD_SERVICE_SOLICITATION_UUID), serviceSolicitationUuid)
        }
        if (serviceSolicitationUuidMask != null) {
            bundle.putParcelable(
                keyForField(FIELD_SERVICE_SOLICITATION_UUID_MASK), serviceSolicitationUuidMask)
        }
        return bundle
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal abstract class ScanFilterImpl29(fwkInstance: FwkScanFilter) : ScanFilterImpl(fwkInstance) {
    override val serviceSolicitationUuid: ParcelUuid?
        get() = fwkInstance.serviceSolicitationUuid
    override val serviceSolicitationUuidMask: ParcelUuid?
        get() = fwkInstance.serviceSolicitationUuidMask
}

@RequiresApi(Build.VERSION_CODES.Q)
internal class ScanFilterImplApi29 internal constructor(
    fwkInstance: FwkScanFilter
) : ScanFilterImpl29(fwkInstance) {
    companion object {
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

        internal fun getFwkScanFilterBuilder(args: ScanFilterArgs): FwkScanFilter.Builder {
            val builder = ScanFilterImplBase.getFwkScanFilterBuilder(args)

            if (args.serviceSolicitationUuid != null) {
                if (args.serviceSolicitationUuidMask == null) {
                    builder.setServiceSolicitationUuid(args.serviceSolicitationUuid)
                } else {
                    builder.setServiceSolicitationUuid(
                        args.serviceSolicitationUuid, args.serviceSolicitationUuidMask)
                }
            }
            return builder
        }
    }

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_SCAN_FILTER), fwkInstance)
        return bundle
    }
}