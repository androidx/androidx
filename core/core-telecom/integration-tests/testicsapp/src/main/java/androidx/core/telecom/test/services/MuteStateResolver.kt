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

import android.os.Build
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks the current global mute state of the device */
class MuteStateResolver {
    private val isCallAudioStateDeprecated =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    private val mMuteState = MutableStateFlow(false)
    val muteState = mMuteState.asStateFlow()

    /** The audio state of the device has changed for devices using API version < UDC */
    fun onCallAudioStateChanged(audioState: CallAudioState?) {
        if (audioState == null || isCallAudioStateDeprecated) return
        mMuteState.value = audioState.isMuted
    }

    /** The audio state of the device has changed for devices using API version UDC+ */
    fun onMuteStateChanged(isMuted: Boolean) {
        if (!isCallAudioStateDeprecated) return
        mMuteState.value = isMuted
    }
}
