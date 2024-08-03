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
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CapabilityExchangeRemote
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * The scope used to initialize extensions that will be used during the call and manage extensions
 * during the call.
 *
 * Extensions should first be initialized in this scope. Once the call is set up, the user provided
 * implementation of [onCall] will be run, which should manage the call and extension states during
 * the lifetime of when the call is active.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAppActions::class)
internal class ExtensionInitializationScopeImpl : ExtensionInitializationScope {
    private companion object {
        const val LOG_TAG = Extensions.LOG_TAG + "(EIS)"
    }

    private var onCreateDelegate: (suspend CallControlScope.() -> Unit)? = null
    private val extensionCreators = HashSet<(CapabilityExchangeRepository) -> Capability>()

    override fun onCall(onCall: suspend CallControlScope.() -> Unit) {
        Log.v(LOG_TAG, "onCall: storing delegate")
        // Capture onCall impl
        onCreateDelegate = onCall
    }

    override fun addParticipantExtension(
        initialParticipants: Set<Participant>,
        initialActiveParticipant: Participant?
    ): ParticipantExtension {
        val participant = ParticipantExtensionImpl(initialParticipants, initialActiveParticipant)
        registerExtension(onExchangeStarted = participant::onExchangeStarted)
        return participant
    }

    /**
     * Register an extension to be created once capability exchange begins.
     *
     * @param onExchangeStarted The capability exchange procedure has begun and the extension needs
     *   to register the callbacks it will be handling as well as return the [Capability] of the
     *   extension, which will be used during capability exchange.
     */
    private fun registerExtension(onExchangeStarted: (CapabilityExchangeRepository) -> Capability) {
        extensionCreators.add(onExchangeStarted)
    }

    /**
     * Collects [CallsManager.CallEvent]s that were received from connected InCallServices on the
     * provided CoroutineScope and optionally consumes the events. If we recognize and consume a
     * [CallsManager.CallEvent], this will create a Coroutine as a child of the [CoroutineScope]
     * provided here to manage the lifecycle of the task.
     *
     * @param scope The CoroutineScope that will be launched to perform the collection of events
     * @param eventFlow The [SharedFlow] representing the incoming [CallsManager.CallEvent]s from
     *   the framework.
     */
    internal fun collectEvents(
        scope: CoroutineScope,
        eventFlow: SharedFlow<CallsManager.CallEvent>
    ) {
        scope.launch {
            Log.i(LOG_TAG, "collectEvents: starting collection")
            eventFlow
                .onCompletion { Log.i(LOG_TAG, "collectEvents: finishing...") }
                .collect {
                    Log.v(LOG_TAG, "collectEvents: received ${it.event}")
                    onEvent(it)
                }
        }
    }

    /**
     * Invokes the user provided implementation of [CallControlScope] when the call is ready.
     *
     * @param scope The enclosing [CallControlScope] passed in by [CallsManager.addCall] to be used
     *   to call [onCall].
     */
    internal fun invokeDelegate(scope: CallControlScope) {
        scope.launch {
            Log.i(LOG_TAG, "invokeDelegate")
            onCreateDelegate?.invoke(scope)
        }
    }

    /**
     * Consumes [CallsManager.CallEvent]s received from remote InCallService implementations.
     *
     * Provides a [CoroutineScope] for events to use to handle the event and set up a session for
     * the lifecycle of the call.
     *
     * @param callEvent The event that we received from an InCallService.
     */
    private fun CoroutineScope.onEvent(callEvent: CallsManager.CallEvent) {
        when (callEvent.event) {
            Extensions.EVENT_JETPACK_CAPABILITY_EXCHANGE -> {
                handleCapabilityExchangeEvent(callEvent.extras)
            }
        }
    }

    /**
     * Starts a Coroutine to handle CapabilityExchange and extensions for the lifecycle of the call.
     *
     * @param extras The extras included as part of the Capability Exchange event.
     */
    private fun CoroutineScope.handleCapabilityExchangeEvent(extras: Bundle) {
        val version = extras.getInt(Extensions.EXTRA_CAPABILITY_EXCHANGE_VERSION)
        val capExchange =
            ICapabilityExchange.Stub.asInterface(
                    extras.getBinder(Extensions.EXTRA_CAPABILITY_EXCHANGE_BINDER)
                )
                ?.let { CapabilityExchangeRemote(it) }
        if (capExchange == null) {
            Log.w(
                LOG_TAG,
                "handleCapabilityExchangeEvent: capExchange binder is null, can" +
                    " not complete cap exchange"
            )
            return
        }
        Log.i(LOG_TAG, "handleCapabilityExchangeEvent: received CE request, v=#$version")
        // Create a child scope for setting up and running the extensions so that we can cancel
        // the child scope when the remote ICS disconnects without affecting the parent scope.
        val connectionScope = CoroutineScope(coroutineContext)
        // Create a new repository for each new connection
        val callbackRepository = CapabilityExchangeRepository(connectionScope)
        val capabilities = extensionCreators.map { it.invoke(callbackRepository) }

        Log.i(LOG_TAG, "handleCapabilityExchangeEvent: beginning exchange, caps=$capabilities")
        try {
            capExchange.beginExchange(capabilities, callbackRepository.listener)
        } catch (e: RemoteException) {
            Log.w(LOG_TAG, "handleCapabilityExchangeEvent: Remote could not be reached: $e")
            // This will cancel the surrounding coroutineScope
            throw e
        }
    }
}
