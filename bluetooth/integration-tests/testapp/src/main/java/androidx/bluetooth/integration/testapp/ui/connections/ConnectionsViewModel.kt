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

package androidx.bluetooth.integration.testapp.ui.connections

import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.cancel

class ConnectionsViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ConnectionsViewModel"

        internal const val NEW_DEVICE = -1
    }

    internal val deviceConnections: Set<DeviceConnection> get() = _deviceConnections
    private val _deviceConnections = mutableSetOf<DeviceConnection>()

    override fun onCleared() {
        super.onCleared()

        _deviceConnections.forEach { it.job?.cancel() }
    }

    fun addDeviceConnectionIfNew(bluetoothDevice: BluetoothDevice): Int {
        val deviceConnection = DeviceConnection(bluetoothDevice)

        val indexOf = _deviceConnections.map { it.bluetoothDevice }.indexOf(bluetoothDevice)
        if (indexOf != -1) {
            return indexOf
        }

        _deviceConnections.add(deviceConnection)
        return NEW_DEVICE
    }

    fun remove(bluetoothDevice: BluetoothDevice) {
        val deviceConnection = _deviceConnections.find { it.bluetoothDevice == bluetoothDevice }
        deviceConnection?.job?.cancel(ConnectionsFragment.MANUAL_DISCONNECT)
        deviceConnection?.job = null

        _deviceConnections.remove(deviceConnection)
    }

    fun deviceConnection(position: Int): DeviceConnection {
        return deviceConnections.elementAt(position)
    }
}
