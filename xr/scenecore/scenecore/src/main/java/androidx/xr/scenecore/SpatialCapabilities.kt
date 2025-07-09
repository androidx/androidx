/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    flag = true,
    value =
        [
            SpatialCapabilities.SPATIAL_CAPABILITY_UI,
            SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT,
            SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL,
            SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT,
            SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO,
            SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY,
        ],
)
internal annotation class SpatialCapability

/** Representation of the spatial capabilities of the current [Scene]. */
public class SpatialCapabilities
internal constructor(@SpatialCapability private val capabilities: Int) {

    public companion object {
        /** The activity can spatialize itself by e.g. adding a spatial panel. */
        public const val SPATIAL_CAPABILITY_UI: Int = 1 shl 0

        /** The activity can create 3D content. */
        public const val SPATIAL_CAPABILITY_3D_CONTENT: Int = 1 shl 1

        /** The activity can enable or disable passthrough. */
        public const val SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL: Int = 1 shl 2

        /** The activity can set its own spatial environment. */
        public const val SPATIAL_CAPABILITY_APP_ENVIRONMENT: Int = 1 shl 3

        /** The activity can use spatial audio. */
        public const val SPATIAL_CAPABILITY_SPATIAL_AUDIO: Int = 1 shl 4

        /** The activity can spatially embed another activity. */
        public const val SPATIAL_CAPABILITY_EMBED_ACTIVITY: Int = 1 shl 5
    }

    override fun toString(): String {
        if (capabilities == 0) {
            return "SpatialCapabilities(NONE)"
        }

        val enabledCapabilities = mutableListOf<String>()
        if (hasCapability(SPATIAL_CAPABILITY_UI)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_UI")
        }
        if (hasCapability(SPATIAL_CAPABILITY_3D_CONTENT)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_3D_CONTENT")
        }
        if (hasCapability(SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL")
        }
        if (hasCapability(SPATIAL_CAPABILITY_APP_ENVIRONMENT)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_APP_ENVIRONMENT")
        }
        if (hasCapability(SPATIAL_CAPABILITY_SPATIAL_AUDIO)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_SPATIAL_AUDIO")
        }
        if (hasCapability(SPATIAL_CAPABILITY_EMBED_ACTIVITY)) {
            enabledCapabilities.add("SPATIAL_CAPABILITY_EMBED_ACTIVITY")
        }
        return "SpatialCapabilities(${enabledCapabilities.joinToString(" | ")})"
    }

    /**
     * Checks if one or more specified capabilities are available.
     *
     * This method tests if **all** of the provided capability flags are set.
     *
     * ### Usage Examples
     *
     * **1. Checking for a single capability:**
     *
     * ```
     * if (capabilities.hasCapability(SPATIAL_CAPABILITY_UI)) { // The session supports UI. }
     * ```
     *
     * **2. Checking if *all* of a set of capabilities are available:** To check if all capabilities
     * from a set is available, combine the flags using a bitwise `or`.
     *
     * ```
     *   if (capabilities.hasCapability(SPATIAL_CAPABILITY_UI or SPATIAL_CAPABILITY_3D_CONTENT)) {
     *       // The session supports both UI and 3D content.
     *   }
     * ```
     *
     * @param capability The capability flag to check. This can be a single `SPATIAL_CAPABILITY_*`
     *   constant or multiple constants combined with a bitwise `or`.
     * @return `true` if all of the specified capabilities is available, `false` otherwise.
     */
    public fun hasCapability(@SpatialCapability capability: Int): Boolean =
        (capabilities and capability) == capability

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialCapabilities) return false

        if (capabilities != other.capabilities) return false

        return true
    }

    override fun hashCode(): Int {
        return capabilities
    }
}
