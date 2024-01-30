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
import android.bluetooth.BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED
import android.bluetooth.BluetoothGattCharacteristic as FwkBluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor as FwkBluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer as FwkBluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback as FwkBluetoothGattServerCallback
import android.bluetooth.BluetoothGattService as FwkBluetoothGattService
import android.bluetooth.BluetoothManager as FwkBluetoothManager
import android.bluetooth.BluetoothProfile as FwkBluetoothProfile
import android.bluetooth.BluetoothStatusCodes as FwkBluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_INDICATE
import androidx.bluetooth.GattCharacteristic.Companion.PROPERTY_NOTIFY
import androidx.bluetooth.GattCommon.UUID_CCCD
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.and
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class for handling operations as a GATT server role
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class GattServer(private val context: Context) {
    interface FrameworkAdapter {
        var fwkGattServer: FwkBluetoothGattServer?
        fun openGattServer(context: Context, fwkCallback: FwkBluetoothGattServerCallback)
        fun closeGattServer()
        fun clearServices()
        fun addService(fwkService: FwkBluetoothGattService)
        fun notifyCharacteristicChanged(
            fwkDevice: FwkBluetoothDevice,
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ): Int?
        fun sendResponse(
            fwkDevice: FwkBluetoothDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        )
    }

    internal interface Session {
        companion object {
            const val STATE_DISCONNECTED = 0
            const val STATE_CONNECTING = 1
            const val STATE_CONNECTED = 2
        }

        val device: BluetoothDevice
        var pendingWriteParts: MutableList<GattServerRequest.WriteCharacteristics.Part>
        suspend fun acceptConnection(block: suspend BluetoothLe.GattServerSessionScope.() -> Unit)
        fun rejectConnection()

        fun sendResponse(requestId: Int, status: Int, offset: Int, value: ByteArray?)

        fun writeCccd(requestId: Int, characteristic: GattCharacteristic, value: ByteArray?)
    }

    private companion object {
        private const val TAG = "GattServer"
    }

    @SuppressLint("ObsoleteSdkInt")
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkAdapter: FrameworkAdapter =
        if (Build.VERSION.SDK_INT >= 33) FrameworkAdapterApi33()
        else if (Build.VERSION.SDK_INT >= 31) FrameworkAdapterApi31()
        else FrameworkAdapterBase()

    suspend fun <R> open(
        services: List<GattService>,
        block: suspend BluetoothLe.GattServerConnectScope.() -> R
    ): R {
        return createServerScope(services).block()
    }

    private fun createServerScope(services: List<GattService>): BluetoothLe.GattServerConnectScope {
        return object : BluetoothLe.GattServerConnectScope {
            private val attributeMap = AttributeMap()

            // Should be accessed only from the callback thread
            private val sessions: MutableMap<FwkBluetoothDevice, Session> = mutableMapOf()
            private val notifyMutex = Mutex()
            private var notifyJob: CompletableDeferred<Boolean>? = null

            override val connectRequests = callbackFlow {
                attributeMap.updateWithServices(services)
                val callback = object : FwkBluetoothGattServerCallback() {
                    override fun onConnectionStateChange(
                        fwkDevice: FwkBluetoothDevice,
                        status: Int,
                        newState: Int
                    ) {
                        when (newState) {
                            FwkBluetoothProfile.STATE_CONNECTED -> {
                                trySend(
                                    BluetoothLe.GattServerConnectRequest(
                                        addSession(fwkDevice)
                                    )
                                )
                            }

                            FwkBluetoothProfile.STATE_DISCONNECTED -> removeSession(fwkDevice)
                        }
                    }

                    override fun onCharacteristicReadRequest(
                        fwkDevice: FwkBluetoothDevice,
                        requestId: Int,
                        offset: Int,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic
                    ) {
                        attributeMap.fromFwkCharacteristic(fwkCharacteristic)?.let { char ->
                            findActiveSessionWithDevice(fwkDevice)?.run {
                                requestChannel.trySend(
                                    GattServerRequest.ReadCharacteristic(
                                        this, requestId, offset, char
                                    )
                                )
                            }
                        } ?: run {
                            fwkAdapter.sendResponse(
                                fwkDevice, requestId, FwkBluetoothGatt.GATT_READ_NOT_PERMITTED,
                                offset, /*value=*/null
                            )
                        }
                    }

                    override fun onCharacteristicWriteRequest(
                        fwkDevice: FwkBluetoothDevice,
                        requestId: Int,
                        fwkCharacteristic: FwkBluetoothGattCharacteristic,
                        preparedWrite: Boolean,
                        responseNeeded: Boolean,
                        offset: Int,
                        value: ByteArray
                    ) {
                        attributeMap.fromFwkCharacteristic(fwkCharacteristic)?.let { char ->
                            findActiveSessionWithDevice(fwkDevice)?.let { session ->
                                if (preparedWrite) {
                                    session.pendingWriteParts.add(
                                        GattServerRequest.WriteCharacteristics.Part(
                                            char,
                                            offset,
                                            value
                                        )
                                    )
                                    fwkAdapter.sendResponse(
                                        fwkDevice, requestId,
                                        FwkBluetoothGatt.GATT_SUCCESS, offset, value
                                    )
                                } else {
                                    session.requestChannel.trySend(
                                        GattServerRequest.WriteCharacteristics(
                                            session,
                                            requestId,
                                            listOf(
                                                GattServerRequest.WriteCharacteristics.Part(
                                                    char,
                                                    0,
                                                    value
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        } ?: run {
                            fwkAdapter.sendResponse(
                                fwkDevice, requestId,
                                GATT_WRITE_NOT_PERMITTED, offset, /*value=*/null
                            )
                        }
                    }

                    override fun onExecuteWrite(
                        fwkDevice: FwkBluetoothDevice,
                        requestId: Int,
                        execute: Boolean
                    ) {
                        findActiveSessionWithDevice(fwkDevice)?.let { session ->
                            if (execute) {
                                session.requestChannel.trySend(
                                    GattServerRequest.WriteCharacteristics(
                                        session,
                                        requestId,
                                        session.pendingWriteParts
                                    )
                                )
                            } else {
                                fwkAdapter.sendResponse(
                                    fwkDevice, requestId,
                                    FwkBluetoothGatt.GATT_SUCCESS, /*offset=*/0, /*value=*/null
                                )
                            }
                            session.pendingWriteParts = mutableListOf()
                        } ?: run {
                            fwkAdapter.sendResponse(
                                fwkDevice, requestId,
                                FwkBluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                /*offset=*/0, /*value=*/null
                            )
                        }
                    }

                    override fun onDescriptorWriteRequest(
                        fwkDevice: FwkBluetoothDevice,
                        requestId: Int,
                        descriptor: FwkBluetoothGattDescriptor,
                        preparedWrite: Boolean,
                        responseNeeded: Boolean,
                        offset: Int,
                        value: ByteArray?
                    ) {
                        findActiveSessionWithDevice(fwkDevice)?.let { session ->
                            if (descriptor.uuid == UUID_CCCD) {
                                attributeMap.fromFwkCharacteristic(descriptor.characteristic)
                                    ?.let { char ->
                                        session.writeCccd(requestId, char, value)
                                    } ?: run {
                                    fwkAdapter.sendResponse(
                                        fwkDevice, requestId,
                                        FwkBluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                        /*offset=*/0, /*value=*/null
                                    )
                                }
                            }
                        } ?: run {
                            fwkAdapter.sendResponse(
                                fwkDevice, requestId,
                                FwkBluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                /*offset=*/0, /*value=*/null
                            )
                        }
                    }

                    override fun onNotificationSent(
                        fwkDevice: FwkBluetoothDevice,
                        status: Int
                    ) {
                        notifyJob?.complete(status == GATT_SUCCESS)
                        notifyJob = null
                    }
                }
                fwkAdapter.openGattServer(context, callback)
                services.forEach { fwkAdapter.addService(it.fwkService) }

                awaitClose {
                    fwkAdapter.closeGattServer()
                }
            }

            override fun updateServices(services: List<GattService>) {
                fwkAdapter.clearServices()
                services.forEach { fwkAdapter.addService(it.fwkService) }
            }

            fun addSession(fwkDevice: FwkBluetoothDevice): Session {
                return Session(BluetoothDevice(fwkDevice)).apply {
                    sessions[fwkDevice] = this
                }
            }

            fun removeSession(fwkDevice: FwkBluetoothDevice) {
                sessions.remove(fwkDevice)
            }

            fun findActiveSessionWithDevice(fwkDevice: FwkBluetoothDevice): Session? {
                return sessions[fwkDevice]?.takeIf {
                    it.state.get() != GattServer.Session.STATE_DISCONNECTED
                }
            }

            inner class Session(override val device: BluetoothDevice) : GattServer.Session {
                // A map from a characteristic to the corresponding
                // client characteristic configuration descriptor value
                val cccdMap = ArrayMap<GattCharacteristic, Int>()
                val subscribedCharacteristicsFlow =
                    MutableStateFlow<Set<GattCharacteristic>>(setOf())

                val state: AtomicInteger = AtomicInteger(GattServer.Session.STATE_CONNECTING)
                val requestChannel = Channel<GattServerRequest>(Channel.UNLIMITED)
                override var pendingWriteParts =
                    mutableListOf<GattServerRequest.WriteCharacteristics.Part>()

                override suspend fun acceptConnection(
                    block: suspend BluetoothLe.GattServerSessionScope.() -> Unit
                ) {
                    if (!state.compareAndSet(
                            GattServer.Session.STATE_CONNECTING,
                            GattServer.Session.STATE_CONNECTED
                        )
                    ) {
                        throw IllegalStateException("the request is already handled")
                    }

                    val scope = object : BluetoothLe.GattServerSessionScope {
                        override val device: BluetoothDevice
                            get() = this@Session.device
                        override val requests = requestChannel.receiveAsFlow()

                        override val subscribedCharacteristics: StateFlow<Set<GattCharacteristic>> =
                            subscribedCharacteristicsFlow.asStateFlow()

                        override suspend fun notify(
                            characteristic: GattCharacteristic,
                            value: ByteArray
                        ) {
                            if (value.size > GattCommon.MAX_ATTR_LENGTH) {
                                throw IllegalArgumentException("too long value to notify")
                            }
                            if (!characteristic.isSubscribable) {
                                throw IllegalArgumentException(
                                    "The characteristic can not be notified"
                                )
                            }
                            // Should not check if the client subscribed to the characteristic.
                            notifyMutex.withLock {
                                CompletableDeferred<Boolean>().also {
                                    // This is completed when the callback is received
                                    notifyJob = it
                                    fwkAdapter.notifyCharacteristicChanged(
                                        device.fwkDevice,
                                        characteristic.fwkCharacteristic,
                                        // Prefer notification over indication
                                        (characteristic.properties and PROPERTY_NOTIFY) == 0,
                                        value
                                    ).let { notifyResult ->
                                        if (notifyResult != FwkBluetoothStatusCodes.SUCCESS) {
                                            throw CancellationException(
                                                "notify failed with " +
                                                    "error: {$notifyResult}"
                                            )
                                        }
                                    }
                                    it.await()
                                }
                            }
                        }
                    }
                    scope.block()
                }

                override fun rejectConnection() {
                    if (!state.compareAndSet(
                            GattServer.Session.STATE_CONNECTING,
                            GattServer.Session.STATE_DISCONNECTED
                        )
                    ) {
                        throw IllegalStateException("the request is already handled")
                    }
                }

                override fun sendResponse(
                    requestId: Int,
                    status: Int,
                    offset: Int,
                    value: ByteArray?
                ) {
                    fwkAdapter
                        .sendResponse(device.fwkDevice, requestId, status, offset, value)
                }

                override fun writeCccd(
                    requestId: Int,
                    characteristic: GattCharacteristic,
                    value: ByteArray?
                ) {
                    if (value == null || value.isEmpty()) {
                        fwkAdapter.sendResponse(
                            device.fwkDevice, requestId,
                            GATT_INVALID_ATTRIBUTE_LENGTH,
                            /*offset=*/0, /*value=*/null
                        )
                        return
                    }
                    val indicate = (value[0] and 0x01).toInt() != 0
                    val notify = (value[0] and 0x02).toInt() != 0

                    if ((indicate && (characteristic.properties and PROPERTY_INDICATE) != 0) ||
                        (notify && (characteristic.properties and PROPERTY_NOTIFY) != 0)
                    ) {
                        fwkAdapter.sendResponse(
                            device.fwkDevice, requestId,
                            GATT_WRITE_NOT_PERMITTED,
                            /*offset=*/0, /*value=*/null
                        )
                        return
                    }
                    if (indicate || notify) {
                        cccdMap[characteristic] = value[0].toInt()
                    } else {
                        cccdMap.remove(characteristic)
                    }
                    // Emit a cloned set
                    subscribedCharacteristicsFlow.update { _ -> cccdMap.keys.toSet() }
                }
            }
        }
    }

    private open class FrameworkAdapterBase : FrameworkAdapter {
        override var fwkGattServer: FwkBluetoothGattServer? = null
        private val isOpen = AtomicBoolean(false)

        @SuppressLint("MissingPermission")
        override fun openGattServer(context: Context, fwkCallback: FwkBluetoothGattServerCallback) {
            if (!isOpen.compareAndSet(false, true))
                throw IllegalStateException("GATT server is already opened")
            val bluetoothManager = context.getSystemService(FwkBluetoothManager::class.java)
            fwkGattServer = bluetoothManager?.openGattServer(context, fwkCallback)
        }

        @SuppressLint("MissingPermission")
        override fun closeGattServer() {
            if (!isOpen.compareAndSet(true, false))
                throw IllegalStateException("GATT server is already closed")
            fwkGattServer?.close()
        }

        @SuppressLint("MissingPermission")
        override fun clearServices() {
            fwkGattServer?.clearServices()
        }

        @SuppressLint("MissingPermission")
        override fun addService(fwkService: FwkBluetoothGattService) {
            fwkGattServer?.addService(fwkService)
        }

        @Suppress("DEPRECATION")
        @SuppressLint("MissingPermission")
        override fun notifyCharacteristicChanged(
            fwkDevice: FwkBluetoothDevice,
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ): Int? {
            fwkCharacteristic.value = value
            return fwkGattServer?.notifyCharacteristicChanged(fwkDevice, fwkCharacteristic, confirm)
                ?.let {
                    if (it) FwkBluetoothStatusCodes.SUCCESS
                    else FwkBluetoothStatusCodes.ERROR_UNKNOWN
                }
        }

        @SuppressLint("MissingPermission")
        override fun sendResponse(
            fwkDevice: FwkBluetoothDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        ) {
            fwkGattServer?.sendResponse(fwkDevice, requestId, status, offset, value)
        }
    }

    @RequiresApi(31)
    private open class FrameworkAdapterApi31 : FrameworkAdapterBase() {

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun openGattServer(context: Context, fwkCallback: FwkBluetoothGattServerCallback) {
            return super.openGattServer(context, fwkCallback)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun closeGattServer() {
            return super.closeGattServer()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun clearServices() {
            return super.clearServices()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun addService(fwkService: FwkBluetoothGattService) {
            return super.addService(fwkService)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            fwkDevice: FwkBluetoothDevice,
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ): Int? {
            return super.notifyCharacteristicChanged(fwkDevice, fwkCharacteristic, confirm, value)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun sendResponse(
            fwkDevice: FwkBluetoothDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        ) {
            return super.sendResponse(fwkDevice, requestId, status, offset, value)
        }
    }

    @RequiresApi(33)
    private open class FrameworkAdapterApi33 : FrameworkAdapterApi31() {
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            fwkDevice: FwkBluetoothDevice,
            fwkCharacteristic: FwkBluetoothGattCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ): Int? {
            return fwkGattServer?.notifyCharacteristicChanged(
                fwkDevice,
                fwkCharacteristic,
                confirm,
                value
            )
        }
    }
}
