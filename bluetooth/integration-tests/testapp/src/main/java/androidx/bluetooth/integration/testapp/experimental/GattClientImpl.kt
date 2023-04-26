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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class GattClientImpl {
    companion object {
        private const val TAG = "GattClientImpl"
        private const val GATT_MAX_MTU = 517
        private val CCCD_UID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private data class ClientTask(
        val taskBlock: () -> Unit
    ) {
        val finished: CompletableDeferred<Boolean> = CompletableDeferred()
        val callbackChannel: Channel<ClientCallback> = Channel()
    }

    private sealed interface ClientCallback {
        class OnCharacteristicRead(
            val characteristic: BluetoothGattCharacteristic,
            val value: ByteArray,
            val status: Int
        ) : ClientCallback

        class OnCharacteristicWrite(
            val characteristic: BluetoothGattCharacteristic,
            val status: Int
        ) : ClientCallback

        class OnDescriptorWrite(
            val descriptor: BluetoothGattDescriptor,
            val status: Int
        ) : ClientCallback
    }

    private interface SubscribeListener {
        fun onCharacteristicNotification(value: ByteArray)
    }

    @SuppressLint("MissingPermission")
    suspend fun <R> connect(
        context: Context,
        device: BluetoothDevice,
        block: BluetoothLe.GattClientScope.() -> R
    ) = coroutineScope {
        val connectResult = CompletableDeferred<Boolean>(parent = coroutineContext.job)
        val finished = Job(parent = coroutineContext.job)
        var currentTask: ClientTask? = null
        val subscribeMap: MutableMap<BluetoothGattCharacteristic, SubscribeListener> = arrayMapOf()

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
                currentTask?.callbackChannel?.trySend(
                    ClientCallback.OnCharacteristicRead(characteristic, value, status))
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                currentTask?.callbackChannel?.trySend(
                    ClientCallback.OnCharacteristicWrite(characteristic, status))
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                currentTask?.callbackChannel?.trySend(
                    ClientCallback.OnDescriptorWrite(descriptor, status))
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                synchronized(subscribeMap) {
                    subscribeMap[characteristic]?.onCharacteristicNotification(value)
                }
            }
        }
        val bluetoothGatt = device.connectGatt(context, /*autoConnect=*/false, callback)
        val tasks: Channel<ClientTask> = Channel(10)

        if (!connectResult.await()) {
            Log.w(TAG, "Failed to connect to the remote GATT server")
            return@coroutineScope
        }
        val gattScope = object : BluetoothLe.GattClientScope {
            suspend fun run() {
                try {
                    tasks.consumeEach { task ->
                        currentTask = task
                        task.taskBlock()
                        task.finished.await()
                        currentTask = null
                    }
                } finally {
                    finished.complete()
                    bluetoothGatt.close()
                    bluetoothGatt.disconnect()
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
                val task = ClientTask {
                    bluetoothGatt.readCharacteristic(characteristic)
                }
                tasks.send(task)
                while (true) {
                    val res = task.callbackChannel.receive()
                    if (res !is ClientCallback.OnCharacteristicRead) continue
                    if (res.characteristic != characteristic) continue

                    task.finished.complete(res.status == GATT_SUCCESS)
                    return if (res.status == GATT_SUCCESS) Result.success(res.value)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override suspend fun writeCharacteristic(
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                writeType: Int
            ): Result<Unit> {
               val task = ClientTask {
                   bluetoothGatt.writeCharacteristic(characteristic, value, writeType)
                }
                tasks.send(task)
                while (true) {
                    val res = task.callbackChannel.receive()
                    if (res !is ClientCallback.OnCharacteristicWrite) continue
                    if (res.characteristic.uuid != characteristic.uuid) continue

                    task.finished.complete(res.status == GATT_SUCCESS)
                    return if (res.status == GATT_SUCCESS) Result.success(Unit)
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
                    }
                    if (!registerSubscribeListener(characteristic, listener)) {
                        cancel(CancellationException("already subscribed"))
                    }

                    val task = ClientTask {
                        bluetoothGatt.setCharacteristicNotification(characteristic, /*enable=*/true)
                        bluetoothGatt.writeDescriptor(
                            cccd,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        )
                    }
                    tasks.send(task)
                    while (true) {
                        val res = task.callbackChannel.receive()
                        if (res !is ClientCallback.OnDescriptorWrite) continue
                        if (res.descriptor != cccd) continue

                        task.finished.complete(res.status == GATT_SUCCESS)
                        if (res.status != GATT_SUCCESS) {
                            cancel(CancellationException("failed to set notification"))
                        }
                        break
                    }

                    this.awaitClose {
                        unregisterSubscribeListener(characteristic)
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
                    tasks.close()
                    finished.join()
                } finally {
                    onClosed()
                }
            }

            private fun registerSubscribeListener(
                characteristic: BluetoothGattCharacteristic,
                callback: SubscribeListener
            ): Boolean {
                synchronized(subscribeMap) {
                    if (subscribeMap.containsKey(characteristic)) {
                        return false
                    }
                    subscribeMap[characteristic] = callback
                    return true
                }
            }

            private fun unregisterSubscribeListener(characteristic: BluetoothGattCharacteristic) {
                synchronized(subscribeMap) {
                    subscribeMap.remove(characteristic)
                }
            }
        }
        coroutineScope {
            launch {
                gattScope.run()
            }
            launch {
                gattScope.block()
            }
        }
    }
}