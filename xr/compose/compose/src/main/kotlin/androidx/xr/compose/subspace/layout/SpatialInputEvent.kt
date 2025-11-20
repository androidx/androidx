/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent.Action
import androidx.xr.scenecore.InputEvent.Pointer
import androidx.xr.scenecore.InputEvent.Source

/**
 * Defines Spatial input events for Compose, representing a user's input in 3D space. These are
 * fired when an [InteractionPolicy] is attached to a Composable.
 *
 * @param source Type of source (e.g. hands, controller, head) that generated this event.
 * @param pointerType Type of the individual pointer (e.g. left, right or default) for this event.
 * @param timestamp Timestamp from
 *   [SystemClock.uptimeMillis](https://developer.android.com/reference/android/os/SystemClock#uptimeMillis())
 *   time base.
 * @param hitPosition The offset of the point of interaction, in pixels, relative to the center of
 *   the Composable this input targets. This is null if an input ray no longer hits the Composable
 *   during an [Action] sequence.
 * @param origin The origin of the ray in the receiver's activity space, in pixels.
 * @param direction A point indicating the direction the ray is pointing, in the receiver's activity
 *   space, in pixels.
 * @param action Actions similar to Android's
 *   [MotionEvent](https://developer.android.com/reference/android/view/MotionEvent) for keeping
 *   track of a sequence of events on the same target, e.g., HOVER_ENTER -> HOVER_MOVE ->
 *   HOVER_EXIT, DOWN -> MOVE -> UP.
 */
public class SpatialInputEvent(
    public val source: Source,
    public val pointerType: Pointer,
    public val action: Action,
    public val timestamp: Long,
    public val hitPosition: Vector3?,
    public val origin: Vector3,
    public val direction: Vector3,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialInputEvent) return false

        if (source != other.source) return false
        if (pointerType != other.pointerType) return false
        if (action != other.action) return false
        if (timestamp != other.timestamp) return false
        if (hitPosition == other.hitPosition) return false
        if (origin != other.origin) return false
        if (direction != other.direction) return false
        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + pointerType.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + hitPosition.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialInputEvent(source=$source, pointerType=$pointerType, action=$action, timestamp=$timestamp, hitPosition=$hitPosition, origin=$origin, direction=$direction)"
    }
}
