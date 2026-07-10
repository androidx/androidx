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

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.OutcomeReceiver
import android.telecom.CallControl
import android.telecom.CallException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.isSpeakerEndpoint

/**
 * Manages the logic for handling unrequested video state upgrades from the platform.
 *
 * On some Android versions (SDK 34-36), the platform may erroneously upgrade an audio call to a
 * video call without an app request, which often results in an unwanted switch to the speakerphone.
 * This class detects such upgrades and provides logic to revert the video state and reroute audio
 * back to the earpiece if necessary.
 *
 * ### Bug Context
 * * **Affected Versions:** Android 14, 15, and 16.
 * * **Fixed In:** Android 17.
 *
 * ### Mitigation Details
 * * **Android 15-16:** We detect this state and revert the video state back to audio.
 * * **Android 14:** No effective mitigation is possible due to the lack of necessary video state
 *   APIs in the platform.
 *
 * ### Known Side Effects
 * * **Aggressive Reversion:** This mitigation path assumes that upgrades to video are unintended
 *   unless initiated by the app. Consequently, *all* remote surface changes upgrading the video
 *   state will be reverted back to audio.
 */
@RequiresApi(34)
internal class UnrequestedVideoManager {
    companion object {
        private val TAG = UnrequestedVideoManager::class.java.simpleName
    }

    @VisibleForTesting internal var mTrackingUnrequestedVideoStateUpgrade: Boolean = false

    /**
     * Determines if the given video state change represents the unrequested video upgrade bug.
     *
     * @param currentCallType The current call type (e.g., audio or video).
     * @param newVideoState The new video state reported by the platform.
     * @return `true` if this is likely the unrequested upgrade bug.
     */
    fun isUnrequestedVideoUpgradeBug(currentCallType: Int, newVideoState: Int): Boolean {
        val sdkHasVideoRemapBug = VERSION.SDK_INT in 35..36
        val isRemoteVideoStateUpgrade =
            currentCallType == CallAttributesCompat.CALL_TYPE_AUDIO_CALL &&
                newVideoState == CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        val result = sdkHasVideoRemapBug && isRemoteVideoStateUpgrade
        if (result) {
            Log.i(
                TAG,
                "isUnrequestedVideoUpgradeBug: Detected unrequested video upgrade bug. " +
                    "currentCallType=$currentCallType, newVideoState=$newVideoState",
            )
        }
        return result
    }

    /**
     * Handles the detected unrequested video state upgrade by preparing for a possible audio
     * reroute and synchronizing the platform's video state.
     *
     * @param platformInterface The platform's CallControl interface.
     * @param callType The call type to synchronize with the platform.
     * @param onRerouteCheck A callback to perform an immediate audio reroute check.
     */
    fun handleUnrequestedVideoStateUpgrade(
        platformInterface: CallControl?,
        callType: Int,
        onRerouteCheck: () -> Unit,
    ) {
        mTrackingUnrequestedVideoStateUpgrade = true
        onRerouteCheck()

        // finally, we need to ensure the platform is in sync with jetpack layer so update the
        // platform call type state
        if (VERSION.SDK_INT >= VERSION_CODES.VANILLA_ICE_CREAM) {
            platformInterface?.requestVideoState(
                callType,
                Runnable::run,
                object : OutcomeReceiver<Void, CallException> {
                    override fun onResult(result: Void?) {
                        Log.d(TAG, "handleUnrequestedVideoStateUpgrade: requestVideoState success")
                    }

                    override fun onError(error: CallException) {
                        Log.e(
                            TAG,
                            "handleUnrequestedVideoStateUpgrade: requestVideoState error=$error",
                        )
                    }
                },
            )
        }
    }

    /**
     * Checks if a reroute to the earpiece is necessary and triggers it via the provided callback.
     *
     * @param currentEndpoint The currently active audio endpoint.
     * @param availableEndpoints The list of all available audio endpoints.
     * @param preferredStartingEndpoint The endpoint preferred at call start.
     * @param lastClientRequestedEndpoint The last endpoint explicitly requested by the client.
     * @param isEndpointChange Indicates if this check is triggered by an endpoint change event.
     * @param currentCallType The current call type of the CallSession.
     * @param onReroute A callback to trigger the audio endpoint change.
     */
    fun maybeRerouteToEarpiece(
        currentEndpoint: CallEndpointCompat?,
        availableEndpoints: List<CallEndpointCompat>,
        preferredStartingEndpoint: CallEndpointCompat?,
        lastClientRequestedEndpoint: CallEndpointCompat?,
        isEndpointChange: Boolean,
        currentCallType: Int,
        onReroute: (CallEndpointCompat) -> Unit,
    ) {
        if (!mTrackingUnrequestedVideoStateUpgrade) {
            return
        }

        // If the call is no longer an audio call, we shouldn't attempt to reroute for this bug
        if (currentCallType != CallAttributesCompat.CALL_TYPE_AUDIO_CALL) {
            Log.i(
                TAG,
                "maybeRerouteToEarpiece: Call is no longer an audio call. Cancelling tracking.",
            )
            mTrackingUnrequestedVideoStateUpgrade = false
            return
        }

        // If we receive the subsequent endpoint change event, we can clear the tracking flag
        // after evaluating the new endpoint.
        if (isEndpointChange) {
            mTrackingUnrequestedVideoStateUpgrade = false
        }

        val endpoint = currentEndpoint ?: return

        val userReqSpeaker =
            userRequestedSpeaker(preferredStartingEndpoint, lastClientRequestedEndpoint)
        val isSpeaker = isSpeakerEndpoint(endpoint)

        // fix the audio route on behalf of the user if the platform switched the route to speaker
        if (isSpeaker && !userReqSpeaker) {
            Log.i(
                TAG,
                "maybeRerouteToEarpiece: Call was wrongfully upgraded to video, and route " +
                    "became SPEAKER due to platform bug. Forcing back to EARPIECE.",
            )

            // Ensure we clear tracking if we actively reroute, to prevent duplicate requests
            mTrackingUnrequestedVideoStateUpgrade = false

            EndpointUtils.getEarpieceEndpoint(availableEndpoints)?.let { onReroute(it) }
                ?: Log.w(
                    TAG,
                    "maybeRerouteToEarpiece: no earpiece endpoint found in availableEndpoints",
                )
        }
    }

    private fun userRequestedSpeaker(
        preferredStartingEndpoint: CallEndpointCompat?,
        lastClientRequestedEndpoint: CallEndpointCompat?,
    ): Boolean {
        return isSpeakerEndpoint(preferredStartingEndpoint) ||
            isSpeakerEndpoint(lastClientRequestedEndpoint)
    }
}
