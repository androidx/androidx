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

package androidx.core.telecom.test.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class AutoVoipConnection : LocalMicrophoneSilenceConnection {
    var mContext: Context? = null

    companion object {
        val TAG: String = AutoVoipConnection::class.java.simpleName
    }

    constructor(c: Context) {
        setConnectionProperties(PROPERTY_SELF_MANAGED)
        setCallerDisplayName("John Voip", TelecomManager.PRESENTATION_ALLOWED)
        setConnectionCapabilities(CAPABILITY_SUPPORT_HOLD or CAPABILITY_MUTE or CAPABILITY_HOLD)
        mContext = c
    }

    override fun onShowIncomingCallUi() {
        Log.i(TAG, "onShowIncomingCallUi")
    }

    @Suppress("OVERRIDE_DEPRECATION") // b/407498327
    override fun onCallAudioStateChanged(state: CallAudioState) {
        Log.i(TAG, "onCallAudioStateChanged")
    }

    override fun onHold() {
        Log.i(TAG, "onHold")
        setOnHold()
    }

    override fun onUnhold() {
        Log.i(TAG, "onUnhold")
        setActive()
    }

    override fun onAnswer() {
        Log.i(TAG, "onAnswer")
        setActive()
    }

    override fun onReject() {
        Log.i(TAG, "onReject")
        onDisconnect()
    }

    override fun onDisconnect() {
        Log.i(TAG, "onDisconnect")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    fun updateSpeakerAndParticipants(speaker: String, participants: Int) {
        val moreExtras = Bundle()
        Log.i(TAG, "updateSpeakerAndParticipants: " + speaker + " : " + participants)
        setCurrentSpeaker(speaker)
        setParticipantCount(participants)
        putExtras(moreExtras)
    }

    override fun onCallMicrophoneSilenceStateChanged(callSilenced: Boolean) {
        Log.i(TAG, "onCallMicrophoneSilencedStateChanged: " + callSilenced)
        setCallMicrophoneSilenceState(callSilenced)
    }
}
