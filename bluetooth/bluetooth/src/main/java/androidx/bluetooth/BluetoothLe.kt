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

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult as FwkScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.UUID
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Entry point for BLE related operations. This class provides a way to perform Bluetooth LE
 * operations such as scanning, advertising, and connection with a respective [BluetoothDevice].
 *
 */
class BluetoothLe constructor(private val context: Context) {

    private companion object {
        private const val TAG = "BluetoothLe"
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter = bluetoothManager?.adapter

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val client = GattClient(context)
    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val server = GattServer(context)

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE Advertising. When the flow is successfully collected,
     * the operation status [AdvertiseResult] will be delivered via the
     * flow [kotlinx.coroutines.channels.Channel].
     *
     * @param advertiseParams [AdvertiseParams] for Bluetooth LE advertising.
     * @return A _cold_ [Flow] with [AdvertiseResult] status in the data stream.
     */
    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    fun advertise(advertiseParams: AdvertiseParams): Flow<Int> = callbackFlow {
        val callback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.d(TAG, "onStartFailure() called with: errorCode = $errorCode")

                when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        trySend(AdvertiseResult.ADVERTISE_FAILED_DATA_TOO_LARGE)

                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        trySend(AdvertiseResult.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)

                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
                        trySend(AdvertiseResult.ADVERTISE_FAILED_INTERNAL_ERROR)

                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        trySend(AdvertiseResult.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                }
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(TAG, "onStartSuccess() called with: settingsInEffect = $settingsInEffect")

                trySend(AdvertiseResult.ADVERTISE_STARTED)
            }
        }

        val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val advertiseSettings = with(AdvertiseSettings.Builder()) {
            setConnectable(advertiseParams.isConnectable)
            setTimeout(advertiseParams.timeoutMillis)
            // TODO(b/290697177) Add when AndroidX is targeting Android U
//            setDiscoverable(advertiseParams.isDiscoverable)
            build()
        }

        val advertiseData = with(AdvertiseData.Builder()) {
            setIncludeDeviceName(advertiseParams.shouldIncludeDeviceName)
            advertiseParams.serviceData.forEach {
                addServiceData(ParcelUuid(it.key), it.value)
            }
            advertiseParams.manufacturerData.forEach {
                addManufacturerData(it.key, it.value)
            }
            advertiseParams.serviceUuids.forEach {
                addServiceUuid(ParcelUuid(it))
            }
            build()
        }

        Log.d(TAG, "bleAdvertiser.startAdvertising($advertiseSettings, $advertiseData) called")
        bleAdvertiser?.startAdvertising(advertiseSettings, advertiseData, callback)

        awaitClose {
            Log.d(TAG, "bleAdvertiser.stopAdvertising() called")
            bleAdvertiser?.stopAdvertising(callback)
        }
    }

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE scanning. Scanning is used to
     * discover advertising devices nearby.
     *
     * @param filters [ScanFilter]s for finding exact Bluetooth LE devices.
     *
     * @return A _cold_ [Flow] of [ScanResult] that matches with the given scan filter.
     */
    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    fun scan(filters: List<ScanFilter> = emptyList()): Flow<ScanResult> = callbackFlow {
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: FwkScanResult) {
                trySend(ScanResult(result))
            }

            override fun onScanFailed(errorCode: Int) {
                // TODO(b/270492198): throw precise exception
                cancel("onScanFailed() called with: errorCode = $errorCode")
            }
        }

        val bleScanner = bluetoothAdapter?.bluetoothLeScanner
        val fwkFilters = filters.map { it.fwkScanFilter }
        val scanSettings = ScanSettings.Builder().build()
        bleScanner?.startScan(fwkFilters, scanSettings, callback)

        awaitClose {
            bleScanner?.stopScan(callback)
        }
    }

    /**
     * Scope for operations as a GATT client role.
     *
     * @see connectGatt
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface GattClientScope {

        /**
         * Gets the services discovered from the remote device
         */
        fun getServices(): List<GattService>

        /**
         * Gets the service of the remote device by UUID.
         *
         * If multiple instances of the same service exist, the first instance of the service
         * is returned.
         */
        fun getService(uuid: UUID): GattService?

        /**
         * Reads the characteristic value from the server.
         *
         * @param characteristic a remote [GattCharacteristic] to read
         * @return The value of the characteristic
         */
        suspend fun readCharacteristic(characteristic: GattCharacteristic):
            Result<ByteArray>

        /**
         * Writes the characteristic value to the server.
         *
         * @param characteristic a remote [GattCharacteristic] to write
         * @param value a value to be written.
         * @param writeType [GattCharacteristic.WRITE_TYPE_DEFAULT],
         * [GattCharacteristic.WRITE_TYPE_NO_RESPONSE], or
         * [GattCharacteristic.WRITE_TYPE_SIGNED].
         * @return the result of the write operation
         */
        suspend fun writeCharacteristic(
            characteristic: GattCharacteristic,
            value: ByteArray,
            writeType: Int
        ): Result<Unit>

        /**
         * Returns a _cold_ [Flow] that contains the indicated value of the given characteristic.
         */
        fun subscribeToCharacteristic(characteristic: GattCharacteristic): Flow<ByteArray>

        /**
         * Suspends the current coroutine until the pending operations are handled and the connection
         * is closed, then it invokes the given [block] before resuming the coroutine.
         */
        suspend fun awaitClose(block: () -> Unit)
    }

    /**
     * Connects to the GATT server on the remote Bluetooth device and
     * invokes the given [block] after the connection is made.
     *
     * The block may not be run if connection fails.
     *
     * @param device a [BluetoothDevice] to connect to
     * @param block a block of code that is invoked after the connection is made.
     *
     * @return a result returned by the given block if the connection was successfully finished
     *         or an failure with the corresponding reason.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    suspend fun <R> connectGatt(
        device: BluetoothDevice,
        block: suspend GattClientScope.() -> R
    ): Result<R> {
        return client.connect(device, block)
    }

    /**
     * Represents a client connection request from a remote device.
     *
     * @property device The remote device connecting to the server.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class GattServerConnectionRequest internal constructor(
        val device: BluetoothDevice,
        private val server: GattServer,
        internal val session: GattServer.Session,
    ) {
        /**
         * Accepts the connection request and handles incoming requests after that.
         *
         * Requests before calling this should be saved.
         *
         * @see GattServerScope
         */
        suspend fun accept(block: suspend GattServerScope.() -> Unit) {
            return server.acceptConnection(this, block)
        }

        /**
         * Rejects the connection request.
         */
        fun reject() {
            return server.rejectConnection(this)
        }
    }

    /**
     * A scope for operations as a GATT server role.
     *
     * Collect [requests] to respond with requests from the client.
     *
     * @see GattServerConnectionRequest#accept()
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface GattServerScope {
        /**
         * A client device connected to the server.
         */
        val device: BluetoothDevice

        /**
         * A _hot_ [Flow] of incoming requests from the client.
         *
         * A request is either [GattServerRequest.ReadCharacteristicRequest] or
         * [GattServerRequest.WriteCharacteristicRequest]
         */
        val requests: Flow<GattServerRequest>

        /**
         * Notifies a client of a characteristic value change.
         *
         * @param characteristic the updated characteristic.
         * @param value the new value of the characteristic.
         */
        fun notify(characteristic: GattCharacteristic, value: ByteArray)
    }

    /**
     * Opens a GATT server.
     *
     * It returns a _cold_ [Flow] of connection requests.
     * If the flow is cancelled, the server will be closed.
     *
     * Only one server at a time can be opened.
     *
     * @param services the services that will be exposed to the clients.
     *
     * @see GattServerConnectionRequest
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun openGattServer(services: List<GattService>): Flow<GattServerConnectionRequest> {
        return server.open(services)
    }

    /**
     * Updates the services of the opened GATT server.
     * It will be ignored if there is no opened server.
     *
     * @param services the new services that will be notified to the clients.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun updateServices(services: List<GattService>) {
        server.updateServices(services)
    }
}
