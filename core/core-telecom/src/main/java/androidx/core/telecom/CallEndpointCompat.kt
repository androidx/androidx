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
import androidx.core.telecom.internal.utils.EndpointUtils
import java.util.Objects
import java.util.UUID

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
) {
    internal var mMackAddress: String = "-1"

    override fun toString(): String {
        return "CallEndpoint(" +
            "name=[$name]," +
            "type=[${EndpointUtils.endpointTypeToString(type)}]," +
            "identifier=[$identifier])"
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
    }

    internal constructor(
        name: String,
        @EndpointType type: Int
    ) : this(name, type, ParcelUuid(UUID.randomUUID())) {}

    internal constructor(
        name: String,
        @EndpointType type: Int,
        address: String
    ) : this(name, type) {
        mMackAddress = address
    }
}
