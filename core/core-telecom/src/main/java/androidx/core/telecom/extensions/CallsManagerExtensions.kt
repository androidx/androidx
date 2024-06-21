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
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CapabilityExchangeListener
import androidx.core.telecom.internal.CapabilityExchangeRemote
import androidx.core.telecom.internal.ParticipantStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
class CallsManagerExtensions {
    companion object {
        internal const val LOG_TAG = "CallsManagerE"
        /** VERSION used for handling future compatibility in capability exchange. */
        internal const val EXTRA_CAPABILITY_EXCHANGE_VERSION = "CAPABILITY_EXCHANGE_VERSION"

        /**
         * BINDER used for handling capability exchange between the ICS and VOIP app sides, sent as
         * part of sendCallEvent in the included extras.
         */
        internal const val EXTRA_CAPABILITY_EXCHANGE_BINDER = "CAPABILITY_EXCHANGE_BINDER"
    }
}

/** Events from the ICS that we have defined as extension events */
internal enum class ExtensionEvent(val eventString: String) {
    CAPABILITY_EXCHANGE("android.telecom.event.CAPABILITY_EXCHANGE")
}

/** An extension event from the ICS and related extras */
internal data class CallEvent(val event: ExtensionEvent, val extras: Bundle)

/**
 * Adds a call with extensions support, which allows an app to implement optional additional actions
 * that go beyond the scope of a call, such as information about meeting participants and icons.
 *
 * Supported Extensions:
 * - The ability to show meeting participants and information about those participants using
 *   [addParticipantExtension]
 *
 * For example, using Participants as an example of extensions:
 * ```
 * scope.launch {
 *         mCallsManager.addCallWithExtensions(attributes,
 *             onAnswerLambda,
 *             onDisconnectLambda,
 *             onSetActiveLambda,
 *             onSetInactiveLambda) {
 *                 // Initialize extensions ...
 *                 // Example: add participants support & associated actions
 *                 val participantExtension = addParticipantExtension(initialParticipants)
 *                 val raiseHandAction = participantExtension.addRaiseHandAction(
 *                         initialRaisedHands) { onHandRaisedStateChanged ->
 *                     // handle raised hand state changed
 *                 }
 *                 val kickParticipantAction = participantExtension.addKickParticipantAction {
 *                         participant ->
 *                     // handle kicking the requested participant
 *                 }
 *                 // Call has been set up, perform in-call actions
 *                 onCall {
 *                     // Example: collect call state updates
 *                     launch {
 *                         callStateFlow.collect { newState ->
 *                             // handle call state updates
 *                         }
 *                     }
 *                     // update participant extensions
 *                     launch {
 *                         participantsFlow.collect { newParticipants ->
 *                             participantExtension.updateParticipants(newParticipants)
 *                             // optionally update raise hand state
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 * }
 * ```
 *
 * @param init The scope used to first initialize Extensions that will be used when the call is
 *   first notified to the platform and UX surfaces. Once the call is set up, the user's
 *   implementation of [ExtensionInitializationScope.onCall] will be called.
 * @see CallsManager.addCall
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
suspend fun CallsManager.addCallWithExtensions(
    callAttributes: CallAttributesCompat,
    onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
    onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
    onSetActive: suspend () -> Unit,
    onSetInactive: suspend () -> Unit,
    init: suspend ExtensionInitializationScope.() -> Unit
) {
    Log.v(CallsManagerExtensions.LOG_TAG, "addCall: begin")
    val eventFlow = MutableSharedFlow<CallEvent>()
    val scope = ExtensionInitializationScope()
    var extensionJob: Job? = null
    scope.init()
    Log.v(CallsManagerExtensions.LOG_TAG, "addCall: init complete")
    addCall(
        callAttributes,
        onAnswer,
        onDisconnect,
        onSetActive,
        onSetInactive,
        onEvent = { event, extras ->
            Log.d(CallsManagerExtensions.LOG_TAG, "onEvent: received $event")
            val foundEvent = ExtensionEvent.values().firstOrNull { it.eventString == event }
            if (foundEvent == null) {
                Log.i(
                    CallsManagerExtensions.LOG_TAG,
                    "No matching event found for $event, ignoring..."
                )
            }
            foundEvent?.let { eventFlow.emit(CallEvent(it, extras)) }
        }
    ) {
        extensionJob = launch {
            Log.d(CallsManagerExtensions.LOG_TAG, "addCall: connecting extensions")
            scope.collectEvents(this, eventFlow)
        }
        Log.i(CallsManagerExtensions.LOG_TAG, "addCall: invoking delegates")
        scope.invokeDelegate(this)
    }
    // Ensure that when the call ends, we also cancel any ongoing coroutines/flows as part of
    // extension work
    extensionJob?.cancelAndJoin()
}

/**
 * The scope used to initialize extensions that will be used during the call and manage extensions
 * during the call.
 *
 * Extensions should first be initialized in this scope. Once the call is set up, the user provided
 * implementation of [onCall] will be run, which should manage the call and extension states during
 * the lifetime of when the call is active.
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
class ExtensionInitializationScope {
    private companion object {
        const val LOG_TAG = CallsManagerExtensions.LOG_TAG + "(EIS)"
    }

    private var onCreateDelegate: (suspend CallControlScope.() -> Unit)? = null
    private val extensions: HashSet<() -> ExtensionCreationDelegate> = HashSet()

    /**
     * User provided callback implementation that is run when the call is ready using the provided
     * [CallControlScope].
     *
     * @param onCall callback invoked when the call has been notified to the framework and the call
     *   is ready
     */
    fun onCall(onCall: suspend CallControlScope.() -> Unit) {
        Log.v(LOG_TAG, "onCall: storing delegate")
        // Capture onCall impl
        onCreateDelegate = onCall
    }

    /**
     * Called by extension functions during initialization to add themselves to the registered
     * extensions that will be notified whenever a remote InCallService wishes to connect.
     *
     * @param logTag The string used in logging to identify which extension is being added
     * @param extension The delegate registered and invoked whenever a remote InCallService wishes
     *   to connect.
     */
    internal fun registerExtension(logTag: String, extension: () -> ExtensionCreationDelegate) {
        extensions.add(extension)
        Log.d(LOG_TAG, "add: adding extension $logTag")
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
    internal fun collectEvents(scope: CoroutineScope, eventFlow: SharedFlow<CallEvent>) {
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
     * Consumes [CallEvent]s received from remote InCallService implementations.
     *
     * Provides a [CoroutineScope] for events to use to handle the event and set up a session for
     * the lifecycle of the call.
     *
     * @param callEvent The event that we received from an InCallService.
     */
    private fun CoroutineScope.onEvent(callEvent: CallEvent) {
        when (callEvent.event) {
            ExtensionEvent.CAPABILITY_EXCHANGE -> handleCapabilityExchangeEvent(callEvent.extras)
        }
    }

    /**
     * Starts a Coroutine to handle CapabilityExchange and extensions for the lifecycle of the call.
     *
     * @param extras The extras included as part of the Capability Exchange event.
     */
    private fun CoroutineScope.handleCapabilityExchangeEvent(extras: Bundle) {
        val version = extras.getInt(CallsManagerExtensions.EXTRA_CAPABILITY_EXCHANGE_VERSION)
        val capExchange =
            ICapabilityExchange.Stub.asInterface(
                    extras.getBinder(CallsManagerExtensions.EXTRA_CAPABILITY_EXCHANGE_BINDER)
                )
                ?.let { CapabilityExchangeRemote(it) }
        if (capExchange == null) return
        Log.i(LOG_TAG, "onEvent: received CE request, v=#$version")
        val extensionCreators = extensions.map { it() }
        val capabilities = extensionCreators.map { extension -> extension.capability }
        // Create a child scope for setting up and running the extensions so that we can cancel
        // the child scope when the remote ICS disconnects without affecting the parent scope.
        val connectionScope = CoroutineScope(coroutineContext)
        Log.i(LOG_TAG, "onEvent: beginning exchange, caps=$capabilities")
        try {
            capExchange.beginExchange(
                capabilities,
                CapabilityExchangeListener(
                    onCreateParticipantExtension = { icsActions, binder ->
                        Log.d(
                            LOG_TAG,
                            "onEvent: onCreateParticipantE with actions " + "$icsActions"
                        )
                        val creator =
                            extensionCreators.first { creator ->
                                creator.capability.featureId == CallsManager.PARTICIPANT
                            }
                        creator.createParticipantExtension(connectionScope, icsActions, binder)
                    },
                    onRemoveExtensions = {
                        Log.d(LOG_TAG, "onEvent: onRemove")
                        // Cancel any ongoing coroutines associated with this connection once
                        // remove is called.
                        connectionScope.cancel()
                    }
                )
            )
        } catch (e: RemoteException) {
            Log.w(LOG_TAG, "onEvent: Remote could not be reached: $e")
        }
    }
}

/**
 * Registers a creation delegate, where the extension can register its [Capability] to be shared
 * with the remote and the delegate method used to create each extension type.
 *
 * @param capability The capability that will be sent to the remote party
 * @param onCreateParticipantExtension The delegate method called to create a Participant extension,
 *   which must create the participant extension on the given scope for the actions that the remote
 *   caller supports.
 */
@ExperimentalAppActions
internal class ExtensionCreationDelegate(
    val capability: Capability,
    private val onCreateParticipantExtension:
        (scope: CoroutineScope, actions: Set<Int>, remote: ParticipantStateListenerRemote) -> Unit
) {
    /** Calls the delegate function registered by the extension that handles participants */
    fun createParticipantExtension(
        scope: CoroutineScope,
        actions: Set<Int>,
        binder: ParticipantStateListenerRemote
    ) {
        onCreateParticipantExtension(scope, actions, binder)
    }
}
