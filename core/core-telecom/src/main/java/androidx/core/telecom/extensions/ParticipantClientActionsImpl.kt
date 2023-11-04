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

package androidx.core.telecom.extensions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal class ParticipantClientActionsImpl(
    internal val mScope: CoroutineScope,
    internal var mNegotiatedActions: Set<Int>,
    internal var mOnInitializationComplete: (ParticipantClientActions) -> Unit
) : ParticipantClientActions, IParticipantStateListener.Stub() {
    private val mParticipantsStateFlow: MutableStateFlow<Set<Participant>> =
        MutableStateFlow(emptySet())
    private val mRaisedHandsStateFlow: MutableStateFlow<Set<Int>> =
        MutableStateFlow(emptySet())
    private val mActiveParticipantStateFlow: MutableStateFlow<Int> =
        MutableStateFlow(-1)

    internal var mIsParticipantExtensionSupported: Boolean = true
    internal var mIsInitializationComplete = false
    private lateinit var mActions: IParticipantActions

    companion object {
        private val TAG = ParticipantClientActionsImpl::class.simpleName
    }

    override val negotiatedActions: Set<Int>
        get() = mNegotiatedActions

    override val isParticipantExtensionSupported: Boolean
        get() = mIsParticipantExtensionSupported

    override val participantsStateFlow: StateFlow<Set<Participant>>
        get() = mParticipantsStateFlow.asStateFlow()

    override val raisedHandsStateFlow: StateFlow<Set<Int>>
        get() = mRaisedHandsStateFlow.asStateFlow()

    override val activeParticipantStateFlow: StateFlow<Int>
        get() = mActiveParticipantStateFlow.asStateFlow()

    override suspend fun toggleHandRaised(isHandRaised: Boolean): CallControlResult {
        val resultCallback = ActionsResultCallback()
        mActions.toggleHandRaised(resultCallback)

        return mScope.async {
            resultCallback.waitForResponse()
        }.await()
    }

    override suspend fun kickParticipant(participantId: Int): CallControlResult {
        val resultCallback = ActionsResultCallback()
        mActions.kickParticipant(participantId, resultCallback)

        return mScope.async {
            resultCallback.waitForResponse()
        }.await()
    }

    override fun updateParticipants(participants: Array<out Participant>?) {
        participants ?: return
        mParticipantsStateFlow.value = participants.toSet()
    }

    override fun updateActiveParticipant(activeParticipant: Int) {
        mActiveParticipantStateFlow.value = activeParticipant
    }

    override fun updateRaisedHandsAction(participantsWithHandsRaised: IntArray?) {
        participantsWithHandsRaised ?: return
        mRaisedHandsStateFlow.value = participantsWithHandsRaised.toSet()
    }

    override fun finishSync(cb: IParticipantActions?) {
        cb ?: return
        mActions = cb
        mIsInitializationComplete = true
        mOnInitializationComplete(this)
    }
    // Todo: Consider defining an internal cache here to handle the case of out of order operations.
}
