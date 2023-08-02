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
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdvertiseSettingsTest {

    companion object {
        val TEST_ADVERTISE_MODE = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
        val TEST_ADVERTISE_TX_POWER_LEVEL = AdvertiseSettings.ADVERTISE_TX_POWER_LOW
        val TEST_ADVERTISE_CONNECTABLE = true
        val TEST_ADVERTISE_TIMEOUT_MILLIS = 0
    }

    @Test
    fun constructorWithValues_createsFrameworkInstanceCorrectly() {
        val advertiseSettings = AdvertiseSettings(
            advertiseMode = TEST_ADVERTISE_MODE,
            advertiseTxPowerLevel = TEST_ADVERTISE_TX_POWER_LEVEL,
            advertiseConnectable = TEST_ADVERTISE_CONNECTABLE,
            advertiseTimeoutMillis = TEST_ADVERTISE_TIMEOUT_MILLIS
        )
        val fwkAdvertiseSettings = advertiseSettings.fwkInstance
        Truth.assertThat(fwkAdvertiseSettings.mode).isEqualTo(TEST_ADVERTISE_MODE)
        Truth.assertThat(fwkAdvertiseSettings.txPowerLevel).isEqualTo(TEST_ADVERTISE_TX_POWER_LEVEL)
        Truth.assertThat(fwkAdvertiseSettings.isConnectable).isEqualTo(TEST_ADVERTISE_CONNECTABLE)
        Truth.assertThat(fwkAdvertiseSettings.timeout).isEqualTo(TEST_ADVERTISE_TIMEOUT_MILLIS)
    }

    @Test
    fun constructorWithFwkInstance_createsAdvertiseSettingsCorrectly() {
        val fwkAdvertiseSettings = FwkAdvertiseSettings.Builder()
            .setAdvertiseMode(TEST_ADVERTISE_MODE)
            .setTxPowerLevel(TEST_ADVERTISE_TX_POWER_LEVEL)
            .setConnectable(TEST_ADVERTISE_CONNECTABLE)
            .setTimeout(TEST_ADVERTISE_TIMEOUT_MILLIS)
            .build()
        val advertiseSettings = AdvertiseSettings(fwkAdvertiseSettings)

        Truth.assertThat(advertiseSettings.advertiseMode).isEqualTo(TEST_ADVERTISE_MODE)
        Truth.assertThat(advertiseSettings.advertiseTxPowerLevel).isEqualTo(
            TEST_ADVERTISE_TX_POWER_LEVEL)
        Truth.assertThat(advertiseSettings.advertiseConnectable).isEqualTo(
            TEST_ADVERTISE_CONNECTABLE)
        Truth.assertThat(advertiseSettings.advertiseTimeoutMillis).isEqualTo(
            TEST_ADVERTISE_TIMEOUT_MILLIS)
    }

    @Test
    fun advertiseSettingsBundleable() {
        val advertiseSettings = AdvertiseSettings(
            advertiseMode = TEST_ADVERTISE_MODE,
            advertiseTxPowerLevel = TEST_ADVERTISE_TX_POWER_LEVEL,
            advertiseConnectable = TEST_ADVERTISE_CONNECTABLE,
            advertiseTimeoutMillis = TEST_ADVERTISE_TIMEOUT_MILLIS,
        )
        val bundle = advertiseSettings.toBundle()

        val advertiseSettingsFromBundle = AdvertiseSettings.CREATOR.fromBundle(bundle)
        Truth.assertThat(advertiseSettingsFromBundle.advertiseMode).isEqualTo(TEST_ADVERTISE_MODE)
        Truth.assertThat(advertiseSettingsFromBundle.advertiseTxPowerLevel).isEqualTo(
            TEST_ADVERTISE_TX_POWER_LEVEL)
        Truth.assertThat(advertiseSettingsFromBundle.advertiseConnectable).isEqualTo(
            TEST_ADVERTISE_CONNECTABLE)
        Truth.assertThat(advertiseSettingsFromBundle.advertiseTimeoutMillis).isEqualTo(
            TEST_ADVERTISE_TIMEOUT_MILLIS)
    }
}