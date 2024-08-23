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

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.ParcelUuid
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.getSpeakerEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isBluetoothAvailable
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isEarpieceEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isWiredHeadsetOrBtEndpoint
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.toCallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.toCallEndpointsCompat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@RequiresApi(VERSION_CODES.O)
internal class CallSessionLegacy(
    private val id: ParcelUuid,
    private val attributes: CallAttributesCompat,
    private val callChannels: CallChannels,
    private val coroutineContext: CoroutineContext,
    val onAnswerCallback: suspend (callType: Int) -> Unit,
    val onDisconnectCallback: suspend (disconnectCause: DisconnectCause) -> Unit,
    val onSetActiveCallback: suspend () -> Unit,
    val onSetInactiveCallback: suspend () -> Unit,
    val onEventCallback: suspend (event: String, extras: Bundle) -> Unit,
    private val preferredStartingCallEndpoint: CallEndpointCompat? = null,
    private val preCallEndpointMapping: PreCallEndpoints? = null,
    private val blockingSessionExecution: CompletableDeferred<Unit>
) : android.telecom.Connection() {
    // instance vars
    private val TAG: String = CallSessionLegacy::class.java.simpleName
    private var mCachedBluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()
    private var mAlreadyRequestedStartingEndpointSwitch: Boolean = false
    private var mAlreadyRequestedSpeaker: Boolean = false
    private var mPreviousCallEndpoint: CallEndpointCompat? = null
    private var mCurrentCallEndpoint: CallEndpointCompat? = null
    private var mAvailableCallEndpoints: List<CallEndpointCompat>? = null
    private var mLastClientRequestedEndpoint: CallEndpointCompat? = null

    companion object {
        private const val WAIT_FOR_BT_TO_CONNECT_TIMEOUT: Long = 1000L
        private const val DELAY_INITIAL_ENDPOINT_SWITCH: Long = 1000L
        // CallStates. All these states mirror the values in the platform.
        const val STATE_INITIALIZING = 0
        const val STATE_NEW = 1
        const val STATE_RINGING = 2
        const val STATE_DIALING = 3
        const val STATE_ACTIVE = 4
        const val STATE_HOLDING = 5
        const val STATE_DISCONNECTED = 6
    }

    /**
     * =========================================================================================
     * Call State Updates
     * =========================================================================================
     */
    override fun onStateChanged(state: Int) {
        Log.v(TAG, "onStateChanged: state=${platformCallStateToString(state)}")
    }

    private fun platformCallStateToString(state: Int): String {
        return when (state) {
            STATE_INITIALIZING -> "INITIALIZING"
            STATE_NEW -> "NEW"
            STATE_DIALING -> "DIALING"
            STATE_RINGING -> "RINGING"
            STATE_ACTIVE -> "ACTIVE"
            STATE_HOLDING -> "HOLDING"
            STATE_DISCONNECTED -> "DISCONNECTED"
            else -> "UNKNOWN"
        }
    }

    /**
     * =========================================================================================
     * Audio Updates
     * =========================================================================================
     */
    @VisibleForTesting
    internal fun toRemappedCallEndpointCompat(endpoint: CallEndpointCompat): CallEndpointCompat {
        if (endpoint.isBluetoothType()) {
            val key = endpoint.name.toString()
            val btEndpointMapping = preCallEndpointMapping?.mBluetoothEndpoints
            return if (btEndpointMapping != null && btEndpointMapping.containsKey(key)) {
                btEndpointMapping[key]!!
            } else {
                endpoint
            }
        } else {
            val key = endpoint.type
            val nonBtEndpointMapping = preCallEndpointMapping?.mNonBluetoothEndpoints
            return if (nonBtEndpointMapping != null && nonBtEndpointMapping.containsKey(key)) {
                nonBtEndpointMapping[key]!!
            } else {
                endpoint
            }
        }
    }

    private fun setCurrentCallEndpoint(state: CallAudioState) {
        mPreviousCallEndpoint = mCurrentCallEndpoint
        mCurrentCallEndpoint = toRemappedCallEndpointCompat(toCallEndpointCompat(state))
        callChannels.currentEndpointChannel.trySend(mCurrentCallEndpoint!!).getOrThrow()
    }

    private fun setAvailableCallEndpoints(state: CallAudioState) {
        val availableEndpoints =
            toCallEndpointsCompat(state).map { toRemappedCallEndpointCompat(it) }.sorted()
        mAvailableCallEndpoints = availableEndpoints
        callChannels.availableEndpointChannel.trySend(availableEndpoints).getOrThrow()
    }

    override fun onCallAudioStateChanged(state: CallAudioState) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            Api28PlusImpl.refreshBluetoothDeviceCache(mCachedBluetoothDevices, state)
        }
        setCurrentCallEndpoint(state)
        setAvailableCallEndpoints(state)
        callChannels.isMutedChannel.trySend(state.isMuted).getOrThrow()
        // On the first call audio state change, determine if the platform started on the correct
        // audio route.  Otherwise, request an endpoint switch.
        switchStartingCallEndpointOnCallStart(mAvailableCallEndpoints!!)
        // In the event the users headset disconnects, they will likely want to continue the call
        // via the speakerphone
        maybeSwitchToSpeakerOnHeadsetDisconnect(
            mCurrentCallEndpoint!!,
            mPreviousCallEndpoint,
            mAvailableCallEndpoints!!,
        )
        // clear out the last user requested CallEndpoint. It's only used to determine if the
        // change in current endpoints was intentional.
        if (mLastClientRequestedEndpoint?.type == mCurrentCallEndpoint?.type) {
            mLastClientRequestedEndpoint = null
        }
    }

    private fun switchStartingCallEndpointOnCallStart(endpoints: List<CallEndpointCompat>) {
        if (preferredStartingCallEndpoint != null) {
            if (!mAlreadyRequestedStartingEndpointSwitch) {
                CoroutineScope(coroutineContext).launch {
                    // Delay the switch to a new [CallEndpointCompat] if there is a BT device
                    // because the request will be overridden once the BT device connects!
                    if (endpoints.any { it.isBluetoothType() }) {
                        Log.i(TAG, "switchStartingCallEndpointOnCallStart: BT delay START")
                        delay(DELAY_INITIAL_ENDPOINT_SWITCH)
                        Log.i(TAG, "switchStartingCallEndpointOnCallStart: BT delay END")
                    }
                    requestEndpointChange(preferredStartingCallEndpoint)
                }
            }
        } else {
            maybeSwitchToSpeakerOnCallStart(mCurrentCallEndpoint!!, endpoints)
        }
        mAlreadyRequestedStartingEndpointSwitch = true
    }

    /**
     * Due to the fact that OEMs may diverge from AOSP telecom platform behavior, Core-Telecom needs
     * to ensure that video calls start with speaker phone if the earpiece is the initial audio
     * route.
     */
    private fun maybeSwitchToSpeakerOnCallStart(
        currentEndpoint: CallEndpointCompat,
        availableEndpoints: List<CallEndpointCompat>
    ) {
        if (!mAlreadyRequestedSpeaker && attributes.isVideoCall()) {
            try {
                val speakerEndpoint = getSpeakerEndpoint(availableEndpoints)
                if (isEarpieceEndpoint(currentEndpoint) && speakerEndpoint != null) {
                    Log.i(
                        TAG,
                        "maybeSwitchToSpeaker: detected a video call that started" +
                            " with the earpiece audio route. requesting switch to speaker."
                    )
                    CoroutineScope(coroutineContext).launch {
                        // Users reported in b/345309071 that the call started on speakerphone
                        // instead of bluetooth.  Upon inspection, the platform was echoing the
                        // earpiece audio route first while BT was still connecting. Avoid
                        // overriding the BT route by waiting a second. TODO:: b/351899854
                        if (isBluetoothAvailable(availableEndpoints)) {
                            delay(WAIT_FOR_BT_TO_CONNECT_TIMEOUT)
                            if (!isBluetoothConnected()) {
                                Log.i(TAG, "maybeSwitchToSpeaker: BT did not connect in time!")
                                requestEndpointChange(speakerEndpoint)
                            } else {
                                Log.i(
                                    TAG,
                                    "maybeSwitchToSpeaker: BT connected! void speaker switch"
                                )
                            }
                        } else {
                            // otherwise, immediately change from earpiece to speaker because the
                            // platform is
                            // not in the process of connecting a BT device.
                            requestEndpointChange(speakerEndpoint)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "maybeSwitchToSpeaker: hit exception=[$e]")
            }
            mAlreadyRequestedSpeaker = true
        }
    }

    private fun isBluetoothConnected(): Boolean {
        return mCurrentCallEndpoint != null &&
            mCurrentCallEndpoint!!.type == CallEndpoint.TYPE_BLUETOOTH
    }

    /**
     * Due to the fact that OEMs may diverge from AOSP telecom platform behavior, Core-Telecom needs
     * to ensure that if a video calls headset disconnects, the speakerphone is defaulted instead of
     * the earpiece route.
     */
    @VisibleForTesting
    fun maybeSwitchToSpeakerOnHeadsetDisconnect(
        newEndpoint: CallEndpointCompat,
        previousEndpoint: CallEndpointCompat?,
        availableEndpoints: List<CallEndpointCompat>
    ) {
        try {
            if (
                attributes.isVideoCall() &&
                    /* Only switch if the users headset disconnects & earpiece is defaulted */
                    isEarpieceEndpoint(newEndpoint) &&
                    isWiredHeadsetOrBtEndpoint(previousEndpoint) &&
                    /* Do not switch request a switch to speaker if the client specifically requested
                     * to switch from the headset from an earpiece */
                    !isEarpieceEndpoint(mLastClientRequestedEndpoint)
            ) {
                val speakerCompat = getSpeakerEndpoint(availableEndpoints)
                if (speakerCompat != null) {
                    Log.i(
                        TAG,
                        "maybeSwitchToSpeakerOnHeadsetDisconnect: headset disconnected while" +
                            " in a video call. requesting switch to speaker."
                    )
                    requestEndpointChange(speakerCompat)
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
    override fun onCallEvent(event: String?, extras: Bundle?) {
        super.onCallEvent(event, extras)
        if (event == null) return
        CoroutineScope(coroutineContext).launch { onEventCallback(event, extras ?: Bundle.EMPTY) }
    }

    /**
     * =========================================================================================
     * CallControl
     * =========================================================================================
     */
    fun getCallId(): ParcelUuid {
        return id
    }

    fun answer(videoState: Int): CallControlResult {
        setVideoState(videoState)
        setActive()
        return CallControlResult.Success()
    }

    fun setConnectionActive(): CallControlResult {
        setActive()
        return CallControlResult.Success()
    }

    fun setConnectionInactive(): CallControlResult {
        return if (
            this.connectionCapabilities.and(CAPABILITY_SUPPORT_HOLD) == CAPABILITY_SUPPORT_HOLD
        ) {
            setOnHold()
            CallControlResult.Success()
        } else {
            CallControlResult.Error(CallException.ERROR_CALL_DOES_NOT_SUPPORT_HOLD)
        }
    }

    fun setConnectionDisconnect(cause: DisconnectCause): CallControlResult {
        setDisconnected(cause)
        destroy()
        return CallControlResult.Success()
    }

    // TODO:: verify the CallEndpoint change was successful. tracking bug: b/283324578
    @Suppress("deprecation")
    fun requestEndpointChange(callEndpoint: CallEndpointCompat): CallControlResult {
        // cache the last CallEndpoint the user requested to reference in audio callbacks
        mLastClientRequestedEndpoint = callEndpoint
        return if (Build.VERSION.SDK_INT < VERSION_CODES.P) {
            Api26PlusImpl.setAudio(callEndpoint, this)
            CallControlResult.Success()
        } else {
            Api28PlusImpl.setAudio(callEndpoint, this, mCachedBluetoothDevices)
        }
    }

    @Suppress("deprecation")
    @RequiresApi(VERSION_CODES.O)
    private object Api26PlusImpl {
        @JvmStatic
        fun setAudio(callEndpoint: CallEndpointCompat, connection: CallSessionLegacy) {
            connection.setAudioRoute(EndpointUtils.mapTypeToRoute(callEndpoint.type))
        }
    }

    @Suppress("deprecation")
    @RequiresApi(VERSION_CODES.P)
    private object Api28PlusImpl {
        @JvmStatic
        fun setAudio(
            callEndpoint: CallEndpointCompat,
            connection: CallSessionLegacy,
            btCache: ArrayList<BluetoothDevice>
        ): CallControlResult {
            if (callEndpoint.type == CallEndpointCompat.TYPE_BLUETOOTH) {
                val btDevice = getBluetoothDeviceFromEndpoint(btCache, callEndpoint)
                if (btDevice != null) {
                    connection.requestBluetoothAudio(btDevice)
                    return CallControlResult.Success()
                }
                return CallControlResult.Error(CallException.ERROR_BLUETOOTH_DEVICE_IS_NULL)
            } else {
                connection.setAudioRoute(EndpointUtils.mapTypeToRoute(callEndpoint.type))
                return CallControlResult.Success()
            }
        }

        @JvmStatic
        fun refreshBluetoothDeviceCache(
            btCacheList: ArrayList<BluetoothDevice>,
            state: CallAudioState
        ) {
            btCacheList.clear()
            btCacheList.addAll(state.supportedBluetoothDevices)
        }

        @JvmStatic
        fun getBluetoothDeviceFromEndpoint(
            btCacheList: ArrayList<BluetoothDevice>,
            endpoint: CallEndpointCompat
        ): BluetoothDevice? {
            for (btDevice in btCacheList) {
                if (bluetoothDeviceMatchesEndpoint(btDevice, endpoint)) {
                    return btDevice
                }
            }
            return null
        }

        fun bluetoothDeviceMatchesEndpoint(
            btDevice: BluetoothDevice,
            endpoint: CallEndpointCompat
        ): Boolean {
            return (btDevice.address?.equals(endpoint.mMackAddress) ?: false)
        }
    }

    /**
     * =========================================================================================
     * CallControlCallbacks
     * =========================================================================================
     */
    override fun onAnswer(videoState: Int) {
        CoroutineScope(coroutineContext).launch {
            // Note the slight deviation here where onAnswer does not put the call into an ACTIVE
            // state as it does in the platform. This behavior is intentional for this path.
            try {
                onAnswerCallback(videoState)
                setActive()
                setVideoState(videoState)
            } catch (e: Exception) {
                handleCallbackFailure(e)
            }
        }
    }

    override fun onUnhold() {
        CoroutineScope(coroutineContext).launch {
            try {
                onSetActiveCallback()
                setActive()
            } catch (e: Exception) {
                handleCallbackFailure(e)
            }
        }
    }

    override fun onHold() {
        CoroutineScope(coroutineContext).launch {
            try {
                onSetInactiveCallback()
                setOnHold()
            } catch (e: Exception) {
                handleCallbackFailure(e)
            }
        }
    }

    private fun handleCallbackFailure(e: Exception) {
        setConnectionDisconnect(DisconnectCause(DisconnectCause.LOCAL))
        blockingSessionExecution.complete(Unit)
        throw e
    }

    override fun onDisconnect() {
        CoroutineScope(coroutineContext).launch {
            try {
                onDisconnectCallback(DisconnectCause(DisconnectCause.LOCAL))
            } catch (e: Exception) {
                throw e
            } finally {
                setConnectionDisconnect(DisconnectCause(DisconnectCause.LOCAL))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    override fun onReject(rejectReason: Int) {
        CoroutineScope(coroutineContext).launch {
            try {
                if (state == Call.STATE_RINGING) {
                    onDisconnectCallback(DisconnectCause(DisconnectCause.REJECTED))
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setConnectionDisconnect(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    override fun onReject(rejectMessage: String) {
        CoroutineScope(coroutineContext).launch {
            try {
                if (state == Call.STATE_RINGING) {
                    onDisconnectCallback(DisconnectCause(DisconnectCause.REJECTED))
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setConnectionDisconnect(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    override fun onReject() {
        CoroutineScope(coroutineContext).launch {
            try {
                if (state == Call.STATE_RINGING) {
                    onDisconnectCallback(DisconnectCause(DisconnectCause.REJECTED))
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setConnectionDisconnect(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    /**
     * =========================================================================================
     * Simple implementation of [CallControlScope] with a [CallSessionLegacy] as the session.
     * =========================================================================================
     */
    class CallControlScopeImpl(
        private val session: CallSessionLegacy,
        callChannels: CallChannels,
        private val blockingSessionExecution: CompletableDeferred<Unit>,
        override val coroutineContext: CoroutineContext
    ) : CallControlScope {
        // handle requests that originate from the client and propagate into platform
        //  return the platforms response which indicates success of the request.
        override fun getCallId(): ParcelUuid {
            return session.getCallId()
        }

        override suspend fun setActive(): CallControlResult {
            return session.setConnectionActive()
        }

        override suspend fun setInactive(): CallControlResult {
            return session.setConnectionInactive()
        }

        override suspend fun answer(callType: Int): CallControlResult {
            return session.answer(callType)
        }

        override suspend fun disconnect(disconnectCause: DisconnectCause): CallControlResult {
            val result = session.setConnectionDisconnect(disconnectCause)
            blockingSessionExecution.complete(Unit)
            return result
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
}
