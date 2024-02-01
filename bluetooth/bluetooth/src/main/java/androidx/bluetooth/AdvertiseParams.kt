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

import android.bluetooth.le.AdvertiseData as FwkAdvertiseData
import android.bluetooth.le.AdvertiseSettings as FwkAdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters as FwkAdvertisingSetParameters
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.DoNotInline
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import java.util.UUID

/**
 * A class to provide a way to adjust advertising preferences and advertise data packet.
 */
class AdvertiseParams(
    /** Whether the device address will be included in the advertisement packet. */
    @get:JvmName("shouldIncludeDeviceAddress")
    val shouldIncludeDeviceAddress: Boolean = false,
    /** Whether the device name will be included in the advertisement packet. */
    @get:JvmName("shouldIncludeDeviceName")
    val shouldIncludeDeviceName: Boolean = false,
    /** Whether the advertisement will indicate connectable. */
    val isConnectable: Boolean = false,
    /**
     * Whether the advertisement will be discoverable.
     *
     * Please note that it would be ignored under API level 34 and [isConnectable] would be
     * used instead.
     */
    val isDiscoverable: Boolean = false,
    /**
     * Advertising duration in milliseconds.
     *
     * It must not exceed 180000 milliseconds. A value of 0 means advertising continues
     * until it is stopped explicitly.
     * @throws IllegalArgumentException if it is not in the range [0..180000].
     */
    @IntRange(from = 0, to = 180000) val durationMillis: Long = 0,
    /**
     * A map of company identifiers to manufacturer specific data.
     * <p>
     * Please refer to the Bluetooth Assigned Numbers document provided by the <a
     * href="https://www.bluetooth.org">Bluetooth SIG</a> for the list of existing company
     * identifiers.
     */
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    /**
     * A map of 16-bit UUIDs of the services to corresponding additional service data.
     */
    val serviceData: Map<UUID, ByteArray> = emptyMap(),
    /**
     * A list of service UUIDs to advertise.
     */
    val serviceUuids: List<UUID> = emptyList(),
    /**
     * A list of service solicitation UUIDs to advertise that we invite to connect.
     */
    val serviceSolicitationUuids: List<UUID> = emptyList()
) {
    @RequiresApi(34)
    private object AdvertiseParamsApi34Impl {
        @JvmStatic
        @DoNotInline
        fun setDiscoverable(builder: FwkAdvertiseSettings.Builder, isDiscoverable: Boolean) {
            builder.setDiscoverable(isDiscoverable)
        }

        @JvmStatic
        @DoNotInline
        fun setDiscoverable(builder: FwkAdvertisingSetParameters.Builder, isDiscoverable: Boolean) {
            builder.setDiscoverable(isDiscoverable)
        }
    }

    @RequiresApi(31)
    private object AdvertiseParamsApi31Impl {
        @JvmStatic
        @DoNotInline
        fun addServiceSolicitationUuid(builder: FwkAdvertiseData.Builder, parcelUuid: ParcelUuid) {
            builder.addServiceSolicitationUuid(parcelUuid)
        }
    }

    @RequiresApi(26)
    private object AdvertiseParamsApi26Impl {
        @JvmStatic
        @DoNotInline
        fun fwkAdvertiseSetParams(
            isConnectable: Boolean,
            isDiscoverable: Boolean
        ): FwkAdvertisingSetParameters = FwkAdvertisingSetParameters.Builder().run {
            setConnectable(isConnectable)
            if (Build.VERSION.SDK_INT >= 34) {
                AdvertiseParamsApi34Impl.setDiscoverable(this, isDiscoverable)
            }
            build()
        }
    }

    internal val fwkAdvertiseSettings: FwkAdvertiseSettings
        get() = FwkAdvertiseSettings.Builder().run {
            setConnectable(isConnectable)
            if (durationMillis > 0) {
                setTimeout(durationMillis.toInt())
            }
            if (Build.VERSION.SDK_INT >= 34) {
                AdvertiseParamsApi34Impl.setDiscoverable(this, isDiscoverable)
            }
            build()
        }

    @RequiresApi(26)
    internal fun fwkAdvertiseSetParams(): FwkAdvertisingSetParameters {
        return AdvertiseParamsApi26Impl.fwkAdvertiseSetParams(isConnectable, isDiscoverable)
    }

    internal val fwkAdvertiseData: FwkAdvertiseData
        get() = FwkAdvertiseData.Builder().run {
            setIncludeDeviceName(shouldIncludeDeviceName)
            serviceData.forEach {
                addServiceData(ParcelUuid(it.key), it.value)
            }
            manufacturerData.forEach {
                addManufacturerData(it.key, it.value)
            }
            serviceUuids.forEach {
                addServiceUuid(ParcelUuid(it))
            }
            if (Build.VERSION.SDK_INT >= 31) {
                serviceSolicitationUuids.forEach {
                    AdvertiseParamsApi31Impl.addServiceSolicitationUuid(this, ParcelUuid(it))
                }
            }
            build()
        }

    init {
        if (durationMillis !in 0..180000)
            throw IllegalArgumentException("Advertise duration must be in [0, 180000]")
    }
}
