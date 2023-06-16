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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.collection.arrayMapOf
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GattClientImpl {

    private companion object {
        private const val TAG = "GattClientImpl"
        private const val GATT_MAX_MTU = 517
        private val CCCD_UID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private sealed interface CallbackResult {
        class OnCharacteristicRead(
            val characteristic: BluetoothGattCharacteristic,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult

        class OnCharacteristicWrite(
            val characteristic: BluetoothGattCharacteristic,
            val status: Int
        ) : CallbackResult

        class OnDescriptorRead(
            val descriptor: BluetoothGattDescriptor,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult
        class OnDescriptorWrite(
            val descriptor: BluetoothGattDescriptor,
            val status: Int
        ) : CallbackResult
    }

    private interface SubscribeListener {
        fun onCharacteristicNotification(value: ByteArray)
        fun finish()
    }

    @SuppressLint("MissingPermission")
    suspend fun <R> connect(
        context: Context,
        device: BluetoothDevice,
        block: suspend BluetoothLe.GattClientScope.() -> R
    ): R? = coroutineScope {
        val connectResult = CompletableDeferred<Boolean>(parent = coroutineContext.job)
        val finished = Job(parent = coroutineContext.job)
        val callbackResultsFlow = MutableSharedFlow<CallbackResult>(
            extraBufferCapacity = Int.MAX_VALUE)
        val subscribeMap: MutableMap<BluetoothGattCharacteristic, SubscribeListener> = arrayMapOf()
        val subscribeMutex = Mutex()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    gatt?.requestMtu(GATT_MAX_MTU)
                } else {
                    connectResult.complete(false)
                    // TODO(b/270492198): throw precise exception
                    finished.completeExceptionally(IllegalStateException("connect failed"))
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                Log.d(TAG, "onMtuChanged() called with: gatt = $gatt, mtu = $mtu, status = $status")
                if (status == GATT_SUCCESS) {
                    gatt?.discoverServices()
                } else {
                    connectResult.complete(false)
                    // TODO(b/270492198): throw precise exception
                    finished.completeExceptionally(IllegalStateException("mtu request failed"))
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                connectResult.complete(status == GATT_SUCCESS)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                callbackResultsFlow.tryEmit(
                    CallbackResult.OnCharacteristicRead(characteristic, value, status))
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                callbackResultsFlow.tryEmit(
                    CallbackResult.OnCharacteristicWrite(characteristic, status))
            }

            override fun onDescriptorRead(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
                value: ByteArray
            ) {
                callbackResultsFlow.tryEmit(
                    CallbackResult.OnDescriptorRead(descriptor, value, status))
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                callbackResultsFlow.tryEmit(
                    CallbackResult.OnDescriptorWrite(descriptor, status))
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                launch {
                    subscribeMutex.withLock {
                        subscribeMap[characteristic]?.onCharacteristicNotification(value)
                    }
                }
            }
        }
        val bluetoothGatt = device.connectGatt(context, /*autoConnect=*/false, callback)

        if (!connectResult.await()) {
            Log.w(TAG, "Failed to connect to the remote GATT server")
            return@coroutineScope null
        }
        val gattScope = object : BluetoothLe.GattClientScope {
            val taskMutex = Mutex()
            suspend fun<R> runTask(block: suspend () -> R): R {
                taskMutex.withLock {
                    return block()
                }
            }

            override fun getServices(): List<BluetoothGattService> {
                return bluetoothGatt.services
            }

            override fun getService(uuid: UUID): BluetoothGattService? {
                return bluetoothGatt.getService(uuid)
            }

            override suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic):
                Result<ByteArray> {
                return runTask {
                    bluetoothGatt.readCharacteristic(characteristic)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicRead>(
                        callbackResultsFlow) {
                        it.characteristic == characteristic
                    }

                    if (res.status == GATT_SUCCESS) Result.success(res.value)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override suspend fun writeCharacteristic(
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                writeType: Int
            ): Result<Unit> {
                return runTask {
                    bluetoothGatt.writeCharacteristic(characteristic, value, writeType)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicWrite>(
                        callbackResultsFlow) {
                        it.characteristic == characteristic
                    }
                    if (res.status == GATT_SUCCESS) Result.success(Unit)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override suspend fun readDescriptor(descriptor: BluetoothGattDescriptor):
                Result<ByteArray> {
                return runTask {
                    bluetoothGatt.readDescriptor(descriptor)
                    val res = takeMatchingResult<CallbackResult.OnDescriptorRead>(
                        callbackResultsFlow) {
                        it.descriptor == descriptor
                    }

                    if (res.status == GATT_SUCCESS) Result.success(res.value)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override suspend fun writeDescriptor(
                descriptor: BluetoothGattDescriptor,
                value: ByteArray
            ): Result<Unit> {
                return runTask {
                    bluetoothGatt.writeDescriptor(descriptor, value)
                    val res = takeMatchingResult<CallbackResult.OnDescriptorWrite>(
                        callbackResultsFlow) {
                        it.descriptor == descriptor
                    }
                    if (res.status == GATT_SUCCESS) Result.success(Unit)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override fun subscribeToCharacteristic(characteristic: BluetoothGattCharacteristic):
                Flow<ByteArray> {
                val cccd = characteristic.getDescriptor(CCCD_UID) ?: return emptyFlow()

                return callbackFlow {
                    val listener = object : SubscribeListener {
                        override fun onCharacteristicNotification(value: ByteArray) {
                            trySend(value)
                        }
                        override fun finish() {
                            cancel("finished")
                        }
                    }
                    if (!registerSubscribeListener(characteristic, listener)) {
                        cancel("already subscribed")
                    }

                    runTask {
                        bluetoothGatt.setCharacteristicNotification(characteristic, /*enable=*/true)
                        bluetoothGatt.writeDescriptor(
                            cccd,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        )
                        val res = takeMatchingResult<CallbackResult.OnDescriptorWrite>(
                            callbackResultsFlow) {
                            it.descriptor == cccd
                        }
                        if (res.status != GATT_SUCCESS) {
                            cancel(CancellationException("failed to set notification"))
                        }
                    }

                    this.awaitClose {
                        launch {
                            unregisterSubscribeListener(characteristic)
                        }
                        bluetoothGatt.setCharacteristicNotification(characteristic,
                            /*enable=*/false)
                        bluetoothGatt.writeDescriptor(
                            cccd,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    }
                }
            }

            override suspend fun awaitClose(onClosed: () -> Unit) {
                try {
                    // Wait for queued tasks done
                    taskMutex.withLock {
                        subscribeMutex.withLock {
                            subscribeMap.values.forEach { it.finish() }
                        }
                    }
                } finally {
                    onClosed()
                }
            }

            private suspend fun registerSubscribeListener(
                characteristic: BluetoothGattCharacteristic,
                callback: SubscribeListener
            ): Boolean {
                subscribeMutex.withLock {
                    if (subscribeMap.containsKey(characteristic)) {
                        return false
                    }
                    subscribeMap[characteristic] = callback
                    return true
                }
            }

            private suspend fun unregisterSubscribeListener(
                characteristic: BluetoothGattCharacteristic
            ) {
                subscribeMutex.withLock {
                    subscribeMap.remove(characteristic)
                }
            }
        }
        gattScope.block()
    }

    private suspend inline fun<reified R : CallbackResult> takeMatchingResult(
        flow: SharedFlow<CallbackResult>,
        crossinline predicate: (R) -> Boolean
    ): R {
        return flow.filter { it is R && predicate(it) }.first() as R
    }
}
