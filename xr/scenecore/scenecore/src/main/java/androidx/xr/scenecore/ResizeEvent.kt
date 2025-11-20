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

import androidx.xr.runtime.math.FloatSize3d

/**
 * A resize event which is sent in response to the User interacting with the [ResizableComponent].
 *
 * @param entity The [Entity] being resized.
 * @param resizeState The state of the resize event.
 * @param newSize The new proposed size of the Entity in local space.
 */
public class ResizeEvent(
    public val entity: Entity,
    public val resizeState: ResizeState,
    public val newSize: FloatSize3d,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResizeEvent) return false

        if (entity != other.entity) return false
        if (resizeState != other.resizeState) return false
        if (newSize != other.newSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + resizeState.hashCode()
        result = 31 * result + newSize.hashCode()
        return result
    }

    public class ResizeState private constructor(private val name: String) {

        public companion object {
            /** The resize state is unknown. */
            @JvmField public val UNKNOWN: ResizeState = ResizeState("UNKNOWN")

            /** The user has started dragging the resize handles. */
            @JvmField public val START: ResizeState = ResizeState("START")

            /** The user is continuing to drag the resize handles. */
            @JvmField public val ONGOING: ResizeState = ResizeState("ONGOING")

            /** The user has stopped dragging the resize handles. */
            @JvmField public val END: ResizeState = ResizeState("END")
        }

        override fun toString(): String = name
    }
}
