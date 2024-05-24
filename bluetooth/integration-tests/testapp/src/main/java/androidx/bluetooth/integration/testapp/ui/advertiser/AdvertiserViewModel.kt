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

package androidx.bluetooth.integration.testapp.ui.advertiser

import android.annotation.SuppressLint
import android.util.Log
import androidx.bluetooth.AdvertiseException
import androidx.bluetooth.AdvertiseParams
import androidx.bluetooth.BluetoothLe
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class AdvertiserViewModel @Inject constructor(private val bluetoothLe: BluetoothLe) : ViewModel() {

    private companion object {
        private const val TAG = "AdvertiserViewModel"
    }

    // TODO(b/309360030) Complete missing AdvertiseParams in testapp
    internal var includeDeviceAddress: Boolean = false
    internal var includeDeviceName: Boolean = true
    internal var connectable: Boolean = true
    internal var discoverable: Boolean = true
    internal var durationMillis: Long = 0
    internal var manufacturerDatas: MutableList<Pair<Int, ByteArray>> = mutableListOf()
    internal var serviceDatas: MutableList<Pair<UUID, ByteArray>> = mutableListOf()
    internal var serviceUuids: MutableList<UUID> = mutableListOf()
    internal var serviceSolicitationUuids: MutableList<UUID> = mutableListOf()

    val advertiseData: List<String>
        get() =
            listOf(
                    manufacturerDatas.map {
                        "Manufacturer Data:\n" +
                            "Company ID: 0x${it.first} Data: 0x${it.second.toString(Charsets.UTF_8)}"
                    },
                    serviceDatas.map {
                        "Service Data:\n" +
                            "UUID: ${it.first} Data: 0x${it.second.toString(Charsets.UTF_8)}"
                    },
                    serviceUuids.map { "128-bit Service UUID:\n" + "$it" },
                    serviceSolicitationUuids.map { "128-bit Service Solicitation UUID:\n" + "$it" }
                )
                .flatten()

    var advertiseJob: Job? = null

    private val advertiseParams: AdvertiseParams
        get() =
            AdvertiseParams(
                includeDeviceAddress,
                includeDeviceName,
                connectable,
                discoverable,
                durationMillis,
                manufacturerDatas.toMap(),
                serviceDatas.toMap(),
                serviceUuids,
                serviceSolicitationUuids
            )

    private val _uiState = MutableStateFlow(AdvertiserUiState())
    val uiState: StateFlow<AdvertiserUiState> = _uiState.asStateFlow()

    fun removeAdvertiseDataAtIndex(index: Int) {
        val manufacturerDataSize = manufacturerDatas.size
        val serviceDataSize = serviceDatas.size
        val serviceUuidsSize = serviceUuids.size

        if (index < manufacturerDataSize) {
            manufacturerDatas.removeAt(index)
        } else if (index < manufacturerDataSize + serviceDataSize) {
            serviceDatas.removeAt(index - manufacturerDataSize)
        } else if (index < manufacturerDataSize + serviceDataSize + serviceUuidsSize) {
            serviceUuids.removeAt(index - manufacturerDataSize - serviceDataSize)
        } else {
            serviceSolicitationUuids.removeAt(
                index - manufacturerDataSize - serviceDataSize - serviceUuidsSize
            )
        }
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    fun startAdvertise() {
        Log.d(TAG, "startAdvertise() called")

        advertiseJob =
            bluetoothLe
                .advertise(advertiseParams)
                .catch { throwable ->
                    Log.e(TAG, "bluetoothLe.advertise catch", throwable)

                    val message =
                        if (throwable is AdvertiseException) {
                            when (throwable.errorCode) {
                                AdvertiseException.DATA_TOO_LARGE ->
                                    "Advertise failed. Data too large"
                                AdvertiseException.TOO_MANY_ADVERTISERS ->
                                    "Advertise failed. Too many advertisers"
                                AdvertiseException.INTERNAL_ERROR ->
                                    "Advertise failed. Internal error"
                                AdvertiseException.UNSUPPORTED ->
                                    "Advertise failed. Feature unsupported"
                                else -> "Advertise failed. Error unknown"
                            }
                        } else if (throwable is IllegalStateException) {
                            throwable.message
                        } else null

                    _uiState.update { it.copy(resultMessage = message) }
                }
                .onEach { advertiseResult ->
                    Log.d(TAG, "bluetoothLe.advertise onEach: $advertiseResult")

                    if (advertiseResult == BluetoothLe.ADVERTISE_STARTED) {
                        _uiState.update {
                            it.copy(isAdvertising = true, resultMessage = "Advertise started")
                        }
                    }
                }
                .onCompletion {
                    Log.d(TAG, "bluetoothLe.advertise onCompletion")
                    _uiState.update {
                        it.copy(isAdvertising = false, resultMessage = "Advertise completed")
                    }
                }
                .launchIn(viewModelScope)
    }

    fun clearResultMessage() {
        _uiState.update { it.copy(resultMessage = null) }
    }
}
