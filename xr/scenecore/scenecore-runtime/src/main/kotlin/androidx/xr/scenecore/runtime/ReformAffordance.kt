/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo

/** Used to create a geometry reform affordance bitmask. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class ReformAffordanceFlag private constructor(private val bit: Int) {
    /**
     * Returns whether the passed mask has this affordance flag enabled
     *
     * @param mask The bit flag mask to check.
     * @return True if the bit flag is enabled, returns false otherwise.
     */
    public fun isSet(mask: Int): Boolean = (mask and bit) != 0

    /**
     * Returns an updated mask by overwriting the designated bit based on enabled parameter
     *
     * @param mask The bit flag mask to update.
     * @param enabled Boolean value to update the bit flag with.
     * @return The updated bit flag mask.
     */
    public fun setEnabled(mask: Int, enabled: Boolean): Int {
        val value = if (enabled) bit else 0
        return (mask and bit.inv()) or value
    }

    public fun toInt(): Int = bit

    public infix fun or(other: ReformAffordanceFlag): ReformAffordanceFlag =
        ReformAffordanceFlag(bit or other.bit)

    public infix fun and(other: ReformAffordanceFlag): ReformAffordanceFlag =
        ReformAffordanceFlag(bit and other.bit)

    public companion object {
        public val NONE: ReformAffordanceFlag = ReformAffordanceFlag(0)
        public val MOVABLE: ReformAffordanceFlag = ReformAffordanceFlag(1 shl 0)
        public val SYSTEM_MOVABLE: ReformAffordanceFlag = ReformAffordanceFlag(1 shl 1)
        public val RESIZABLE: ReformAffordanceFlag = ReformAffordanceFlag(1 shl 2)
        public val ROTATABLE: ReformAffordanceFlag = ReformAffordanceFlag(1 shl 3)
        public val SNAPPABLE: ReformAffordanceFlag = ReformAffordanceFlag(1 shl 4)

        public fun anySet(mask: Int): Boolean = mask != 0
    }
}

/**
 * This represents the possible SceneViewerXR interaction states. Order here matches SceneViewerXR
 * interactions state machine.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class GeometryAffordanceState private constructor(private val state: Int) {
    public companion object {
        public val NONE: GeometryAffordanceState = GeometryAffordanceState(-1)
        public val INITIALIZED: GeometryAffordanceState = GeometryAffordanceState(0)
        public val IDLE: GeometryAffordanceState = GeometryAffordanceState(1)
        public val TRANSLATION: GeometryAffordanceState = GeometryAffordanceState(2)
        public val ROTATION: GeometryAffordanceState = GeometryAffordanceState(3)
        public val ONE_HANDED_SCALE: GeometryAffordanceState = GeometryAffordanceState(4)
        public val TWO_HANDED_SCALE: GeometryAffordanceState = GeometryAffordanceState(5)

        public fun fromInt(state: Int): GeometryAffordanceState {
            return when (state) {
                0 -> INITIALIZED
                1 -> IDLE
                2 -> TRANSLATION
                3 -> ROTATION
                4 -> ONE_HANDED_SCALE
                5 -> TWO_HANDED_SCALE
                else -> NONE
            }
        }
    }
}
