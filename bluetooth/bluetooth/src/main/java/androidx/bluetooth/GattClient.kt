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
import android.bluetooth.BluetoothDevice as FwkBluetoothDevice
import android.bluetooth.BluetoothGatt as FwkBluetoothGatt
import android.bluetooth.BluetoothGattCallback as FwkBluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic as FwkBluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor as FwkBluetoothGattDescriptor
import android.bluetooth.BluetoothGattService as FwkBluetoothGattService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_NOTIFY
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_WRITE_NO_RESPONSE
import androidx.bluetooth.GattCommon.MAX_ATTR_LENGTH
import androidx.bluetooth.GattCommon.UUID_CCCD
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** A class for handling operations as a GATT client role. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GattClient(private val context: Context) {

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    companion object {
        private const val TAG = "GattClient"

        /** The maximum ATT size + header(3) */
        private const val GATT_MAX_MTU = MAX_ATTR_LENGTH + 3

        private const val CONNECT_TIMEOUT_MS = 30_000L
    }

    interface FrameworkAdapter {
        var fwkBluetoothGatt: FwkBluetoothGatt?

        fun connectGatt(
            context: Context,
            fwkDevice: FwkBluetoothDevice,
            fwkCallback: FwkBluetoothGattCallback
        ): Boolean

        fun requestMtu(mtu: Int)

        fun discoverServices()

        fun getServices(): List<FwkBluetoothGattService>

        fun getService(uuid: UUID): FwkBluetoothGattService?

        fun readCharacteristic(fwkCharacteristic: FwkBluetoothGattCharacteristic)

        fun writeCharacteristic(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        )

        fun writeDescriptor(fwkDescriptor: FwkBluetoothGattDescriptor, value: ByteArray)

        fun setCharacteristicNotification(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            enable: Boolean
        )

        fun closeGatt()
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkAdapter: FrameworkAdapter =
        if (Build.VERSION.SDK_INT >= 33) FrameworkAdapterApi33()
        else if (Build.VERSION.SDK_INT >= 31) FrameworkAdapterApi31() else FrameworkAdapterBase()

    private sealed interface CallbackResult {
        class OnCharacteristicRead(
            val characteristic: GattCharacteristic,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult

        class OnCharacteristicWrite(val characteristic: GattCharacteristic, val status: Int) :
            CallbackResult

        class OnDescriptorRead(
            val fwkDescriptor: FwkBluetoothGattDescriptor,
            val value: ByteArray,
            val status: Int
        ) : CallbackResult

        class OnDescriptorWrite(val fwkDescriptor: FwkBluetoothGattDescriptor, val status: Int) :
            CallbackResult
    }

    private interface SubscribeListener {
        fun onCharacteristicNotification(value: ByteArray)

        fun finish()
    }

    @SuppressLint("MissingPermission")
    suspend fun <R> connect(device: BluetoothDevice, block: suspend GattClientScope.() -> R): R =
        coroutineScope {
            val connectResult = CompletableDeferred<Unit>(parent = coroutineContext.job)
            val callbackResultsFlow =
                MutableSharedFlow<CallbackResult>(extraBufferCapacity = Int.MAX_VALUE)
            val subscribeMap = mutableMapOf<FwkBluetoothGattCharacteristic, SubscribeListener>()
            val subscribeMutex = Mutex()
            val attributeMap = AttributeMap()
            val servicesFlow = MutableStateFlow<List<GattService>>(listOf())

            val fwkCallback =
                object : FwkBluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: FwkBluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        if (newState == FwkBluetoothGatt.STATE_CONNECTED) {
                            fwkAdapter.requestMtu(GATT_MAX_MTU)
                        } else {
                            cancel("connect failed")
                        }
                    }

                    override fun onMtuChanged(gatt: FwkBluetoothGatt?, mtu: Int, status: Int) {
                        if (status == FwkBluetoothGatt.GATT_SUCCESS) {
                            fwkAdapter.discoverServices()
                        } else {
                            cancel("mtu request failed")
                        }
                    }

                    override fun onServicesDiscovered(gatt: FwkBluetoothGatt?, status: Int) {
                        attributeMap.updateWithFrameworkServices(fwkAdapter.getServices())
                        if (status == FwkBluetoothGatt.GATT_SUCCESS) connectResult.complete(Unit)
                        else cancel("service discover failed")
                        servicesFlow.tryEmit(attributeMap.getServices())
                        if (connectResult.isActive) {
                            if (status == FwkBluetoothGatt.GATT_SUCCESS)
                                connectResult.complete(Unit)
                            else connectResult.cancel("service discover failed")
                        }
                    }

                    override fun onServiceChanged(gatt: FwkBluetoothGatt) {
                        // TODO: under API 31, we have to subscribe to the service changed
                        // characteristic.
                        fwkAdapter.discoverServices()
                    }

                    override fun onCharacteristicRead(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int
                    ) {
                        attributeMap.fromFwkCharacteristic(fwkCharacteristic)?.let {
                            callbackResultsFlow.tryEmit(
                                CallbackResult.OnCharacteristicRead(it, value, status)
                            )
                        }
                    }

                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onCharacteristicRead(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        status: Int
                    ) {
                        onCharacteristicRead(
                            fwkBluetoothGatt,
                            fwkCharacteristic,
                            fwkCharacteristic.value,
                            status
                        )
                    }

                    override fun onCharacteristicWrite(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        status: Int
                    ) {
                        attributeMap.fromFwkCharacteristic(fwkCharacteristic)?.let {
                            callbackResultsFlow.tryEmit(
                                CallbackResult.OnCharacteristicWrite(it, status)
                            )
                        }
                    }

                    override fun onDescriptorRead(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkDescriptor: FwkBluetoothGattDescriptor,
                        status: Int,
                        value: ByteArray
                    ) {
                        callbackResultsFlow.tryEmit(
                            CallbackResult.OnDescriptorRead(fwkDescriptor, value, status)
                        )
                    }

                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onDescriptorRead(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkDescriptor: FwkBluetoothGattDescriptor,
                        status: Int
                    ) {
                        onDescriptorRead(
                            fwkBluetoothGatt,
                            fwkDescriptor,
                            status,
                            fwkDescriptor.value
                        )
                    }

                    override fun onDescriptorWrite(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkDescriptor: FwkBluetoothGattDescriptor,
                        status: Int
                    ) {
                        callbackResultsFlow.tryEmit(
                            CallbackResult.OnDescriptorWrite(fwkDescriptor, status)
                        )
                    }

                    override fun onCharacteristicChanged(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        value: ByteArray
                    ) {
                        launch {
                            subscribeMutex.withLock {
                                subscribeMap[fwkCharacteristic]?.onCharacteristicNotification(value)
                            }
                        }
                    }

                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onCharacteristicChanged(
                        fwkBluetoothGatt: FwkBluetoothGatt,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                    ) {
                        onCharacteristicChanged(
                            fwkBluetoothGatt,
                            fwkCharacteristic,
                            fwkCharacteristic.value
                        )
                    }
                }

            if (!fwkAdapter.connectGatt(context, device.fwkDevice, fwkCallback)) {
                throw CancellationException("failed to connect")
            }

            withTimeout(CONNECT_TIMEOUT_MS) { connectResult.await() }

            val gattClientScope =
                object : GattClientScope {
                    val taskMutex = Mutex()

                    suspend fun <R> runTask(block: suspend () -> R): R {
                        taskMutex.withLock {
                            return block()
                        }
                    }

                    override val servicesFlow: StateFlow<List<GattService>> =
                        servicesFlow.asStateFlow()

                    override fun getService(uuid: UUID): GattService? {
                        return fwkAdapter.getService(uuid)?.let { attributeMap.fromFwkService(it) }
                    }

                    override suspend fun readCharacteristic(
                        characteristic: GattCharacteristic
                    ): Result<ByteArray> {
                        if (characteristic.properties and GattCharacteristic.PROPERTY_READ == 0) {
                            return Result.failure(
                                IllegalArgumentException("can't read the characteristic")
                            )
                        }
                        return runTask {
                            fwkAdapter.readCharacteristic(characteristic.fwkCharacteristic)
                            val res =
                                takeMatchingResult<CallbackResult.OnCharacteristicRead>(
                                    callbackResultsFlow
                                ) {
                                    it.characteristic == characteristic
                                }

                            if (res.status == FwkBluetoothGatt.GATT_SUCCESS)
                                Result.success(res.value)
                            // TODO: throw precise reason if we can gather the info
                            else Result.failure(CancellationException("fail"))
                        }
                    }

                    override suspend fun writeCharacteristic(
                        characteristic: GattCharacteristic,
                        value: ByteArray
                    ): Result<Unit> {
                        val writeType =
                            if (characteristic.properties and PROPERTY_WRITE_NO_RESPONSE != 0)
                                FwkBluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            else if (characteristic.properties and PROPERTY_WRITE != 0)
                                FwkBluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            else throw IllegalArgumentException("can't write to the characteristic")

                        if (value.size > MAX_ATTR_LENGTH) {
                            throw IllegalArgumentException("too long value to write")
                        }

                        return runTask {
                            fwkAdapter.writeCharacteristic(
                                characteristic.fwkCharacteristic,
                                value,
                                writeType
                            )
                            val res =
                                takeMatchingResult<CallbackResult.OnCharacteristicWrite>(
                                    callbackResultsFlow
                                ) {
                                    it.characteristic == characteristic
                                }
                            if (res.status == FwkBluetoothGatt.GATT_SUCCESS) Result.success(Unit)
                            // TODO: throw precise reason if we can gather the info
                            else
                                Result.failure(
                                    CancellationException("fail with error = ${res.status}")
                                )
                        }
                    }

                    override fun subscribeToCharacteristic(
                        characteristic: GattCharacteristic
                    ): Flow<ByteArray> {
                        if (!characteristic.isSubscribable) {
                            return emptyFlow()
                        }
                        val cccd =
                            characteristic.fwkCharacteristic.getDescriptor(UUID_CCCD)
                                ?: return emptyFlow()

                        return callbackFlow {
                            val listener =
                                object : SubscribeListener {
                                    override fun onCharacteristicNotification(value: ByteArray) {
                                        trySend(value)
                                    }

                                    override fun finish() {
                                        close()
                                    }
                                }
                            if (
                                !registerSubscribeListener(
                                    characteristic.fwkCharacteristic,
                                    listener
                                )
                            ) {
                                throw IllegalStateException("already subscribed")
                            }

                            runTask {
                                fwkAdapter.setCharacteristicNotification(
                                    characteristic.fwkCharacteristic,
                                    /*enable=*/ true
                                )

                                val cccdValue =
                                    // Prefer notification over indication
                                    if ((characteristic.properties and PROPERTY_NOTIFY) != 0)
                                        FwkBluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    else FwkBluetoothGattDescriptor.ENABLE_INDICATION_VALUE

                                fwkAdapter.writeDescriptor(cccd, cccdValue)
                                val res =
                                    takeMatchingResult<CallbackResult.OnDescriptorWrite>(
                                        callbackResultsFlow
                                    ) {
                                        it.fwkDescriptor == cccd
                                    }
                                if (res.status != FwkBluetoothGatt.GATT_SUCCESS) {
                                    cancel("failed to set notification")
                                }
                            }

                            awaitClose {
                                launch {
                                    unregisterSubscribeListener(characteristic.fwkCharacteristic)
                                }
                                fwkAdapter.setCharacteristicNotification(
                                    characteristic.fwkCharacteristic,
                                    /*enable=*/ false
                                )
                                fwkAdapter.writeDescriptor(
                                    cccd,
                                    FwkBluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                                )
                            }
                        }
                    }

                    private suspend fun registerSubscribeListener(
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        callback: SubscribeListener
                    ): Boolean {
                        subscribeMutex.withLock {
                            if (subscribeMap.containsKey(fwkCharacteristic)) {
                                return false
                            }
                            subscribeMap[fwkCharacteristic] = callback
                            return true
                        }
                    }

                    private suspend fun unregisterSubscribeListener(
                        fwkCharacteristic: FwkBluetoothGattCharacteristic
                    ) {
                        subscribeMutex.withLock { subscribeMap.remove(fwkCharacteristic) }
                    }
                }

            coroutineContext.job.invokeOnCompletion { fwkAdapter.closeGatt() }

            gattClientScope.block()
        }

    private suspend inline fun <reified R : CallbackResult> takeMatchingResult(
        flow: SharedFlow<CallbackResult>,
        crossinline predicate: (R) -> Boolean
    ): R {
        return flow.filter { it is R && predicate(it) }.first() as R
    }

    private open class FrameworkAdapterBase : FrameworkAdapter {

        override var fwkBluetoothGatt: FwkBluetoothGatt? = null

        @SuppressLint("MissingPermission")
        override fun connectGatt(
            context: Context,
            fwkDevice: FwkBluetoothDevice,
            fwkCallback: FwkBluetoothGattCallback
        ): Boolean {
            fwkBluetoothGatt = fwkDevice.connectGatt(context, /* autoConnect= */ false, fwkCallback)
            return fwkBluetoothGatt != null
        }

        @SuppressLint("MissingPermission")
        override fun requestMtu(mtu: Int) {
            fwkBluetoothGatt?.requestMtu(mtu)
        }

        @SuppressLint("MissingPermission")
        override fun discoverServices() {
            fwkBluetoothGatt?.discoverServices()
        }

        override fun getServices(): List<FwkBluetoothGattService> {
            return fwkBluetoothGatt?.services ?: listOf()
        }

        override fun getService(uuid: UUID): FwkBluetoothGattService? {
            return fwkBluetoothGatt?.getService(uuid)
        }

        @SuppressLint("MissingPermission")
        override fun readCharacteristic(fwkCharacteristic: FwkBluetoothGattCharacteristic) {
            fwkBluetoothGatt?.readCharacteristic(fwkCharacteristic)
        }

        @Suppress("DEPRECATION")
        @SuppressLint("MissingPermission")
        override fun writeCharacteristic(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            fwkCharacteristic.value = value
            fwkBluetoothGatt?.writeCharacteristic(fwkCharacteristic)
        }

        @Suppress("DEPRECATION")
        @SuppressLint("MissingPermission")
        override fun writeDescriptor(fwkDescriptor: FwkBluetoothGattDescriptor, value: ByteArray) {
            fwkDescriptor.value = value
            fwkBluetoothGatt?.writeDescriptor(fwkDescriptor)
        }

        @SuppressLint("MissingPermission")
        override fun setCharacteristicNotification(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            enable: Boolean
        ) {
            fwkBluetoothGatt?.setCharacteristicNotification(fwkCharacteristic, enable)
        }

        @SuppressLint("MissingPermission")
        override fun closeGatt() {
            fwkBluetoothGatt?.close()
            fwkBluetoothGatt?.disconnect()
        }
    }

    @RequiresApi(31)
    private open class FrameworkAdapterApi31 : FrameworkAdapterBase() {

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun connectGatt(
            context: Context,
            fwkDevice: FwkBluetoothDevice,
            fwkCallback: FwkBluetoothGattCallback
        ): Boolean {
            return super.connectGatt(context, fwkDevice, fwkCallback)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun requestMtu(mtu: Int) {
            return super.requestMtu(mtu)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun discoverServices() {
            return super.discoverServices()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun readCharacteristic(fwkCharacteristic: FwkBluetoothGattCharacteristic) {
            return super.readCharacteristic(fwkCharacteristic)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeCharacteristic(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            return super.writeCharacteristic(fwkCharacteristic, value, writeType)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeDescriptor(fwkDescriptor: FwkBluetoothGattDescriptor, value: ByteArray) {
            return super.writeDescriptor(fwkDescriptor, value)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun setCharacteristicNotification(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            enable: Boolean
        ) {
            return super.setCharacteristicNotification(fwkCharacteristic, enable)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun closeGatt() {
            return super.closeGatt()
        }
    }

    @RequiresApi(33)
    private open class FrameworkAdapterApi33 : FrameworkAdapterApi31() {
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeCharacteristic(
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int
        ) {
            fwkBluetoothGatt?.writeCharacteristic(fwkCharacteristic, value, writeType)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun writeDescriptor(fwkDescriptor: FwkBluetoothGattDescriptor, value: ByteArray) {
            fwkBluetoothGatt?.writeDescriptor(fwkDescriptor, value)
        }
    }
}
