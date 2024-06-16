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

import androidx.core.telecom.extensions.IActionsResultCallback
import androidx.core.telecom.extensions.ICallDetailsListener
import androidx.core.telecom.extensions.ICapabilityExchange
import androidx.core.telecom.extensions.ICapabilityExchangeListener
import androidx.core.telecom.extensions.IParticipantActions
import androidx.core.telecom.extensions.IParticipantStateListener
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.util.ExperimentalAppActions

/** Remote interface for communicating back to the remote interface the result of an Action. */
@ExperimentalAppActions
internal class ActionsResultCallbackRemote(binder: IActionsResultCallback) :
    IActionsResultCallback by binder

/**
 * Implements the binder interface that is used by the remote InCallService in order to perform an
 * Action.
 *
 * @param setHandRaised The method invoked when the remote InCallService requests to raise this
 *   user's hand
 * @param kickParticipant The method invoked when the remote InCAllService requests that a
 *   participant is kicked
 */
@ExperimentalAppActions
internal class ParticipantActions(
    private val setHandRaised: (state: Boolean, cb: ActionsResultCallbackRemote) -> Unit,
    private val kickParticipant: (participant: Participant, cb: ActionsResultCallbackRemote) -> Unit
) : IParticipantActions.Stub() {
    override fun setHandRaised(handRaisedState: Boolean, cb: IActionsResultCallback?) {
        cb?.let { setHandRaised.invoke(handRaisedState, ActionsResultCallbackRemote(cb)) }
    }

    override fun kickParticipant(participant: Participant, cb: IActionsResultCallback?) {
        cb?.let { kickParticipant.invoke(participant, ActionsResultCallbackRemote(cb)) }
    }
}

/**
 * Remote interface used to notify the ICS of participant state information
 *
 * @param binder The remote binder interface to wrap
 */
@ExperimentalAppActions
internal class ParticipantStateListenerRemote(binder: IParticipantStateListener) :
    IParticipantStateListener by binder

/**
 * The remote interface used to begin capability exchange with the InCallService.
 *
 * @param binder the remote binder interface.
 */
@ExperimentalAppActions
internal class CapabilityExchangeRemote(binder: ICapabilityExchange) :
    ICapabilityExchange by binder

/**
 * The implementation of the capability exchange listener, which is used by the InCallService to
 * create and remove extensions.
 *
 * @param onCreateParticipantExtension The method called when the remote InCallService is creating a
 *   participant extension.
 * @param onRemoveExtensions The method called when the remote InCallService is being removed.
 */
@ExperimentalAppActions
internal class CapabilityExchangeListener(
    val onCreateParticipantExtension:
        (actions: Set<Int>, binder: ParticipantStateListenerRemote) -> Unit =
        { _, _ ->
        },
    val onRemoveExtensions: () -> Unit = {}
) : ICapabilityExchangeListener.Stub() {
    override fun onCreateParticipantExtension(
        version: Int,
        actions: IntArray?,
        l: IParticipantStateListener?
    ) {
        l?.let {
            onCreateParticipantExtension.invoke(
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
        onRemoveExtensions.invoke()
    }
}
