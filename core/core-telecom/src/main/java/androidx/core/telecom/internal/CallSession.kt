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
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils
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
    val onAnswerCallback: suspend (callType: Int) -> Boolean,
    val onDisconnectCallback: suspend (disconnectCause: DisconnectCause) -> Boolean,
    val onSetActiveCallback: suspend () -> Boolean,
    val onSetInactiveCallback: suspend () -> Boolean,
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

    class CallEventCallbackImpl(private val callChannels: CallChannels) :
        android.telecom.CallEventCallback {
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

        override fun onEvent(event: String, extras: Bundle) {
            TODO("Implement when events are agreed upon by ICS and package")
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
    inner class CallControlReceiver(deferred: CompletableDeferred<Boolean>) :
        OutcomeReceiver<Void, CallException> {
        private val mResultDeferred: CompletableDeferred<Boolean> = deferred

        override fun onResult(r: Void?) {
            mResultDeferred.complete(true)
        }

        override fun onError(error: CallException) {
            mResultDeferred.complete(false)
        }
    }

    fun getCallId(): ParcelUuid {
        return mPlatformInterface!!.callId
    }

    suspend fun setActive(): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        mPlatformInterface?.setActive(Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun setInactive(): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        mPlatformInterface?.setInactive(Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun answer(videoState: Int): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        mPlatformInterface?.answer(videoState, Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    suspend fun requestEndpointChange(endpoint: android.telecom.CallEndpoint): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        mPlatformInterface?.requestCallEndpointChange(
            endpoint,
            Runnable::run, CallControlReceiver(result)
        )
        result.await()
        return result.getCompleted()
    }

    suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
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
            val clientResponse: Boolean = onSetActiveCallback()
            wasCompleted.accept(clientResponse)
        }
    }

    fun onSetInactive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = onSetInactiveCallback()
            wasCompleted.accept(clientResponse)
        }
    }

    fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = onAnswerCallback(videoState)
            wasCompleted.accept(clientResponse)
        }
    }

    fun onDisconnect(cause: DisconnectCause, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = onDisconnectCallback(cause)
            wasCompleted.accept(clientResponse)
            blockingSessionExecution.complete(Unit)
        }
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

        override suspend fun setActive(): Boolean {
            return session.setActive()
        }

        override suspend fun setInactive(): Boolean {
            return session.setInactive()
        }

        override suspend fun answer(callType: Int): Boolean {
            return session.answer(callType)
        }

        override suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
            val response = session.disconnect(disconnectCause)
            blockingSessionExecution.complete(Unit)
            return response
        }

        override suspend fun requestEndpointChange(endpoint: CallEndpointCompat):
            Boolean {
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
