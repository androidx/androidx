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

package androidx.xr.projected.testapp.audio

import androidx.lifecycle.ViewModel
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel holding UI and device state for [AudioActivity]. */
@OptIn(ExperimentalProjectedApi::class)
class AudioViewModel : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _inputDevices = MutableStateFlow<List<String>>(emptyList())
    val inputDevices: StateFlow<List<String>> = _inputDevices.asStateFlow()

    private val _outputDevices = MutableStateFlow<List<String>>(emptyList())
    val outputDevices: StateFlow<List<String>> = _outputDevices.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setAudioDevices(inputs: List<String>, outputs: List<String>) {
        _inputDevices.value = inputs
        _outputDevices.value = outputs
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }
}
