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
import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.ParcelUuid
import android.telecom.CallAudioState
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallEndpointCompat.Companion.EndpointType
import androidx.core.telecom.R
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
internal class EndpointUtils {

    companion object {
        private val TAG: String = EndpointUtils::class.java.simpleName.toString()

        /** [AudioDeviceInfo]s to [CallEndpointCompat]s */
        fun getEndpointsFromAudioDeviceInfo(
            c: Context,
            adiArr: List<AudioDeviceInfo>?
        ): List<CallEndpointCompat> {
            if (adiArr == null) {
                return listOf()
            }
            val endpoints: MutableList<CallEndpointCompat> = mutableListOf()
            val omittedDevices = StringBuilder("omitting devices =[")
            adiArr.toList().forEach { audioDeviceInfo ->
                val endpoint = getEndpointFromAudioDeviceInfo(c, audioDeviceInfo)
                if (endpoint.type != CallEndpointCompat.TYPE_UNKNOWN) {
                    endpoints.add(endpoint)
                } else {
                    omittedDevices.append(
                        "(type=[${audioDeviceInfo.type}]," +
                            " name=[${audioDeviceInfo.productName}]),"
                    )
                }
            }
            omittedDevices.append("]")
            Log.i(TAG, omittedDevices.toString())
            // Sort by endpoint type.  The first element has the highest priority!
            endpoints.sort()
            return endpoints
        }

        /** [AudioDeviceInfo] --> [CallEndpointCompat] */
        private fun getEndpointFromAudioDeviceInfo(
            c: Context,
            adi: AudioDeviceInfo
        ): CallEndpointCompat {
            val newEndpoint =
                CallEndpointCompat(
                    remapAudioDeviceNameToEndpointName(c, adi),
                    remapAudioDeviceTypeToCallEndpointType(adi.type),
                    ParcelUuid(UUID.randomUUID())
                )
            if (SDK_INT >= P && newEndpoint.isBluetoothType()) {
                newEndpoint.mMackAddress = adi.address
            }
            return newEndpoint
        }

        private fun remapAudioDeviceNameToEndpointName(
            c: Context,
            audioDeviceInfo: AudioDeviceInfo
        ): String {
            return when (audioDeviceInfo.type) {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ->
                    c.getString(R.string.callendpoint_name_earpiece)
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
                    c.getString(R.string.callendpoint_name_speaker)
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_HEADSET ->
                    c.getString(R.string.callendpoint_name_wiredheadset)
                else -> audioDeviceInfo.productName.toString()
            }
        }

        internal fun remapAudioDeviceTypeToCallEndpointType(
            audioDeviceInfoType: Int
        ): (@EndpointType Int) {
            return when (audioDeviceInfoType) {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> CallEndpointCompat.TYPE_EARPIECE
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> CallEndpointCompat.TYPE_SPEAKER
                // Wired Headset Devices
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_HEADSET -> CallEndpointCompat.TYPE_WIRED_HEADSET
                // Bluetooth Devices
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_BLE_BROADCAST -> CallEndpointCompat.TYPE_BLUETOOTH
                // Everything else is defaulted to TYPE_UNKNOWN
                else -> CallEndpointCompat.TYPE_UNKNOWN
            }
        }

        fun getSpeakerEndpoint(endpoints: List<CallEndpointCompat>): CallEndpointCompat? {
            for (e in endpoints) {
                if (e.type == CallEndpointCompat.TYPE_SPEAKER) {
                    return e
                }
            }
            return null
        }

        fun isBluetoothAvailable(endpoints: List<CallEndpointCompat>): Boolean {
            for (e in endpoints) {
                if (e.type == CallEndpointCompat.TYPE_BLUETOOTH) {
                    return true
                }
            }
            return false
        }

        fun isEarpieceEndpoint(endpoint: CallEndpointCompat?): Boolean {
            if (endpoint == null) {
                return false
            }
            return endpoint.type == CallEndpointCompat.TYPE_EARPIECE
        }

        fun isWiredHeadsetOrBtEndpoint(endpoint: CallEndpointCompat?): Boolean {
            if (endpoint == null) {
                return false
            }
            return endpoint.type == CallEndpointCompat.TYPE_BLUETOOTH ||
                endpoint.type == CallEndpointCompat.TYPE_WIRED_HEADSET
        }

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
                if (SDK_INT >= P) {
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
            return (bitMap.and(CallAudioState.ROUTE_WIRED_HEADSET)) ==
                CallAudioState.ROUTE_WIRED_HEADSET
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
        fun toCallEndpointCompat(endpoint: android.telecom.CallEndpoint): CallEndpointCompat {
            return CallEndpointCompat(
                endpoint.endpointName,
                endpoint.endpointType,
                endpoint.identifier
            )
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
        fun getBluetoothEndpoints(state: CallAudioState): ArrayList<CallEndpointCompat> {
            val endpoints: ArrayList<CallEndpointCompat> = ArrayList()
            val supportedBluetoothDevices = state.supportedBluetoothDevices
            for (bluetoothDevice in supportedBluetoothDevices) {
                endpoints.add(getCallEndpointFromBluetoothDevice(bluetoothDevice))
            }
            return endpoints
        }

        @JvmStatic
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
        fun getCallEndpointFromAudioState(state: CallAudioState): CallEndpointCompat {
            return getCallEndpointFromBluetoothDevice(state.activeBluetoothDevice)
        }
    }
}
