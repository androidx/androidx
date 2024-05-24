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
import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Entry point for BLE related operations. This class provides a way to perform Bluetooth LE
 * operations such as scanning, advertising, and connection with a respective [BluetoothDevice].
 */
class BluetoothLe(context: Context) {

    companion object {
        /** Advertise started successfully. */
        const val ADVERTISE_STARTED: Int = 10100

        internal lateinit var packageName: String
            private set
    }

    init {
        packageName = context.applicationContext.packageName
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ADVERTISE_STARTED,
    )
    annotation class AdvertiseResult

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as FwkBluetoothManager?
    private val bluetoothAdapter = bluetoothManager?.adapter

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var advertiseImpl: AdvertiseImpl? =
        bluetoothAdapter?.bluetoothLeAdvertiser?.let(::getAdvertiseImpl)

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var scanImpl: ScanImpl? = bluetoothAdapter?.bluetoothLeScanner?.let(::getScanImpl)

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val client: GattClient by
        lazy(LazyThreadSafetyMode.PUBLICATION) { GattClient(context.applicationContext) }

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val server: GattServer by
        lazy(LazyThreadSafetyMode.PUBLICATION) { GattServer(context.applicationContext) }

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE advertising
     *
     * Note that this method may not complete if the duration is set to 0. To stop advertising, in
     * that case, you should cancel the coroutine.
     *
     * @param advertiseParams [AdvertiseParams] for Bluetooth LE advertising.
     * @return a _cold_ [Flow] of [ADVERTISE_STARTED] if advertising is started.
     * @throws AdvertiseException if the advertise fails.
     * @throws IllegalArgumentException if the advertise parameters are not valid.
     */
    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    fun advertise(advertiseParams: AdvertiseParams): Flow<@AdvertiseResult Int> {
        return advertiseImpl?.advertise(advertiseParams)
            ?: callbackFlow { close(AdvertiseException(AdvertiseException.UNSUPPORTED)) }
    }

    /**
     * Returns a _cold_ [Flow] to start Bluetooth LE scanning. Scanning is used to discover
     * advertising devices nearby.
     *
     * @param filters [ScanFilter]s for finding exact Bluetooth LE devices.
     * @return a _cold_ [Flow] of [ScanResult] that matches with the given scan filter.
     * @throws ScanException if the scan fails.
     */
    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    fun scan(filters: List<ScanFilter> = emptyList()): Flow<ScanResult> {
        return scanImpl?.scan(filters)
            ?: callbackFlow { close(ScanException(ScanException.UNSUPPORTED)) }
    }

    /**
     * Connects to the GATT server on the remote Bluetooth device and invokes the given [block]
     * after the connection is made.
     *
     * The block may not be run if connection fails.
     *
     * @param device a [BluetoothDevice] to connect to
     * @param block a block of code that is invoked after the connection is made
     * @return a result returned by the given block if the connection was successfully finished or a
     *   failure with the corresponding reason
     * @throws CancellationException if connect failed or it's canceled
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
     * @see GattServerConnectRequest
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun openGattServer(services: List<GattService>): GattServerConnectFlow {
        return server.open(services)
    }
}
