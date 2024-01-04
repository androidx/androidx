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

import android.bluetooth.le.ScanResult as FwkScanResult
import android.os.ParcelUuid
import java.util.UUID

/**
 * Represents a scan result for Bluetooth LE scan.
 *
 * The ScanResult class is used by Bluetooth LE applications to scan for and discover Bluetooth LE
 * devices. When a Bluetooth LE application scans for devices, it will receive a list of
 * [ScanResult] objects that contain information about the scanned devices. The application can
 * then use this information to determine which devices it wants to connect to.
 *
 * @property device Remote device found
 * @property deviceAddress Bluetooth address for the remote device found
 * @property timestampNanos Device timestamp when the result was last seen
 * @property serviceUuids A list of service UUIDs within advertisement that are used to identify the
 * bluetooth GATT services.
 *
 */
class ScanResult internal constructor(private val fwkScanResult: FwkScanResult) {

    /** Remote Bluetooth device found. */
    val device: BluetoothDevice
        get() = BluetoothDevice(fwkScanResult.device)

    // TODO(kihongs) Find a way to get address type from framework scan result
    /** Bluetooth address for the remote device found. */
    val deviceAddress: BluetoothAddress
        get() = BluetoothAddress(fwkScanResult.device.address,
            BluetoothAddress.ADDRESS_TYPE_UNKNOWN)

    /** Device timestamp when the advertisement was last seen. */
    val timestampNanos: Long
        get() = fwkScanResult.timestampNanos

    /**
     * Returns the manufacturer specific data associated with the manufacturer id.
     *
     * @param manufacturerId The manufacturer id of the scanned device
     * @return the manufacturer specific data associated with the manufacturer id, or @{code null}
     * if the manufacturer specific data is not present
     */
    fun getManufacturerSpecificData(manufacturerId: Int): ByteArray? {
        return fwkScanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
    }

    /**
     * A list of service UUIDs within advertisement that are used to identify the bluetooth GATT
     * services.
     */
    val serviceUuids: List<UUID>
        get() = fwkScanResult.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()

    /**
     * Returns the service data associated with the service UUID.
     *
     * @param serviceUuid The service UUID of the service data
     * @return the service data associated with the specified service UUID, or `null`
     * if the service UUID is not found
     */
    fun getServiceData(serviceUuid: UUID): ByteArray? {
        return fwkScanResult.scanRecord?.getServiceData(ParcelUuid(serviceUuid))
    }

    /**
     * Checks if this object represents a connectable scan result.
     *
     * @return {@code true} if the scanned device is connectable.
     */
    fun isConnectable(): Boolean {
        return fwkScanResult.isConnectable
    }
}
