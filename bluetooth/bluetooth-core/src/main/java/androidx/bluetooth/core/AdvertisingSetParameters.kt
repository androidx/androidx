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

import android.bluetooth.le.AdvertisingSetParameters as FwkAdvertisingSetParameters
import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.bluetooth.core.utils.Bundleable
import androidx.bluetooth.core.utils.Utils

/**
 * TODO: Add docs
 * TODO: Support API 21
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.O)
class AdvertisingSetParameters internal constructor(
    internal val fwkInstance: FwkAdvertisingSetParameters
    ) : Bundleable {

    companion object {
        internal const val FIELD_FWK_ADVERTISING_SET_PARAMETERS = 0

        val CREATOR: Bundleable.Creator<AdvertisingSetParameters> =
            object : Bundleable.Creator<AdvertisingSetParameters> {
                override fun fromBundle(bundle: Bundle): AdvertisingSetParameters {
                    val fwkAdvertisingSetParameters =
                        Utils.getParcelableFromBundle(
                            bundle,
                            keyForField(FIELD_FWK_ADVERTISING_SET_PARAMETERS),
                            android.bluetooth.le.AdvertisingSetParameters::class.java
                        ) ?: throw IllegalArgumentException(
                            "Bundle doesn't include a framework advertising set parameters"
                        )
                    return AdvertisingSetParameters(fwkAdvertisingSetParameters)
                }
            }

        internal fun keyForField(field: Int): String {
            return field.toString(Character.MAX_RADIX)
        }

        internal fun buildFwkAdvertisingSetParameters(
            connectable: Boolean = false,
            scannable: Boolean = false,
            isLegacy: Boolean = false,
            isAnonymous: Boolean = false,
            includeTxPower: Boolean = false,
            primaryPhy: Int = FwkBluetoothDevice.PHY_LE_1M,
            secondaryPhy: Int = FwkBluetoothDevice.PHY_LE_1M,
            txPowerLevel: Int = TX_POWER_MEDIUM
        ): FwkAdvertisingSetParameters {
            val builder = FwkAdvertisingSetParameters.Builder()
                .setConnectable(connectable)
                .setScannable(scannable)
                .setLegacyMode(isLegacy)
                .setAnonymous(isAnonymous)
                .setIncludeTxPower(includeTxPower)
                .setPrimaryPhy(primaryPhy)
                .setSecondaryPhy(secondaryPhy)
                .setTxPowerLevel(txPowerLevel)

            return builder.build()
        }

        /**
         * Advertise on low frequency, around every 1000ms. This is the default and preferred
         * advertising mode as it consumes the least power.
         */
        const val INTERVAL_HIGH = FwkAdvertisingSetParameters.INTERVAL_HIGH

        /**
         * Advertise on medium frequency, around every 250ms. This is balanced between advertising
         * frequency and power consumption.
         */
        const val INTERVAL_MEDIUM = FwkAdvertisingSetParameters.INTERVAL_MEDIUM

        /**
         * Perform high frequency, low latency advertising, around every 100ms. This has the highest
         * power consumption and should not be used for continuous background advertising.
         */
        const val INTERVAL_LOW = FwkAdvertisingSetParameters.INTERVAL_LOW

        /**
         * Minimum value for advertising interval.
         */
        const val INTERVAL_MIN = FwkAdvertisingSetParameters.INTERVAL_MIN

        /**
         * Maximum value for advertising interval.
         */
        const val INTERVAL_MAX = FwkAdvertisingSetParameters.INTERVAL_MAX

        /**
         * Advertise using the lowest transmission (TX) power level. Low transmission power can be
         * used to restrict the visibility range of advertising packets.
         */
        const val TX_POWER_ULTRA_LOW = FwkAdvertisingSetParameters.TX_POWER_ULTRA_LOW

        /**
         * Advertise using low TX power level.
         */
        const val TX_POWER_LOW = FwkAdvertisingSetParameters.TX_POWER_LOW

        /**
         * Advertise using medium TX power level.
         */
        const val TX_POWER_MEDIUM = FwkAdvertisingSetParameters.TX_POWER_MEDIUM

        /**
         * Advertise using high TX power level. This corresponds to largest visibility range of the
         * advertising packet.
         */
        const val TX_POWER_HIGH = FwkAdvertisingSetParameters.TX_POWER_HIGH

        /**
         * Minimum value for TX power.
         */
        const val TX_POWER_MIN = FwkAdvertisingSetParameters.TX_POWER_MIN

        /**
         * Maximum value for TX power.
         */
        const val TX_POWER_MAX = FwkAdvertisingSetParameters.TX_POWER_MAX

        /**
         * The maximum limited advertisement duration as specified by the Bluetooth
         * SIG
         */
        private const val LIMITED_ADVERTISING_MAX_MILLIS = 180_000
    }

    val connectable: Boolean
        get() = fwkInstance.isConnectable
    val scannable: Boolean
        get() = fwkInstance.isScannable
    val isLegacy: Boolean
        get() = fwkInstance.isLegacy
    val isAnonymous: Boolean
        get() = fwkInstance.isAnonymous
    val includeTxPower: Boolean
        get() = fwkInstance.includeTxPower()
    val primaryPhy: Int
        get() = fwkInstance.primaryPhy
    val secondaryPhy: Int
        get() = fwkInstance.secondaryPhy
    val interval: Int
        get() = fwkInstance.interval
    val txPowerLevel: Int
        get() = fwkInstance.txPowerLevel

    constructor(
        connectable: Boolean = false,
        scannable: Boolean = false,
        isLegacy: Boolean = false,
        isAnonymous: Boolean = false,
        includeTxPower: Boolean = false,
        primaryPhy: Int = FwkBluetoothDevice.PHY_LE_1M,
        secondaryPhy: Int = FwkBluetoothDevice.PHY_LE_1M,
        txPowerLevel: Int = TX_POWER_MEDIUM
    ) : this(buildFwkAdvertisingSetParameters(
        connectable,
        scannable,
        isLegacy,
        isAnonymous,
        includeTxPower,
        primaryPhy,
        secondaryPhy,
        txPowerLevel
    ))

    override fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(keyForField(FIELD_FWK_ADVERTISING_SET_PARAMETERS), fwkInstance)
        return bundle
    }
}