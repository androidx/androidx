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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager

internal class Utils {
    companion object {
        private val TAG = Utils.Companion::class.java.simpleName

        private val defaultBuildAdapter =
            object : BuildVersionAdapter {
                override fun hasInvalidBuildVersion(): Boolean {
                    Log.i(TAG, "hasInvalidBuildVersion: " + "versionSdkInt=[${VERSION.SDK_INT}]")
                    return VERSION.SDK_INT < VERSION_CODES.O
                }

                override fun getCurrentSdk(): Int {
                    return VERSION.SDK_INT
                }
            }
        private var mBuildVersion: BuildVersionAdapter = defaultBuildAdapter

        internal fun setUtils(utils: BuildVersionAdapter) {
            mBuildVersion = utils
        }

        internal fun resetUtils() {
            mBuildVersion = defaultBuildAdapter
        }

        /**
         * Determines if the library should use the legacy ConnectionService path based on the
         * configuration set during [CallsManager.registerAppWithTelecom].
         */
        @RequiresApi(VERSION_CODES.O)
        fun shouldUseBackwardsCompatImplementation(): Boolean {
            return getCurrentSdk() <= CallsManager.mBackwardsCompatUpperBound
        }

        fun hasInvalidBuildVersion(): Boolean {
            return mBuildVersion.hasInvalidBuildVersion()
        }

        fun getCurrentSdk(): Int {
            return mBuildVersion.getCurrentSdk()
        }

        fun verifyBuildVersion() {
            if (mBuildVersion.hasInvalidBuildVersion()) {
                throw UnsupportedOperationException(
                    "Core-Telecom only supports builds from" +
                        " Oreo (Android 8) and above.  In order to utilize Core-Telecom, your " +
                        "device must be updated."
                )
            }
        }

        fun isBuildAtLeastP(): Boolean {
            return VERSION.SDK_INT >= VERSION_CODES.P
        }

        /**
         * Checks if the application has the necessary Bluetooth permissions.
         *
         * For API level 31 and above, this checks for BLUETOOTH_CONNECT permission. For API levels
         * 28-30, it checks for BLUETOOTH and BLUETOOTH_ADMIN permissions.
         *
         * @param context The application context.
         * @return `true` if the required permissions are granted, `false` otherwise.
         */
        fun hasBluetoothPermissions(context: Context): Boolean {
            return if (VERSION.SDK_INT >= VERSION_CODES.S) {
                // API level 31+
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // API levels 28-30
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_ADMIN,
                    ) == PackageManager.PERMISSION_GRANTED
            }
        }

        @RequiresApi(VERSION_CODES.O)
        fun remapJetpackCapsToPlatformCaps(
            @CallsManager.Companion.Capability clientBitmapSelection: Int,
            useTransactionalApis: Boolean,
        ): Int {
            // start to build the PhoneAccount that will be registered via the platform API
            var platformCapabilities: Int = PhoneAccount.CAPABILITY_SELF_MANAGED
            // Add transactional capabilities ONLY if not using the backwards compat path.
            if (useTransactionalApis) {
                platformCapabilities =
                    PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS or
                        platformCapabilities
            }
            if (hasJetpackVideoCallingCapability(clientBitmapSelection)) {
                platformCapabilities =
                    PhoneAccount.CAPABILITY_VIDEO_CALLING or
                        PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                        platformCapabilities
            }

            if (hasJetpackSteamingCapability(clientBitmapSelection)) {
                platformCapabilities =
                    PhoneAccount.CAPABILITY_SUPPORTS_CALL_STREAMING or platformCapabilities
            }

            return platformCapabilities
        }

        fun hasCapability(targetCapability: Int, bitMap: Int): Boolean {
            return (bitMap.and(targetCapability)) == targetCapability
        }

        @RequiresApi(VERSION_CODES.O)
        private fun hasJetpackVideoCallingCapability(bitMap: Int): Boolean {
            return hasCapability(CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING, bitMap)
        }

        @RequiresApi(VERSION_CODES.O)
        private fun hasJetpackSteamingCapability(bitMap: Int): Boolean {
            return hasCapability(CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING, bitMap)
        }

        fun getBundleWithPhoneAccountHandle(
            callAttributes: CallAttributesCompat,
            handle: PhoneAccountHandle,
        ): Bundle {
            val extras = Bundle()
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
            if (!callAttributes.isOutgoingCall()) {
                extras.putParcelable(
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    callAttributes.address,
                )
            }
            return extras
        }
    }
}
