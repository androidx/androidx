/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils

/** The production implementation of [BluetoothDeviceChecker] that uses the Android framework. */
@RequiresApi(Build.VERSION_CODES.O)
internal class ProductionBluetoothDeviceChecker(private val context: Context) :
    BluetoothDeviceChecker {
    companion object {
        private val TAG = ProductionBluetoothDeviceChecker::class.java.simpleName
    }

    override fun hasAvailableNonWatchDevice(availableEndpoints: List<CallEndpointCompat>): Boolean {
        if (!EndpointUtils.hasSufficientBluetoothPermission(context)) {
            Log.w(TAG, "Permission denied. Assuming a BT device could be present.")
            return true
        }

        return try {
            val bluetoothAdapter =
                (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    ?: return false

            EndpointUtils.hasAvailableNonWearableDevice(context, availableEndpoints) { endpoint ->
                getBluetoothDevice(context, endpoint, bluetoothAdapter)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Security Exception hit. Assuming a BT device could be present.", e)
            return true
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getBluetoothDevice(
        context: Context,
        endpoint: CallEndpointCompat,
        adapter: BluetoothAdapter,
    ): BluetoothDevice? {
        if (!EndpointUtils.hasSufficientBluetoothPermission(context)) {
            return null
        }
        return adapter.bondedDevices.find { it.name == endpoint.name }
    }
}
