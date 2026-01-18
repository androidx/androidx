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

package androidx.window.area

import androidx.annotation.RestrictTo
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.layout.WindowMetrics

/**
 * The current state of a window area. The [WindowArea] can represent a part of or an entire display
 * in the system. These values can be used to modify the UI to show/hide controls and determine when
 * features can be enabled.
 */
public class WindowArea
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(

    /**
     * The [WindowMetrics] that represent the size of the area. Used to determine if the behavior
     * desired fits the size of the window area available.
     */
    public val windowMetrics: WindowMetrics,

    /** The [Type] of this window area */
    public val type: Type,

    /** [WindowAreaToken] token to identify the specific WindowArea */
    public val token: WindowAreaToken,

    /**
     * Map of [WindowAreaCapability.Operation]s to their status objects that this [WindowArea]
     * currently supports
     */
    internal val capabilityMap: Map<WindowAreaCapability.Operation, WindowAreaCapability>,
) {

    /**
     * Returns the [WindowAreaCapability] corresponding to the [operation] provided. If this
     * [WindowAreaCapability] does not exist for this [WindowArea], a [WindowAreaCapability] with a
     * [WINDOW_AREA_STATUS_UNSUPPORTED] value is returned.
     */
    public fun getCapability(operation: WindowAreaCapability.Operation): WindowAreaCapability {
        return capabilityMap[operation]
            ?: WindowAreaCapability(operation, WINDOW_AREA_STATUS_UNSUPPORTED)
    }

    /** Represents a type of [WindowArea] */
    public class Type private constructor(private val rawValue: Int) {
        override fun toString(): String {
            return when (this) {
                TYPE_REAR_FACING -> "REAR FACING"
                else -> "UNKNOWN"
            }
        }

        public companion object {
            /**
             * Type of window area that is facing the same direction as the rear camera(s) on the
             * device.
             */
            @JvmField public val TYPE_REAR_FACING: Type = Type(0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is WindowArea &&
            windowMetrics == other.windowMetrics &&
            token == other.token &&
            type == other.type &&
            capabilityMap.entries == other.capabilityMap.entries
    }

    override fun hashCode(): Int {
        var result = windowMetrics.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + capabilityMap.entries.hashCode()
        return result
    }

    override fun toString(): String {
        return "WindowAreaInfo{ Metrics: $windowMetrics, type: $type, " +
            "Capabilities: ${capabilityMap.entries} }"
    }
}
