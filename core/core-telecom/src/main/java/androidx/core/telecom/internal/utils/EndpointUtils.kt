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

package androidx.core.telecom.internal.utils

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.telecom.CallAudioState
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpoint

@RequiresApi(Build.VERSION_CODES.O)
internal class EndpointUtils {

    companion object {
        fun wrapCallAudioStateIntoCurrentEndpoint(state: CallAudioState): CallEndpoint {
            val type: Int = mapRouteToType(state.route)
            return if (type == CallEndpoint.TYPE_BLUETOOTH && SDK_INT >= P) {
                BluetoothApi28PlusImpl.getCallEndpointFromAudioState(state)
            } else {
                CallEndpoint(endpointTypeToString(type), type)
            }
        }

        fun wrapCallAudioStateIntoAvailableEndpoints(state: CallAudioState): List<CallEndpoint> {
            val endpoints: ArrayList<CallEndpoint> = ArrayList()
            val bitMask = state.supportedRouteMask
            if (hasEarpieceType(bitMask)) {
                endpoints.add(
                    CallEndpoint(
                        endpointTypeToString(CallEndpoint.TYPE_EARPIECE),
                        CallEndpoint.TYPE_EARPIECE
                    )
                )
            }
            if (hasBluetoothType(bitMask)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    endpoints.addAll(BluetoothApi28PlusImpl.getBluetoothEndpoints(state))
                } else {
                    endpoints.add(
                        CallEndpoint(
                            endpointTypeToString(CallEndpoint.TYPE_BLUETOOTH),
                            CallEndpoint.TYPE_BLUETOOTH
                        )
                    )
                }
            }
            if (hasWiredHeadsetType(bitMask)) {
                endpoints.add(
                    CallEndpoint(
                        endpointTypeToString(CallEndpoint.TYPE_WIRED_HEADSET),
                        CallEndpoint.TYPE_WIRED_HEADSET
                    )
                )
            }
            if (hasSpeakerType(bitMask)) {
                endpoints.add(
                    CallEndpoint(
                        endpointTypeToString(CallEndpoint.TYPE_SPEAKER),
                        CallEndpoint.TYPE_SPEAKER
                    )
                )
            }
            if (hasStreamingType(bitMask)) {
                endpoints.add(
                    CallEndpoint(
                        endpointTypeToString(CallEndpoint.TYPE_STREAMING),
                        CallEndpoint.TYPE_STREAMING
                    )
                )
            }
            return endpoints
        }

        private fun hasEarpieceType(bitMap: Int): Boolean {
            return (bitMap.and(CallAudioState.ROUTE_EARPIECE)) == CallAudioState.ROUTE_EARPIECE
        }

        fun hasBluetoothType(bitMap: Int): Boolean {
            return (bitMap.and(CallAudioState.ROUTE_BLUETOOTH)) == CallAudioState.ROUTE_BLUETOOTH
        }

        fun hasWiredHeadsetType(bitMap: Int): Boolean {
            return (bitMap.and(CallAudioState.ROUTE_WIRED_HEADSET)
                ) == CallAudioState.ROUTE_WIRED_HEADSET
        }

        fun hasSpeakerType(bitMap: Int): Boolean {
            return (bitMap.and(CallAudioState.ROUTE_SPEAKER)) == CallAudioState.ROUTE_SPEAKER
        }

        fun hasStreamingType(bitMap: Int): Boolean {
            return (bitMap.and(CallAudioState.ROUTE_STREAMING)) == CallAudioState.ROUTE_STREAMING
        }

        fun mapRouteToType(route: Int): @CallEndpoint.Companion.EndpointType Int {
            return when (route) {
                CallAudioState.ROUTE_EARPIECE -> CallEndpoint.TYPE_EARPIECE
                CallAudioState.ROUTE_BLUETOOTH -> CallEndpoint.TYPE_BLUETOOTH
                CallAudioState.ROUTE_WIRED_HEADSET -> CallEndpoint.TYPE_WIRED_HEADSET
                CallAudioState.ROUTE_SPEAKER -> CallEndpoint.TYPE_SPEAKER
                CallAudioState.ROUTE_STREAMING -> CallEndpoint.TYPE_STREAMING
                else -> CallEndpoint.TYPE_UNKNOWN
            }
        }

        fun mapTypeToRoute(route: Int): Int {
            return when (route) {
                CallEndpoint.TYPE_EARPIECE -> CallAudioState.ROUTE_EARPIECE
                CallEndpoint.TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                CallEndpoint.TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
                CallEndpoint.TYPE_SPEAKER -> CallAudioState.ROUTE_SPEAKER
                CallEndpoint.TYPE_STREAMING -> CallAudioState.ROUTE_STREAMING
                else -> CallAudioState.ROUTE_EARPIECE
            }
        }

        fun endpointTypeToString(endpointType: Int): String {
            return when (endpointType) {
                CallEndpoint.TYPE_EARPIECE -> "EARPIECE"
                CallEndpoint.TYPE_BLUETOOTH -> "BLUETOOTH"
                CallEndpoint.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
                CallEndpoint.TYPE_SPEAKER -> "SPEAKER"
                CallEndpoint.TYPE_STREAMING -> "EXTERNAL"
                else -> "UNKNOWN ($endpointType)"
            }
        }
    }

    @RequiresApi(34)
    object Api34PlusImpl {
        @JvmStatic
        @DoNotInline
        fun fromPlatformEndpointToAndroidXEndpoint(endpoint: android.telecom.CallEndpoint):
            CallEndpoint {
            return CallEndpoint(endpoint.endpointName, endpoint.endpointType, endpoint.identifier)
        }

        @JvmStatic
        @DoNotInline
        fun fromPlatformEndpointsToAndroidXEndpoints(endpoints: List<android.telecom.CallEndpoint>):
            List<CallEndpoint> {
            val res = ArrayList<CallEndpoint>()
            for (e in endpoints) {
                res.add(CallEndpoint(e.endpointName, e.endpointType, e.identifier))
            }
            return res
        }

        @JvmStatic
        @DoNotInline
        fun toPlatformEndpoint(e: CallEndpoint): android.telecom.CallEndpoint {
            return android.telecom.CallEndpoint(e.name, e.type, e.identifier)
        }
    }

    @RequiresApi(28)
    object BluetoothApi28PlusImpl {
        @JvmStatic
        @DoNotInline
        fun getBluetoothEndpoints(state: CallAudioState):
            ArrayList<CallEndpoint> {
            val endpoints: ArrayList<CallEndpoint> = ArrayList()
            val supportedBluetoothDevices = state.supportedBluetoothDevices
            for (bluetoothDevice in supportedBluetoothDevices) {
                endpoints.add(getCallEndpointFromBluetoothDevice(bluetoothDevice))
            }
            return endpoints
        }

        @JvmStatic
        @DoNotInline
        fun getCallEndpointFromBluetoothDevice(btDevice: BluetoothDevice?): CallEndpoint {
            var endpointName: String = "Bluetooth Device"
            var endpointIdentity: String = "Unknown Address"
            if (btDevice != null) {
                endpointIdentity = btDevice.address
                try {
                    endpointName = btDevice.name
                } catch (e: SecurityException) {
                    // pass through
                }
            }
            return CallEndpoint(endpointName, CallEndpoint.TYPE_BLUETOOTH, endpointIdentity)
        }

        @JvmStatic
        @DoNotInline
        fun getCallEndpointFromAudioState(state: CallAudioState): CallEndpoint {
            return getCallEndpointFromBluetoothDevice(state.activeBluetoothDevice)
        }
    }
}