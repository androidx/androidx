/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected.testapp.input

import androidx.lifecycle.ViewModel
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel holding UI state, camera toggle status, and input event logs for Input test. */
@OptIn(ExperimentalProjectedApi::class)
class InputViewModel : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _cameraToggleStatus = MutableStateFlow("Default State")
    val cameraToggleStatus: StateFlow<String> = _cameraToggleStatus.asStateFlow()

    private val _eventLogs = MutableStateFlow<List<String>>(emptyList())
    val eventLogs: StateFlow<List<String>> = _eventLogs.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun setCameraToggleStatus(status: String) {
        _cameraToggleStatus.value = status
    }

    fun addEventLog(log: String) {
        _eventLogs.value = _eventLogs.value + log
    }

    fun clearEventLogs() {
        _eventLogs.value = emptyList()
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }
}
