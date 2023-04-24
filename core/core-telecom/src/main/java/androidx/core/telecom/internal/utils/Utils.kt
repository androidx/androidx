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

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributes
import androidx.core.telecom.CallException
import androidx.core.telecom.CallsManager

internal class Utils {
    companion object {
        private val defaultBuildAdapter =
            object : BuildVersionAdapter {
                /**
                 * Helper method that determines if the device has a build that contains the Telecom V2
                 * VoIP APIs. These include [TelecomManager#addCall], android.telecom.CallControl,
                 * android.telecom.CallEventCallback but are not limited to only those classes.
                 */
                override fun hasPlatformV2Apis(): Boolean {
                    return VERSION.SDK_INT >= 34 || VERSION.CODENAME == "UpsideDownCake"
                }

                override fun hasInvalidBuildVersion(): Boolean {
                    return VERSION.SDK_INT < VERSION_CODES.O
                }
            }
        private var mBuildVersion: BuildVersionAdapter = defaultBuildAdapter

        internal fun setUtils(utils: BuildVersionAdapter) {
            mBuildVersion = utils
        }

        internal fun resetUtils() {
            mBuildVersion = defaultBuildAdapter
        }

        fun hasPlatformV2Apis(): Boolean {
            return mBuildVersion.hasPlatformV2Apis()
        }

        fun hasInvalidBuildVersion(): Boolean {
            return mBuildVersion.hasInvalidBuildVersion()
        }

        fun verifyBuildVersion() {
            if (mBuildVersion.hasInvalidBuildVersion()) {
                throw UnsupportedOperationException(CallException.ERROR_BUILD_VERSION)
            }
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

        fun getBundleWithPhoneAccountHandle(
            callAttributes: CallAttributes,
            handle: PhoneAccountHandle
        ): Bundle {
            return if (VERSION.SDK_INT >= VERSION_CODES.M) {
                Api23PlusImpl.createExtras(callAttributes, handle)
            } else {
                Bundle()
            }
        }

        @RequiresApi(VERSION_CODES.M)
        private object Api23PlusImpl {
            @JvmStatic
            @DoNotInline
            fun createExtras(callAttributes: CallAttributes, handle: PhoneAccountHandle): Bundle {
                val extras = Bundle()
                extras.putParcelable(
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    handle
                )
                if (!callAttributes.isOutgoingCall()) {
                    extras.putParcelable(
                        TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        callAttributes.address
                    )
                }
                return extras
            }
        }
    }
}