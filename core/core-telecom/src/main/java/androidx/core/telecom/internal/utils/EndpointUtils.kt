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

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.telecom.CallAudioState
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallEndpointCompat.Companion.EndpointType
import androidx.core.telecom.R
import androidx.core.telecom.internal.CallEndpointUuidTracker.getUuid
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
internal class EndpointUtils {

    companion object {
        const val BLUETOOTH_DEVICE_DEFAULT_NAME = "Bluetooth Device"
        private val TAG: String = EndpointUtils::class.java.simpleName.toString()

        internal fun maybeRemoveEarpieceIfWiredEndpointPresent(
            endpoints: MutableList<CallEndpointCompat>
        ): MutableList<CallEndpointCompat> {
            if (endpoints.any { it.type == CallEndpointCompat.TYPE_WIRED_HEADSET }) {
                endpoints.removeIf { it.type == CallEndpointCompat.TYPE_EARPIECE }
            }
            return endpoints
        }

        /** [AudioDeviceInfo]s to [CallEndpointCompat]s */
        fun getEndpointsFromAudioDeviceInfo(
            c: Context,
            flowId: Int,
            adiArr: List<AudioDeviceInfo>?,
        ): List<CallEndpointCompat> {
            if (adiArr == null) {
                return listOf()
            }
            val endpoints: MutableList<CallEndpointCompat> = mutableListOf()
            var foundWiredHeadset = false
            val omittedDevices = StringBuilder("omitting devices =[")
            adiArr.filterNotNull().forEach { audioDeviceInfo ->
                val endpoint = getEndpointFromAudioDeviceInfo(c, flowId, audioDeviceInfo)
                if (endpoint.type != CallEndpointCompat.TYPE_UNKNOWN) {
                    if (endpoint.type == CallEndpointCompat.TYPE_WIRED_HEADSET) {
                        foundWiredHeadset = true
                    }
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
            if (foundWiredHeadset) {
                endpoints.removeIf { it.type == CallEndpointCompat.TYPE_EARPIECE }
            }
            // Sort by endpoint type.  The first element has the highest priority!
            endpoints.sort()
            return endpoints
        }

        /** [AudioDeviceInfo] --> [CallEndpointCompat] */
        private fun getEndpointFromAudioDeviceInfo(
            c: Context,
            flowId: Int,
            adi: AudioDeviceInfo,
        ): CallEndpointCompat {
            val endpointName = remapAudioDeviceNameToEndpointName(c, adi)
            val endpointType = remapAudioDeviceTypeToCallEndpointType(adi.type)
            val newEndpoint =
                CallEndpointCompat(
                    endpointName,
                    endpointType,
                    getUuid(flowId, endpointType, endpointName),
                )
            if (SDK_INT >= P && newEndpoint.isBluetoothType()) {
                newEndpoint.mMackAddress = adi.address
            }
            return newEndpoint
        }

        private fun remapAudioDeviceNameToEndpointName(
            c: Context,
            audioDeviceInfo: AudioDeviceInfo,
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

        fun isSpeakerEndpoint(endpoint: CallEndpointCompat?): Boolean {
            if (endpoint == null) {
                return false
            }
            return endpoint.type == CallEndpointCompat.TYPE_SPEAKER
        }

        fun isWiredHeadsetOrBtEndpoint(endpoint: CallEndpointCompat?): Boolean {
            if (endpoint == null) {
                return false
            }
            return endpoint.type == CallEndpointCompat.TYPE_BLUETOOTH ||
                endpoint.type == CallEndpointCompat.TYPE_WIRED_HEADSET
        }

        fun toCallEndpointCompat(state: CallAudioState, sessionId: Int): CallEndpointCompat {
            val type: Int = mapRouteToType(state.route)
            return if (
                isBluetoothType(type) && buildIsAtLeastP() && hasActiveBluetoothDevice(state)
            ) {
                BluetoothApi28PlusImpl.getCallEndpointFromAudioState(state, sessionId)
            } else {
                CallEndpointCompat(endpointTypeToString(type), type, getUuid(sessionId, type))
            }
        }

        private fun isBluetoothType(type: Int): Boolean {
            return type == CallEndpointCompat.TYPE_BLUETOOTH
        }

        private fun buildIsAtLeastP(): Boolean {
            return SDK_INT >= P
        }

        @RequiresApi(P)
        private fun hasActiveBluetoothDevice(state: CallAudioState): Boolean {
            return state.activeBluetoothDevice != null
        }

        fun toCallEndpointsCompat(state: CallAudioState, sessionId: Int): List<CallEndpointCompat> {
            val endpoints: ArrayList<CallEndpointCompat> = ArrayList()
            val bitMask = state.supportedRouteMask
            if (hasEarpieceType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_EARPIECE),
                        CallEndpointCompat.TYPE_EARPIECE,
                        getUuid(sessionId, CallEndpointCompat.TYPE_EARPIECE),
                    )
                )
            }
            if (hasBluetoothType(bitMask)) {
                if (SDK_INT >= P) {
                    endpoints.addAll(BluetoothApi28PlusImpl.getBluetoothEndpoints(state, sessionId))
                } else {
                    endpoints.add(
                        CallEndpointCompat(
                            endpointTypeToString(CallEndpointCompat.TYPE_BLUETOOTH),
                            CallEndpointCompat.TYPE_BLUETOOTH,
                            getUuid(
                                sessionId,
                                CallEndpointCompat.TYPE_BLUETOOTH,
                                endpointTypeToString(CallEndpointCompat.TYPE_BLUETOOTH),
                            ),
                        )
                    )
                }
            }
            if (hasWiredHeadsetType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_WIRED_HEADSET),
                        CallEndpointCompat.TYPE_WIRED_HEADSET,
                        getUuid(sessionId, CallEndpointCompat.TYPE_WIRED_HEADSET),
                    )
                )
            }
            if (hasSpeakerType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_SPEAKER),
                        CallEndpointCompat.TYPE_SPEAKER,
                        getUuid(sessionId, CallEndpointCompat.TYPE_SPEAKER),
                    )
                )
            }
            if (hasStreamingType(bitMask)) {
                endpoints.add(
                    CallEndpointCompat(
                        endpointTypeToString(CallEndpointCompat.TYPE_STREAMING),
                        CallEndpointCompat.TYPE_STREAMING,
                        getUuid(sessionId, CallEndpointCompat.TYPE_STREAMING),
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
                CallEndpointCompat.TYPE_BLUETOOTH -> BLUETOOTH_DEVICE_DEFAULT_NAME
                CallEndpointCompat.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
                CallEndpointCompat.TYPE_SPEAKER -> "SPEAKER"
                CallEndpointCompat.TYPE_STREAMING -> "EXTERNAL"
                else -> "UNKNOWN ($endpointType)"
            }
        }

        internal fun getMaskedMacAddress(address: String?): String {
            return if (address == null) {
                ""
            } else {
                "[**:**:**:**:" + address.takeLast(4) + "]"
            }
        }

        /**
         * Checks if a given Bluetooth device is a wearable.
         *
         * @return `true` if the device is a wearable, `false` otherwise. Returns `false` if
         *   permissions are missing, assuming it's a valid audio device to be safe.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        private fun isWearableDevice(context: Context, device: BluetoothDevice?): Boolean {
            if (!hasSufficientBluetoothPermission(context)) {
                Log.w(TAG, "Permission denied. Assuming a BT device could be present.")
                return false
            }
            return try {
                device?.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE
            } catch (e: SecurityException) {
                Log.w(TAG, "isWearableDevice: Permission denied", e)
                false
            }
        }

        /**
         * The centralized logic to check for an available, non-wearable Bluetooth device.
         *
         * @param endpoints The list of available call endpoints.
         * @param deviceLookup A lambda function that resolves a CallEndpointCompat to a
         *   BluetoothDevice.
         * @return `true` if a non-wearable Bluetooth device is found.
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun hasAvailableNonWearableDevice(
            context: Context,
            endpoints: List<CallEndpointCompat>,
            deviceLookup: (endpoint: CallEndpointCompat) -> BluetoothDevice?,
        ): Boolean {
            if (!hasSufficientBluetoothPermission(context)) {
                Log.w(TAG, "Permission denied. Assuming a BT device could be present.")
                return true
            }
            return try {
                endpoints.any { endpoint ->
                    if (endpoint.isBluetoothType()) {
                        val device = deviceLookup(endpoint)
                        !isWearableDevice(context, device)
                    } else {
                        false
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Security Exception hit. Assuming a BT device could be present.", e)
                return true
            }
        }

        fun hasSufficientBluetoothPermission(context: Context): Boolean {
            // For Android 12 (S) and above, check for BLUETOOTH_CONNECT
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                // For older versions, check for BLUETOOTH
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }

    @RequiresApi(34)
    object Api34PlusImpl {
        @JvmStatic
        @DoNotInline
        fun toCallEndpoint(e: CallEndpointCompat): android.telecom.CallEndpoint {
            return android.telecom.CallEndpoint(e.name, e.type, e.identifier)
        }
    }

    @RequiresApi(28)
    object BluetoothApi28PlusImpl {
        @JvmStatic
        fun getBluetoothEndpoints(
            state: CallAudioState,
            sessionId: Int,
        ): ArrayList<CallEndpointCompat> {
            val endpoints: ArrayList<CallEndpointCompat> = ArrayList()
            val supportedBluetoothDevices = state.supportedBluetoothDevices
            for (bluetoothDevice in supportedBluetoothDevices) {
                endpoints.add(getCallEndpointFromBluetoothDevice(bluetoothDevice, sessionId))
            }
            return endpoints
        }

        @JvmStatic
        fun getCallEndpointFromBluetoothDevice(
            btDevice: BluetoothDevice,
            sessionId: Int,
        ): CallEndpointCompat {
            var endpointName: String? = null
            var mackAddress: String? = null
            try {
                endpointName = btDevice.name
                mackAddress = btDevice.address
            } catch (se: SecurityException) {
                // A SecurityException will be thrown if the user has no granted the
                // BLUETOOTH_CONNECT permission
                se.printStackTrace()
            }
            // Account for [BluetoothDevice#getName()] returning a null value
            if (endpointName == null) {
                endpointName = BLUETOOTH_DEVICE_DEFAULT_NAME
            }
            // Account for [BluetoothDevice#getAddress()] returning a null value
            if (mackAddress == null) {
                mackAddress = UUID.randomUUID().toString()
                Log.i(
                    "BluetoothApi28PlusImpl",
                    "setting mac_address[${getMaskedMacAddress(mackAddress)}]",
                )
            }
            return CallEndpointCompat(
                endpointName,
                CallEndpointCompat.TYPE_BLUETOOTH,
                sessionId,
                mackAddress = mackAddress,
            )
        }

        @JvmStatic
        fun getCallEndpointFromAudioState(
            state: CallAudioState,
            sessionId: Int,
        ): CallEndpointCompat {
            return getCallEndpointFromBluetoothDevice(state.activeBluetoothDevice, sessionId)
        }
    }
}
