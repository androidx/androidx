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

package androidx.bluetooth.integration.testapp.ui.connections

import android.annotation.SuppressLint
import android.util.Log
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattClientScope
import androidx.bluetooth.integration.testapp.data.connection.DeviceConnection
import androidx.bluetooth.integration.testapp.data.connection.OnCharacteristicActionClick
import androidx.bluetooth.integration.testapp.data.connection.Status
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectionsViewModel @Inject constructor(private val bluetoothLe: BluetoothLe) : ViewModel() {

    internal companion object {
        private const val TAG = "ConnectionsViewModel"

        internal const val NEW_DEVICE = -1
    }

    internal val deviceConnections: Set<DeviceConnection>
        get() = _deviceConnections

    private val _deviceConnections = mutableSetOf<DeviceConnection>()

    private val _uiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()

        _deviceConnections.forEach { it.job?.cancel() }
    }

    fun addDeviceConnectionIfNew(bluetoothDevice: BluetoothDevice): Int {
        val indexOf = _deviceConnections.map { it.bluetoothDevice }.indexOf(bluetoothDevice)
        if (indexOf != -1) {
            return indexOf
        }

        _deviceConnections.add(DeviceConnection(bluetoothDevice))
        return NEW_DEVICE
    }

    fun removeDeviceConnection(deviceConnection: DeviceConnection) {
        deviceConnection.job?.cancel(ConnectionsFragment.MANUAL_DISCONNECT)
        deviceConnection.job = null
        _deviceConnections.remove(deviceConnection)

        updateUi()
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceConnection: DeviceConnection) {
        Log.d(TAG, "connect() called with: deviceConnection = $deviceConnection")

        deviceConnection.job =
            viewModelScope.launch {
                deviceConnection.status = Status.CONNECTING
                updateUi()

                try {
                    Log.d(
                        TAG,
                        "bluetoothLe.connectGatt() called with: " +
                            "deviceConnection.bluetoothDevice = ${deviceConnection.bluetoothDevice}"
                    )

                    bluetoothLe.connectGatt(deviceConnection.bluetoothDevice) {
                        Log.d(TAG, "bluetoothLe.connectGatt result: services() = $services")

                        deviceConnection.status = Status.CONNECTED
                        launch {
                            servicesFlow.collect {
                                deviceConnection.services = it
                                updateUi()
                            }
                        }

                        deviceConnection.onCharacteristicActionClick =
                            object : OnCharacteristicActionClick {
                                override fun onClick(
                                    deviceConnection: DeviceConnection,
                                    characteristic: GattCharacteristic,
                                    action: @OnCharacteristicActionClick.Action Int
                                ) {
                                    when (action) {
                                        OnCharacteristicActionClick.READ ->
                                            readCharacteristic(
                                                this@connectGatt,
                                                deviceConnection,
                                                characteristic
                                            )
                                        OnCharacteristicActionClick.WRITE ->
                                            _uiState.update {
                                                it.copy(
                                                    showDialogForWrite =
                                                        Pair(this@connectGatt, characteristic)
                                                )
                                            }
                                        OnCharacteristicActionClick.SUBSCRIBE ->
                                            subscribeToCharacteristic(
                                                this@connectGatt,
                                                characteristic
                                            )
                                    }
                                }
                            }

                        awaitCancellation()
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) {
                        Log.e(TAG, "connectGatt() CancellationException", exception)
                    } else {
                        Log.e(TAG, "connectGatt() exception", exception)
                    }

                    deviceConnection.status = Status.DISCONNECTED
                    updateUi()
                }
            }
    }

    fun disconnect(deviceConnection: DeviceConnection) {
        deviceConnection.job?.cancel(ConnectionsFragment.MANUAL_DISCONNECT)
        deviceConnection.job = null

        updateUi()
    }

    fun writeDialogShown() {
        _uiState.update { it.copy(showDialogForWrite = null) }
    }

    fun resultMessageShown() {
        _uiState.update { it.copy(resultMessage = null) }
    }

    private fun updateUi() {
        _uiState.update { it.copy(lastConnectionUpdate = System.currentTimeMillis()) }
    }

    private fun readCharacteristic(
        gattClientScope: GattClientScope,
        deviceConnection: DeviceConnection,
        characteristic: GattCharacteristic
    ) {
        viewModelScope.launch {
            Log.d(TAG, "readCharacteristic() called with: characteristic = $characteristic")

            val result = gattClientScope.readCharacteristic(characteristic)
            Log.d(TAG, "readCharacteristic() result: result = $result")

            deviceConnection.storeValueFor(characteristic, result.getOrNull())
            updateUi()
        }
    }

    fun writeCharacteristic(
        gattClientScope: GattClientScope,
        characteristic: GattCharacteristic,
        valueString: String
    ) {
        val value = valueString.toByteArray()

        viewModelScope.launch {
            Log.d(
                TAG,
                "writeCharacteristic() called with: " +
                    "characteristic = $characteristic, " +
                    "value = ${value.decodeToString()}"
            )

            val result = gattClientScope.writeCharacteristic(characteristic, value)
            Log.d(TAG, "writeCharacteristic() result: result = $result")

            _uiState.update {
                it.copy(resultMessage = "Called write with: $valueString, result = $result")
            }
        }
    }

    private fun subscribeToCharacteristic(
        gattClientScope: GattClientScope,
        characteristic: GattCharacteristic
    ) {
        gattClientScope
            .subscribeToCharacteristic(characteristic)
            .onStart { Log.d(TAG, "subscribeToCharacteristic() onStart") }
            .onEach { Log.d(TAG, "subscribeToCharacteristic() onEach: ${it.decodeToString()}") }
            .onCompletion { Log.e(TAG, "subscribeToCharacteristic onCompletion", it) }
            .launchIn(viewModelScope)
    }
}
