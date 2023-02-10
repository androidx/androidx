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

import android.os.Build.VERSION
import android.telecom.PhoneAccount
import androidx.core.telecom.CallsManager

/**
 * @hide
 */
class Utils {
    companion object {
        const val ERROR_BUILD_VERSION: String = "At this present time, the API call does" +
            " not support the build your device is on. Only U builds and above can use the" +
            " [CallsManager] class."

        /**
         * Helper method that determines if the device has a build that contains the Telecom V2
         * VoIP APIs. These include [TelecomManager#addCall], android.telecom.CallControl,
         * android.telecom.CallEventCallback but are not limited to only those classes.
         */
        fun hasPlatformV2Apis(): Boolean {
            return VERSION.SDK_INT >= 34 || VERSION.CODENAME == "UpsideDownCake"
        }

        fun remapJetpackCapabilitiesToPlatformCapabilities(
            @CallsManager.Companion.Capability clientBitmapSelection: Int
        ): Int {
            var remappedCapabilities = 0

            if (hasJetpackVideoCallingCapability(clientBitmapSelection)) {
                remappedCapabilities =
                    PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                        remappedCapabilities
            }

            if (hasJetpackSteamingCapability(clientBitmapSelection)) {
                remappedCapabilities =
                    PhoneAccount.CAPABILITY_SUPPORTS_CALL_STREAMING or
                        remappedCapabilities
            }
            return remappedCapabilities
        }

        fun hasCapability(targetCapability: Int, bitMap: Int): Boolean {
            return (bitMap.and(targetCapability)) == targetCapability
        }

        private fun hasJetpackVideoCallingCapability(bitMap: Int): Boolean {
            return hasCapability(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING, bitMap)
        }

        private fun hasJetpackSteamingCapability(bitMap: Int): Boolean {
            return hasCapability(CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING, bitMap)
        }
    }
}