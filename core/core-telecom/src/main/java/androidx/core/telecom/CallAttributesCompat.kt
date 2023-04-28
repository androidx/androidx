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

package androidx.core.telecom

import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.internal.utils.CallAttributesUtils
import androidx.core.telecom.internal.utils.Utils
import java.util.Objects

/**
 * CallAttributes represents a set of properties that define a new Call.  Applications should build
 * an instance of this class and use [CallsManager.addCall] to start a new call with Telecom.
 *
 * @param displayName  Display name of the person on the other end of the call
 * @param address Address of the call. Note, this can be extended to a meeting link
 * @param direction The direction (Outgoing/Incoming) of the new Call
 * @param callType Information related to data being transmitted (voice, video, etc. )
 * @param callCapabilities Allows a package to opt into capabilities on the telecom side,
 *                         on a per-call basis
 */
class CallAttributesCompat constructor(
    val displayName: CharSequence,
    val address: Uri,
    @Direction val direction: Int,
    @CallType val callType: Int = CALL_TYPE_AUDIO_CALL,
    @CallCapability val callCapabilities: Int = SUPPORTS_SET_INACTIVE
) {
    internal var mHandle: PhoneAccountHandle? = null

    override fun toString(): String {
        return "CallAttributes(" +
            "displayName=[$displayName], " +
            "address=[$address], " +
            "direction=[${directionToString()}], " +
            "callType=[${callTypeToString()}], " +
            "capabilities=[${capabilitiesToString()}])"
    }

    override fun equals(other: Any?): Boolean {
        return other is CallAttributesCompat &&
            displayName == other.displayName &&
            address == other.address &&
            direction == other.direction &&
            callType == other.callType &&
            callCapabilities == other.callCapabilities
    }

    override fun hashCode(): Int {
        return Objects.hash(displayName, address, direction, callType, callCapabilities)
    }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(DIRECTION_INCOMING, DIRECTION_OUTGOING)
        @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
        annotation class Direction

        /**
         * Indicates that the call is an incoming call.
         */
        const val DIRECTION_INCOMING = 1

        /**
         * Indicates that the call is an outgoing call.
         */
        const val DIRECTION_OUTGOING = 2

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(CALL_TYPE_AUDIO_CALL, CALL_TYPE_VIDEO_CALL)
        @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
        annotation class CallType

        /**
         * Used when answering or dialing a call to indicate that the call does not have a video
         * component
         */
        const val CALL_TYPE_AUDIO_CALL = 1

        /**
         * Indicates video transmission is supported
         */
        const val CALL_TYPE_VIDEO_CALL = 2

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(SUPPORTS_SET_INACTIVE, SUPPORTS_STREAM, SUPPORTS_TRANSFER, flag = true)
        @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
        annotation class CallCapability

        /**
         * This call being created can be set to inactive (traditionally referred to as hold).  This
         * means that once a new call goes active, if the active call needs to be held in order to
         * place or receive an incoming call, the active call will be placed on hold.  otherwise,
         * the active call may be disconnected.
         */
        const val SUPPORTS_SET_INACTIVE = 1 shl 1

        /**
         * This call can be streamed from a root device to another device to continue the call
         * without completely transferring it. The call continues to take place on the source
         * device, however media and control are streamed to another device.
         */
        const val SUPPORTS_STREAM = 1 shl 2

        /**
         * This call can be completely transferred from one endpoint to another.
         */
        const val SUPPORTS_TRANSFER = 1 shl 3
    }

    @RequiresApi(34)
    internal fun toCallAttributes(
        phoneAccountHandle: PhoneAccountHandle
    ): android.telecom.CallAttributes {
        return CallAttributesUtils.Api34PlusImpl.toTelecomCallAttributes(
            phoneAccountHandle,
            direction,
            displayName,
            address,
            callType,
            callCapabilities
        )
    }

    private fun directionToString(): String {
        return if (direction == DIRECTION_OUTGOING) {
            "Outgoing"
        } else {
            "Incoming"
        }
    }

    private fun callTypeToString(): String {
        return if (callType == CALL_TYPE_AUDIO_CALL) {
            "Audio"
        } else {
            "Video"
        }
    }

    internal fun hasSupportsSetInactiveCapability(): Boolean {
        return Utils.hasCapability(SUPPORTS_SET_INACTIVE, callCapabilities)
    }

    private fun hasStreamCapability(): Boolean {
        return Utils.hasCapability(SUPPORTS_STREAM, callCapabilities)
    }

    private fun hasTransferCapability(): Boolean {
        return Utils.hasCapability(SUPPORTS_TRANSFER, callCapabilities)
    }

    private fun capabilitiesToString(): String {
        val sb = StringBuilder()
        sb.append("[")
        if (hasSupportsSetInactiveCapability()) {
            sb.append("SetInactive")
        }
        if (hasStreamCapability()) {
            sb.append(", Stream")
        }
        if (hasTransferCapability()) {
            sb.append(", Transfer")
        }
        sb.append("])")
        return sb.toString()
    }

    internal fun isOutgoingCall(): Boolean {
        return direction == DIRECTION_OUTGOING
    }
}