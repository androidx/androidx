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
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpoint
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
internal class CallSession(coroutineContext: CoroutineContext) {
    private val mCoroutineContext = coroutineContext
    private var mPlatformInterface: android.telecom.CallControl? = null
    private var mClientInterface: CallControlCallback? = null

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
                EndpointUtils.Api34PlusImpl.fromPlatformEndpointToAndroidXEndpoint(endpoint)
            ).getOrThrow()
        }

        override fun onAvailableCallEndpointsChanged(
            endpoints: List<android.telecom.CallEndpoint>
        ) {
            callChannels.availableEndpointChannel.trySend(
                EndpointUtils.Api34PlusImpl.fromPlatformEndpointsToAndroidXEndpoints(endpoints)
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
     * pass in the clients callback implementation for CallControlCallback that is set in the
     * CallsManager#addCall scope.
     */
    fun setCallControlCallback(clientCallbackImpl: CallControlCallback) {
        mClientInterface = clientCallbackImpl
    }

    fun hasClientSetCallbacks(): Boolean {
        return mClientInterface != null
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
            val clientResponse: Boolean = mClientInterface!!.onSetActive()
            wasCompleted.accept(clientResponse)
        }
    }

    fun onSetInactive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = mClientInterface!!.onSetInactive()
            wasCompleted.accept(clientResponse)
        }
    }

    fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = mClientInterface!!.onAnswer(videoState)
            wasCompleted.accept(clientResponse)
        }
    }

    fun onDisconnect(cause: DisconnectCause, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            val clientResponse: Boolean = mClientInterface!!.onDisconnect(cause)
            wasCompleted.accept(clientResponse)
        }
    }

    /**
     * =========================================================================================
     *  Simple implementation of [CallControlScope] with a [CallSession] as the session.
     * =========================================================================================
     */
    class CallControlScopeImpl(
        private val session: CallSession,
        callChannels: CallChannels
    ) : CallControlScope {
        //  handle actionable/handshake events that originate in the platform
        //  and require a response from the client
        override fun setCallback(callControlCallback: CallControlCallback) {
            session.setCallControlCallback(callControlCallback)
        }

        // handle requests that originate from the client and propagate into platform
        //  return the platforms response which indicates success of the request.
        override fun getCallId(): ParcelUuid {
            verifySessionCallbacks()
            return session.getCallId()
        }

        override suspend fun setActive(): Boolean {
            verifySessionCallbacks()
            return session.setActive()
        }

        override suspend fun setInactive(): Boolean {
            verifySessionCallbacks()
            return session.setInactive()
        }

        override suspend fun answer(callType: Int): Boolean {
            verifySessionCallbacks()
            return session.answer(callType)
        }

        override suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
            verifySessionCallbacks()
            return session.disconnect(disconnectCause)
        }

        override suspend fun requestEndpointChange(endpoint: CallEndpoint):
            Boolean {
            verifySessionCallbacks()
            return session.requestEndpointChange(
                EndpointUtils.Api34PlusImpl.toPlatformEndpoint(endpoint)
            )
        }

        // Send these events out to the client to collect
        override val currentCallEndpoint: Flow<CallEndpoint> =
            callChannels.currentEndpointChannel.receiveAsFlow()

        override val availableEndpoints: Flow<List<CallEndpoint>> =
            callChannels.availableEndpointChannel.receiveAsFlow()

        override val isMuted: Flow<Boolean> =
            callChannels.isMutedChannel.receiveAsFlow()

        private fun verifySessionCallbacks() {
            if (!session.hasClientSetCallbacks()) {
                throw androidx.core.telecom.CallException(
                    androidx.core.telecom.CallException.ERROR_CALLBACKS_CODE)
            }
        }
    }
}
