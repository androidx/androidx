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
import android.bluetooth.BluetoothDevice as FwkDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
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
internal class GattServerImpl(private val context: Context) {
    private companion object {
        private const val TAG = "GattServerImpl"
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
    private val sessions: MutableMap<FwkDevice, Session> = mutableMapOf()
    private var gattServer: BluetoothGattServer? = null
    private val attributeMap = AttributeMap()

    @SuppressLint("MissingPermission")
    fun open(services: List<GattService>):
        Flow<BluetoothLe.GattServerConnectionRequest> = callbackFlow {
        if (gattServer != null) {
            throw IllegalStateException("GATT server is already opened")
        }
        attributeMap.updateWithServices(services)
        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: FwkDevice,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    val session = addSession(device)
                    trySend(
                        BluetoothLe.GattServerConnectionRequest(
                            BluetoothDevice.of(device),
                            this@GattServerImpl,
                            session
                        )
                    )
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    removeSession(device)
                }
            }

            override fun onCharacteristicReadRequest(
                device: FwkDevice,
                requestId: Int,
                offset: Int,
                characteristic: FwkCharacteristic
            ) {
                attributeMap.fromFwkCharacteristic(characteristic)?.let {
                    findActiveSessionWithDevice(device)?.requestChannel?.trySend(
                        GattServerRequest.ReadCharacteristicRequest(
                            this@GattServerImpl, device, requestId, offset, it
                        )
                    )
                } ?: run {
                    sendResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED, offset,
                        /*value=*/null)
                }
            }

            override fun onCharacteristicWriteRequest(
                device: FwkDevice,
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
                            this@GattServerImpl,
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
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        gattServer = bluetoothManager?.openGattServer(context, callback)
        services.forEach { gattServer?.addService(it.fwkService) }

        awaitClose {
            gattServer?.close()
            gattServer = null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun<R> acceptConnection(
        request: BluetoothLe.GattServerConnectionRequest,
        block: BluetoothLe.GattServerScope.() -> R
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
                gattServer?.notifyCharacteristicChanged(
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

    internal fun findActiveSessionWithDevice(device: FwkDevice): Session? {
        return sessions[device]?.takeIf {
            it.state.get() != Session.State.DISCONNECTED
        }
    }

    internal fun addSession(device: FwkDevice): Session {
        return Session().apply {
            sessions[device] = this
        }
    }

    internal fun removeSession(device: FwkDevice) {
        sessions.remove(device)
    }

    @SuppressLint("MissingPermission")
    internal fun sendResponse(
        device: FwkDevice,
        requestId: Int,
        status: Int,
        offset: Int,
        value: ByteArray?
    ) {
        gattServer?.sendResponse(device, requestId, status, offset, value)
    }
}
