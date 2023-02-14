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

import android.bluetooth.le.AdvertiseSettings as FwkAdvertiseSettings
import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * The {@link AdvertiseSettings} provide a way to adjust advertising preferences for each
 * Bluetooth LE advertisement instance. Use the constructor to create an instance of this class.
 */
@SuppressWarnings("HiddenSuperclass") // Bundleable
class AdvertiseSettings internal constructor(internal val fwkInstance: FwkAdvertiseSettings) :
    Bundleable {

    companion object {
        internal const val FIELD_FWK_ADVERTISE_SETTINGS = 0

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmField
        val CREATOR: Bundleable.Creator<AdvertiseSettings> =
            object : Bundleable.Creator<AdvertiseSettings> {
                override fun fromBundle(bundle: Bundle): AdvertiseSettings {
                    val fwkAdvertiseSettings =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_ADVERTISE_SETTINGS),
                            android.bluetooth.le.AdvertiseSettings::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework advertise settings"
                        )
                    return AdvertiseSettings(fwkAdvertiseSettings)
                }
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        internal fun buildFwkAdvertiseSettings(
            advertiseMode: Int,
            advertiseTxPowerLevel: Int,
            advertiseConnectable: Boolean,
            advertiseTimeoutMillis: Int,
        ): FwkAdvertiseSettings {
            val builder = FwkAdvertiseSettings.Builder()
                .setConnectable(advertiseConnectable)
                .setAdvertiseMode(advertiseMode)
                .setTxPowerLevel(advertiseTxPowerLevel)
                .setTimeout(advertiseTimeoutMillis)

            return builder.build()
        }

        /**
        * Perform Bluetooth LE advertising in low power mode. This is the default and preferred
        * advertising mode as it consumes the least power.
        */
        const val ADVERTISE_MODE_LOW_POWER = FwkAdvertiseSettings.ADVERTISE_MODE_LOW_POWER

        /**
         * Perform Bluetooth LE advertising in balanced power mode. This is balanced between
         * advertising frequency and power consumption.
         */
        const val ADVERTISE_MODE_BALANCED = FwkAdvertiseSettings.ADVERTISE_MODE_BALANCED

        /**
         * Perform Bluetooth LE advertising in low latency, high power mode. This has the highest
         * power consumption and should not be used for continuous background advertising.
         */
        const val ADVERTISE_MODE_LOW_LATENCY = FwkAdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY

        /**
         * Advertise using the lowest transmission (TX) power level. Low transmission power can be
         * used to restrict the visibility range of advertising packets.
         */
        const val ADVERTISE_TX_POWER_ULTRA_LOW = FwkAdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW

        /**
         * Advertise using low TX power level.
         */
        const val ADVERTISE_TX_POWER_LOW = FwkAdvertiseSettings.ADVERTISE_TX_POWER_LOW

        /**
         * Advertise using medium TX power level.
         */
        const val ADVERTISE_TX_POWER_MEDIUM = FwkAdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM

        /**
         * Advertise using high TX power level. This corresponds to largest visibility range of the
         * advertising packet.
         */
        const val ADVERTISE_TX_POWER_HIGH = FwkAdvertiseSettings.ADVERTISE_TX_POWER_HIGH
    }

    val advertiseMode: Int
        /** Returns the advertise mode. */
        get() = fwkInstance.mode
    val advertiseTxPowerLevel: Int
        /** Returns the TX power level for advertising. */
        get() = fwkInstance.txPowerLevel
    val advertiseConnectable: Boolean
        /** Returns whether the advertisement will indicate connectable. */
        get() = fwkInstance.isConnectable
    val advertiseTimeoutMillis: Int
        /** Returns the advertising time limit in milliseconds. */
        get() = fwkInstance.timeout

    constructor(
        advertiseMode: Int = ADVERTISE_MODE_LOW_POWER,
        advertiseTxPowerLevel: Int = ADVERTISE_TX_POWER_MEDIUM,
        advertiseConnectable: Boolean = true,
        advertiseTimeoutMillis: Int = 0
    ) : this(buildFwkAdvertiseSettings(
        advertiseMode,
        advertiseTxPowerLevel,
        advertiseConnectable,
        advertiseTimeoutMillis
    ))

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_ADVERTISE_SETTINGS), fwkInstance)
        return bundle
    }
}