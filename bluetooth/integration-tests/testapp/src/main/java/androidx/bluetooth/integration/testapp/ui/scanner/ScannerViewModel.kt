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

package androidx.bluetooth.integration.testapp.ui.scanner

import android.annotation.SuppressLint
import android.util.Log
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanException
import androidx.bluetooth.ScanResult
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
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

@HiltViewModel
class ScannerViewModel @Inject constructor(private val bluetoothLe: BluetoothLe) : ViewModel() {

    internal companion object {
        private const val TAG = "ScannerViewModel"
    }

    private val scanResultsMap = mutableMapOf<UUID, ScanResult>()

    var scanJob: Job? = null

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startScan() {
        Log.d(TAG, "startScan() called")

        scanJob =
            bluetoothLe
                .scan()
                .onStart {
                    Log.d(TAG, "bluetoothLe.scan() onStart")
                    _uiState.update { it.copy(isScanning = true, resultMessage = "Scan started") }
                }
                .filterNot { scanResultsMap.containsKey(it.device.id) }
                .catch { throwable ->
                    Log.e(TAG, "bluetoothLe.scan() catch", throwable)

                    val message =
                        if (throwable is ScanException) {
                            when (throwable.errorCode) {
                                ScanException.APPLICATION_REGISTRATION_FAILED ->
                                    "Scan failed. Application registration failed"
                                ScanException.INTERNAL_ERROR -> "Scan failed. Internal error"
                                ScanException.UNSUPPORTED -> "Scan failed. Feature unsupported"
                                ScanException.OUT_OF_HARDWARE_RESOURCES ->
                                    "Scan failed. Out of hardware resources"
                                ScanException.SCANNING_TOO_FREQUENTLY ->
                                    "Scan failed. Scanning too frequently"
                                else -> "Scan failed. Error unknown"
                            }
                        } else if (throwable is IllegalStateException) {
                            throwable.message
                        } else null

                    _uiState.update { it.copy(resultMessage = message) }
                }
                .onEach { scanResult ->
                    Log.d(TAG, "bluetoothLe.scan() onEach: $scanResult")
                    scanResultsMap[scanResult.device.id] = scanResult
                    _uiState.update { it.copy(scanResults = scanResultsMap.values.toList()) }
                }
                .onCompletion { throwable ->
                    Log.e(TAG, "bluetoothLe.scan() onCompletion", throwable)

                    _uiState.update {
                        it.copy(isScanning = false, resultMessage = "Scan completed")
                    }
                }
                .launchIn(viewModelScope)
    }

    fun clearResultMessage() {
        _uiState.update { it.copy(resultMessage = null) }
    }
}
