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

package androidx.bluetooth.integration.testapp.ui.gatt_server

import android.util.Log
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattServerRequest
import androidx.bluetooth.GattService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

@HiltViewModel
class GattServerViewModel @Inject constructor(private val bluetoothLe: BluetoothLe) : ViewModel() {

    private companion object {
        private const val TAG = "GattServerViewModel"
    }

    var gattServerJob: Job? = null

    private val _gattServerServices = mutableListOf<GattService>()
    val gattServerServices: List<GattService> = _gattServerServices

    private val gattServerServicesCharacteristicValueMap =
        mutableMapOf<GattCharacteristic, ByteArray>()

    private val _uiState = MutableStateFlow(GattServerUiState())
    val uiState: StateFlow<GattServerUiState> = _uiState.asStateFlow()

    fun addGattService(gattService: GattService) {
        _gattServerServices.add(gattService)
    }

    fun addGattCharacteristic(service: GattService, characteristic: GattCharacteristic) {
        val index = _gattServerServices.indexOf(service)
        if (index < 0) return
        _gattServerServices[index] =
            GattService(
                service.uuid,
                service.characteristics.toMutableList().apply { add(characteristic) }
            )
    }

    fun openGattServer() {
        Log.d(TAG, "openGattServer() called")

        gattServerJob =
            bluetoothLe
                .openGattServer(gattServerServices)
                .onStart {
                    Log.d(
                        TAG,
                        "bluetoothLe.openGattServer() called with: " +
                            "gattServerServices = $gattServerServices"
                    )
                    _uiState.update { it.copy(isGattServerOpen = true) }
                }
                .onEach {
                    Log.d(TAG, "connectRequests.collected: GattServerConnectRequest = $it")

                    it.accept {
                        Log.d(
                            TAG,
                            "GattServerConnectRequest accepted: GattServerSessionScope = $it"
                        )

                        requests.collect { gattServerRequest ->
                            Log.d(TAG, "requests collected: gattServerRequest = $gattServerRequest")

                            when (gattServerRequest) {
                                is GattServerRequest.ReadCharacteristic -> {
                                    val characteristic = gattServerRequest.characteristic
                                    val value = readGattCharacteristicValue(characteristic)

                                    _uiState.update { state ->
                                        state.copy(
                                            resultMessage =
                                                "Read value: " +
                                                    "${value.decodeToString()} for characteristic" +
                                                    " = ${characteristic.uuid}"
                                        )
                                    }

                                    gattServerRequest.sendResponse(value)
                                }
                                is GattServerRequest.WriteCharacteristics -> {
                                    val characteristic = gattServerRequest.parts[0].characteristic
                                    val value = gattServerRequest.parts[0].value

                                    _uiState.update { state ->
                                        state.copy(
                                            resultMessage =
                                                "Writing value: " +
                                                    "${value.decodeToString()} to characteristic" +
                                                    " = ${characteristic.uuid}"
                                        )
                                    }

                                    updateGattCharacteristicValue(characteristic, value)
                                    gattServerRequest.sendResponse()
                                }
                                else -> {
                                    throw NotImplementedError("Unknown request")
                                }
                            }
                        }
                    }
                }
                .onCompletion {
                    Log.d(TAG, "bluetoothLe.openGattServer completed")
                    _uiState.update { it.copy(isGattServerOpen = false) }
                }
                .launchIn(viewModelScope)
    }

    fun resultMessageShown() {
        _uiState.update { it.copy(resultMessage = null) }
    }

    private fun readGattCharacteristicValue(characteristic: GattCharacteristic): ByteArray {
        return gattServerServicesCharacteristicValueMap[characteristic] ?: ByteArray(0)
    }

    private fun updateGattCharacteristicValue(
        characteristic: GattCharacteristic,
        value: ByteArray
    ) {
        gattServerServicesCharacteristicValueMap[characteristic] = value
    }
}
