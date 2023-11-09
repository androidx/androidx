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

import android.bluetooth.BluetoothManager as FwkBluetoothManager
import android.bluetooth.le.BluetoothLeScanner as FwkBluetoothLeScanner
import android.bluetooth.le.ScanCallback as FwkScanCallback
import android.bluetooth.le.ScanResult as FwkScanResult
import android.bluetooth.le.ScanSettings as FwkScanSettings
import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Entry point for BLE related operations. This class provides a way to perform Bluetooth LE
 * operations such as scanning, advertising, and connection with a respective [BluetoothDevice].
 */
class BluetoothLe(context: Context) {

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

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as FwkBluetoothManager?
    private val bluetoothAdapter = bluetoothManager?.adapter

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val client: GattClient by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GattClient(context.applicationContext)
    }

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val server: GattServer by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GattServer(context.applicationContext)
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var advertiseImpl: AdvertiseImpl? =
        bluetoothAdapter?.bluetoothLeAdvertiser?.let(::getAdvertiseImpl)

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var onStartScanListener: OnStartScanListener? = null

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE advertising
     *
     * Note that this method may not complete if the duration is set to 0.
     * To stop advertising, in that case, you should cancel the coroutine.
     *
     * @param advertiseParams [AdvertiseParams] for Bluetooth LE advertising.
     * @return a _cold_ [Flow] of [AdvertiseResult]
     *
     * @throws IllegalArgumentException if the advertise parameters are not valid.
     */
    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    fun advertise(advertiseParams: AdvertiseParams): Flow<@AdvertiseResult Int> {
        return advertiseImpl?.advertise(advertiseParams) ?: flowOf(
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED
        )
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
        val callback = object : FwkScanCallback() {
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
        val scanSettings = FwkScanSettings.Builder().build()
        bleScanner?.startScan(fwkFilters, scanSettings, callback)
        onStartScanListener?.onStartScan(bleScanner)

        awaitClose {
            bleScanner?.stopScan(callback)
        }
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
     * Opens a GATT server.
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
        fun onStartScan(scanner: FwkBluetoothLeScanner?)
    }
}
