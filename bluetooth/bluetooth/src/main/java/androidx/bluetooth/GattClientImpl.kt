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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattDescriptor as FwkDescriptor
import android.bluetooth.BluetoothGattService as FwkService
import android.content.Context
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.CancellationException
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

/**
 * A class for handling operations as a GATT client role.
 */
internal class GattClientImpl {
    private companion object {
        private const val TAG = "GattClientImpl"

        /**
         * The maximum ATT size(512) + header(3)
         */
        private const val GATT_MAX_MTU = 515
        private val CCCD_UID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private sealed interface CallbackResult {
        class OnCharacteristicRead(
            val characteristic: GattCharacteristic,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult

        class OnCharacteristicWrite(
            val characteristic: GattCharacteristic,
            val status: Int
        ) : CallbackResult

        class OnDescriptorRead(
            val descriptor: FwkDescriptor,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult

        class OnDescriptorWrite(
            val descriptor: FwkDescriptor,
            val status: Int
        ) : CallbackResult
    }

    private interface SubscribeListener {
        fun onCharacteristicNotification(value: ByteArray)
        fun finish()
    }

    /**
     * A mapping from framework instances to BluetoothX instances.
     */
    private class AttributeMap {
        val services: MutableMap<FwkService, GattService> = mutableMapOf()
        val characteristics: MutableMap<FwkCharacteristic, GattCharacteristic> =
            mutableMapOf()
        fun update(services: List<FwkService>) {
            this.services.clear()
            characteristics.clear()

            services.forEach { serv ->
                this.services[serv] = GattService(serv)
                serv.characteristics.forEach { char ->
                    characteristics[char] = GattCharacteristic(char)
                }
            }
        }

        fun getServices(): List<GattService> {
            return services.values.toList()
        }

        fun fromFwkService(service: FwkService): GattService? {
            return services[service]
        }

        fun fromFwkCharacteristic(characteristic: FwkCharacteristic): GattCharacteristic? {
            return characteristics[characteristic]
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun <R> connect(
        context: Context,
        device: BluetoothDevice,
        block: suspend BluetoothLe.GattClientScope.() -> R
    ): R? = coroutineScope {
        val connectResult = CompletableDeferred<Boolean>(parent = coroutineContext.job)
        val finished = Job(parent = coroutineContext.job)
        val callbackResultsFlow =
            MutableSharedFlow<CallbackResult>(extraBufferCapacity = Int.MAX_VALUE)
        val subscribeMap: MutableMap<FwkCharacteristic, SubscribeListener> = mutableMapOf()
        val subscribeMutex = Mutex()
        val attributeMap = AttributeMap()

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
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt?.discoverServices()
                } else {
                    connectResult.complete(false)
                    // TODO(b/270492198): throw precise exception
                    finished.completeExceptionally(IllegalStateException("mtu request failed"))
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                gatt?.let {
                    attributeMap.update(it.services)
                }
                connectResult.complete(status == BluetoothGatt.GATT_SUCCESS)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: FwkCharacteristic,
                value: ByteArray,
                status: Int
            ) {
               attributeMap.fromFwkCharacteristic(characteristic)?.let {
                   callbackResultsFlow.tryEmit(
                       CallbackResult.OnCharacteristicRead(it, value, status)
                   )
               }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: FwkCharacteristic,
                status: Int
            ) {
                attributeMap.fromFwkCharacteristic(characteristic)?.let {
                    callbackResultsFlow.tryEmit(
                        CallbackResult.OnCharacteristicWrite(it, status)
                    )
                }
            }

            override fun onDescriptorRead(
                gatt: BluetoothGatt,
                descriptor: FwkDescriptor,
                status: Int,
                value: ByteArray
            ) {
                callbackResultsFlow.tryEmit(
                    CallbackResult.OnDescriptorRead(descriptor, value, status))
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: FwkDescriptor,
                status: Int
            ) {
                callbackResultsFlow.tryEmit(CallbackResult.OnDescriptorWrite(descriptor, status))
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: FwkCharacteristic,
                value: ByteArray
            ) {
                launch {
                    subscribeMutex.withLock {
                        subscribeMap[characteristic]?.onCharacteristicNotification(value)
                    }
                }
            }
        }
        val bluetoothGatt = device.fwkDevice.connectGatt(context, /*autoConnect=*/false, callback)

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

            override fun getServices(): List<GattService> {
                return attributeMap.getServices()
            }

            override fun getService(uuid: UUID): GattService? {
                return bluetoothGatt.getService(uuid)?.let { attributeMap.fromFwkService(it) }
            }

            override suspend fun readCharacteristic(characteristic: GattCharacteristic):
                Result<ByteArray> {
                if (characteristic.properties and GattCharacteristic.PROPERTY_READ == 0) {
                    return Result.failure(IllegalArgumentException("can't read the characteristic"))
                }
                return runTask {
                    bluetoothGatt.readCharacteristic(characteristic.fwkCharacteristic)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicRead>(
                        callbackResultsFlow) {
                        it.characteristic == characteristic
                    }

                    if (res.status == BluetoothGatt.GATT_SUCCESS) Result.success(res.value)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override suspend fun writeCharacteristic(
                characteristic: GattCharacteristic,
                value: ByteArray,
                writeType: Int
            ): Result<Unit> {
                if (characteristic.properties and GattCharacteristic.PROPERTY_WRITE == 0) {
                    return Result.failure(
                        IllegalArgumentException("can't write to the characteristic"))
                }
                return runTask {
                    bluetoothGatt.writeCharacteristic(
                        characteristic.fwkCharacteristic, value, writeType)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicWrite>(
                        callbackResultsFlow) {
                        it.characteristic == characteristic
                    }
                    if (res.status == BluetoothGatt.GATT_SUCCESS) Result.success(Unit)
                    else Result.failure(RuntimeException("fail"))
                }
            }

            override fun subscribeToCharacteristic(characteristic: GattCharacteristic):
                Flow<ByteArray> {
                if (characteristic.properties and GattCharacteristic.PROPERTY_NOTIFY == 0) {
                    return emptyFlow()
                }
                val cccd = characteristic.fwkCharacteristic.getDescriptor(CCCD_UID)
                    ?: return emptyFlow()

                return callbackFlow {
                    val listener = object : SubscribeListener {
                        override fun onCharacteristicNotification(value: ByteArray) {
                            trySend(value)
                        }
                        override fun finish() {
                            cancel("finished")
                        }
                    }
                    if (!registerSubscribeListener(characteristic.fwkCharacteristic, listener)) {
                        cancel("already subscribed")
                    }

                    runTask {
                        bluetoothGatt.setCharacteristicNotification(
                            characteristic.fwkCharacteristic, /*enable=*/true)
                        bluetoothGatt.writeDescriptor(
                            cccd,
                            FwkDescriptor.ENABLE_NOTIFICATION_VALUE
                        )
                        val res = takeMatchingResult<CallbackResult.OnDescriptorWrite>(
                            callbackResultsFlow) {
                            it.descriptor == cccd
                        }
                        if (res.status != BluetoothGatt.GATT_SUCCESS) {
                            cancel(CancellationException("failed to set notification"))
                        }
                    }

                    this.awaitClose {
                        launch {
                            unregisterSubscribeListener(characteristic.fwkCharacteristic)
                        }
                        bluetoothGatt.setCharacteristicNotification(
                            characteristic.fwkCharacteristic, /*enable=*/false)
                        bluetoothGatt.writeDescriptor(
                            cccd,
                            FwkDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    }
                }
            }

            override suspend fun awaitClose(block: () -> Unit) {
                try {
                    // Wait for queued tasks done
                    taskMutex.withLock {
                        subscribeMutex.withLock {
                            subscribeMap.values.forEach { it.finish() }
                        }
                    }
                } finally {
                    block()
                }
            }

            private suspend fun registerSubscribeListener(
                characteristic: FwkCharacteristic,
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
                characteristic: FwkCharacteristic
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
