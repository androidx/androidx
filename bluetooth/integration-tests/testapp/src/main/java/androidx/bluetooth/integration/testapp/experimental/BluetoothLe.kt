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

package androidx.bluetooth.integration.testapp.experimental

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Singleton class. Entry point for BLE related operations.
 */
class BluetoothLe(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothLe"
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    fun scan(
        settings: ScanSettings
    ): Flow<ScanResult> =
        callbackFlow {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    trySend(result)
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d(TAG, "onScanFailed() called with: errorCode = $errorCode")
                }
            }

            val bluetoothAdapter = bluetoothManager?.adapter
            val bleScanner = bluetoothAdapter?.bluetoothLeScanner

            bleScanner?.startScan(null, settings, callback)

            awaitClose {
                Log.d(TAG, "awaitClose() called")
                bleScanner?.stopScan(callback)
            }
        }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    fun advertise(
        settings: AdvertiseSettings,
        data: AdvertiseData
    ): Flow<AdvertiseResult> =
        callbackFlow {
            val callback = object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    // TODO(ofy) Map to proper errorCodes
                    Log.d(TAG, "onStartFailure() called with: errorCode = $errorCode")
                    trySend(AdvertiseResult.ADVERTISE_FAILED_INTERNAL_ERROR)
                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    trySend(AdvertiseResult.ADVERTISE_STARTED)
                }
            }

            val bluetoothAdapter = bluetoothManager?.adapter
            val bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

            bleAdvertiser?.startAdvertising(settings, data, callback)

            awaitClose {
                Log.d(TAG, "awaitClose() called")
                bleAdvertiser?.stopAdvertising(callback)
            }
        }

    interface GattClientScope {

        fun getServices(): List<BluetoothGattService>
        fun getService(uuid: UUID): BluetoothGattService?

        suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic):
            Result<ByteArray>
        suspend fun writeCharacteristic(
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        ): Result<Unit>
        fun subscribeToCharacteristic(characteristic: BluetoothGattCharacteristic): Flow<ByteArray>
        suspend fun awaitClose(onClosed: () -> Unit)
    }

    suspend fun <R> connectGatt(
        context: Context,
        device: BluetoothDevice,
        block: GattClientScope.() -> R
    ) {
        GattClientImpl().connect(context, device, block)
    }

    @SuppressLint("MissingPermission")
    fun gattServer(): Flow<GattServerCallback> =
        callbackFlow {
            val callback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(
                    device: BluetoothDevice?,
                    status: Int,
                    newState: Int
                ) {
                    trySend(
                        GattServerCallback.OnConnectionStateChange(device, status, newState)
                    )
                }

                override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                    trySend(
                        GattServerCallback.OnServiceAdded(status, service)
                    )
                }

                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    trySend(
                        GattServerCallback.OnCharacteristicReadRequest(
                            device,
                            requestId,
                            offset,
                            characteristic
                        )
                    )
                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    trySend(
                        GattServerCallback.OnCharacteristicWriteRequest(
                            device,
                            requestId,
                            characteristic,
                            preparedWrite,
                            responseNeeded,
                            offset,
                            value
                        )
                    )
                }

                override fun onDescriptorReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    descriptor: BluetoothGattDescriptor?
                ) {
                    trySend(
                        GattServerCallback.OnDescriptorReadRequest(
                            device,
                            requestId,
                            offset,
                            descriptor
                        )
                    )
                }

                override fun onDescriptorWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    descriptor: BluetoothGattDescriptor?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    trySend(
                        GattServerCallback.OnDescriptorWriteRequest(
                            device,
                            requestId,
                            descriptor,
                            preparedWrite,
                            responseNeeded,
                            offset,
                            value
                        )
                    )
                }

                override fun onExecuteWrite(
                    device: BluetoothDevice?,
                    requestId: Int,
                    execute: Boolean
                ) {
                    trySend(
                        GattServerCallback.OnExecuteWrite(device, requestId, execute)
                    )
                }

                override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
                    trySend(
                        GattServerCallback.OnNotificationSent(device, status)
                    )
                }

                override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                    trySend(
                        GattServerCallback.OnMtuChanged(device, mtu)
                    )
                }

                override fun onPhyUpdate(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    trySend(
                        GattServerCallback.OnPhyUpdate(device, txPhy, rxPhy, status)
                    )
                }

                override fun onPhyRead(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    trySend(
                        GattServerCallback.OnPhyRead(device, txPhy, rxPhy, status)
                    )
                }
            }

            val bluetoothGattServer = bluetoothManager?.openGattServer(context, callback)

            awaitClose {
                Log.d(TAG, "awaitClose() called")
                bluetoothGattServer?.close()
            }
        }
}
