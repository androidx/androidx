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
 * Represents the directional state of system navigation gestures, such as predictive back.
 *
 * A state is either:
 * - [Idle]: no gesture is currently active, the UI is settled.
 * - [InProgress]: a navigation gesture is ongoing.
 *
 * Each state exposes three pieces of contextual information contributed by the active handler:
 * - [backInfo]: contextual information describing what is available when navigating back.
 * - [currentInfo]: the single item representing the active destination.
 * - [forwardInfo]: contextual information describing what is available when navigating forward.
 *
 * These values do not represent literal back or forward stacks. Instead, they carry app-defined
 * [NavigationEventInfo] used to inform the UI (for example, showing titles, previews, or
 * affordances).
 *
 * @param T The type of contextual information associated with the navigation state.
 */
public sealed class NavigationEventState<out T : NavigationEventInfo> {

    /**
     * Contextual information describing the application's *back* state for this snapshot.
     *
     * This is **not** a back stack. The list contains app-defined [NavigationEventInfo] elements
     * (e.g., titles or metadata) that help render back affordances in the UI. Items are ordered
     * nearest-first (most relevant first) and the list may be empty.
     *
     * In nested hierarchies, this list may include contributions from the active handler and its
     * enabled ancestors (nearest ancestor first), per the back merge policy.
     */
    public abstract val backInfo: List<T>

    /**
     * Information about the current [NavigationEventState].
     *
     * In the [Idle] state, this represents the settled UI state. In the [InProgress] state, this
     * represents the UI state the gesture is navigating towards.
     */
    public abstract val currentInfo: T

    /**
     * The forward-facing contextual information for this snapshot.
     *
     * This is **not** a forward stack. The list contains app-defined [NavigationEventInfo] elements
     * that help render forward affordances in the UI. Items are ordered nearest-first and the list
     * may be empty.
     *
     * Unlike [backInfo], this list is never merged across ancestors; it reflects only the active
     * handler.
     */
    public abstract val forwardInfo: List<T>

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
     * @property currentInfo The contextual information representing the active destination.
     * @property backInfo Contextual information describing what is available when navigating back.
     *   May be empty if no back navigation is possible.
     * @property forwardInfo Contextual information describing what is available when navigating
     *   forward. May be empty if no forward navigation is possible.
     */
    public class Idle<out T : NavigationEventInfo>
    @PublishedApi
    internal constructor(
        override val currentInfo: T,
        override val backInfo: List<T> = emptyList(),
        override val forwardInfo: List<T> = emptyList(),
    ) : NavigationEventState<T>() {

        override fun toString(): String {
            return "Idle(currentInfo=$currentInfo, backInfo=$backInfo, forwardInfo=$forwardInfo)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Idle<*>

            if (backInfo != other.backInfo) return false
            if (currentInfo != other.currentInfo) return false
            if (forwardInfo != other.forwardInfo) return false

            return true
        }

        override fun hashCode(): Int {
            var result = backInfo.hashCode()
            result = 31 * result + currentInfo.hashCode()
            result = 31 * result + forwardInfo.hashCode()
            return result
        }
    }

    /**
     * A navigation gesture is actively in progress.
     *
     * @property currentInfo The contextual information representing the destination the gesture is
     *   navigating towards.
     * @property backInfo Contextual information describing what is available when navigating back.
     *   May be empty if no back navigation is possible.
     * @property forwardInfo Contextual information describing what is available when navigating
     *   forward. May be empty if no forward navigation is possible.
     * @property latestEvent The latest [NavigationEvent] in the gesture sequence, containing
     *   details like touch position and progress.
     */
    public class InProgress<out T : NavigationEventInfo>
    internal constructor(
        override val currentInfo: T,
        override val backInfo: List<T> = emptyList(),
        override val forwardInfo: List<T> = emptyList(),
        public val latestEvent: NavigationEvent,
    ) : NavigationEventState<T>() {

        override fun toString(): String {
            return "InProgress(currentInfo=$currentInfo, backInfo=$backInfo, forwardInfo=$forwardInfo, latestEvent=$latestEvent)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as InProgress<*>

            if (backInfo != other.backInfo) return false
            if (currentInfo != other.currentInfo) return false
            if (forwardInfo != other.forwardInfo) return false
            if (latestEvent != other.latestEvent) return false

            return true
        }

        override fun hashCode(): Int {
            var result = backInfo.hashCode()
            result = 31 * result + currentInfo.hashCode()
            result = 31 * result + forwardInfo.hashCode()
            result = 31 * result + latestEvent.hashCode()
            return result
        }
    }
}
