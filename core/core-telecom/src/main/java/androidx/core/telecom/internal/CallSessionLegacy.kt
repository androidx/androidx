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
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallException
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@RequiresApi(VERSION_CODES.O)
internal class CallSessionLegacy(
    private val id: ParcelUuid,
    private val callChannels: CallChannels,
    private val coroutineContext: CoroutineContext,
    val onAnswerCallback: suspend (callType: Int) -> Unit,
    val onDisconnectCallback: suspend (disconnectCause: DisconnectCause) -> Unit,
    val onSetActiveCallback: suspend () -> Unit,
    val onSetInactiveCallback: suspend () -> Unit,
    private val blockingSessionExecution: CompletableDeferred<Unit>
) : android.telecom.Connection() {
    // instance vars
    private val TAG: String = CallSessionLegacy::class.java.simpleName
    private var mCachedBluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()

    /**
     * Stubbed supported capabilities for legacy connections.
     */
    @ExperimentalAppActions
    private val supportedCapabilities = mutableListOf(Capability())

    companion object {
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
     *                Call State Updates
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
     *                Audio Updates
     * =========================================================================================
     */
    override fun onCallAudioStateChanged(state: CallAudioState) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            Api28PlusImpl.refreshBluetoothDeviceCache(mCachedBluetoothDevices, state)
        }
        callChannels.currentEndpointChannel.trySend(
            EndpointUtils.toCallEndpointCompat(state)
        ).getOrThrow()

        callChannels.availableEndpointChannel.trySend(
            EndpointUtils.toCallEndpointsCompat(state)
        ).getOrThrow()

        callChannels.isMutedChannel.trySend(state.isMuted).getOrThrow()
    }

    /**
     * =========================================================================================
     *                Call Event Updates
     * =========================================================================================
     */
    @ExperimentalAppActions
    override fun onCallEvent(event: String?, extras: Bundle?) {
        super.onCallEvent(event, extras)
        // Call events are sent via Call#sendCallEvent(event, extras). Begin initial capability
        // exchange procedure once we know that the ICS supports it.
        if (event == CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE) {
            Log.i(TAG, "onCallEvent: EVENT_JETPACK_CAPABILITY_EXCHANGE: " +
                "beginning capability exchange.")
            // Launch a new coroutine from the context of the current coroutine
            CoroutineScope(coroutineContext).launch {
                CapabilityExchangeUtils.initiateVoipAppCapabilityExchange(
                    extras!!, supportedCapabilities, TAG)
            }
        }
    }

    /**
     * =========================================================================================
     *                CallControl
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
        return if (this.connectionCapabilities.and(CAPABILITY_SUPPORT_HOLD)
            == CAPABILITY_SUPPORT_HOLD) {
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
        @DoNotInline
        fun setAudio(callEndpoint: CallEndpointCompat, connection: CallSessionLegacy) {
            connection.setAudioRoute(EndpointUtils.mapTypeToRoute(callEndpoint.type))
        }
    }

    @Suppress("deprecation")
    @RequiresApi(VERSION_CODES.P)
    private object Api28PlusImpl {
        @JvmStatic
        @DoNotInline
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
        @DoNotInline
        fun refreshBluetoothDeviceCache(
            btCacheList: ArrayList<BluetoothDevice>,
            state: CallAudioState
        ) {
            btCacheList.clear()
            btCacheList.addAll(state.supportedBluetoothDevices)
        }

        @JvmStatic
        @DoNotInline
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
     *                           CallControlCallbacks
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
                onDisconnectCallback(
                    DisconnectCause(DisconnectCause.LOCAL)
                )
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
                    onDisconnectCallback(
                        DisconnectCause(DisconnectCause.REJECTED)
                    )
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    override fun onReject(rejectMessage: String) {
        CoroutineScope(coroutineContext).launch {
            try {
                if (state == Call.STATE_RINGING) {
                    onDisconnectCallback(
                        DisconnectCause(DisconnectCause.REJECTED)
                    )
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    override fun onReject() {
        CoroutineScope(coroutineContext).launch {
            try {
                if (state == Call.STATE_RINGING) {
                    onDisconnectCallback(
                        DisconnectCause(DisconnectCause.REJECTED)
                    )
                }
            } catch (e: Exception) {
                throw e
            } finally {
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                blockingSessionExecution.complete(Unit)
            }
        }
    }

    /**
     * =========================================================================================
     *  Simple implementation of [CallControlScope] with a [CallSessionLegacy] as the session.
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

        override suspend fun requestEndpointChange(endpoint: CallEndpointCompat):
            CallControlResult {
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
