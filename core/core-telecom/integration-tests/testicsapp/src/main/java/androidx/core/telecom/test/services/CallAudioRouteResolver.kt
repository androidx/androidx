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

package androidx.core.telecom.test.services

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import androidx.annotation.RequiresApi
import androidx.core.telecom.test.Compatibility
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Tracks the current state of the available and current audio route on the device while in call,
 * taking into account the device's android API version.
 *
 * @param coroutineScope The scope attached to the lifecycle of the Service
 * @param callData The stream of calls that are active on this device
 * @param onChangeAudioRoute The callback used when user has requested to change the audio route on
 *   the device for devices running an API version < UDC
 * @param onRequestBluetoothAudio The callback used when the user has requested to change the audio
 *   route for devices running on API version < UDC
 * @param onRequestEndpointChange The callback used when the user has requested to change the
 *   endpoint for devices running API version UDC+
 */
class CallAudioRouteResolver(
    private val coroutineScope: CoroutineScope,
    callData: StateFlow<List<CallData>>,
    private val onChangeAudioRoute: (Int) -> Unit,
    private val onRequestBluetoothAudio: (BluetoothDevice) -> Unit,
    private val onRequestEndpointChange:
        (CallEndpoint, Executor, OutcomeReceiver<Void, CallEndpointException>) -> Unit
) {
    private val mIsCallAudioStateDeprecated =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    // Maps the CallAudioEndpoint to the associated BluetoothDevice (if applicable) for bkwds
    // compatibility with devices running on API version < UDC
    data class EndpointEntry(val endpoint: CallAudioEndpoint, val device: BluetoothDevice? = null)

    private val mCurrentEndpoint: MutableStateFlow<CallAudioEndpoint?> = MutableStateFlow(null)
    private val mAvailableEndpoints: MutableStateFlow<List<CallAudioEndpoint>> =
        MutableStateFlow(emptyList())
    val currentEndpoint: StateFlow<CallAudioEndpoint?> = mCurrentEndpoint.asStateFlow()
    val availableEndpoints = mAvailableEndpoints.asStateFlow()

    private val mCallAudioState: MutableStateFlow<CallAudioState?> = MutableStateFlow(null)
    private val mEndpoints: MutableStateFlow<List<EndpointEntry>> = MutableStateFlow(emptyList())
    private val mCurrentCallEndpoint: MutableStateFlow<CallEndpoint?> = MutableStateFlow(null)
    private val mAvailableCallEndpoints: MutableStateFlow<List<CallEndpoint>> =
        MutableStateFlow(emptyList())

    init {
        if (!mIsCallAudioStateDeprecated) {
            // bkwds compat functionality
            mCallAudioState
                .filterNotNull()
                .combine(callData) { state, data ->
                    if (data.isNotEmpty()) {
                        mCurrentEndpoint.value = getCurrentEndpoint(state)
                        mEndpoints.value = createEndpointEntries(state)
                        mAvailableEndpoints.value = mEndpoints.value.map { it.endpoint }
                    } else {
                        mCurrentEndpoint.value = null
                        mEndpoints.value = emptyList()
                        mAvailableEndpoints.value = emptyList()
                    }
                }
                .launchIn(coroutineScope)
        } else {
            // UDC+ functionality
            mAvailableCallEndpoints
                .combine(callData) { endpoints, data ->
                    val availableEndpoints =
                        if (data.isNotEmpty()) {
                            endpoints.mapNotNull(::createCallAudioEndpoint)
                        } else {
                            emptyList()
                        }
                    mAvailableEndpoints.value = availableEndpoints
                    availableEndpoints
                }
                .combine(mCurrentCallEndpoint) { available, current ->
                    if (available.isEmpty()) {
                        mCurrentEndpoint.value = null
                    }
                    val audioEndpoint = current?.let { createCallAudioEndpoint(it) }
                    mCurrentEndpoint.value = available.firstOrNull { it.id == audioEndpoint?.id }
                }
                .launchIn(coroutineScope)
        }
    }

    /** The audio state reported from the ICS has changed. */
    fun onCallAudioStateChanged(audioState: CallAudioState?) {
        if (mIsCallAudioStateDeprecated) return
        mCallAudioState.value = audioState
    }

    /** The call endpoint reported from the ICS has changed. */
    fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        if (!mIsCallAudioStateDeprecated) return
        mCurrentCallEndpoint.value = callEndpoint
    }

    /** The available endpoints reported from the ICS have changed. */
    fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        if (!mIsCallAudioStateDeprecated) return
        mAvailableCallEndpoints.value = availableEndpoints
    }

    /**
     * Request to change the audio route using the provided [CallAudioEndpoint.id].
     *
     * @return true if the operation succeeded, false if it did not because the endpoint doesn't
     *   exist.
     */
    suspend fun onChangeAudioRoute(id: String): Boolean {
        if (mIsCallAudioStateDeprecated) {
            val endpoint =
                mAvailableCallEndpoints.value.firstOrNull { it.identifier.toString() == id }
            if (endpoint == null) return false
            return coroutineScope.async { onRequestEndpointChange(endpoint) }.await()
        } else {
            val endpoint = mEndpoints.value.firstOrNull { it.endpoint.id == id }
            if (endpoint == null) return false
            if (endpoint.endpoint.audioRoute != AudioRoute.BLUETOOTH) {
                onChangeAudioRoute(getAudioState(endpoint.endpoint.audioRoute))
                return true
            } else {
                if (endpoint.device == null) return false
                onRequestBluetoothAudio(endpoint.device)
                return true
            }
        }
    }

    /** Send a request to the InCallService to change the current endpoint. */
    private suspend fun onRequestEndpointChange(endpoint: CallEndpoint): Boolean =
        suspendCancellableCoroutine { continuation ->
            onRequestEndpointChange(
                endpoint,
                Runnable::run,
                @RequiresApi(Build.VERSION_CODES.S)
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) {
                        continuation.resume(true)
                    }

                    override fun onError(error: CallEndpointException) {
                        continuation.resume(false)
                    }
                }
            )
        }

    /** Maps from the Telecom [CallAudioState] to the app's [CallAudioEndpoint] */
    private fun getCurrentEndpoint(callAudioState: CallAudioState): CallAudioEndpoint {
        if (CallAudioState.ROUTE_BLUETOOTH != callAudioState.route) {
            return CallAudioEndpoint(
                id = getAudioEndpointId(callAudioState.route),
                audioRoute = getAudioEndpointRoute(callAudioState.route)
            )
        }
        val device: BluetoothDevice? = callAudioState.activeBluetoothDevice
        if (device?.address != null) {
            return CallAudioEndpoint(
                id = device.address,
                audioRoute = AudioRoute.BLUETOOTH,
                frameworkName = getName(device)
            )
        }
        val exactMatch = mEndpoints.value.firstOrNull { it.device == device }
        if (exactMatch != null) return exactMatch.endpoint
        return CallAudioEndpoint(
            id = "",
            audioRoute = AudioRoute.BLUETOOTH,
            frameworkName = device?.let { getName(it) }
        )
    }

    /** Create the [CallAudioEndpoint] from the telecom [CallEndpoint] for API UDC+ */
    private fun createCallAudioEndpoint(endpoint: CallEndpoint): CallAudioEndpoint? {
        val id = Compatibility.getEndpointIdentifier(endpoint) ?: return null
        val type = Compatibility.getEndpointType(endpoint) ?: return null
        val name = Compatibility.getEndpointName(endpoint) ?: return null
        return CallAudioEndpoint(id, getAudioRouteFromEndpointType(type), name)
    }

    /** Reconstruct the available audio routes from telecom state and construct [EndpointEntry]s */
    private fun createEndpointEntries(callAudioState: CallAudioState): List<EndpointEntry> {
        return buildList {
            if (CallAudioState.ROUTE_EARPIECE and callAudioState.supportedRouteMask > 0) {
                add(
                    EndpointEntry(
                        CallAudioEndpoint(
                            id = getAudioEndpointId(CallAudioState.ROUTE_EARPIECE),
                            audioRoute = AudioRoute.EARPIECE
                        )
                    )
                )
            }
            if (CallAudioState.ROUTE_SPEAKER and callAudioState.supportedRouteMask > 0) {
                add(
                    EndpointEntry(
                        CallAudioEndpoint(
                            id = getAudioEndpointId(CallAudioState.ROUTE_SPEAKER),
                            audioRoute = AudioRoute.SPEAKER
                        )
                    )
                )
            }
            if (CallAudioState.ROUTE_WIRED_HEADSET and callAudioState.supportedRouteMask > 0) {
                add(
                    EndpointEntry(
                        CallAudioEndpoint(
                            id = getAudioEndpointId(CallAudioState.ROUTE_WIRED_HEADSET),
                            audioRoute = AudioRoute.HEADSET
                        )
                    )
                )
            }
            if (CallAudioState.ROUTE_STREAMING and callAudioState.supportedRouteMask > 0) {
                add(
                    EndpointEntry(
                        CallAudioEndpoint(
                            id = getAudioEndpointId(CallAudioState.ROUTE_STREAMING),
                            audioRoute = AudioRoute.STREAMING
                        )
                    )
                )
            }
            // For Bluetooth, cache the BluetoothDevices associated with the route so we can choose
            // them later
            if (CallAudioState.ROUTE_BLUETOOTH and callAudioState.supportedRouteMask > 0) {
                addAll(
                    callAudioState.supportedBluetoothDevices.map { device ->
                        EndpointEntry(
                            CallAudioEndpoint(
                                id = device.address?.toString() ?: UUID.randomUUID().toString(),
                                audioRoute = AudioRoute.BLUETOOTH,
                                frameworkName = getName(device)
                            ),
                            device
                        )
                    }
                )
            }
        }
    }

    private fun getName(device: BluetoothDevice): String? {
        var name = Compatibility.getBluetoothDeviceAlias(device)
        if (name.isFailure) {
            name = getBluetoothDeviceName(device)
        }
        return name.getOrDefault(null)
    }

    private fun getBluetoothDeviceName(device: BluetoothDevice): Result<String> {
        return try {
            Result.success(device.name ?: "")
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    private fun getAudioEndpointId(audioState: Int): String {
        return when (audioState) {
            CallAudioState.ROUTE_EARPIECE -> "Earpiece"
            CallAudioState.ROUTE_SPEAKER -> "Speaker"
            CallAudioState.ROUTE_WIRED_HEADSET -> "Headset"
            CallAudioState.ROUTE_BLUETOOTH -> "Bluetooth"
            CallAudioState.ROUTE_STREAMING -> "Streaming"
            else -> "Unknown"
        }
    }

    private fun getAudioRouteFromEndpointType(endpointType: Int): AudioRoute {
        return when (endpointType) {
            CallEndpoint.TYPE_EARPIECE -> AudioRoute.EARPIECE
            CallEndpoint.TYPE_SPEAKER -> AudioRoute.SPEAKER
            CallEndpoint.TYPE_WIRED_HEADSET -> AudioRoute.HEADSET
            CallEndpoint.TYPE_BLUETOOTH -> AudioRoute.BLUETOOTH
            CallEndpoint.TYPE_STREAMING -> AudioRoute.STREAMING
            else -> {
                AudioRoute.UNKNOWN
            }
        }
    }

    private fun getAudioEndpointRoute(audioState: Int): AudioRoute {
        return when (audioState) {
            CallAudioState.ROUTE_EARPIECE -> AudioRoute.EARPIECE
            CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
            CallAudioState.ROUTE_WIRED_HEADSET -> AudioRoute.HEADSET
            CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
            CallAudioState.ROUTE_STREAMING -> AudioRoute.STREAMING
            else -> AudioRoute.UNKNOWN
        }
    }

    private fun getAudioState(audioRoute: AudioRoute): Int {
        return when (audioRoute) {
            AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
            AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
            AudioRoute.HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
            AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            AudioRoute.STREAMING -> CallAudioState.ROUTE_STREAMING
            else -> CallAudioState.ROUTE_EARPIECE
        }
    }
}
