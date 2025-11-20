/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat

/**
 * Manages the logic for automatically switching to the speaker during a video call.
 *
 * This class centralizes the decision-making process to avoid code duplication between different
 * call session implementations.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class VideoCallSpeakerManager(private val bluetoothDeviceChecker: BluetoothDeviceChecker) {
    companion object {
        private val TAG = VideoCallSpeakerManager::class.java.simpleName
    }

    /**
     * Determines if a switch to the speaker is appropriate.
     *
     * The conditions for switching are:
     * 1. It is a video call.
     * 2. The audio is currently on the earpiece.
     * 3. A speaker endpoint is available.
     * 4. There are no other non-watch Bluetooth devices available.
     *
     * @param isVideoCall True if the current call is a video call.
     * @param currentEndpoint The currently active audio endpoint.
     * @param availableEndpoints The list of all available audio endpoints.
     * @return `true` if the conditions are met and a switch to speaker is recommended.
     */
    fun shouldSwitchToSpeaker(
        isVideoCall: Boolean,
        currentEndpoint: CallEndpointCompat?,
        availableEndpoints: List<CallEndpointCompat>,
    ): Boolean {
        // Condition 1: Must be a video call.
        if (!isVideoCall) {
            return false
        }

        // Condition 2: Must start on the earpiece.
        if (currentEndpoint?.type != CallEndpointCompat.TYPE_EARPIECE) {
            Log.d(TAG, "shouldSwitchToSpeaker: Skipping, audio not on earpiece.")
            return false
        }

        // Condition 3: A speaker endpoint must be available.
        val speaker = availableEndpoints.find { it.type == CallEndpointCompat.TYPE_SPEAKER }
        if (speaker == null) {
            Log.d(TAG, "shouldSwitchToSpeaker: Skipping, no speaker available.")
            return false
        }

        // Condition 4: No other non-watch Bluetooth device should be available.
        // -- Perform a cheap check to see if any BT device exists. If not, we can switch.
        if (!availableEndpoints.any { it.isBluetoothType() }) {
            Log.i(
                TAG,
                "shouldSwitchToSpeaker: No BT devices found. Recommending switch to speaker.",
            )
            return true
        }
        // -- Only if a BT device exists, perform the costly permission check.
        val hasNonWatchDevice =
            bluetoothDeviceChecker.hasAvailableNonWatchDevice(availableEndpoints)
        // -- Log the result of the costly check and return the final decision.
        if (hasNonWatchDevice) {
            Log.i(TAG, "shouldSwitchToSpeaker: Skipping, a non-watch BT device" + " is available.")
        } else {
            // This case now specifically means "only a watch BT device is available"
            Log.i(
                TAG,
                "shouldSwitchToSpeaker: Only watch BT device is available," +
                    " recommending switch to SPEAKER.",
            )
        }
        return !hasNonWatchDevice
    }
}
