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

import android.bluetooth.BluetoothAdapter as FwkBluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BluetoothAdapterTest {
    @Rule @JvmField
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 31) {
            GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
            )
        } else GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val hasBluetooth = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    private var adapterNameChangedlock: ReentrantLock = ReentrantLock()
    private var conditionAdapterNameChanged: Condition = adapterNameChangedlock.newCondition()
    private var isAdapterNameChanged: Boolean = false

    @Test
    fun constructorFromBluetoothManager_validAddress() {
        if (!hasBluetooth)
            return
        val fwkBluetoothAdapter =
            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
        val bluetoothAdapter = BluetoothAdapter(fwkBluetoothAdapter)
        assertTrue(BluetoothAdapter.checkBluetoothAddress(bluetoothAdapter.address))
    }

    @Test
    fun checkAddressTest() {
        if (!hasBluetooth)
            return
        val fwkBluetoothAdapter =
            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
        val bluetoothAdapter = BluetoothAdapter(fwkBluetoothAdapter)

        Assert.assertTrue(FwkBluetoothAdapter.checkBluetoothAddress(bluetoothAdapter.address))
        Assert.assertFalse(BluetoothAdapter.checkBluetoothAddress("hello a random string"))
        Assert.assertTrue(BluetoothAdapter.checkBluetoothAddress("00:43:A8:23:10:F0"))
    }

    @Test
    fun remoteDeviceTest() {
        if (!hasBluetooth)
            return
        val fwkBluetoothAdapter =
            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
        val bluetoothAdapter = BluetoothAdapter(fwkBluetoothAdapter)
        // Check correct name
        val stringDevice = bluetoothAdapter.getRemoteDevice("00:01:02:03:04:05")
        val byteDevice = bluetoothAdapter.getRemoteDevice(ByteArray(6) {
                index -> index.toByte()
        })
        assertNotNull(stringDevice)
        assertNotNull(byteDevice)
        assertEquals(byteDevice.address, stringDevice.address)
    }

    @Test
    fun nameTest() {
        if (!hasBluetooth)
            return
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        context.registerReceiver(mAdapterNameChangeReceiver, filter)

        val fwkBluetoothAdapter =
            BluetoothManager(ApplicationProvider.getApplicationContext()).getAdapter()
        val bluetoothAdapter = BluetoothAdapter(fwkBluetoothAdapter)
        // Check correct name
        val testName = "Bluetooth:Test-name_?~"
        val originalName = bluetoothAdapter.name
        bluetoothAdapter.name = testName
        assertTrue(waitForAdapterNameChange())
        isAdapterNameChanged = false
        Assert.assertEquals(testName, bluetoothAdapter.name)
        bluetoothAdapter.name = originalName
        assertTrue(waitForAdapterNameChange())
        isAdapterNameChanged = false
        Assert.assertEquals(originalName, bluetoothAdapter.name)
    }

    private fun waitForAdapterNameChange(): Boolean {
        try {
            Log.e("BluetoothAdapterTest", "trying")
            adapterNameChangedlock.lock()
            // Wait for the Adapter name to be changed
            Log.e("BluetoothAdapterTest", "waiting")
            conditionAdapterNameChanged.await(5000, TimeUnit.MILLISECONDS)
            Log.e("BluetoothAdapterTest", "finish waiting")
        } catch (e: InterruptedException) {
            Log.e("BluetoothAdapterTest", "waitForAdapterNameChange: interrrupted")
        } finally {
            adapterNameChangedlock.unlock()
        }
        return isAdapterNameChanged
    }

    private val mAdapterNameChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED) {
                adapterNameChangedlock.lock()
                isAdapterNameChanged = true
                try {
                    conditionAdapterNameChanged.signal()
                } catch (ex: IllegalMonitorStateException) {
                    Log.e("BluetoothAdapterTest", "error: $ex")
                } finally {
                    adapterNameChangedlock.unlock()
                }
            }
        }
    }
}