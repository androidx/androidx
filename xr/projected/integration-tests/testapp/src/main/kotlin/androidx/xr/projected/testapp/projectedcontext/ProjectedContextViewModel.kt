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

package androidx.xr.projected.testapp.projectedcontext

import androidx.lifecycle.ViewModel
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel holding UI state and telemetry for ProjectedContext testing. */
@OptIn(ExperimentalProjectedApi::class)
class ProjectedContextViewModel : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _packageName = MutableStateFlow("")
    val packageName: StateFlow<String> = _packageName.asStateFlow()

    private val _relaunchCount = MutableStateFlow(0)
    val relaunchCount: StateFlow<Int> = _relaunchCount.asStateFlow()

    private val _restartCount = MutableStateFlow(0)
    val restartCount: StateFlow<Int> = _restartCount.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun setDeviceName(name: String?) {
        _deviceName.value = name
    }

    fun setPackageName(pkg: String) {
        _packageName.value = pkg
    }

    fun setRelaunchCount(count: Int) {
        _relaunchCount.value = count
    }

    fun setRestartCount(count: Int) {
        _restartCount.value = count
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }
}
