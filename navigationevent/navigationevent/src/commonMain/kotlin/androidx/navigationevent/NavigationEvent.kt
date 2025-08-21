/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent

import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents a system navigation event, such as a predictive back gesture or a back button press.
 *
 * This class standardizes various platform signals (e.g., touch gestures, key events) into a
 * single, consistent format. This allows handlers like [NavigationEventDispatcher] to process
 * navigation actions uniformly without needing to know the specific source of the event.
 *
 * Note that not all parameters apply to every type of navigation event. For example, [touchX] and
 * [touchY] are only relevant for gesture-based navigation.
 */
public class NavigationEvent(
    /**
     * The absolute X coordinate of the touch point for this event, in pixels, in the coordinate
     * space of the screen. For events not triggered by a touch gesture (e.g., a key press), this
     * will be `0.0F`.
     */
    @FloatRange(from = 0.0) public val touchX: Float = 0.0F,
    /**
     * The absolute Y coordinate of the touch point for this event, in pixels, in the coordinate
     * space of the screen. For events not triggered by a touch gesture (e.g., a key press), this
     * will be `0.0F`.
     */
    @FloatRange(from = 0.0) public val touchY: Float = 0.0F,
    /**
     * A normalized value from `0.0F` to `1.0F` indicating how far the navigation action has
     * progressed.
     *
     * For continuous gestures like a swipe, this value will update incrementally. For discrete
     * actions like a button press, a single event with `progress` of `0.0F` may be sent when the
     * action starts, followed by a completion signal.
     */
    @FloatRange(from = 0.0, to = 1.0) public val progress: Float = 0.0F,
    /**
     * Indicates which screen edge a swipe-based navigation gesture originates from. For non-swipe
     * events, this will be [EDGE_NONE].
     */
    public val swipeEdge: @SwipeEdge Int = EDGE_NONE,
    /**
     * The timestamp in milliseconds when this navigation event occurred. This is useful for
     * synchronizing animations or for debugging event sequences.
     */
    public val frameTimeMillis: Long = 0,
) {

    /** Defines the possible screen edges from which a swipe gesture can originate. */
    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EDGE_LEFT, EDGE_RIGHT, EDGE_NONE)
    public annotation class SwipeEdge

    public companion object {
        /** Indicates the navigation gesture originates from the left edge of the screen. */
        public const val EDGE_LEFT: Int = 0

        /** Indicates the navigation gesture originates from the right edge of the screen. */
        public const val EDGE_RIGHT: Int = 1

        /**
         * Indicates the navigation event was not caused by an edge swipe. This applies to actions
         * like a 3-button navigation press or a hardware back button event.
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
        return "NavigationEvent(touchX=$touchX, touchY=$touchY, progress=$progress, " +
            "swipeEdge=$swipeEdge, frameTimeMillis=$frameTimeMillis)"
    }
}
