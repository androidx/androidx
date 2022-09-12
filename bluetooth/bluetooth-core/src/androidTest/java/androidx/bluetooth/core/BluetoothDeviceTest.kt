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

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothDeviceTest {
    @Rule
    @JvmField
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 31) {
            GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
            )
        } else GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    fun constructor_createsFromAdapter() {
        val bluetoothAdapter =
            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
                ?: return // Bluetooth not available on this device
        assertTrue("Bluetooth is not enabled", bluetoothAdapter.isEnabled)

        val testAddress = "10:43:A8:23:10:F0"
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(testAddress)

        assertEquals(testAddress, bluetoothDevice.address)
        assertEquals(BluetoothDevice.BOND_NONE, bluetoothDevice.bondState)
        assertEquals(BluetoothDevice.DEVICE_TYPE_UNKNOWN, bluetoothDevice.type)
        assertNull(bluetoothDevice.name)
    }

    /**
     * TODO: setAlias then getAlias doesn't work on all versions
     * TODO: Write a test that cover different behavior on different SDK versions. setAlias requires
     * CDM association
     *
     **/

//    @Test
//    fun setAlias_getAlias_workCorrectly() {
//
//        val bluetoothAdapter =
//            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
//                ?: return // Bluetooth not available on this device
//        assertTrue("Bluetooth is not enabled", bluetoothAdapter.isEnabled)
//
//        val testAddress = "10:43:A8:23:10:F0"
//        val testAlias = "!@#Dep trai<>?"
//        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(testAddress)
//        val bluetoothDevice2 = bluetoothAdapter.getRemoteDevice(testAddress)
//        Log.d("BluetoothX", "trying to call from test")
//        val oldAlias = bluetoothDevice.alias
//
//        Log.d("BluetoothX", "oldAlias from $oldAlias")
//        Log.d("BluetoothX", "trying to call from test")
//        bluetoothDevice.alias = testAlias
//        assertEquals(testAlias, bluetoothDevice.alias)
//        assertEquals(testAlias, bluetoothDevice2.alias)
//        bluetoothDevice2.alias = oldAlias
//        assertEquals(oldAlias, bluetoothDevice.alias)
//        assertEquals(oldAlias, bluetoothDevice2.alias)
//    }
}