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
import androidx.compose.runtime.Immutable
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_BACK
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_FORWARD

/**
 * Represents the physical state of a navigation gesture, such as a predictive back or forward
 * swipe.
 *
 * This object explicitly separates the gesture's transition state (i.e., whether a gesture is
 * active and its progress) from the navigation history state (i.e., *what* destinations are on the
 * stack).
 *
 * The state is either [Idle] (no gesture is active) or [InProgress] (a gesture is actively being
 * tracked).
 */
@Immutable
public sealed class NavigationEventTransitionState {

    /**
     * Represents the state where no navigation gesture is currently in progress. This is the
     * default state.
     */
    public object Idle : NavigationEventTransitionState() {

        override fun toString(): String {
            return "Idle()"
        }
    }

    /**
     * Represents the state where a navigation gesture is actively in progress.
     *
     * This state is entered when a gesture begins (e.g., `onBackStarted`) and is updated with new
     * events (e.g., `onBackProgressed`) until the gesture is either completed or cancelled, at
     * which point the state returns to [Idle].
     *
     * @param latestEvent The most recent [NavigationEvent] in the gesture sequence, containing
     *   details like touch position and progress (from 0.0 to 1.0).
     * @param direction The direction of the transition, either [TRANSITIONING_FORWARD] or
     *   [TRANSITIONING_BACK].
     */
    public class InProgress(
        public val latestEvent: NavigationEvent,
        @get:Direction @param:Direction public val direction: Int,
    ) : NavigationEventTransitionState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as InProgress

            if (direction != other.direction) return false
            if (latestEvent != other.latestEvent) return false

            return true
        }

        override fun hashCode(): Int {
            var result = direction
            result = 31 * result + latestEvent.hashCode()
            return result
        }

        override fun toString(): String {
            return "InProgress(latestEvent=$latestEvent, direction=$direction)"
        }
    }

    /**
     * Annotation for defining the direction of a navigation transition.
     *
     * @see TRANSITIONING_FORWARD
     * @see TRANSITIONING_BACK
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
    )
    @IntDef(TRANSITIONING_UNKNOWN, TRANSITIONING_FORWARD, TRANSITIONING_BACK)
    public annotation class Direction

    public companion object {

        /** The transition state is unknown or has not been specified. */
        internal const val TRANSITIONING_UNKNOWN: Int = 0

        /** Forward transition in progress. */
        public const val TRANSITIONING_FORWARD: Int = 1

        /** Back transition in progress. */
        public const val TRANSITIONING_BACK: Int = -1
    }
}
