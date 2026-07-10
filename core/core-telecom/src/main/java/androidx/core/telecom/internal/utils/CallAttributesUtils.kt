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

import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat

/**
 * Internal utilities for converting [CallAttributesCompat] constants and attributes to the platform
 * [android.telecom.CallAttributes] equivalents.
 */
internal object CallAttributesUtils {

    /** Maps [CallAttributesCompat.CallType] to [android.telecom.CallAttributes.CallType]. */
    private fun remapCallType(callType: Int): Int =
        when (callType) {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL -> android.telecom.CallAttributes.AUDIO_CALL
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL -> android.telecom.CallAttributes.VIDEO_CALL
            else -> android.telecom.CallAttributes.VIDEO_CALL
        }

    /**
     * Maps [CallAttributesCompat.CallCapability] bitmask to
     * [android.telecom.CallAttributes.getCallCapabilities] bitmask.
     */
    private fun remapCapabilities(callCapabilities: Int): Int {
        var platformCapabilities = 0
        if (Utils.hasCapability(CallAttributesCompat.SUPPORTS_SET_INACTIVE, callCapabilities)) {
            platformCapabilities =
                platformCapabilities or android.telecom.CallAttributes.SUPPORTS_SET_INACTIVE
        }
        if (Utils.hasCapability(CallAttributesCompat.SUPPORTS_STREAM, callCapabilities)) {
            platformCapabilities =
                platformCapabilities or android.telecom.CallAttributes.SUPPORTS_STREAM
        }
        if (Utils.hasCapability(CallAttributesCompat.SUPPORTS_TRANSFER, callCapabilities)) {
            platformCapabilities =
                platformCapabilities or android.telecom.CallAttributes.SUPPORTS_TRANSFER
        }
        return platformCapabilities
    }

    /** Implementation for Android U (API 34) and above. */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    internal object Api34PlusImpl {
        @JvmStatic
        fun toTelecomCallAttributes(
            phoneAccountHandle: PhoneAccountHandle,
            direction: Int,
            displayName: CharSequence,
            address: Uri,
            callType: Int,
            callCapabilities: Int,
        ): android.telecom.CallAttributes {
            return android.telecom.CallAttributes.Builder(
                    phoneAccountHandle,
                    direction,
                    displayName,
                    address,
                )
                .setCallType(remapCallType(callType))
                .setCallCapabilities(remapCapabilities(callCapabilities))
                .build()
        }
    }

    /** Implementation for 36.1 sdk and above. */
    @RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
    internal object Api36Point1PlusImpl {
        @JvmStatic
        fun toTelecomCallAttributes(
            phoneAccountHandle: PhoneAccountHandle,
            direction: Int,
            displayName: CharSequence,
            address: Uri,
            callType: Int,
            callCapabilities: Int,
            isLogExcluded: Boolean,
        ): android.telecom.CallAttributes {
            return android.telecom.CallAttributes.Builder(
                    phoneAccountHandle,
                    direction,
                    displayName,
                    address,
                )
                .setCallType(remapCallType(callType))
                .setCallCapabilities(remapCapabilities(callCapabilities))
                .setLogExcluded(isLogExcluded)
                .build()
        }
    }
}
