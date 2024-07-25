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
 * Implements the action to kick a Participant that part of the call and is being tracked via
 * [CallExtensionsScope.addParticipantExtension]
 *
 * @param participants A [StateFlow] representing the current Set of Participants that are in the
 *   call.
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal class KickParticipantClientAction(
    private val participants: StateFlow<Set<Participant>>,
) {
    companion object {
        const val TAG = CallExtensionsScope.TAG + "(KPCA)"
    }

    /**
     * Whether or not kicking participants is supported by the remote.
     *
     * if `true`, then requests to kick participants will be sent to the remote application. If
     * `false`, then the remote doesn't support this action and requests will fail.
     *
     * Should not be queried until [CallExtensionsScope.onConnected] is called.
     */
    var isSupported by Delegates.notNull<Boolean>()
    // The binder interface that allows this action to send events to the remote
    private var remoteActions: ParticipantActionsRemote? = null

    /**
     * Request to kick a [participant] in the call.
     *
     * Note: This operation succeeding does not mean that the participant was kicked, it only means
     * that the request was received by the remote application.
     *
     * @param participant The participant to kick
     * @return The result of whether or not this request was successfully sent to the remote
     *   application
     */
    suspend fun requestKickParticipant(participant: Participant): CallControlResult {
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
