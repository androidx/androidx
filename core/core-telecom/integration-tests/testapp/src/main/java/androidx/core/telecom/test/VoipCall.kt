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

package androidx.core.telecom.test

import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat

@RequiresApi(34)
class VoipCall {
    private val TAG = VoipCall::class.simpleName

    var mAdapter: CallListAdapter? = null
    var mCallControl: CallControlScope? = null
    var mParticipantControl: ParticipantControl? = null
    var mCurrentEndpoint: CallEndpointCompat? = null
    var mAvailableEndpoints: List<CallEndpointCompat>? = ArrayList()
    var mIsMuted = false
    var mTelecomCallId: String = ""

    val mOnSetActiveLambda: suspend () -> Unit = {
        Log.i(TAG, "onSetActive: completing")
        mAdapter?.updateCallState(mTelecomCallId, "Active")
    }

    val mOnSetInActiveLambda: suspend () -> Unit = {
        Log.i(TAG, "onSetInactive: completing")
        mAdapter?.updateCallState(mTelecomCallId, "Inactive")
    }

    val mOnAnswerLambda: suspend (type: Int) -> Unit = {
        Log.i(TAG, "onAnswer: callType=[$it]")
        mAdapter?.updateCallState(mTelecomCallId, "Answered")
    }

    val mOnDisconnectLambda: suspend (cause: DisconnectCause) -> Unit = {
        Log.i(TAG, "onDisconnect: disconnectCause=[$it]")
        mAdapter?.updateCallState(mTelecomCallId, "Disconnected")
    }

    fun setCallControl(callControl: CallControlScope) {
        mCallControl = callControl
    }

    fun setParticipantControl(participantControl: ParticipantControl) {
        mParticipantControl = participantControl
    }

    fun setCallAdapter(adapter: CallListAdapter?) {
        mAdapter = adapter
    }

    fun setCallId(callId: String) {
        mTelecomCallId = callId
    }

    fun onParticipantsChanged(participants: List<ParticipantState>) {
        mAdapter?.updateParticipants(mTelecomCallId, participants)
    }

    fun onCallEndpointChanged(endpoint: CallEndpointCompat) {
        Log.i(TAG, "onCallEndpointChanged: endpoint=$endpoint")
        mCurrentEndpoint = endpoint
        mAdapter?.updateEndpoint(mTelecomCallId, endpoint.name.toString())
    }

    fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpointCompat>) {
        Log.i(TAG, "onAvailableCallEndpointsChanged:")
        for (endpoint in endpoints) {
            Log.i(TAG, "onAvailableCallEndpointsChanged: --> endpoint=$endpoint")
        }
        mAvailableEndpoints = endpoints
    }

    fun onMuteStateChanged(isMuted: Boolean) {
        Log.i(TAG, "onMuteStateChanged: isMuted=$isMuted")
        mIsMuted = isMuted
    }

    fun getEndpointType(type: Int): CallEndpointCompat? {
        for (endpoint in mAvailableEndpoints!!) {
            if (endpoint.type == type) {
                return endpoint
            }
        }
        return null
    }
}
