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

package androidx.core.telecom.test.VoipAppWithExtensions

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_INCOMING
import androidx.core.telecom.CallAttributesCompat.Companion.DIRECTION_OUTGOING
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantParcelable
import androidx.core.telecom.extensions.toParticipant
import androidx.core.telecom.test.ITestAppControl
import androidx.core.telecom.test.ITestAppControlCallback
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalAppActions::class)
@RequiresApi(Build.VERSION_CODES.O)
open class VoipAppWithExtensionsControl : Service() {
    var mCallsManager: CallsManager? = null
    private var mScope: CoroutineScope? = null
    private var mCallback: ITestAppControlCallback? = null
    private var participantsFlow: MutableStateFlow<Set<Participant>> = MutableStateFlow(emptySet())
    private var activeParticipantFlow: MutableStateFlow<Participant?> = MutableStateFlow(null)
    private var raisedHandsFlow: MutableStateFlow<List<Participant>> = MutableStateFlow(emptyList())
    // TODO:: b/364316364 should be Pair(callId:String, value: Boolean)
    private var isLocallySilencedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        val TAG = VoipAppWithExtensionsControl::class.java.simpleName
        val CLASS_NAME = VoipAppWithExtensionsControl::class.java.canonicalName
    }

    private val mBinder: ITestAppControl.Stub =
        object : ITestAppControl.Stub() {

            override fun setCallback(callback: ITestAppControlCallback) {
                mCallback = callback
            }

            val mOnSetActiveLambda: suspend () -> Unit = { Log.i(TAG, "onSetActive: completing") }

            val mOnSetInActiveLambda: suspend () -> Unit = {
                Log.i(TAG, "onSetInactive: completing")
            }

            val mOnAnswerLambda: suspend (type: Int) -> Unit = {
                Log.i(TAG, "onAnswer: callType=[$it]")
            }

            val mOnDisconnectLambda: suspend (cause: DisconnectCause) -> Unit = {
                Log.i(TAG, "onDisconnect: disconnectCause=[$it]")
            }

            override fun addCall(
                requestId: Int,
                capabilities: List<Capability>,
                isOutgoing: Boolean
            ) {
                Log.i(TAG, "VoipAppWithExtensionsControl: addCall: request")
                runBlocking {
                    val call = VoipCall(mCallsManager!!, mCallback, capabilities)
                    mScope?.launch {
                        with(call) {
                            addCall(
                                CallAttributesCompat(
                                    "displayName" /* TODO:: make helper */,
                                    Uri.parse("tel:123") /* TODO:: make helper */,
                                    if (isOutgoing) DIRECTION_OUTGOING else DIRECTION_INCOMING
                                ),
                                mOnAnswerLambda,
                                mOnDisconnectLambda,
                                mOnSetActiveLambda,
                                mOnSetInActiveLambda
                            ) {
                                launch { setActive() }
                                isMuted
                                    .onEach { mCallback?.onGlobalMuteStateChanged(it) }
                                    .launchIn(this)

                                participantsFlow
                                    .onEach {
                                        TestUtils.printParticipants(it, "VoIP participants")
                                        participantStateUpdater?.updateParticipants(it)
                                    }
                                    .launchIn(this)
                                raisedHandsFlow
                                    .onEach {
                                        TestUtils.printParticipants(it, "VoIP raised hands")
                                        raiseHandStateUpdater?.updateRaisedHands(it)
                                    }
                                    .launchIn(this)
                                activeParticipantFlow
                                    .onEach {
                                        Log.i(TAG, "VOIP active participant: $it")
                                        participantStateUpdater?.updateActiveParticipant(it)
                                    }
                                    .launchIn(this)
                                isLocallySilencedFlow
                                    .drop(1) // ignore the first value from the voip app
                                    // since only values from the test should be sent!
                                    .onEach {
                                        Log.i(TAG, "VoIP isLocallySilenced=[$it]")
                                        // TODO:: b/364316364 gate on callId
                                        localCallSilenceUpdater?.updateIsLocallySilenced(it)
                                    }
                                    .launchIn(this)
                                mCallback?.onCallAdded(requestId, this.getCallId().toString())
                            }
                        }
                    }
                }
            }

            override fun updateParticipants(setOfParticipants: List<ParticipantParcelable>) {
                participantsFlow.value = setOfParticipants.map { it.toParticipant() }.toSet()
            }

            override fun updateActiveParticipant(participant: ParticipantParcelable?) {
                activeParticipantFlow.value = participant?.toParticipant()
            }

            override fun updateRaisedHands(raisedHandsParticipants: List<ParticipantParcelable>) {
                raisedHandsFlow.value = raisedHandsParticipants.map { it.toParticipant() }
            }

            // TODO:: b/364316364 add CallId arg.  Should be changing on a per call basis
            override fun updateIsLocallySilenced(isLocallySilenced: Boolean) {
                isLocallySilencedFlow.value = isLocallySilenced
            }
        }

    override fun onBind(intent: Intent?): IBinder? {
        mScope = CoroutineScope(Dispatchers.Default)
        if (intent?.component?.className.equals(getClassName())) {
            mCallsManager = CallsManager(applicationContext)
            return mBinder
        }
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mScope?.cancel(CancellationException("Control interface is unbinding"))
        mScope = null
        mCallback = null
        participantsFlow.value = emptySet()
        activeParticipantFlow.value = null
        raisedHandsFlow.value = emptyList()
        return false
    }

    open fun getClassName(): String? {
        return CLASS_NAME
    }
}
