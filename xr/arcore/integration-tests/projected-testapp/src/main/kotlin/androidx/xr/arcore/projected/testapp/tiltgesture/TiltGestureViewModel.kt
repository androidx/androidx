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

package androidx.xr.arcore.projected.testapp.tiltgesture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.arcore.Tilt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalGesturesApi::class)
internal class TiltGestureViewModel : ViewModel() {
    private val _tilt = MutableStateFlow<Tilt>(Tilt.UP)
    private val _progress = MutableStateFlow(0f)
    private val _message = MutableStateFlow("Initializing...")

    internal val tilt = _tilt.asStateFlow()
    internal val progress = _progress.asStateFlow()
    internal val message = _message.asStateFlow()

    internal fun setTiltGestureState(tilt: Tilt, progress: Float) {
        viewModelScope.launch {
            _tilt.emit(tilt)
            _progress.emit(progress)
        }
    }

    internal fun setMessage(message: String) {
        viewModelScope.launch { _message.emit(message) }
    }
}
