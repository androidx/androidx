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
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.bluetooth.utils.addressType
import java.util.UUID

/**
 * Represents a scan result for Bluetooth LE scan.
 *
 * The ScanResult class is used by Bluetooth LE applications to scan for and discover Bluetooth LE
 * devices. When a Bluetooth LE application scans for devices, it will receive a list of
 * [ScanResult] objects that contain information about the scanned devices. The application can then
 * use this information to determine which devices it wants to connect to.
 *
 * @property device Remote device found
 * @property deviceAddress Bluetooth address for the remote device found
 * @property timestampNanos Device timestamp when the result was last seen
 * @property serviceUuids A list of service UUIDs within advertisement that are used to identify the
 *   bluetooth GATT services.
 */
class ScanResult
@RestrictTo(RestrictTo.Scope.LIBRARY)
constructor(private val fwkScanResult: FwkScanResult) {

    companion object {
        /** Periodic advertising interval is not present in the packet. */
        const val PERIODIC_INTERVAL_NOT_PRESENT: Int = FwkScanResult.PERIODIC_INTERVAL_NOT_PRESENT
    }

    @RequiresApi(29)
    private object ScanResultApi29Impl {
        @JvmStatic
        fun serviceSolicitationUuids(fwkScanResult: FwkScanResult): List<ParcelUuid> =
            fwkScanResult.scanRecord?.serviceSolicitationUuids.orEmpty()
    }

    @RequiresApi(26)
    private object ScanResultApi26Impl {
        @JvmStatic
        fun isConnectable(fwkScanResult: FwkScanResult): Boolean = fwkScanResult.isConnectable

        @JvmStatic
        fun periodicAdvertisingInterval(fwkScanResult: FwkScanResult): Long =
            (fwkScanResult.periodicAdvertisingInterval * 1.25).toLong()
    }

    /** Remote Bluetooth device found. */
    val device: BluetoothDevice = BluetoothDevice(fwkScanResult.device)

    /** Bluetooth address for the remote device found. */
    val deviceAddress: BluetoothAddress =
        BluetoothAddress(fwkScanResult.device.address, fwkScanResult.device.addressType())

    /** Device timestamp when the advertisement was last seen. */
    val timestampNanos: Long
        get() = fwkScanResult.timestampNanos

    /**
     * Returns the manufacturer specific data associated with the manufacturer id.
     *
     * @param manufacturerId The manufacturer id of the scanned device
     * @return the manufacturer specific data associated with the manufacturer id, or @{code null}
     *   if the manufacturer specific data is not present
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
     * Returns a list of service solicitation UUIDs within the advertisement that are used to
     * identify the Bluetooth GATT services.
     *
     * Please note that this will return an `emptyList()` on versions before
     * [android.os.Build.VERSION_CODES.Q].
     */
    val serviceSolicitationUuids: List<ParcelUuid>
        get() =
            if (Build.VERSION.SDK_INT >= 29) {
                ScanResultApi29Impl.serviceSolicitationUuids(fwkScanResult)
            } else {
                emptyList()
            }

    /** Returns a map of service UUID and its corresponding service data. */
    val serviceData: Map<ParcelUuid, ByteArray>
        get() = fwkScanResult.scanRecord?.serviceData.orEmpty()

    /**
     * Returns the service data associated with the service UUID.
     *
     * @param serviceUuid The service UUID of the service data
     * @return the service data associated with the specified service UUID, or `null` if the service
     *   UUID is not found
     */
    fun getServiceData(serviceUuid: UUID): ByteArray? {
        return fwkScanResult.scanRecord?.getServiceData(ParcelUuid(serviceUuid))
    }

    /**
     * Checks if this object represents a connectable scan result.
     *
     * @return {@code true} if the scanned device is connectable.
     *
     * Please note that this will return {@code true} on versions before
     * [android.os.Build.VERSION_CODES.Q].
     */
    fun isConnectable(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) {
            ScanResultApi26Impl.isConnectable(fwkScanResult)
        } else {
            true
        }
    }

    /** Returns the received signal strength in dBm. The valid range is [-127, 126]. */
    val rssi: Int
        get() = fwkScanResult.rssi

    /**
     * Returns the periodic advertising interval in milliseconds ranging from 7.5ms to 81918.75ms A
     * value of [PERIODIC_INTERVAL_NOT_PRESENT] means periodic advertising interval is not present.
     *
     * Please note that this will return [PERIODIC_INTERVAL_NOT_PRESENT] on versions before
     * [android.os.Build.VERSION_CODES.Q].
     */
    val periodicAdvertisingInterval: Long
        get() =
            if (Build.VERSION.SDK_INT >= 26) {
                // Framework returns interval in units of 1.25ms.
                ScanResultApi26Impl.periodicAdvertisingInterval(fwkScanResult)
            } else {
                PERIODIC_INTERVAL_NOT_PRESENT.toLong()
            }
}
