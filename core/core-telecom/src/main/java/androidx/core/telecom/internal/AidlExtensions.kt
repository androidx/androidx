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

package androidx.core.telecom.internal

import android.util.Log
import androidx.core.telecom.extensions.Extensions
import androidx.core.telecom.extensions.IActionsResultCallback
import androidx.core.telecom.extensions.ICallDetailsListener
import androidx.core.telecom.extensions.ICapabilityExchange
import androidx.core.telecom.extensions.ICapabilityExchangeListener
import androidx.core.telecom.extensions.IParticipantActions
import androidx.core.telecom.extensions.IParticipantStateListener
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantParcelable
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Remote interface for communicating back to the remote interface the result of an Action. */
@ExperimentalAppActions
internal class ActionsResultCallbackRemote(binder: IActionsResultCallback) :
    IActionsResultCallback by binder

/**
 * Implements the binder interface that is used by the remote InCallService in order to perform an
 * Action and calls the delegate callback function if an action has registered to listen.
 */
@ExperimentalAppActions
internal class ParticipantActionCallbackRepository(coroutineScope: CoroutineScope) {
    companion object {
        private const val LOG_TAG = Extensions.LOG_TAG + "(PACR)"
    }

    /**
     * The callback that is called when the remote InCallService changes the raised hand state of
     * this user.
     */
    var raiseHandStateCallback: (suspend (Boolean) -> Unit)? = null

    /**
     * The callback that is called when the remote InCallService requests to kick a participant
     * using its id.
     */
    var kickParticipantCallback: (suspend (String) -> Unit)? = null

    /** Listener used to handle event callbacks from the remote. */
    val eventListener =
        object : IParticipantActions.Stub() {
            override fun setHandRaised(handRaisedState: Boolean, cb: IActionsResultCallback?) {
                cb?.let {
                    coroutineScope.launch {
                        Log.i(LOG_TAG, "from remote: raiseHandStateChanged=$handRaisedState")
                        raiseHandStateCallback?.invoke(handRaisedState)
                        ActionsResultCallbackRemote(cb).onSuccess()
                    }
                }
            }

            override fun kickParticipant(participantId: String, cb: IActionsResultCallback?) {
                cb?.let {
                    coroutineScope.launch {
                        Log.i(LOG_TAG, "from remote: kickParticipant=$participantId")
                        kickParticipantCallback?.invoke(participantId)
                        ActionsResultCallbackRemote(cb).onSuccess()
                    }
                }
            }
        }
}

/** Remote interface used by InCallServices to send action events to the VOIP application. */
@ExperimentalAppActions
internal class ParticipantActionsRemote(binder: IParticipantActions) :
    IParticipantActions by binder {
    fun kickParticipant(participant: Participant, cb: IActionsResultCallback?) {
        kickParticipant(participant.id, cb)
    }
}

/**
 * Remote interface used to notify the ICS of participant state information
 *
 * @param binder The remote binder interface to wrap
 */
@ExperimentalAppActions
internal class ParticipantStateListenerRemote(private val binder: IParticipantStateListener) {
    fun updateParticipants(participants: Set<Participant>) {
        binder.updateParticipants(
            participants.map(Participant::toParticipantParcelable).toTypedArray()
        )
    }

    fun updateActiveParticipant(activeParticipant: Participant?) {
        binder.updateActiveParticipant(activeParticipant?.id)
    }

    fun updateRaisedHandsAction(participants: List<Participant>) {
        binder.updateRaisedHandsAction(participants.map { it.id }.toTypedArray())
    }

    fun finishSync(actions: IParticipantActions) {
        binder.finishSync(actions)
    }
}

/**
 * The remote interface used to begin capability exchange with the InCallService.
 *
 * @param binder the remote binder interface.
 */
@ExperimentalAppActions
internal class CapabilityExchangeRemote(binder: ICapabilityExchange) :
    ICapabilityExchange by binder

/**
 * Remote interface for [ICapabilityExchangeListener] that InCallServices use to communicate with
 * the remote VOIP application.
 */
@ExperimentalAppActions
internal class CapabilityExchangeListenerRemote(binder: ICapabilityExchangeListener) :
    ICapabilityExchangeListener by binder

/**
 * Adapter class that implements [IParticipantStateListener] AIDL and calls the associated callbacks
 */
@ExperimentalAppActions
internal class ParticipantStateListener(
    private val updateParticipants: (Set<Participant>) -> Unit,
    private val updateActiveParticipantId: (String?) -> Unit,
    private val updateRaisedHandIds: (List<String>) -> Unit,
    private val finishSync: (ParticipantActionsRemote?) -> Unit
) : IParticipantStateListener.Stub() {
    override fun updateParticipants(participants: Array<out ParticipantParcelable>?) {
        updateParticipants.invoke(
            participants?.map { Participant(it.id, it.name) }?.toSet() ?: emptySet()
        )
    }

    override fun updateActiveParticipant(activeParticipantId: String?) {
        updateActiveParticipantId.invoke(activeParticipantId)
    }

    override fun updateRaisedHandsAction(participants: Array<out String>?) {
        updateRaisedHandIds.invoke(participants?.toList() ?: emptyList())
    }

    override fun finishSync(cb: IParticipantActions?) {
        if (cb == null) {
            Log.w("AidlExtensions", "finishSync returned null actions!")
        }
        finishSync.invoke(cb?.let { ParticipantActionsRemote(it) })
    }
}

/**
 * The repository containing the methods used during capability exchange to create each extension.
 * Extensions will use this to register themselves as handlers of these callbacks.
 *
 * @param connectionScope The [CoroutineScope] that governs this connection to the remote. This
 *   scope will be cancelled by this class when the remote notifies us that the connection is being
 *   torn down.
 */
@ExperimentalAppActions
internal class CapabilityExchangeRepository(private val connectionScope: CoroutineScope) {

    /** A request to create the ParticipantExtension has been received */
    var onCreateParticipantExtension:
        ((CoroutineScope, Set<Int>, ParticipantStateListenerRemote) -> Unit)? =
        null

    val listener =
        object : ICapabilityExchangeListener.Stub() {
            override fun onCreateParticipantExtension(
                version: Int,
                actions: IntArray?,
                l: IParticipantStateListener?
            ) {
                l?.let {
                    onCreateParticipantExtension?.invoke(
                        connectionScope,
                        actions?.toSet() ?: emptySet(),
                        ParticipantStateListenerRemote(l)
                    )
                }
            }

            override fun onCreateCallDetailsExtension(
                version: Int,
                actions: IntArray?,
                l: ICallDetailsListener?,
                packageName: String?
            ) {
                TODO("Not yet implemented")
            }

            override fun onRemoveExtensions() {
                connectionScope.cancel("remote has removed extensions")
            }
        }
}
