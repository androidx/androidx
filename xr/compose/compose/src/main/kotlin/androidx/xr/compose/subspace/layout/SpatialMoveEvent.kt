/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose

/**
 * An event representing a change in pose, scale, and size.
 *
 * @property type The current type of the drag event.
 * @property pose The new pose of the composable in the subspace, relative to its parent, with its
 *   translation being expressed in pixels.
 * @property previousPose The pose of the composable from the previous event. When [type] is
 *   [SpatialMoveEventType.Start], this should be equal to [pose].
 * @property scale The scale of the composable as a result of its motion. This value will change
 *   with the composable's depth when scaleWithDistance is true on the modifier.
 * @property previousScale The scale of the composable from the previous event. When [type] is
 *   [SpatialMoveEventType.Start], this should be equal to [scale].
 * @property size The unscaled initial [IntVolumeSize] value that includes the width, height and
 *   depth of the composable. To get the current perceived (scaled) size, multiply this value by
 *   [scale].
 * @see transformingMovable
 * @see movable
 */
public class SpatialMoveEvent(
    public val type: SpatialMoveEventType,
    public val pose: Pose,
    public val previousPose: Pose,
    public val scale: Float,
    public val previousScale: Float,
    public val size: IntVolumeSize,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialMoveEvent) return false
        if (type != other.type) return false
        if (pose != other.pose) return false
        if (previousPose != other.previousPose) return false
        if (scale != other.scale) return false
        if (previousScale != other.previousScale) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + pose.hashCode()
        result = 31 * result + previousPose.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + previousScale.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialMoveEvent(type=$type, pose=$pose, previousPose=$previousPose, " +
            "scale=$scale, previousScale=$previousScale, size=$size)"
    }
}

/** An enum representing the phases of dragging. */
@JvmInline
public value class SpatialMoveEventType private constructor(private val value: Int) {
    public companion object {
        /** The phase where the drag event starts */
        public val Start: SpatialMoveEventType = SpatialMoveEventType(0)
        /** The phase where the user continuously drags */
        public val Moving: SpatialMoveEventType = SpatialMoveEventType(1)
        /** The phase where the drag event ends */
        public val End: SpatialMoveEventType = SpatialMoveEventType(2)
    }
}
