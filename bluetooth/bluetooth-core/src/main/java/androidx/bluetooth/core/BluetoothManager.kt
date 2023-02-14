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
import android.bluetooth.BluetoothGattServer as FwkBluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback as FwkBluetoothGattServerCallback
import android.bluetooth.BluetoothManager as FwkBluetoothManager
import android.bluetooth.BluetoothProfile as FwkBluetoothProfile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

import androidx.annotation.RequiresFeature
import androidx.annotation.RequiresPermission

/**
 * High level manager used to obtain an instance of an {@link BluetoothAdapter}
 * and to conduct overall Bluetooth Management.
 * <p>
 * Use {@link android.content.Context#getSystemService(java.lang.String)}
 * with {@link Context#BLUETOOTH_SERVICE} to create an {@link BluetoothManager},
 * then call {@link #getAdapter} to obtain the {@link BluetoothAdapter}.
 * </p>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>
 * For more information about using BLUETOOTH, read the <a href=
 * "{@docRoot}guide/topics/connectivity/bluetooth.html">Bluetooth</a> developer
 * guide.
 * </p>
 * </div>
 *
 * @see Context#getSystemService
 * @see BluetoothAdapter#getDefaultAdapter()
 *
 * @hide
 */
// @SystemService(Context.BLUETOOTH_SERVICE)
@RequiresFeature(
    name = PackageManager.FEATURE_BLUETOOTH,
    enforcement = "android.content.pm.PackageManager#hasSystemFeature"
)
class BluetoothManager(context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"
        private const val DBG = false
    }

    private val fwkBluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as FwkBluetoothManager

    /**
     * Get the BLUETOOTH Adapter for this device.
     *
     * @return the BLUETOOTH Adapter
     */
    fun getAdapter(): BluetoothAdapter? {
        return BluetoothAdapter(fwkBluetoothManager.adapter ?: return null)
    }

    /**
     * Get the current connection state of the profile to the remote device.
     *
     *
     * This is not specific to any application configuration but represents
     * the connection state of the local Bluetooth adapter for certain profile.
     * This can be used by applications like status bar which would just like
     * to know the state of Bluetooth.
     *
     * @param device Remote bluetooth device.
     * @param profile GATT or GATT_SERVER
     * @return State of the profile connection. One of [FwkBluetoothProfile.STATE_CONNECTED],
     * [FwkBluetoothProfile.STATE_CONNECTING], [FwkBluetoothProfile.STATE_DISCONNECTED],
     * [FwkBluetoothProfile.STATE_DISCONNECTING]
     * // TODO(ofy) Change FwkBluetoothProfile to core.BluetoothProfile when it is available
     */
//    @RequiresLegacyBluetoothPermission
//    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    // TODO(ofy) Change FwkBluetoothDevice to core.BluetoothDevice when it is available
    fun getConnectionState(device: FwkBluetoothDevice, profile: Int): Int {
        return fwkBluetoothManager.getConnectionState(device, profile)
    }

    /**
     * Get connected devices for the specified profile.
     *
     *
     *  Return the set of devices which are in state [FwkBluetoothProfile.STATE_CONNECTED]
     *  // TODO(ofy) Change FwkBluetoothProfile to core.BluetoothProfile when it is available
     *
     *
     * This is not specific to any application configuration but represents
     * the connection state of Bluetooth for this profile.
     * This can be used by applications like status bar which would just like
     * to know the state of Bluetooth.
     *
     * @param profile GATT or GATT_SERVER
     * @return List of devices. The list will be empty on error.
     */
//    @RequiresLegacyBluetoothPermission
//    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    // TODO(ofy) Change FwkBluetoothDevice to core.BluetoothDevice when it is available
    fun getConnectedDevices(profile: Int): List<FwkBluetoothDevice> {
        return fwkBluetoothManager.getConnectedDevices(profile)
    }

    /**
     * Get a list of devices that match any of the given connection
     * states.
     *
     *
     *  If none of the devices match any of the given states,
     * an empty list will be returned.
     *
     *
     * This is not specific to any application configuration but represents
     * the connection state of the local Bluetooth adapter for this profile.
     * This can be used by applications like status bar which would just like
     * to know the state of the local adapter.
     *
     * @param profile GATT or GATT_SERVER
     * @param states Array of states. States can be one of [FwkBluetoothProfile.STATE_CONNECTED],
     * [FwkBluetoothProfile.STATE_CONNECTING], [FwkBluetoothProfile.STATE_DISCONNECTED],
     * [FwkBluetoothProfile.STATE_DISCONNECTING],
     * // TODO(ofy) Change FwkBluetoothProfile to core.BluetoothProfile when it is available
     * @return List of devices. The list will be empty on error.
     */
//    @RequiresLegacyBluetoothPermission
//    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    // TODO(ofy) Change FwkBluetoothDevice to core.BluetoothDevice when it is available
    fun getDevicesMatchingConnectionStates(
        profile: Int,
        states: IntArray
    ): List<FwkBluetoothDevice> {
        return fwkBluetoothManager.getDevicesMatchingConnectionStates(profile, states)
    }

    /**
     * Open a GATT Server
     * The callback is used to deliver results to Caller, such as connection status as well
     * as the results of any other GATT server operations.
     * The method returns a BluetoothGattServer instance. You can use BluetoothGattServer
     * to conduct GATT server operations.
     *
     * @param context App context
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     * @return BluetoothGattServer instance
     */
//    @RequiresBluetoothConnectPermission
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    // TODO(ofy) Change FwkBluetoothGattServerCallback to core.BluetoothGattServerCallback when it is available
    // TODO(ofy) Change FwkBluetoothGattServer to core.BluetoothGattServer when it is available
    fun openGattServer(
        context: Context,
        callback: FwkBluetoothGattServerCallback
    ): FwkBluetoothGattServer {
        return fwkBluetoothManager.openGattServer(context, callback)
    }
}
