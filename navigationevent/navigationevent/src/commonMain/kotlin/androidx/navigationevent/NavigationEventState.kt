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

/**
 * Represents the state of a system navigation gesture, like a predictive back swipe. It's either
 * [Idle] (no gesture in progress) or [InProgress] (a gesture is active).
 *
 * @param T The type of optional information associated with the navigation event. This is typically
 *   provided by the UI layer to give context about the navigation state.
 */
public sealed class NavigationEventState<out T : NavigationEventInfo> {

    /**
     * Information about the current [NavigationEventState].
     *
     * In the [Idle] state, this represents the settled UI state. In the [InProgress] state, this
     * represents the UI state the gesture is navigating towards.
     */
    public abstract val currentInfo: T

    /**
     * The progress of the current navigation gesture, typically from 0.0f to 1.0f.
     *
     * Returns `0f` when the state is [Idle]. When the state is [InProgress], it reflects the
     * completion progress of the ongoing gesture from its `latestEvent`.
     */
    public val progress: Float
        get() {
            return when (this) {
                is Idle -> 0f
                is InProgress -> latestEvent.progress
            }
        }

    /**
     * The UI is settled, and no navigation gesture is currently active.
     *
     * @property currentInfo Information about the current UI state.
     */
    public class Idle<out T : NavigationEventInfo>
    @PublishedApi
    internal constructor(override val currentInfo: T) : NavigationEventState<T>() {

        override fun toString(): String {
            return "Idle(currentInfo=$currentInfo)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Idle<*>) return false
            return currentInfo == other.currentInfo
        }

        override fun hashCode(): Int {
            return currentInfo.hashCode()
        }
    }

    /**
     * A navigation gesture is actively in progress.
     *
     * @property currentInfo The UI state the gesture is navigating towards.
     * @property previousInfo The UI state the gesture is navigating away from.
     * @property latestEvent The latest [NavigationEvent] in the gesture sequence, containing
     *   details like touch position and progress.
     */
    public class InProgress<out T : NavigationEventInfo>
    internal constructor(
        override val currentInfo: T,
        public val previousInfo: T?,
        public val latestEvent: NavigationEvent,
    ) : NavigationEventState<T>() {

        override fun toString(): String {
            return "InProgress(currentInfo=$currentInfo, previousInfo=$previousInfo, " +
                "latestEvent=$latestEvent)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InProgress<*>) return false

            if (currentInfo != other.currentInfo) return false
            if (previousInfo != other.previousInfo) return false
            if (latestEvent != other.latestEvent) return false

            return true
        }

        override fun hashCode(): Int {
            var result = currentInfo.hashCode()
            result = 31 * result + (previousInfo?.hashCode() ?: 0)
            result = 31 * result + latestEvent.hashCode()
            return result
        }
    }
}
