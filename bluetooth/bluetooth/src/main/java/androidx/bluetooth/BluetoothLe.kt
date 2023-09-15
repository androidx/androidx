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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult as FwkScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.job

/**
 * Entry point for BLE related operations. This class provides a way to perform Bluetooth LE
 * operations such as scanning, advertising, and connection with a respective [BluetoothDevice].
 */
class BluetoothLe constructor(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothLe"

        /** Advertise started successfully. */
        const val ADVERTISE_STARTED: Int = 101

        /** Advertise failed to start because the data is too large. */
        const val ADVERTISE_FAILED_DATA_TOO_LARGE: Int = 102

        /** Advertise failed to start because the advertise feature is not supported. */
        const val ADVERTISE_FAILED_FEATURE_UNSUPPORTED: Int = 103

        /** Advertise failed to start because of an internal error. */
        const val ADVERTISE_FAILED_INTERNAL_ERROR: Int = 104

        /** Advertise failed to start because of too many advertisers. */
        const val ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: Int = 105
    }

    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ADVERTISE_STARTED,
        ADVERTISE_FAILED_DATA_TOO_LARGE,
        ADVERTISE_FAILED_FEATURE_UNSUPPORTED,
        ADVERTISE_FAILED_INTERNAL_ERROR,
        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
    )
    annotation class AdvertiseResult

    @RequiresApi(34)
    private object BluetoothLeApi34Impl {
        @JvmStatic
        @DoNotInline
        fun setDiscoverable(
            builder: AdvertiseSettings.Builder,
            isDiscoverable: Boolean
        ): AdvertiseSettings.Builder {
            builder.setDiscoverable(isDiscoverable)
            return builder
        }
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter = bluetoothManager?.adapter

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val client: GattClient by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GattClient(context)
    }
    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val server: GattServer by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GattServer(context)
    }

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var onStartScanListener: OnStartScanListener? = null

    /**
     * Starts Bluetooth LE advertising
     *
     * Note that this method may not complete if the duration is set to 0.
     * To stop advertising, in that case, you should cancel the coroutine.
     *
     * @param advertiseParams [AdvertiseParams] for Bluetooth LE advertising.
     * @param block an optional block of code that is invoked when advertising is started or failed.
     *
     * @throws IllegalArgumentException if the advertise parameters are not valid.
     */
    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    suspend fun advertise(
        advertiseParams: AdvertiseParams,
        block: (suspend (@AdvertiseResult Int) -> Unit)? = null
    ) {
        val result = CompletableDeferred<Int>()

        val callback = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.d(TAG, "onStartFailure() called with: errorCode = $errorCode")

                when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        result.complete(BluetoothLe.ADVERTISE_FAILED_DATA_TOO_LARGE)

                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        result.complete(BluetoothLe.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)

                    ADVERTISE_FAILED_INTERNAL_ERROR ->
                        result.complete(BluetoothLe.ADVERTISE_FAILED_INTERNAL_ERROR)

                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        result.complete(BluetoothLe.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                }
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                result.complete(ADVERTISE_STARTED)
            }
        }

        val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val advertiseSettings = with(AdvertiseSettings.Builder()) {
            setConnectable(advertiseParams.isConnectable)
            advertiseParams.durationMillis.let {
                if (it !in 0..655350)
                    throw IllegalArgumentException("advertise duration must be in [0, 655350]")
                setTimeout(it)
            }
            if (Build.VERSION.SDK_INT >= 34) {
                BluetoothLeApi34Impl.setDiscoverable(this, advertiseParams.isDiscoverable)
            }
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

        bleAdvertiser?.startAdvertising(advertiseSettings, advertiseData, callback)

        coroutineContext.job.invokeOnCompletion {
            bleAdvertiser?.stopAdvertising(callback)
        }
        result.await().let {
            block?.invoke(it)
            if (it == ADVERTISE_STARTED) {
                if (advertiseParams.durationMillis > 0) {
                    delay(advertiseParams.durationMillis.toLong())
                } else {
                    awaitCancellation()
                }
            }
        }
    }

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE scanning.
     * Scanning is used to discover advertising devices nearby.
     *
     * @param filters [ScanFilter]s for finding exact Bluetooth LE devices
     *
     * @return a _cold_ [Flow] of [ScanResult] that matches with the given scan filter
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
        onStartScanListener?.onStartScan(bleScanner)

        awaitClose {
            bleScanner?.stopScan(callback)
        }
    }

    /**
     * Scope for operations as a GATT client role.
     *
     * @see BluetoothLe.connectGatt
     */
    interface GattClientScope {

        /**
         * A flow of GATT services discovered from the remote device.
         *
         * If the services of the remote device has changed, the new services will be
         * discovered and emitted automatically.
         */
        val servicesFlow: StateFlow<List<GattService>>

        /**
         * GATT services recently discovered from the remote device.
         *
         * Note that this can be changed, subscribe to [servicesFlow] to get notified
         * of services changes.
         */
        val services: List<GattService> get() = servicesFlow.value

        /**
         * Gets the service of the remote device by UUID.
         *
         * If multiple instances of the same service exist, the first instance of the services
         * is returned.
         */
        fun getService(uuid: UUID): GattService?

        /**
         * Reads the characteristic value from the server.
         *
         * @param characteristic a remote [GattCharacteristic] to read
         * @return the value of the characteristic
         */
        suspend fun readCharacteristic(characteristic: GattCharacteristic):
            Result<ByteArray>

        /**
         * Writes the characteristic value to the server.
         *
         * @param characteristic a remote [GattCharacteristic] to write
         * @param value a value to be written.
         * @return the result of the write operation
         */
        suspend fun writeCharacteristic(
            characteristic: GattCharacteristic,
            value: ByteArray
        ): Result<Unit>

        /**
         * Returns a _cold_ [Flow] that contains the indicated value of the given characteristic.
         */
        fun subscribeToCharacteristic(characteristic: GattCharacteristic): Flow<ByteArray>
    }

    /**
     * Connects to the GATT server on the remote Bluetooth device and
     * invokes the given [block] after the connection is made.
     *
     * The block may not be run if connection fails.
     *
     * @param device a [BluetoothDevice] to connect to
     * @param block a block of code that is invoked after the connection is made
     *
     * @throws CancellationException if connect failed or it's canceled
     * @return a result returned by the given block if the connection was successfully finished
     *         or a failure with the corresponding reason
     *
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    suspend fun <R> connectGatt(
        device: BluetoothDevice,
        block: suspend GattClientScope.() -> R
    ): R {
        return client.connect(device, block)
    }

    /**
     * A scope for handling connect requests from remote devices.
     *
     * @property connectRequests connect requests from remote devices.
     *
     * @see BluetoothLe#openGattServer
     */
    interface GattServerConnectScope {
        /**
         * A _hot_ flow of [GattServerConnectRequest].
         */
        val connectRequests: Flow<GattServerConnectRequest>

        /**
         * Updates the services of the opened GATT server.
         *
         * @param services the new services that will be notified to the clients.
         */
        fun updateServices(services: List<GattService>)
    }

    /**
     * A scope for operations as a GATT server role.
     *
     * A scope is created for each remote device.
     *
     * Collect [requests] to respond with requests from the client.
     *
     * @see GattServerConnectRequest#accept()
     */
    interface GattServerSessionScope {
        /**
         * A client device connected to the server.
         */
        val device: BluetoothDevice

        /**
         * A _hot_ [Flow] of incoming requests from the client.
         *
         * A request is either [GattServerRequest.ReadCharacteristic] or
         * [GattServerRequest.WriteCharacteristics]
         */
        val requests: Flow<GattServerRequest>

        /**
         * Notifies a client of a characteristic value change.
         *
         * @param characteristic the updated characteristic
         * @param value the new value of the characteristic
         *
         * @return `true` if the notification sent successfully, otherwise `false`
         */
        suspend fun notify(characteristic: GattCharacteristic, value: ByteArray): Boolean
    }

    /**
     * Represents a connect request from a remote device.
     *
     * @property device the remote device connecting to the server
     */
    class GattServerConnectRequest internal constructor(
        private val session: GattServer.Session,
    ) {
        val device: BluetoothDevice
            get() = session.device
        /**
         * Accepts the connect request and handles incoming requests after that.
         *
         * Requests from the client before calling this should be saved.
         *
         * @param block a block of code that is invoked after the connection is made.
         *
         * @see GattServerSessionScope
         */
        suspend fun accept(block: suspend GattServerSessionScope.() -> Unit) {
            return session.acceptConnection(block)
        }

        /**
         * Rejects the connect request.
         *
         * All the requests from the client will be rejected.
         */
        fun reject() {
            return session.rejectConnection()
        }
    }

    /**
     * Opens a GATT server.
     *
     *
     * Only one server at a time can be opened.
     *
     * @param services the services that will be exposed to the clients
     * @param block a block of code that is invoked after the server is opened
     *
     * @see GattServerConnectRequest
     */
    suspend fun <R> openGattServer(
        services: List<GattService>,
        block: suspend GattServerConnectScope.() -> R
    ): R {
        return server.open(services, block)
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun interface OnStartScanListener {
        fun onStartScan(scanner: BluetoothLeScanner?)
    }
}
