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

package androidx.navigationevent

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Common event used to wrap signals from the platform so that they can be handled properly by the
 * [NavigationEventDispatcher]
 */
public class NavigationEvent(
    /**
     * Absolute X location of the touch point of this event in the coordinate space of the screen
     * that received this navigation event.
     */
    public val touchX: Float,
    /**
     * Absolute Y location of the touch point of this event in the coordinate space of the screen
     * that received this navigation event.
     */
    public val touchY: Float,
    /** Value between 0 and 1 on how far along the back gesture is. */
    public val progress: Float,
    /** Indicates which edge the swipe starts from. */
    public val swipeEdge: @SwipeEdge Int,
    /** Frame time of the navigation event. */
    public val frameTimeMillis: Long = 0,
) {

    /**  */
    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDGE_LEFT, EDGE_RIGHT, EDGE_NONE)
    public annotation class SwipeEdge

    public companion object {
        /** Indicates that the edge swipe starts from the left edge of the screen */
        public const val EDGE_LEFT: Int = 0

        /** Indicates that the edge swipe starts from the right edge of the screen */
        public const val EDGE_RIGHT: Int = 1

        /**
         * Indicates that the back event was not triggered by an edge swipe back gesture. This
         * applies to cases like using the back button in 3-button navigation or pressing a hardware
         * back button.
         */
        public const val EDGE_NONE: Int = 2
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavigationEvent

        if (touchX != other.touchX) return false
        if (touchY != other.touchY) return false
        if (progress != other.progress) return false
        if (swipeEdge != other.swipeEdge) return false
        if (frameTimeMillis != other.frameTimeMillis) return false

        return true
    }

    override fun hashCode(): Int {
        var result = touchX.hashCode()
        result = 31 * result + touchY.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + swipeEdge
        result = 31 * result + frameTimeMillis.hashCode()
        return result
    }

    override fun toString(): String {
        return "NavigationEvent(touchX=$touchX, touchY=$touchY, progress=$progress, swipeEdge=$swipeEdge, frameTimeMillis=$frameTimeMillis)"
    }
}
