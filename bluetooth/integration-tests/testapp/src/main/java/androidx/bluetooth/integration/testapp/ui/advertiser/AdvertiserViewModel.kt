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
import androidx.bluetooth.AdvertiseParams
import androidx.bluetooth.BluetoothLe
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AdvertiserViewModel @Inject constructor(
    private val bluetoothLe: BluetoothLe
) : ViewModel() {

    private companion object {
        private const val TAG = "AdvertiserViewModel"
    }

    var includeDeviceAddress = false
    var includeDeviceName = false
    var connectable = false
    var discoverable = false
    var duration: Duration = Duration.ZERO
    var manufacturerDatas = mutableListOf<Pair<Int, ByteArray>>()
    var serviceDatas = mutableListOf<Pair<UUID, ByteArray>>()
    var serviceUuids = mutableListOf<UUID>()

    val advertiseData: List<String>
        get() = listOf(
            manufacturerDatas
                .map { "Manufacturer Data:\n" +
                    "Company ID: 0x${it.first} Data: 0x${it.second.toString(Charsets.UTF_8)}" },
            serviceDatas
                .map { "Service Data:\n" +
                    "UUID: ${it.first} Data: 0x${it.second.toString(Charsets.UTF_8)}" },
            serviceUuids
                .map { "128-bit Service UUID:\n" +
                    "$it" }
        ).flatten()

    var advertiseJob: Job? = null

    private val advertiseParams: AdvertiseParams
        get() = AdvertiseParams(
            includeDeviceAddress,
            includeDeviceName,
            connectable,
            discoverable,
            duration,
            manufacturerDatas.toMap(),
            serviceDatas.toMap(),
            serviceUuids
        )

    private val _uiState = MutableStateFlow(AdvertiserUiState())
    val uiState: StateFlow<AdvertiserUiState> = _uiState.asStateFlow()

    fun removeAdvertiseDataAtIndex(index: Int) {
        val manufacturerDataSize = manufacturerDatas.size
        val serviceDataSize = serviceDatas.size

        if (index < manufacturerDataSize) {
            manufacturerDatas.removeAt(index)
        } else if (index < serviceDataSize + manufacturerDataSize) {
            serviceDatas.removeAt(index - manufacturerDataSize)
        } else {
            serviceUuids.removeAt(index - manufacturerDataSize - serviceDataSize)
        }
    }

    // Permissions are handled by MainActivity requestBluetoothPermissions
    @SuppressLint("MissingPermission")
    fun startAdvertise() {
        Log.d(TAG, "startAdvertise() called")

        advertiseJob = viewModelScope.launch {
            Log.d(TAG, "bluetoothLe.advertise() called with: advertiseParams = $advertiseParams")
            _uiState.update {
                it.copy(isAdvertising = true)
            }

            bluetoothLe.advertise(advertiseParams) {
                Log.d(TAG, "bluetoothLe.advertise result: AdvertiseResult = $it")

                val message = when (it) {
                    BluetoothLe.ADVERTISE_STARTED ->
                        "ADVERTISE_STARTED"

                    BluetoothLe.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        "ADVERTISE_FAILED_DATA_TOO_LARGE"

                    BluetoothLe.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"

                    BluetoothLe.ADVERTISE_FAILED_INTERNAL_ERROR ->
                        "ADVERTISE_FAILED_INTERNAL_ERROR"

                    BluetoothLe.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"

                    else -> null
                }
                _uiState.update { state ->
                    state.copy(resultMessage = message)
                }
            }
        }

        advertiseJob?.invokeOnCompletion {
            Log.d(TAG, "bluetoothLe.advertise completed")
            _uiState.update {
                it.copy(isAdvertising = false, resultMessage = "ADVERTISE_COMPLETED")
            }
        }
    }

    fun resultMessageShown() {
        _uiState.update {
            it.copy(resultMessage = null)
        }
    }
}
