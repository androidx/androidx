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

package androidx.core.telecom.internal

import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ParcelUuid
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@RequiresApi(34)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Suppress("ClassVerificationFailure")
internal class CallSession(
    coroutineContext: CoroutineContext,
    val onAnswerCallback: suspend (callType: Int) -> Unit,
    val onDisconnectCallback: suspend (disconnectCause: DisconnectCause) -> Unit,
    val onSetActiveCallback: suspend () -> Unit,
    val onSetInactiveCallback: suspend () -> Unit,
    private val blockingSessionExecution: CompletableDeferred<Unit>
) {
    private val mCoroutineContext = coroutineContext
    private var mPlatformInterface: android.telecom.CallControl? = null

    class CallControlCallbackImpl(private val callSession: CallSession) :
        android.telecom.CallControlCallback {
        override fun onSetActive(wasCompleted: Consumer<Boolean>) {
            callSession.onSetActive(wasCompleted)
        }

        override fun onSetInactive(wasCompleted: Consumer<Boolean>) {
            callSession.onSetInactive(wasCompleted)
        }

        override fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
            callSession.onAnswer(videoState, wasCompleted)
        }

        override fun onDisconnect(
            disconnectCause: DisconnectCause,
            wasCompleted: Consumer<Boolean>
        ) {
            callSession.onDisconnect(disconnectCause, wasCompleted)
        }

        override fun onCallStreamingStarted(wasCompleted: Consumer<Boolean>) {
            TODO("Implement with the CallStreaming code")
        }
    }

    class CallEventCallbackImpl(
        private val callChannels: CallChannels,
        private val coroutineContext: CoroutineContext
    ) :
        android.telecom.CallEventCallback {
        private val CALL_EVENT_CALLBACK_TAG = CallEventCallbackImpl::class.simpleName
        /**
         * Stubbed supported capabilities for v2 connections.
         */
        @ExperimentalAppActions
        private val supportedCapabilities = mutableListOf(Capability())
        override fun onCallEndpointChanged(
            endpoint: android.telecom.CallEndpoint
        ) {
            callChannels.currentEndpointChannel.trySend(
                EndpointUtils.Api34PlusImpl.toCallEndpointCompat(endpoint)
            ).getOrThrow()
        }

        override fun onAvailableCallEndpointsChanged(
            endpoints: List<android.telecom.CallEndpoint>
        ) {
            callChannels.availableEndpointChannel.trySend(
                EndpointUtils.Api34PlusImpl.toCallEndpointsCompat(endpoints)
            ).getOrThrow()
        }

        override fun onMuteStateChanged(isMuted: Boolean) {
            callChannels.isMutedChannel.trySend(isMuted).getOrThrow()
        }

        override fun onCallStreamingFailed(reason: Int) {
            TODO("Implement with the CallStreaming code")
        }

        @ExperimentalAppActions
        override fun onEvent(event: String, extras: Bundle) {
            // Call events are sent via Call#sendCallEvent(event, extras). Begin initial capability
            // exchange procedure once we know that the ICS supports it.
            if (event == CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE) {
                Log.i(CALL_EVENT_CALLBACK_TAG, "onEvent: EVENT_JETPACK_CAPABILITY_EXCHANGE: " +
                        "beginning capability exchange.")
                // Launch a new coroutine from the context of the current coroutine
                CoroutineScope(coroutineContext).launch {
                        CapabilityExchangeUtils.initiateVoipAppCapabilityExchange(
                            extras, supportedCapabilities, CALL_EVENT_CALLBACK_TAG)
                }
            }
        }
    }

    /**
     * CallControl is set by CallsManager#addCall when the CallControl object is returned by the
     * platform
     */
    fun setCallControl(control: android.telecom.CallControl) {
        mPlatformInterface = control
    }

    /**
     * Custom OutcomeReceiver that handles the Platform responses to a CallControl API call
     */
    inner class CallControlReceiver(deferred: CompletableDeferred<CallControlResult>) :
        OutcomeReceiver<Void, CallException> {
        private val mResultDeferred: CompletableDeferred<CallControlResult> = deferred

        override fun onResult(r: Void?) {
            mResultDeferred.complete(CallControlResult.Success())
        }

        override fun onError(error: CallException) {
            mResultDeferred.complete(CallControlResult.Error(
                androidx.core.telecom.CallException.fromTelecomCode(error.code)))
        }
    }

    fun getCallId(): ParcelUuid {
        return mPlatformInterface!!.callId
    }

    suspend fun setActive(): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.setActive(Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun setInactive(): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.setInactive(Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun answer(videoState: Int): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.answer(videoState, Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun requestEndpointChange(endpoint: android.telecom.CallEndpoint): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.requestCallEndpointChange(
            endpoint,
            Runnable::run, CallControlReceiver(result)
        )
        result.await()
        return result.getCompleted()
    }

    suspend fun disconnect(disconnectCause: DisconnectCause): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.disconnect(
            disconnectCause,
            Runnable::run,
            CallControlReceiver(result)
        )
        result.await()
        return result.getCompleted()
    }

    /**
     * CallControlCallback
     */
    fun onSetActive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            try {
                onSetActiveCallback()
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    fun onSetInactive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            try {
                onSetInactiveCallback()
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            try {
                onAnswerCallback(videoState)
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    fun onDisconnect(cause: DisconnectCause, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            try {
                onDisconnectCallback(cause)
                wasCompleted.accept(true)
            } catch (e: Exception) {
                wasCompleted.accept(false)
                throw e
            } finally {
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    private fun handleCallbackFailure(wasCompleted: Consumer<Boolean>, e: Exception) {
        wasCompleted.accept(false)
        blockingSessionExecution.complete(Unit)
        throw e
    }

    /**
     * =========================================================================================
     *  Simple implementation of [CallControlScope] with a [CallSession] as the session.
     * =========================================================================================
     */
    class CallControlScopeImpl(
        private val session: CallSession,
        callChannels: CallChannels,
        private val blockingSessionExecution: CompletableDeferred<Unit>,
        override val coroutineContext: CoroutineContext
    ) : CallControlScope {
        // handle requests that originate from the client and propagate into platform
        //  return the platforms response which indicates success of the request.
        override fun getCallId(): ParcelUuid {
            CoroutineScope(session.mCoroutineContext).launch {
            }
            return session.getCallId()
        }

        override suspend fun setActive(): CallControlResult {
            return session.setActive()
        }

        override suspend fun setInactive(): CallControlResult {
            return session.setInactive()
        }

        override suspend fun answer(callType: Int): CallControlResult {
            return session.answer(callType)
        }

        override suspend fun disconnect(disconnectCause: DisconnectCause): CallControlResult {
            val response = session.disconnect(disconnectCause)
            blockingSessionExecution.complete(Unit)
            return response
        }

        override suspend fun requestEndpointChange(endpoint: CallEndpointCompat):
            CallControlResult {
            return session.requestEndpointChange(
                EndpointUtils.Api34PlusImpl.toCallEndpoint(endpoint)
            )
        }

        // Send these events out to the client to collect
        override val currentCallEndpoint: Flow<CallEndpointCompat> =
            callChannels.currentEndpointChannel.receiveAsFlow()

        override val availableEndpoints: Flow<List<CallEndpointCompat>> =
            callChannels.availableEndpointChannel.receiveAsFlow()

        override val isMuted: Flow<Boolean> =
            callChannels.isMutedChannel.receiveAsFlow()
    }
}
