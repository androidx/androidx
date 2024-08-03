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

package androidx.core.telecom.extensions

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.ParticipantActionsRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.properties.Delegates
import kotlinx.coroutines.flow.StateFlow

/**
 * Implements the action to kick a Participant that is part of the call and is being tracked via
 * [CallExtensionScope.addParticipantExtension]
 *
 * @param participants A [StateFlow] representing the current Set of Participants that are in the
 *   call.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAppActions::class)
internal class KickParticipantActionImpl(
    private val participants: StateFlow<Set<Participant>>,
) : KickParticipantAction {
    companion object {
        const val TAG = CallExtensionScopeImpl.TAG + "(KPCA)"
    }

    override var isSupported by Delegates.notNull<Boolean>()
    // The binder interface that allows this action to send events to the remote
    private var remoteActions: ParticipantActionsRemote? = null

    override suspend fun requestKickParticipant(participant: Participant): CallControlResult {
        Log.d(TAG, "kickParticipant: participant=$participant")
        if (remoteActions == null) {
            Log.w(TAG, "kickParticipant: no binder, isSupported=$isSupported")
            // TODO: This needs to have its own CallException result
            return CallControlResult.Error(CallException.ERROR_UNKNOWN)
        }
        if (!participants.value.contains(participant)) {
            Log.d(TAG, "kickParticipant: couldn't find participant=$participant")
            return CallControlResult.Success()
        }
        val cb = ActionsResultCallback()
        remoteActions?.kickParticipant(participant, cb)
        val result = cb.waitForResponse()
        Log.d(TAG, "kickParticipant: participant=$participant, result=$result")
        return result
    }

    /** Called when capability exchange has completed and we can initialize this action */
    internal fun initialize(isSupported: Boolean) {
        Log.d(TAG, "initialize: isSupported=$isSupported")
        this.isSupported = isSupported
    }

    /** Called when the remote application has connected and will receive action event requests */
    internal fun connect(remote: ParticipantActionsRemote?) {
        Log.d(TAG, "connect: remote is null=${remote == null}")
        remoteActions = remote
    }
}
