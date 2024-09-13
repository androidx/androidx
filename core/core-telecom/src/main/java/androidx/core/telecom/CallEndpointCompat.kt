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

import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.internal.CallEndpointUuidTracker
import androidx.core.telecom.internal.utils.EndpointUtils
import java.util.Objects

/**
 * Constructor for a [CallEndpointCompat] object.
 *
 * @param name Human-readable name associated with the endpoint
 * @param type The type of endpoint through which call media being routed Allowed values:
 *   [TYPE_EARPIECE] , [TYPE_BLUETOOTH] , [TYPE_WIRED_HEADSET] , [TYPE_SPEAKER] , [TYPE_STREAMING] ,
 *   [TYPE_UNKNOWN]
 * @param identifier A unique identifier for this endpoint on the device
 */
@RequiresApi(VERSION_CODES.O)
public class CallEndpointCompat(
    public val name: CharSequence,
    public val type: Int,
    public val identifier: ParcelUuid
) : Comparable<CallEndpointCompat> {
    internal var mMackAddress: String = UNKNOWN_MAC_ADDRESS

    override fun toString(): String {
        return "CallEndpoint(" +
            "name=[$name]," +
            "type=[${EndpointUtils.endpointTypeToString(type)}]," +
            "identifier=[$identifier])"
    }

    /**
     * Compares this [CallEndpointCompat] to the other [CallEndpointCompat] for order. Returns a
     * positive number if this type rank is greater than the other value. Returns a negative number
     * if this type rank is less than the other value. Sort the CallEndpoint by type. Ranking them
     * by:
     * 1. TYPE_WIRED_HEADSET
     * 2. TYPE_BLUETOOTH
     * 3. TYPE_SPEAKER
     * 4. TYPE_EARPIECE
     * 5. TYPE_STREAMING
     * 6. TYPE_UNKNOWN If two endpoints have the same type, the name is compared to determine the
     *    value.
     */
    override fun compareTo(other: CallEndpointCompat): Int {
        // sort by type
        val res = this.getTypeRank().compareTo(other.getTypeRank())
        if (res != 0) {
            return res
        }
        // break ties using alphabetic order
        return this.name.toString().compareTo(other.name.toString())
    }

    override fun equals(other: Any?): Boolean {
        return other is CallEndpointCompat &&
            name == other.name &&
            type == other.type &&
            identifier == other.identifier
    }

    override fun hashCode(): Int {
        return Objects.hash(name, type, identifier)
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            TYPE_UNKNOWN,
            TYPE_EARPIECE,
            TYPE_BLUETOOTH,
            TYPE_WIRED_HEADSET,
            TYPE_SPEAKER,
            TYPE_STREAMING
        )
        @Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
        public annotation class EndpointType

        /** Indicates that the type of endpoint through which call media flows is unknown type. */
        public const val TYPE_UNKNOWN: Int = -1

        /** Indicates that the type of endpoint through which call media flows is an earpiece. */
        public const val TYPE_EARPIECE: Int = 1

        /** Indicates that the type of endpoint through which call media flows is a Bluetooth. */
        public const val TYPE_BLUETOOTH: Int = 2

        /**
         * Indicates that the type of endpoint through which call media flows is a wired headset.
         */
        public const val TYPE_WIRED_HEADSET: Int = 3

        /** Indicates that the type of endpoint through which call media flows is a speakerphone. */
        public const val TYPE_SPEAKER: Int = 4

        /** Indicates that the type of endpoint through which call media flows is an external. */
        public const val TYPE_STREAMING: Int = 5

        internal const val UNKNOWN_MAC_ADDRESS: String = "-1"
    }

    internal constructor(
        name: String,
        @EndpointType type: Int,
        sessionId: Int,
        mackAddress: String = "-1"
    ) : this(name, type, CallEndpointUuidTracker.getUuid(sessionId, type, name)) {
        mMackAddress = mackAddress
    }

    /** Internal helper to determine if this [CallEndpointCompat] is EndpointType#TYPE_BLUETOOTH */
    internal fun isBluetoothType(): Boolean {
        return type == TYPE_BLUETOOTH
    }

    private fun getTypeRank(): Int {
        return when (this.type) {
            TYPE_WIRED_HEADSET -> return 0
            TYPE_BLUETOOTH -> return 1
            TYPE_SPEAKER -> return 2
            TYPE_EARPIECE -> return 3
            TYPE_STREAMING -> return 4
            else -> 5
        }
    }
}
