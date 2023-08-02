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

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice as FwkDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattDescriptor as FwkDescriptor
import android.bluetooth.BluetoothGattService as FwkService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GattClient(private val context: Context) {
    interface FrameworkAdapter {
        var bluetoothGatt: BluetoothGatt?
        fun connectGatt(
            context: Context,
            device: FwkDevice,
            callback: BluetoothGattCallback
        ): Boolean
        fun requestMtu(mtu: Int)

        fun discoverServices()

        fun getServices(): List<FwkService>
        fun getService(uuid: UUID): FwkService?
        fun readCharacteristic(characteristic: FwkCharacteristic)
        fun writeCharacteristic(
            characteristic: FwkCharacteristic,
            value: ByteArray,
            writeType: Int
        )

        fun writeDescriptor(descriptor: FwkDescriptor, value: ByteArray)

        fun setCharacteristicNotification(characteristic: FwkCharacteristic, enable: Boolean)
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    companion object {
        private const val TAG = "GattClient"

        /**
         * The maximum ATT size(512) + header(3)
         */
        private const val GATT_MAX_MTU = 515
        private val CCCD_UID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    @SuppressLint("ObsoleteSdkInt")
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkAdapter: FrameworkAdapter =
        if (Build.VERSION.SDK_INT >= 33) FrameworkAdapterApi33()
        else FrameworkAdapterBase()

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

    @SuppressLint("MissingPermission")
    suspend fun <R> connect(
        device: BluetoothDevice,
        block: suspend BluetoothLe.GattClientScope.() -> R
    ): Result<R> = coroutineScope {
        val connectResult = CompletableDeferred<Unit>(parent = coroutineContext.job)
        val callbackResultsFlow =
            MutableSharedFlow<CallbackResult>(extraBufferCapacity = Int.MAX_VALUE)
        val subscribeMap: MutableMap<FwkCharacteristic, SubscribeListener> = mutableMapOf()
        val subscribeMutex = Mutex()
        val attributeMap = AttributeMap()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    fwkAdapter.requestMtu(GATT_MAX_MTU)
                } else {
                    connectResult.cancel("connect failed")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    fwkAdapter.discoverServices()
                } else {
                    connectResult.cancel("mtu request failed")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                attributeMap.updateWithFrameworkServices(fwkAdapter.getServices())
                if (status == BluetoothGatt.GATT_SUCCESS) connectResult.complete(Unit)
                else connectResult.cancel("service discover failed")
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
                    CallbackResult.OnDescriptorRead(descriptor, value, status)
                )
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
        if (!fwkAdapter.connectGatt(context, device.fwkDevice, callback)) {
            return@coroutineScope Result.failure(CancellationException("failed to connect"))
        }

        try {
            connectResult.await()
        } catch (e: Throwable) {
            return@coroutineScope Result.failure(e)
        }
        val gattScope = object : BluetoothLe.GattClientScope {
            val taskMutex = Mutex()
            suspend fun <R> runTask(block: suspend () -> R): R {
                taskMutex.withLock {
                    return block()
                }
            }

            override fun getServices(): List<GattService> {
                return attributeMap.getServices()
            }

            override fun getService(uuid: UUID): GattService? {
                return fwkAdapter.getService(uuid)?.let { attributeMap.fromFwkService(it) }
            }

            override suspend fun readCharacteristic(characteristic: GattCharacteristic):
                Result<ByteArray> {
                if (characteristic.properties and GattCharacteristic.PROPERTY_READ == 0) {
                    return Result.failure(IllegalArgumentException("can't read the characteristic"))
                }
                return runTask {
                    fwkAdapter.readCharacteristic(characteristic.fwkCharacteristic)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicRead>(
                        callbackResultsFlow
                    ) {
                        it.characteristic == characteristic
                    }

                    if (res.status == BluetoothGatt.GATT_SUCCESS) Result.success(res.value)
                    // TODO: throw precise reason if we can gather the info
                    else Result.failure(CancellationException("fail"))
                }
            }

            override suspend fun writeCharacteristic(
                characteristic: GattCharacteristic,
                value: ByteArray,
                writeType: Int
            ): Result<Unit> {
                if (characteristic.properties and GattCharacteristic.PROPERTY_WRITE == 0) {
                    return Result.failure(
                        IllegalArgumentException("can't write to the characteristic")
                    )
                }
                return runTask {
                    fwkAdapter.writeCharacteristic(
                        characteristic.fwkCharacteristic, value, writeType)
                    val res = takeMatchingResult<CallbackResult.OnCharacteristicWrite>(
                        callbackResultsFlow
                    ) {
                        it.characteristic == characteristic
                    }
                    if (res.status == BluetoothGatt.GATT_SUCCESS) Result.success(Unit)
                    // TODO: throw precise reason if we can gather the info
                    else Result.failure(CancellationException("fail"))
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
                            close()
                        }
                    }
                    if (!registerSubscribeListener(characteristic.fwkCharacteristic, listener)) {
                        throw IllegalStateException("already subscribed")
                    }

                    runTask {
                        fwkAdapter.setCharacteristicNotification(
                            characteristic.fwkCharacteristic, /*enable=*/true
                        )

                        fwkAdapter.writeDescriptor(cccd, FwkDescriptor.ENABLE_NOTIFICATION_VALUE)
                        val res = takeMatchingResult<CallbackResult.OnDescriptorWrite>(
                            callbackResultsFlow
                        ) {
                            it.descriptor == cccd
                        }
                        if (res.status != BluetoothGatt.GATT_SUCCESS) {
                            cancel("failed to set notification")
                        }
                    }

                    this.awaitClose {
                        launch {
                            unregisterSubscribeListener(characteristic.fwkCharacteristic)
                        }
                        fwkAdapter.setCharacteristicNotification(
                            characteristic.fwkCharacteristic, /*enable=*/false
                        )

                        fwkAdapter.writeDescriptor(cccd, FwkDescriptor.DISABLE_NOTIFICATION_VALUE)
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
        try {
            Result.success(gattScope.block())
        } catch (e: CancellationException) {
            Result.failure(e)
        }
    }

    private suspend inline fun <reified R : CallbackResult> takeMatchingResult(
        flow: SharedFlow<CallbackResult>,
        crossinline predicate: (R) -> Boolean
    ): R {
        return flow.filter { it is R && predicate(it) }.first() as R
    }

    private open class FrameworkAdapterBase : FrameworkAdapter {
        override var bluetoothGatt: BluetoothGatt? = null

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun connectGatt(
            context: Context,
            device: FwkDevice,
            callback: BluetoothGattCallback
        ): Boolean {
            bluetoothGatt = device.connectGatt(context, /*autoConnect=*/false, callback)
            return bluetoothGatt != null
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun requestMtu(mtu: Int) {
            bluetoothGatt?.requestMtu(mtu)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun discoverServices() {
            bluetoothGatt?.discoverServices()
        }

        override fun getServices(): List<FwkService> {
            return bluetoothGatt?.services ?: listOf()
        }

        override fun getService(uuid: UUID): FwkService? {
            return bluetoothGatt?.getService(uuid)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun readCharacteristic(characteristic: FwkCharacteristic) {
            bluetoothGatt?.readCharacteristic(characteristic)
        }

        @Suppress("DEPRECATION")
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeCharacteristic(
            characteristic: FwkCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            characteristic.value = value
            bluetoothGatt?.writeCharacteristic(characteristic)
        }

        @Suppress("DEPRECATION")
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeDescriptor(descriptor: FwkDescriptor, value: ByteArray) {
            descriptor.value = value
            bluetoothGatt?.writeDescriptor(descriptor)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun setCharacteristicNotification(
            characteristic: FwkCharacteristic,
            enable: Boolean
        ) {
            bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        }
    }

    private open class FrameworkAdapterApi33 : FrameworkAdapterBase() {
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeCharacteristic(
            characteristic: FwkCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            bluetoothGatt?.writeCharacteristic(characteristic, value, writeType)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeDescriptor(
            descriptor: FwkDescriptor,
            value: ByteArray
        ) {
            bluetoothGatt?.writeDescriptor(descriptor, value)
        }
    }
}
