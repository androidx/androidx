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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Class for handling operations as a GATT server role
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class GattServer(private val context: Context) {
    interface FrameworkAdapter {
        var gattServer: BluetoothGattServer?
        fun openGattServer(context: Context, callback: BluetoothGattServerCallback)
        fun closeGattServer()
        fun clearServices()
        fun addService(service: BluetoothGattService)
        fun notifyCharacteristicChanged(
            device: FwkBluetoothDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        )
        fun sendResponse(
            device: FwkBluetoothDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        )
    }

    private companion object {
        private const val TAG = "GattServer"
    }

    internal class Session {
        enum class State {
            DISCONNECTED,
            CONNECTING,
            CONNECTED,
        }

        var state: AtomicReference<State> = AtomicReference(State.CONNECTING)
        val requestChannel = Channel<GattServerRequest>(Channel.UNLIMITED)
    }

    // Should be accessed only from the callback thread
    private val sessions: MutableMap<FwkBluetoothDevice, Session> = mutableMapOf()
    private val attributeMap = AttributeMap()

    @SuppressLint("ObsoleteSdkInt")
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkAdapter: FrameworkAdapter =
        if (Build.VERSION.SDK_INT >= 33) FrameworkAdapterApi33()
        else FrameworkAdapterBase()

    fun open(services: List<GattService>):
        Flow<BluetoothLe.GattServerConnectionRequest> = callbackFlow {
        attributeMap.updateWithServices(services)
        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: FwkBluetoothDevice,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        trySend(
                            BluetoothLe.GattServerConnectionRequest(
                                BluetoothDevice(device),
                                this@GattServer,
                                addSession(device)
                            )
                        )
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> removeSession(device)
                }
            }

            override fun onCharacteristicReadRequest(
                device: FwkBluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: FwkCharacteristic
            ) {
                attributeMap.fromFwkCharacteristic(characteristic)?.let {
                    findActiveSessionWithDevice(device)?.requestChannel?.trySend(
                        GattServerRequest.ReadCharacteristicRequest(
                            this@GattServer, device, requestId, offset, it
                        )
                    )
                } ?: run {
                    sendResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED, offset,
                        /*value=*/null)
                }
            }

            override fun onCharacteristicWriteRequest(
                device: FwkBluetoothDevice,
                requestId: Int,
                characteristic: FwkCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                attributeMap.fromFwkCharacteristic(characteristic)?.let {
                    findActiveSessionWithDevice(device)?.requestChannel?.trySend(
                        GattServerRequest.WriteCharacteristicRequest(
                            this@GattServer,
                            device,
                            requestId,
                            it,
                            preparedWrite,
                            responseNeeded,
                            offset,
                            value
                        )
                    )
                } ?: run {
                    sendResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                        offset, /*value=*/null)
                }
            }
        }
        fwkAdapter.openGattServer(context, callback)
        services.forEach { fwkAdapter.addService(it.fwkService) }

        awaitClose {
            fwkAdapter.closeGattServer()
        }
    }

    fun updateServices(services: List<GattService>) {
        fwkAdapter.clearServices()
        services.forEach { fwkAdapter.addService(it.fwkService) }
    }

    suspend fun<R> acceptConnection(
        request: BluetoothLe.GattServerConnectionRequest,
        block: suspend BluetoothLe.GattServerScope.() -> R
    ) = coroutineScope {
        val session = request.session
        if (!session.state.compareAndSet(Session.State.CONNECTING, Session.State.CONNECTED)) {
            throw IllegalStateException("the request is already handled")
        }
        val scope = object : BluetoothLe.GattServerScope {
            override val device: BluetoothDevice
                get() = request.device
            override val requests = session.requestChannel.receiveAsFlow()

            override fun notify(
                characteristic: GattCharacteristic,
                value: ByteArray
            ) {
                fwkAdapter.notifyCharacteristicChanged(
                    request.device.fwkDevice, characteristic.fwkCharacteristic, false, value)
            }
        }
        scope.block()
    }

    @SuppressLint("MissingPermission")
    fun rejectConnection(request: BluetoothLe.GattServerConnectionRequest) {
        if (!request.session.state.compareAndSet(
                Session.State.CONNECTING, Session.State.DISCONNECTED)) {
            throw IllegalStateException("the request is already handled")
        }
    }

    internal fun findActiveSessionWithDevice(device: FwkBluetoothDevice): Session? {
        return sessions[device]?.takeIf {
            it.state.get() != Session.State.DISCONNECTED
        }
    }

    internal fun addSession(device: FwkBluetoothDevice): Session {
        return Session().apply {
            sessions[device] = this
        }
    }

    internal fun removeSession(device: FwkBluetoothDevice) {
        sessions.remove(device)
    }

    internal fun sendResponse(
        device: FwkBluetoothDevice,
        requestId: Int,
        status: Int,
        offset: Int,
        value: ByteArray?
    ) {
        fwkAdapter.sendResponse(device, requestId, status, offset, value)
    }

    private open class FrameworkAdapterBase : FrameworkAdapter {
        override var gattServer: BluetoothGattServer? = null
        private val isOpen = AtomicBoolean(false)
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun openGattServer(context: Context, callback: BluetoothGattServerCallback) {
            if (!isOpen.compareAndSet(false, true))
                throw IllegalStateException("GATT server is already opened")
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            gattServer = bluetoothManager?.openGattServer(context, callback)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun closeGattServer() {
            if (!isOpen.compareAndSet(true, false))
                throw IllegalStateException("GATT server is already closed")
            gattServer?.close()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun clearServices() {
            gattServer?.clearServices()
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun addService(service: BluetoothGattService) {
            gattServer?.addService(service)
        }

        @Suppress("DEPRECATION")
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            device: FwkBluetoothDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ) {
            characteristic.value = value
            gattServer?.notifyCharacteristicChanged(device, characteristic, confirm)
        }

        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun sendResponse(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            status: Int,
            offset: Int,
            value: ByteArray?
        ) {
            gattServer?.sendResponse(device, requestId, status, offset, value)
        }
    }

    private open class FrameworkAdapterApi33 : FrameworkAdapterBase() {
        @RequiresPermission(BLUETOOTH_CONNECT)
        override fun notifyCharacteristicChanged(
            device: FwkBluetoothDevice,
            characteristic: FwkCharacteristic,
            confirm: Boolean,
            value: ByteArray
        ) {
            gattServer?.notifyCharacteristicChanged(device, characteristic, confirm, value)
        }
    }
}
