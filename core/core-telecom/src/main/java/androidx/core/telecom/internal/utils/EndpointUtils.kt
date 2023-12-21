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
import androidx.core.telecom.CallEndpointCompat

@RequiresApi(Build.VERSION_CODES.O)
internal class EndpointUtils {

    companion object {
        fun toCallEndpointCompat(state: CallAudioState): CallEndpointCompat {
            val type: Int = mapRouteToType(state.route)
            return if (type == CallEndpointCompat.TYPE_BLUETOOTH && SDK_INT >= P) {
                BluetoothApi28PlusImpl.getCallEndpointFromAudioState(state)
            } else {
                CallEndpointCompat(endpointTypeToString(type), type)
            }
        }

        fun toCallEndpointsCompat(state: CallAudioState): List<CallEndpointCompat> {
            val endpoints: ArrayList<CallEndpointCompat> = ArrayList()
            val bitMask = state.supportedRouteMask
            if (hasEarpieceType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_EARPIECE),
                        CallEndpointCompat.TYPE_EARPIECE
                    )
                )
            }
            if (hasBluetoothType(bitMask)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    endpoints.addAll(BluetoothApi28PlusImpl.getBluetoothEndpoints(state))
                } else {
                    endpoints.add(
                        CallEndpointCompat(
                            endpointTypeToString(CallEndpointCompat.TYPE_BLUETOOTH),
                            CallEndpointCompat.TYPE_BLUETOOTH
                        )
                    )
                }
            }
            if (hasWiredHeadsetType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_WIRED_HEADSET),
                        CallEndpointCompat.TYPE_WIRED_HEADSET
                    )
                )
            }
            if (hasSpeakerType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_SPEAKER),
                        CallEndpointCompat.TYPE_SPEAKER
                    )
                )
            }
            if (hasStreamingType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_STREAMING),
                        CallEndpointCompat.TYPE_STREAMING
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

        fun mapRouteToType(route: Int): @CallEndpointCompat.Companion.EndpointType Int {
            return when (route) {
                CallAudioState.ROUTE_EARPIECE -> CallEndpointCompat.TYPE_EARPIECE
                CallAudioState.ROUTE_BLUETOOTH -> CallEndpointCompat.TYPE_BLUETOOTH
                CallAudioState.ROUTE_WIRED_HEADSET -> CallEndpointCompat.TYPE_WIRED_HEADSET
                CallAudioState.ROUTE_SPEAKER -> CallEndpointCompat.TYPE_SPEAKER
                CallAudioState.ROUTE_STREAMING -> CallEndpointCompat.TYPE_STREAMING
                else -> CallEndpointCompat.TYPE_UNKNOWN
            }
        }

        fun mapTypeToRoute(route: Int): Int {
            return when (route) {
                CallEndpointCompat.TYPE_EARPIECE -> CallAudioState.ROUTE_EARPIECE
                CallEndpointCompat.TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                CallEndpointCompat.TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
                CallEndpointCompat.TYPE_SPEAKER -> CallAudioState.ROUTE_SPEAKER
                CallEndpointCompat.TYPE_STREAMING -> CallAudioState.ROUTE_STREAMING
                else -> CallAudioState.ROUTE_EARPIECE
            }
        }

        fun endpointTypeToString(endpointType: Int): String {
            return when (endpointType) {
                CallEndpointCompat.TYPE_EARPIECE -> "EARPIECE"
                CallEndpointCompat.TYPE_BLUETOOTH -> "BLUETOOTH"
                CallEndpointCompat.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
                CallEndpointCompat.TYPE_SPEAKER -> "SPEAKER"
                CallEndpointCompat.TYPE_STREAMING -> "EXTERNAL"
                else -> "UNKNOWN ($endpointType)"
            }
        }
    }

    @RequiresApi(34)
    object Api34PlusImpl {
        @JvmStatic
        @DoNotInline
        fun toCallEndpointCompat(endpoint: android.telecom.CallEndpoint):
            CallEndpointCompat {
            return CallEndpointCompat(
                endpoint.endpointName,
                endpoint.endpointType,
                endpoint.identifier
            )
        }

        @JvmStatic
        @DoNotInline
        fun toCallEndpointsCompat(endpoints: List<android.telecom.CallEndpoint>):
            List<CallEndpointCompat> {
            val res = ArrayList<CallEndpointCompat>()
            for (e in endpoints) {
                res.add(CallEndpointCompat(e.endpointName, e.endpointType, e.identifier))
            }
            return res
        }

        @JvmStatic
        @DoNotInline
        fun toCallEndpoint(e: CallEndpointCompat): android.telecom.CallEndpoint {
            return android.telecom.CallEndpoint(e.name, e.type, e.identifier)
        }
    }

    @RequiresApi(28)
    object BluetoothApi28PlusImpl {
        @JvmStatic
        @DoNotInline
        fun getBluetoothEndpoints(state: CallAudioState):
            ArrayList<CallEndpointCompat> {
            val endpoints: ArrayList<CallEndpointCompat> = ArrayList()
            val supportedBluetoothDevices = state.supportedBluetoothDevices
            for (bluetoothDevice in supportedBluetoothDevices) {
                endpoints.add(getCallEndpointFromBluetoothDevice(bluetoothDevice))
            }
            return endpoints
        }

        @JvmStatic
        @DoNotInline
        fun getCallEndpointFromBluetoothDevice(btDevice: BluetoothDevice?): CallEndpointCompat {
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
            return CallEndpointCompat(
                endpointName,
                CallEndpointCompat.TYPE_BLUETOOTH,
                endpointIdentity
            )
        }

        @JvmStatic
        @DoNotInline
        fun getCallEndpointFromAudioState(state: CallAudioState): CallEndpointCompat {
            return getCallEndpointFromBluetoothDevice(state.activeBluetoothDevice)
        }
    }
}