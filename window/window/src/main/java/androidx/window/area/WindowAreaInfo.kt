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

import android.os.Binder
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_PRESENT_ON_AREA
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_ACTIVITY_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.layout.WindowMetrics

/**
 * The current state of a window area. The [WindowAreaInfo] can represent a part of or an entire
 * display in the system. These values can be used to modify the UI to show/hide controls and
 * determine when features can be enabled.
 */
@ExperimentalWindowApi
class WindowAreaInfo internal constructor(

    /**
     * The [WindowMetrics] that represent the size of the area. Used to determine if the behavior
     * desired fits the size of the window area available.
     */
    var metrics: WindowMetrics,

    /**
     * The [Type] of this window area
     */
    val type: Type,

    /**
     * [Binder] token to identify the specific WindowArea
     */
    val token: Binder,

    private val windowAreaComponent: WindowAreaComponent
) {

    internal val capabilityMap = HashMap<WindowAreaCapability.Operation, WindowAreaCapability>()

    /**
     * Returns the [WindowAreaCapability] corresponding to the [operation] provided. If this
     * [WindowAreaCapability] does not exist for this [WindowAreaInfo], a [WindowAreaCapability]
     * with a [WINDOW_AREA_STATUS_UNSUPPORTED] value is returned.
     */
    fun getCapability(operation: WindowAreaCapability.Operation): WindowAreaCapability {
        return capabilityMap[operation] ?: WindowAreaCapability(
            operation,
            WINDOW_AREA_STATUS_UNSUPPORTED
        )
    }

    /**
     * Returns the current active [WindowAreaSession] is one is currently active for the provided
     * [operation]
     *
     * @throws IllegalStateException if there is no active session for the provided [operation]
     */
    fun getActiveSession(operation: WindowAreaCapability.Operation): WindowAreaSession? {
        if (getCapability(operation).status != WINDOW_AREA_STATUS_ACTIVE) {
            throw IllegalStateException("No session is currently active")
        }

        if (type == Type.TYPE_REAR_FACING) {
            // TODO(b/273807246) We should cache instead of always creating a new session
            return createRearFacingSession(operation)
        }
        return null
    }

    private fun createRearFacingSession(
        operation: WindowAreaCapability.Operation
    ): WindowAreaSession {
        return when (operation) {
            OPERATION_TRANSFER_ACTIVITY_TO_AREA -> RearDisplaySessionImpl(windowAreaComponent)
            OPERATION_PRESENT_ON_AREA ->
                RearDisplayPresentationSessionPresenterImpl(
                    windowAreaComponent,
                    windowAreaComponent.rearDisplayPresentation!!
                )
            else -> {
                throw IllegalArgumentException("Invalid operation provided")
            }
        }
    }

    /**
     * Represents a type of [WindowAreaInfo]
     */
    @ExperimentalWindowApi
    class Type private constructor(private val description: String) {
        override fun toString(): String {
            return description
        }

        companion object {
            /**
             * Type of window area that is facing the same direction as the rear camera(s) on the
             * device.
             */
            @JvmField
            val TYPE_REAR_FACING = Type("REAR FACING")
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is WindowAreaInfo &&
            metrics == other.metrics &&
            type == other.type &&
            capabilityMap.entries == other.capabilityMap.entries
    }

    override fun hashCode(): Int {
        var result = metrics.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + capabilityMap.entries.hashCode()
        return result
    }

    override fun toString(): String {
        return "WindowAreaInfo{ Metrics: $metrics, type: $type, " +
            "Capabilities: ${capabilityMap.entries} }"
    }
}
