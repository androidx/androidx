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
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel

class ScannerViewModel : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"

        internal const val NEW_DEVICE = -1
    }

    internal val results: List<ScanResult> get() = _results.values.toList()
    private val _results = mutableMapOf<String, ScanResult>()

    internal val devices: Set<DeviceConnection> get() = _devices
    private val _devices = mutableSetOf<DeviceConnection>()

    fun addScanResultIfNew(scanResult: ScanResult): Boolean {
        val deviceAddress = scanResult.device.address

        if (_results.containsKey(deviceAddress)) {
            return false
        }

        _results[deviceAddress] = scanResult
        return true
    }

    fun addDeviceConnectionIfNew(scanResult: ScanResult): Int {
        val deviceConnection = DeviceConnection(scanResult)

        val indexOf = _devices.map { it.scanResult }.indexOf(scanResult)
        if (indexOf != -1) {
            // Index 0 is Results page; Tabs for devices start from 1.
            return indexOf + 1
        }

        _devices.add(deviceConnection)
        return NEW_DEVICE
    }

    fun deviceConnection(position: Int): DeviceConnection {
        // Index 0 is Results page; Tabs for devices start from 1.
        return devices.elementAt(position - 1)
    }
}

class DeviceConnection(val scanResult: ScanResult) {
    var status = Status.NOT_CONNECTED
    var services = emptyList<BluetoothGattService>()
}

enum class Status {
    NOT_CONNECTED, CONNECTING, CONNECTED, CONNECTION_FAILED
}
