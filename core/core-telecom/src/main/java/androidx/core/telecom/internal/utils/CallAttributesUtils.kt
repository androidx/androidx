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
import android.telecom.PhoneAccountHandle
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat

@RequiresApi(34)
internal class CallAttributesUtils {
    internal object Api34PlusImpl {
        @JvmStatic
        @DoNotInline
        fun toTelecomCallAttributes(
            phoneAccountHandle: PhoneAccountHandle,
            direction: Int,
            displayName: CharSequence,
            address: Uri,
            callType: Int,
            callCapabilities: Int
        ): android.telecom.CallAttributes {
            return android.telecom.CallAttributes.Builder(
                phoneAccountHandle,
                direction,
                displayName,
                address
            )
                .setCallType(remapCallType(callType))
                .setCallCapabilities(remapCapabilities(callCapabilities))
                .build()
        }

        private fun remapCallType(callType: Int): Int {
            return if (callType == CallAttributesCompat.CALL_TYPE_AUDIO_CALL) {
                android.telecom.CallAttributes.AUDIO_CALL
            } else {
                android.telecom.CallAttributes.VIDEO_CALL
            }
        }

        private fun remapCapabilities(callCapabilities: Int): Int {
            var bitMap: Int = 0
            if (hasSupportsSetInactiveCapability(callCapabilities)) {
                bitMap = bitMap or android.telecom.CallAttributes.SUPPORTS_SET_INACTIVE
            }
            if (hasStreamCapability(callCapabilities)) {
                bitMap = bitMap or android.telecom.CallAttributes.SUPPORTS_STREAM
            }
            if (hasTransferCapability(callCapabilities)) {
                bitMap = bitMap or android.telecom.CallAttributes.SUPPORTS_TRANSFER
            }
            return bitMap
        }

        private fun hasSupportsSetInactiveCapability(callCapabilities: Int): Boolean {
            return Utils.hasCapability(CallAttributesCompat.SUPPORTS_SET_INACTIVE, callCapabilities)
        }

        private fun hasStreamCapability(callCapabilities: Int): Boolean {
            return Utils.hasCapability(CallAttributesCompat.SUPPORTS_STREAM, callCapabilities)
        }

        private fun hasTransferCapability(callCapabilities: Int): Boolean {
            return Utils.hasCapability(CallAttributesCompat.SUPPORTS_TRANSFER, callCapabilities)
        }
    }
}