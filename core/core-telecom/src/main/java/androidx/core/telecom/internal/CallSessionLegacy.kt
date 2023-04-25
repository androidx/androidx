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
import android.os.ParcelUuid
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.utils.EndpointUtils
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@RequiresApi(VERSION_CODES.O)
internal class CallSessionLegacy(
    private val id: ParcelUuid,
    private val callChannels: CallChannels,
    private val coroutineContext: CoroutineContext
) : android.telecom.Connection() {
    // instance vars
    private val TAG: String = CallSessionLegacy::class.java.simpleName
    private var mClientInterface: CallControlCallback? = null
    private var mCachedBluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()

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

    fun setCallControlCallback(callControlCallback: CallControlCallback) {
        mClientInterface = callControlCallback
    }

    fun hasClientSetCallbacks(): Boolean {
        return mClientInterface != null
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
     *                CallControl
     * =========================================================================================
     */

    fun getCallId(): ParcelUuid {
        return id
    }

    fun answer(videoState: Int): Boolean {
        setVideoState(videoState)
        setActive()
        return true
    }

    fun setConnectionActive(): Boolean {
        setActive()
        return true
    }

    fun setConnectionInactive(): Boolean {
        setOnHold()
        return true
    }

    fun setConnectionDisconnect(cause: DisconnectCause): Boolean {
        setDisconnected(cause)
        destroy()
        return true
    }

    @Suppress("deprecation")
    fun requestEndpointChange(callEndpoint: CallEndpointCompat): Boolean {
        return if (Build.VERSION.SDK_INT < VERSION_CODES.P) {
            Api26PlusImpl.setAudio(callEndpoint, this)
            true
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
        ): Boolean {
            if (callEndpoint.type == CallEndpointCompat.TYPE_BLUETOOTH) {
                val btDevice = getBluetoothDeviceFromEndpoint(btCache, callEndpoint)
                if (btDevice != null) {
                    connection.requestBluetoothAudio(btDevice)
                    return true
                }
                return false
            } else {
                connection.setAudioRoute(EndpointUtils.mapTypeToRoute(callEndpoint.type))
                return true
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

        fun bluetoothDeviceMatchesEndpoint(btDevice: BluetoothDevice, endpoint: CallEndpointCompat):
            Boolean {
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
            val clientCanAnswer = mClientInterface!!.onSetActive()
            if (clientCanAnswer) {
                setActive()
                setVideoState(videoState)
            }
        }
    }

    override fun onUnhold() {
        CoroutineScope(coroutineContext).launch {
            val clientCanUnhold = mClientInterface!!.onSetActive()
            if (clientCanUnhold) {
                setActive()
            }
        }
    }

    override fun onHold() {
        CoroutineScope(coroutineContext).launch {
            val clientCanHold = mClientInterface!!.onSetInactive()
            if (clientCanHold) {
                setOnHold()
            }
        }
    }

    override fun onDisconnect() {
        CoroutineScope(coroutineContext).launch {
            mClientInterface!!.onDisconnect(
                DisconnectCause(DisconnectCause.LOCAL)
            )
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        }
    }

    /**
     * =========================================================================================
     *  Simple implementation of [CallControlScope] with a [CallSessionLegacy] as the session.
     * =========================================================================================
     */
    class CallControlScopeImpl(
        private val session: CallSessionLegacy,
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
            return session.setConnectionActive()
        }

        override suspend fun setInactive(): Boolean {
            verifySessionCallbacks()
            return session.setConnectionInactive()
        }

        override suspend fun answer(callType: Int): Boolean {
            verifySessionCallbacks()
            return session.answer(callType)
        }

        override suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
            verifySessionCallbacks()
            return session.setConnectionDisconnect(disconnectCause)
        }

        override suspend fun requestEndpointChange(endpoint: CallEndpointCompat): Boolean {
            verifySessionCallbacks()
            return session.requestEndpointChange(endpoint)
        }

        // Send these events out to the client to collect
        override val currentCallEndpoint: Flow<CallEndpointCompat> =
            callChannels.currentEndpointChannel.receiveAsFlow()

        override val availableEndpoints: Flow<List<CallEndpointCompat>> =
            callChannels.availableEndpointChannel.receiveAsFlow()

        override val isMuted: Flow<Boolean> =
            callChannels.isMutedChannel.receiveAsFlow()

        private fun verifySessionCallbacks() {
            if (!session.hasClientSetCallbacks()) {
                throw CallException(CallException.ERROR_CALLBACKS_CODE)
            }
        }
    }
}
