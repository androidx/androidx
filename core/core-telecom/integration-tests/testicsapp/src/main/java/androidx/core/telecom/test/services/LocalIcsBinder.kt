/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test.services

import kotlinx.coroutines.flow.StateFlow

/** Local interface used to define the local connection between a component and this Service. */
interface LocalIcsBinder {
    /** Connector used during Service binding to capture the instance of this class */
    interface Connector {
        fun getService(): LocalIcsBinder
    }

    /** the state of active calls on this device */
    val callData: StateFlow<List<CallData>>
    /** The state of global mute on this device */
    val isMuted: StateFlow<Boolean>
    /** The current audio route that the active call is using */
    val currentAudioEndpoint: StateFlow<CallAudioEndpoint?>
    /** The available audio routes for the active call */
    val availableAudioEndpoints: StateFlow<List<CallAudioEndpoint>>

    /** Request to change the mute state of the device */
    fun onChangeMuteState(isMuted: Boolean)

    /** Request to change the current audio route on the device */
    suspend fun onChangeAudioRoute(id: String)
}
