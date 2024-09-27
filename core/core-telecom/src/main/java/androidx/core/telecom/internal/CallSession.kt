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
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isBluetoothAvailable
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isEarpieceEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isWiredHeadsetOrBtEndpoint
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val onStateChangedCallback: MutableSharedFlow<CallStateEvent>,
    private val onEventCallback: suspend (event: String, extras: Bundle) -> Unit,
    private val blockingSessionExecution: CompletableDeferred<Unit>
) : android.telecom.CallControlCallback, android.telecom.CallEventCallback, AutoCloseable {
    private val mCallSessionId: Int = CallEndpointUuidTracker.startSession()
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
    internal val mJetpackToPlatformCallEndpoint: HashMap<ParcelUuid, CallEndpoint> = HashMap()

    init {
        CoroutineScope(coroutineContext).launch {
            val state =
                if (attributes.isOutgoingCall()) CallStateEvent.DIALING else CallStateEvent.RINGING
            onStateChangedCallback.emit(state)
        }
    }

    companion object {
        private val TAG: String = CallSession::class.java.simpleName
        private const val WAIT_FOR_BT_TO_CONNECT_TIMEOUT: Long = 1000L
        private const val SWITCH_TO_SPEAKER_TIMEOUT: Long = WAIT_FOR_BT_TO_CONNECT_TIMEOUT + 1000L

        // TODO:: b/369153472 , remove delay and instead wait until onCallEndpointChanged
        //    provides the bluetooth endpoint before requesting the switch
        private const val DELAY_INITIAL_ENDPOINT_SWITCH: Long = 2000L
        private const val INITIAL_ENDPOINT_SWITCH_TIMEOUT: Long =
            DELAY_INITIAL_ENDPOINT_SWITCH + 1000L
    }

    @VisibleForTesting
    fun getIsCurrentEndpointSet(): CompletableDeferred<Unit> {
        return mIsCurrentEndpointSet
    }

    @VisibleForTesting
    fun getIsAvailableEndpointsSet(): CompletableDeferred<Unit> {
        return mIsAvailableEndpointsSet
    }

    @VisibleForTesting
    fun setCurrentCallEndpoint(endpoint: CallEndpointCompat) {
        mCurrentCallEndpoint = endpoint
    }

    @VisibleForTesting
    fun setAvailableCallEndpoints(endpoints: List<CallEndpointCompat>) {
        mAvailableEndpoints = endpoints
    }

    /**
     * =========================================================================================
     * Audio Updates
     * =========================================================================================
     */
    @VisibleForTesting
    internal fun toRemappedCallEndpointCompat(platformEndpoint: CallEndpoint): CallEndpointCompat {
        val jetpackUuid =
            CallEndpointUuidTracker.getUuid(
                mCallSessionId,
                platformEndpoint.endpointType,
                platformEndpoint.endpointName.toString()
            )
        mJetpackToPlatformCallEndpoint[jetpackUuid] = platformEndpoint
        val j =
            CallEndpointCompat(
                platformEndpoint.endpointName,
                platformEndpoint.endpointType,
                jetpackUuid
            )
        Log.d(TAG, " n=[${platformEndpoint.endpointName}]  plat=[${platformEndpoint}] --> jet=[$j]")
        return j
    }

    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
        // cache the previous call endpoint for maybeSwitchToSpeakerOnHeadsetDisconnect. This
        // is used to determine if the last endpoint was BT and the new endpoint is EARPIECE.
        val previousCallEndpoint = mCurrentCallEndpoint
        // due to the [CallsManager#getAvailableStartingCallEndpoints] API, endpoints the client
        // has can be different from the ones coming from the platform. Hence, a remapping is needed
        mCurrentCallEndpoint = toRemappedCallEndpointCompat(endpoint)
        // send the current call endpoint out to the client
        callChannels.currentEndpointChannel.trySend(mCurrentCallEndpoint!!).getOrThrow()
        Log.i(TAG, "onCallEndpointChanged: endpoint=[$endpoint]")
        // maybeSwitchToSpeakerOnCallStart needs to know when the initial current endpoint is set
        if (!mIsCurrentEndpointSet.isCompleted) {
            mIsCurrentEndpointSet.complete(Unit)
            Log.i(TAG, "onCallEndpointChanged: mCurrentCallEndpoint was set")
        }
        maybeSwitchToSpeakerOnHeadsetDisconnect(mCurrentCallEndpoint!!, previousCallEndpoint)
        // clear out the last user requested CallEndpoint. It's only used to determine if the
        // change in current endpoints was intentional for maybeSwitchToSpeakerOnHeadsetDisconnect
        if (mLastClientRequestedEndpoint?.type == endpoint.endpointType) {
            mLastClientRequestedEndpoint = null
        }
    }

    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        // due to the [CallsManager#getAvailableStartingCallEndpoints] API, endpoints the client
        // has can be different from the ones coming from the platform. Hence, a remapping is needed
        mAvailableEndpoints = endpoints.map { toRemappedCallEndpointCompat(it) }.sorted()
        // send the current call endpoints out to the client
        callChannels.availableEndpointChannel.trySend(mAvailableEndpoints).getOrThrow()
        Log.i(TAG, "onAvailableCallEndpointsChanged: endpoints=[$endpoints]")
        // maybeSwitchToSpeakerOnCallStart needs to know when the initial current endpoints are set
        if (!mIsAvailableEndpointsSet.isCompleted) {
            mIsAvailableEndpointsSet.complete(Unit)
            Log.i(TAG, "onAvailableCallEndpointsChanged: mAvailableEndpoints was set")
        }
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        CoroutineScope(coroutineContext).launch {
            if (isMuted) {
                onStateChangedCallback.emit(CallStateEvent.GLOBAL_MUTED)
            } else {
                onStateChangedCallback.emit(CallStateEvent.GLOBAL_UNMUTE)
            }
        }
        callChannels.isMutedChannel.trySend(isMuted).getOrThrow()
    }

    /**
     * This function should only be run once at the start of CallSession to determine if the
     * starting CallEndpointCompat should be switched based on the call properties or user request.
     */
    suspend fun maybeSwitchStartingEndpoint(preferredStartingCallEndpoint: CallEndpointCompat?) {
        if (preferredStartingCallEndpoint != null) {
            switchStartingCallEndpointOnCallStart(preferredStartingCallEndpoint)
        } else {
            maybeSwitchToSpeakerOnCallStart()
        }
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
                    maybeDelaySwitchToSpeaker(speakerCompat)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "maybeSwitchToSpeaker: hit exception=[$e]")
        }
    }

    // Users reported in b/345309071 that the call started on speakerphone instead
    // of bluetooth.  Upon inspection, the platform was echoing the earpiece audio
    // route first while BT was still connecting. Avoid overriding the BT route by
    // waiting a second. TODO:: b/351899854
    suspend fun maybeDelaySwitchToSpeaker(speakerCompat: CallEndpointCompat): Boolean {
        if (isBluetoothAvailable(mAvailableEndpoints)) {
            // The platform could potentially be connecting to BT. wait...
            delay(WAIT_FOR_BT_TO_CONNECT_TIMEOUT)
            // only switch to speaker if BT did not connect
            if (!isBluetoothConnected()) {
                Log.i(TAG, "maybeDelaySwitchToSpeaker: BT did not connect in time!")
                requestEndpointChange(speakerCompat)
                return true
            }
            Log.i(TAG, "maybeDelaySwitchToSpeaker: BT connected! voiding speaker switch.")
            return false
        } else {
            // otherwise, immediately change from earpiece to speaker because the platform is
            // not in the process of connecting a BT device.
            Log.i(TAG, "maybeDelaySwitchToSpeaker: no BT route available.")
            requestEndpointChange(speakerCompat)
            return true
        }
    }

    private fun isBluetoothConnected(): Boolean {
        return mCurrentCallEndpoint != null &&
            mCurrentCallEndpoint!!.type == CallEndpoint.TYPE_BLUETOOTH
    }

    suspend fun switchStartingCallEndpointOnCallStart(startingCallEndpoint: CallEndpointCompat) {
        try {
            withTimeout(INITIAL_ENDPOINT_SWITCH_TIMEOUT) {
                Log.i(TAG, "switchStartingCallEndpointOnCallStart: before awaitAll")
                awaitAll(mIsAvailableEndpointsSet)
                Log.i(TAG, "switchStartingCallEndpointOnCallStart: after awaitAll")
                // Delay the switch to a new [CallEndpointCompat] if there is a BT device
                // because the request will be overridden once the BT device connects!
                if (mAvailableEndpoints.any { it.isBluetoothType() }) {
                    Log.i(TAG, "switchStartingCallEndpointOnCallStart: BT delay START")
                    delay(DELAY_INITIAL_ENDPOINT_SWITCH)
                    Log.i(TAG, "switchStartingCallEndpointOnCallStart: BT delay END")
                }
                val res = requestEndpointChange(startingCallEndpoint)
                Log.i(TAG, "switchStartingCallEndpointOnCallStart: result=$res")
            }
        } catch (e: Exception) {
            Log.e(TAG, "switchStartingCallEndpointOnCallStart: hit exception=[$e]")
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
     * =========================================================================================
     * Call Event Updates
     * =========================================================================================
     */
    override fun onCallStreamingFailed(reason: Int) {
        TODO("Implement with the CallStreaming code")
    }

    override fun onEvent(event: String, extras: Bundle) {
        CoroutineScope(coroutineContext).launch { onEventCallback(event, extras) }
    }

    /**
     * =========================================================================================
     * CallControl
     * =========================================================================================
     */

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

    private fun moveState(result: CallControlResult, callState: CallStateEvent) {
        if (result == CallControlResult.Success()) {
            CoroutineScope(coroutineContext).launch { onStateChangedCallback.emit(callState) }
        }
    }

    suspend fun setActive(): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.setActive(Runnable::run, CallControlReceiver(result))
        result.await()
        val callControlResult = result.getCompleted()
        moveState(callControlResult, CallStateEvent.ACTIVE)
        return callControlResult
    }

    suspend fun setInactive(): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.setInactive(Runnable::run, CallControlReceiver(result))
        result.await()
        val callControlResult = result.getCompleted()
        moveState(callControlResult, CallStateEvent.INACTIVE)
        return callControlResult
    }

    suspend fun answer(videoState: Int): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.answer(videoState, Runnable::run, CallControlReceiver(result))
        result.await()
        val callControlResult = result.getCompleted()
        moveState(callControlResult, CallStateEvent.ACTIVE)
        return callControlResult
    }

    fun sendEvent(event: String, extras: Bundle = Bundle.EMPTY) {
        if (mPlatformInterface == null) {
            Log.w(TAG, "sendEvent: platform interface is not set up, [$event] dropped")
            return
        }
        mPlatformInterface!!.sendEvent(event, extras)
    }

    suspend fun requestEndpointChange(endpoint: CallEndpointCompat): CallControlResult {
        val job: CompletableDeferred<CallControlResult> = CompletableDeferred()
        // cache the last CallEndpoint the user requested to reference in
        // onCurrentCallEndpointChanged. This is helpful for determining if the user intentionally
        // requested a CallEndpoint switch or a headset was disconnected ...
        mLastClientRequestedEndpoint = endpoint
        val potentiallyRemappedEndpoint: CallEndpoint =
            if (mJetpackToPlatformCallEndpoint.containsKey(endpoint.identifier)) {
                mJetpackToPlatformCallEndpoint[endpoint.identifier]!!
            } else {
                EndpointUtils.Api34PlusImpl.toCallEndpoint(endpoint)
            }
        if (mPlatformInterface == null) {
            return CallControlResult.Error(androidx.core.telecom.CallException.ERROR_UNKNOWN)
        }
        Log.d(TAG, "jet=[${endpoint}] --> plat=[${potentiallyRemappedEndpoint}]")
        mPlatformInterface!!.requestCallEndpointChange(
            potentiallyRemappedEndpoint,
            Runnable::run,
            CallControlReceiver(job)
        )
        job.await()
        val platformResult = job.getCompleted()
        if (platformResult != CallControlResult.Success()) {
            mLastClientRequestedEndpoint = null
        }
        return platformResult
    }

    suspend fun disconnect(disconnectCause: DisconnectCause): CallControlResult {
        val result: CompletableDeferred<CallControlResult> = CompletableDeferred()
        mPlatformInterface?.disconnect(disconnectCause, Runnable::run, CallControlReceiver(result))
        result.await()
        val callControlResult = result.getCompleted()
        moveState(callControlResult, CallStateEvent.DISCONNECTED)
        return result.getCompleted()
    }

    /** CallControlCallback */
    override fun onSetActive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(coroutineContext).launch {
            try {
                onSetActiveCallback()
                wasCompleted.accept(true)
                onStateChangedCallback.emit(CallStateEvent.ACTIVE)
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
                onStateChangedCallback.emit(CallStateEvent.INACTIVE)
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
                onStateChangedCallback.emit(CallStateEvent.ACTIVE)
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
                onStateChangedCallback.emit(CallStateEvent.DISCONNECTED)
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
            return session.requestEndpointChange(endpoint)
        }

        // Send these events out to the client to collect
        override val currentCallEndpoint: Flow<CallEndpointCompat> =
            callChannels.currentEndpointChannel.receiveAsFlow()

        override val availableEndpoints: Flow<List<CallEndpointCompat>> =
            callChannels.availableEndpointChannel.receiveAsFlow()

        override val isMuted: Flow<Boolean> = callChannels.isMutedChannel.receiveAsFlow()
    }

    override fun close() {
        Log.i(TAG, "close: CallSessionId=[$mCallSessionId]")
        CallEndpointUuidTracker.endSession(mCallSessionId)
    }
}
