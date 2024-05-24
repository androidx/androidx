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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test cases for [BluetoothDevice] */
@RunWith(JUnit4::class)
class BluetoothDeviceTest {

    @Rule
    @JvmField
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 31) {
            GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else
            GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH
            )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bluetoothLe = BluetoothLe(context)

    @Before
    fun setUp() {
        assumeNotNull(bluetoothAdapter)
    }

    @Test
    fun constructorWithFwkInstance() {
        val fwkBluetoothDevice = bluetoothAdapter!!.getRemoteDevice("00:01:02:03:04:05")
        val bluetoothDevice = BluetoothDevice(fwkBluetoothDevice)

        assertThat(fwkBluetoothDevice.bondState).isEqualTo(bluetoothDevice.bondState)
        assertThat(fwkBluetoothDevice.name).isEqualTo(bluetoothDevice.name)
    }

    @Test
    fun equalIdWithEqualAddress() {
        val address = "00:01:02:03:04:05"
        val fwkBluetoothDevice1 = bluetoothAdapter!!.getRemoteDevice(address)
        val fwkBluetoothDevice2 = bluetoothAdapter.getRemoteDevice(address)

        val bluetoothDevice1 = BluetoothDevice(fwkBluetoothDevice1)
        val bluetoothDevice2 = BluetoothDevice(fwkBluetoothDevice2)

        assertThat(bluetoothDevice1.id).isEqualTo(bluetoothDevice2.id)
    }

    @Test
    fun differentIdWithDifferentAddress() {
        val address1 = "00:01:02:03:04:05"
        val address2 = "05:04:03:02:01:00"
        val fwkBluetoothDevice1 = bluetoothAdapter!!.getRemoteDevice(address1)
        val fwkBluetoothDevice2 = bluetoothAdapter.getRemoteDevice(address2)

        val bluetoothDevice1 = BluetoothDevice(fwkBluetoothDevice1)
        val bluetoothDevice2 = BluetoothDevice(fwkBluetoothDevice2)

        assertThat(bluetoothDevice1.id).isNotEqualTo(bluetoothDevice2.id)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun differentIdWithSameAddressDifferentAddressType() {
        val address = "00:01:02:03:04:05"
        val fwkBluetoothDevice1 =
            bluetoothAdapter!!.getRemoteLeDevice(address, FwkBluetoothDevice.ADDRESS_TYPE_PUBLIC)
        val fwkBluetoothDevice2 =
            bluetoothAdapter.getRemoteLeDevice(address, FwkBluetoothDevice.ADDRESS_TYPE_RANDOM)

        val bluetoothDevice1 = BluetoothDevice(fwkBluetoothDevice1)
        val bluetoothDevice2 = BluetoothDevice(fwkBluetoothDevice2)

        assertThat(bluetoothDevice1.id).isNotEqualTo(bluetoothDevice2.id)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun differentIdDifferentRandomAddressType() {
        val address1 = "57:96:C7:1A:45:9C"
        val address2 = "00:11:02:03:04:C0"
        val fwkBluetoothDevice1 =
            bluetoothAdapter!!.getRemoteLeDevice(address1, FwkBluetoothDevice.ADDRESS_TYPE_RANDOM)
        val fwkBluetoothDevice2 =
            bluetoothAdapter.getRemoteLeDevice(address2, FwkBluetoothDevice.ADDRESS_TYPE_RANDOM)

        val bluetoothDevice1 = BluetoothDevice(fwkBluetoothDevice1)
        val bluetoothDevice2 = BluetoothDevice(fwkBluetoothDevice2)

        assertThat(bluetoothDevice1.id).isNotEqualTo(bluetoothDevice2.id)
    }
}
