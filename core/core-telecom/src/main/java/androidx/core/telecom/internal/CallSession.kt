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
import android.telecom.CallControl
import android.telecom.CallEndpoint
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.getSpeakerEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isEarpieceEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isWiredHeadsetOrBtEndpoint
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@RequiresApi(34)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Suppress("ClassVerificationFailure")
internal class CallSession(
    val coroutineContext: CoroutineContext,
    val attributes: CallAttributesCompat,
    val onAnswerCallback: suspend (callType: Int) -> Unit,
    val onDisconnectCallback: suspend (disconnectCause: DisconnectCause) -> Unit,
    val onSetActiveCallback: suspend () -> Unit,
    val onSetInactiveCallback: suspend () -> Unit,
    private val callChannels: CallChannels,
    private val onEventCallback: suspend (event: String, extras: Bundle) -> Unit,
    private val blockingSessionExecution: CompletableDeferred<Unit>
) : android.telecom.CallControlCallback, android.telecom.CallEventCallback {
    private var mPlatformInterface: CallControl? = null
    // cache the latest current and available endpoints
    private var mCurrentCallEndpoint: CallEndpointCompat? = null
    private var mAvailableEndpoints: List<CallEndpointCompat> = ArrayList()
    private var mLastClientRequestedEndpoint: CallEndpointCompat? = null
    // use CompletableDeferred objects to signal when all the endpoint values have initially
    // been received from the platform.
    private val mIsCurrentEndpointSet = CompletableDeferred<Unit>()
    private val mIsAvailableEndpointsSet = CompletableDeferred<Unit>()
    private val mIsCurrentlyDisplayingVideo = attributes.isVideoCall()

    companion object {
        private val TAG: String = CallSession::class.java.simpleName
        private const val SWITCH_TO_SPEAKER_TIMEOUT: Long = 1000L
    }

    fun getIsCurrentEndpointSet(): CompletableDeferred<Unit> {
        return mIsCurrentEndpointSet
    }

    fun getIsAvailableEndpointsSet(): CompletableDeferred<Unit> {
        return mIsAvailableEndpointsSet
    }

    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        val previousCallEndpoint = mCurrentCallEndpoint
        mCurrentCallEndpoint = EndpointUtils.Api34PlusImpl.toCallEndpointCompat(endpoint)
        callChannels.currentEndpointChannel.trySend(mCurrentCallEndpoint!!).getOrThrow()
        Log.i(TAG, "onCallEndpointChanged: endpoint=[$endpoint]")
        if (!mIsCurrentEndpointSet.isCompleted) {
            mIsCurrentEndpointSet.complete(Unit)
            Log.i(TAG, "onCallEndpointChanged: mCurrentCallEndpoint was set")
        }
        maybeSwitchToSpeakerOnHeadsetDisconnect(mCurrentCallEndpoint!!, previousCallEndpoint)
        // clear out the last user requested CallEndpoint. It's only used to determine if the
        // change in current endpoints was intentional.
        mLastClientRequestedEndpoint = null
    }

    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        mAvailableEndpoints = EndpointUtils.Api34PlusImpl.toCallEndpointsCompat(endpoints)
        callChannels.availableEndpointChannel.trySend(mAvailableEndpoints).getOrThrow()
        Log.i(TAG, "onAvailableCallEndpointsChanged: endpoints=[$endpoints]")
        if (!mIsAvailableEndpointsSet.isCompleted) {
            mIsAvailableEndpointsSet.complete(Unit)
            Log.i(TAG, "onAvailableCallEndpointsChanged: mAvailableEndpoints was set")
        }
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        callChannels.isMutedChannel.trySend(isMuted).getOrThrow()
    }

    override fun onCallStreamingFailed(reason: Int) {
        TODO("Implement with the CallStreaming code")
    }

    override fun onEvent(event: String, extras: Bundle) {
        Log.i(TAG, "onEvent: received $event")
        CoroutineScope(coroutineContext).launch { onEventCallback(event, extras) }
    }

    /**
     * Due to the fact that OEMs may diverge from AOSP telecom platform behavior, Core-Telecom needs
     * to ensure that video calls start with speaker phone if the earpiece is the initial audio
     * route.
     */
    suspend fun maybeSwitchToSpeakerOnCallStart() {
        if (!attributes.isVideoCall()) {
            return
        }
        try {
            withTimeout(SWITCH_TO_SPEAKER_TIMEOUT) {
                Log.i(TAG, "maybeSwitchToSpeaker: before awaitAll")
                awaitAll(mIsCurrentEndpointSet, mIsAvailableEndpointsSet)
                Log.i(TAG, "maybeSwitchToSpeaker: after awaitAll")
                val speakerCompat = getSpeakerEndpoint(mAvailableEndpoints)
                if (isEarpieceEndpoint(mCurrentCallEndpoint) && speakerCompat != null) {
                    Log.i(
                        TAG,
                        "maybeSwitchToSpeaker: detected a video call that started" +
                            " with the earpiece audio route. requesting switch to speaker."
                    )
                    mPlatformInterface?.requestCallEndpointChange(
                        EndpointUtils.Api34PlusImpl.toCallEndpoint(speakerCompat),
                        Runnable::run,
                        {}
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "maybeSwitchToSpeaker: hit exception=[$e]")
        }
    }

    /**
     * Due to the fact that OEMs may diverge from AOSP telecom platform behavior, Core-Telecom needs
     * to ensure that if a video calls headset disconnects, the speakerphone is defaulted instead of
     * the earpiece route.
     */
    @VisibleForTesting
    fun maybeSwitchToSpeakerOnHeadsetDisconnect(
        newEndpoint: CallEndpointCompat,
        previousEndpoint: CallEndpointCompat?
    ) {
        try {
            if (
                mIsCurrentlyDisplayingVideo &&
                    /* Only switch if the users headset disconnects & earpiece is defaulted */
                    isEarpieceEndpoint(newEndpoint) &&
                    isWiredHeadsetOrBtEndpoint(previousEndpoint) &&
                    /* Do not switch request a switch to speaker if the client specifically requested
                     * to switch from the headset from an earpiece */
                    !isEarpieceEndpoint(mLastClientRequestedEndpoint)
            ) {
                val speakerCompat = getSpeakerEndpoint(mAvailableEndpoints)
                if (speakerCompat != null) {
                    Log.i(
                        TAG,
                        "maybeSwitchToSpeakerOnHeadsetDisconnect: headset disconnected while" +
                            " in a video call. requesting switch to speaker."
                    )
                    mPlatformInterface?.requestCallEndpointChange(
                        EndpointUtils.Api34PlusImpl.toCallEndpoint(speakerCompat),
                        Runnable::run,
                        {}
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "maybeSwitchToSpeakerOnHeadsetDisconnect: exception=[$e]")
        }
    }

    /**
     * CallControl is set by CallsManager#addCall when the CallControl object is returned by the
     * platform
     */
    fun setCallControl(control: CallControl) {
        mPlatformInterface = control
    }

    /** Custom OutcomeReceiver that handles the Platform responses to a CallControl API call */
    inner class CallControlReceiver(deferred: CompletableDeferred<CallControlResult>) :
        OutcomeReceiver<Void, CallException> {
        private val mResultDeferred: CompletableDeferred<CallControlResult> = deferred

        override fun onResult(r: Void?) {
            mResultDeferred.complete(CallControlResult.Success())
        }

        override fun onError(error: CallException) {
            mResultDeferred.complete(
                CallControlResult.Error(
                    androidx.core.telecom.CallException.fromTelecomCode(error.code)
                )
            )
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

    suspend fun requestEndpointChange(endpoint: CallEndpoint): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        // cache the last CallEndpoint the user requested to reference in
        // onCurrentCallEndpointChanged. This is helpful for determining if the user intentionally
        // requested a CallEndpoint switch or a headset was disconnected ...
        mLastClientRequestedEndpoint = EndpointUtils.Api34PlusImpl.toCallEndpointCompat(endpoint)
        mPlatformInterface?.requestCallEndpointChange(
            endpoint,
            Runnable::run,
            CallControlReceiver(result)
        )
        result.await()
        return result.getCompleted()
    }

    suspend fun disconnect(disconnectCause: DisconnectCause): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.disconnect(disconnectCause, Runnable::run, CallControlReceiver(result))
        result.await()
        return result.getCompleted()
    }

    /** CallControlCallback */
    override fun onSetActive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(coroutineContext).launch {
            try {
                onSetActiveCallback()
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    override fun onSetInactive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(coroutineContext).launch {
            try {
                onSetInactiveCallback()
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    override fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(coroutineContext).launch {
            try {
                onAnswerCallback(videoState)
                wasCompleted.accept(true)
            } catch (e: Exception) {
                handleCallbackFailure(wasCompleted, e)
            }
        }
    }

    override fun onDisconnect(cause: DisconnectCause, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(coroutineContext).launch {
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

    override fun onCallStreamingStarted(wasCompleted: Consumer<Boolean>) {
        TODO("Implement with the CallStreaming code")
    }

    private fun handleCallbackFailure(wasCompleted: Consumer<Boolean>, e: Exception) {
        wasCompleted.accept(false)
        blockingSessionExecution.complete(Unit)
        throw e
    }

    /**
     * =========================================================================================
     * Simple implementation of [CallControlScope] with a [CallSession] as the session.
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
            CoroutineScope(session.coroutineContext).launch {}
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

        override suspend fun requestEndpointChange(
            endpoint: CallEndpointCompat
        ): CallControlResult {
            return session.requestEndpointChange(
                EndpointUtils.Api34PlusImpl.toCallEndpoint(endpoint)
            )
        }

        // Send these events out to the client to collect
        override val currentCallEndpoint: Flow<CallEndpointCompat> =
            callChannels.currentEndpointChannel.receiveAsFlow()

        override val availableEndpoints: Flow<List<CallEndpointCompat>> =
            callChannels.availableEndpointChannel.receiveAsFlow()

        override val isMuted: Flow<Boolean> = callChannels.isMutedChannel.receiveAsFlow()
    }
}
