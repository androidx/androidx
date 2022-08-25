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

import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.bluetooth.le.AdvertisingSetParameters as FwkAdvertisingSetParameters
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RequiresApi(Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class AdvertisingSetParametersTest {

    companion object {
        val TEST_CONNECTABLE = true
        val TEST_SCANNABLE = true
        val TEST_IS_LEGACY = true
        val TEST_IS_ANONYMOUS = false
        val TEST_INCLUDE_TX_POWER = false
        val TEST_PRIMARY_PHY = FwkBluetoothDevice.PHY_LE_1M
        val TEST_SECONDARY_PHY = FwkBluetoothDevice.PHY_LE_2M
        val TEST_TX_POWER_LEVEL = FwkAdvertisingSetParameters.TX_POWER_LOW
    }

    @Test
    fun constructorWithValues_createsFrameworkInstanceCorrectly() {
        val advertisingSetParameters = AdvertisingSetParameters(
            connectable = TEST_CONNECTABLE,
            scannable = TEST_SCANNABLE,
            isLegacy = TEST_IS_LEGACY,
            isAnonymous = TEST_IS_ANONYMOUS,
            includeTxPower = TEST_INCLUDE_TX_POWER,
            primaryPhy = TEST_PRIMARY_PHY,
            secondaryPhy = TEST_SECONDARY_PHY,
            txPowerLevel = TEST_TX_POWER_LEVEL
        )
        val fwkAdvertisingSetParameters = advertisingSetParameters.fwkInstance
        assertThat(fwkAdvertisingSetParameters.isConnectable).isEqualTo(TEST_CONNECTABLE)
        assertThat(fwkAdvertisingSetParameters.isScannable).isEqualTo(TEST_SCANNABLE)
        assertThat(fwkAdvertisingSetParameters.isLegacy).isEqualTo(TEST_IS_LEGACY)
        assertThat(fwkAdvertisingSetParameters.isAnonymous).isEqualTo(TEST_IS_ANONYMOUS)
        assertThat(fwkAdvertisingSetParameters.includeTxPower()).isEqualTo(TEST_INCLUDE_TX_POWER)
        assertThat(fwkAdvertisingSetParameters.primaryPhy).isEqualTo(TEST_PRIMARY_PHY)
        assertThat(fwkAdvertisingSetParameters.secondaryPhy).isEqualTo(TEST_SECONDARY_PHY)
        assertThat(fwkAdvertisingSetParameters.txPowerLevel).isEqualTo(TEST_TX_POWER_LEVEL)
    }

    @Test
    fun constructorWithFwkInstance_createsAdvertisingSetParametersCorrectly() {
        val fwkAdvertisingSetParameters = FwkAdvertisingSetParameters.Builder()
            .setConnectable(TEST_CONNECTABLE)
            .setScannable(TEST_SCANNABLE)
            .setLegacyMode(TEST_IS_LEGACY)
            .setAnonymous(TEST_IS_ANONYMOUS)
            .setIncludeTxPower(TEST_INCLUDE_TX_POWER)
            .setPrimaryPhy(TEST_PRIMARY_PHY)
            .setSecondaryPhy(TEST_SECONDARY_PHY)
            .setTxPowerLevel(TEST_TX_POWER_LEVEL)
            .build()
        val advertisingSetParameters = AdvertisingSetParameters(fwkAdvertisingSetParameters)
        assertThat(advertisingSetParameters.connectable).isEqualTo(TEST_CONNECTABLE)
        assertThat(advertisingSetParameters.scannable).isEqualTo(TEST_SCANNABLE)
        assertThat(advertisingSetParameters.isLegacy).isEqualTo(TEST_IS_LEGACY)
        assertThat(advertisingSetParameters.isAnonymous).isEqualTo(TEST_IS_ANONYMOUS)
        assertThat(advertisingSetParameters.includeTxPower).isEqualTo(TEST_INCLUDE_TX_POWER)
        assertThat(advertisingSetParameters.primaryPhy).isEqualTo(TEST_PRIMARY_PHY)
        assertThat(advertisingSetParameters.secondaryPhy).isEqualTo(TEST_SECONDARY_PHY)
        assertThat(advertisingSetParameters.txPowerLevel).isEqualTo(TEST_TX_POWER_LEVEL)
    }

    @Test
    fun advertisingSetParametersBundleable() {
        val advertisingSetParameters = AdvertisingSetParameters(
            connectable = TEST_CONNECTABLE,
            scannable = TEST_SCANNABLE,
            isLegacy = TEST_IS_LEGACY,
            isAnonymous = TEST_IS_ANONYMOUS,
            includeTxPower = TEST_INCLUDE_TX_POWER,
            primaryPhy = TEST_PRIMARY_PHY,
            secondaryPhy = TEST_SECONDARY_PHY,
            txPowerLevel = TEST_TX_POWER_LEVEL
        )
        val bundle = advertisingSetParameters.toBundle()

        val advertisingSetParametersFromBundle = AdvertisingSetParameters.CREATOR.fromBundle(bundle)
        assertThat(advertisingSetParametersFromBundle.connectable).isEqualTo(TEST_CONNECTABLE)
        assertThat(advertisingSetParametersFromBundle.scannable).isEqualTo(TEST_SCANNABLE)
        assertThat(advertisingSetParametersFromBundle.isLegacy).isEqualTo(TEST_IS_LEGACY)
        assertThat(advertisingSetParametersFromBundle.isAnonymous).isEqualTo(TEST_IS_ANONYMOUS)
        assertThat(advertisingSetParametersFromBundle.includeTxPower)
            .isEqualTo(TEST_INCLUDE_TX_POWER)
        assertThat(advertisingSetParametersFromBundle.primaryPhy).isEqualTo(TEST_PRIMARY_PHY)
        assertThat(advertisingSetParametersFromBundle.secondaryPhy).isEqualTo(TEST_SECONDARY_PHY)
        assertThat(advertisingSetParametersFromBundle.txPowerLevel).isEqualTo(TEST_TX_POWER_LEVEL)
    }
}