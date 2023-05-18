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

package androidx.bluetooth.integration.testapp.ui.scanner

// TODO(ofy) Migrate to androidx.bluetooth.ScanResult once in place
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class ScannerViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"

        internal const val NEW_DEVICE = -1
    }

    internal val scanResults: List<ScanResult> get() = _scanResults.values.toList()
    private val _scanResults = mutableMapOf<String, ScanResult>()

    internal val deviceConnections: Set<DeviceConnection> get() = _deviceConnections
    private val _deviceConnections = mutableSetOf<DeviceConnection>()

    override fun onCleared() {
        super.onCleared()

        _deviceConnections.forEach { it.job?.cancel() }
    }

    fun addScanResultIfNew(scanResult: ScanResult): Boolean {
        val deviceAddress = scanResult.device.address

        if (_scanResults.containsKey(deviceAddress)) {
            return false
        }

        _scanResults[deviceAddress] = scanResult
        return true
    }

    fun addDeviceConnectionIfNew(bluetoothDevice: BluetoothDevice): Int {
        val deviceConnection = DeviceConnection(bluetoothDevice)

        val indexOf = _deviceConnections.map { it.bluetoothDevice }.indexOf(bluetoothDevice)
        if (indexOf != -1) {
            // Index 0 is Results page; Tabs for devices start from 1.
            return indexOf + 1
        }

        _deviceConnections.add(deviceConnection)
        return NEW_DEVICE
    }

    fun remove(bluetoothDevice: BluetoothDevice) {
        val deviceConnection = _deviceConnections.find { it.bluetoothDevice == bluetoothDevice }
        deviceConnection?.job?.cancel(ScannerFragment.MANUAL_DISCONNECT)
        deviceConnection?.job = null

        _deviceConnections.remove(deviceConnection)
    }

    fun deviceConnection(position: Int): DeviceConnection {
        // Index 0 is Results page; Tabs for devices start from 1.
        return deviceConnections.elementAt(position - 1)
    }
}

class DeviceConnection(
    val bluetoothDevice: BluetoothDevice
) {
    var job: Job? = null
    var status = Status.NOT_CONNECTED
    var services = emptyList<BluetoothGattService>()
}

enum class Status {
    NOT_CONNECTED, CONNECTING, CONNECTED, CONNECTION_FAILED
}
