/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.testdialerapp

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// A singleton object to manage the active call state.
object CallManager {
    private val _call = MutableStateFlow<Call?>(null)
    val call = _call.asStateFlow()

    private val _callState = MutableStateFlow<Int?>(null)
    val callState = _callState.asStateFlow()

    private val callback =
        object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                _callState.value = state
            }
        }

    @Suppress("DEPRECATION")
    fun registerCall(call: Call) {
        // Unregister any previous call
        _call.value?.unregisterCallback(callback)

        _call.value = call
        _callState.value = call.state
        call.registerCallback(callback)
    }

    fun unregisterCall() {
        _call.value?.unregisterCallback(callback)
        _call.value = null
        _callState.value = null
    }

    fun hangup() {
        _call.value?.disconnect()
    }
}
