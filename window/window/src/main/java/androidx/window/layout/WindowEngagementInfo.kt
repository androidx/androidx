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
package androidx.window.layout

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * Contains the window's engagement mode.
 *
 * @property engagementModes The current set of active user [EngagementMode]-s, indicating how the
 *   user is interacting with the application (e.g. visually, through audio, or both). This is
 *   designed for experiences like XR Glasses, where the presentation can change dynamically. For
 *   example, the Glasses display may turn off to transition to an audio-only experience. Apps can
 *   observe this state to adapt their behavior, such as pausing visual rendering, without this
 *   change affecting the Activity lifecycle. This ensures the user's session remains continuous and
 *   uninterrupted.
 */
public class WindowEngagementInfo
@RestrictTo(LIBRARY_GROUP)
constructor(
    /** The current user engagement modes, indicating how the user is interacting with the app. */
    public val engagementModes: Set<EngagementMode> =
        setOf(EngagementMode.VISUALS_ON, EngagementMode.AUDIO_ON) // Default
) {

    /**
     * Represents a distinct user engagement mode with an application.
     *
     * @see WindowEngagementInfo.engagementModes
     */
    public class EngagementMode private constructor(private val id: Int) {
        override fun toString(): String =
            when (id) {
                1 -> "VISUALS_ON"
                2 -> "AUDIO_ON"
                else -> "UNKNOWN($id)"
            }

        override fun equals(other: Any?): Boolean = (other is EngagementMode) && this.id == other.id

        override fun hashCode(): Int = id.hashCode()

        public companion object {
            /**
             * Indicates the engagement mode includes a visual presentation. When this mode is
             * active, the user can visually see the app UI on a visible window.
             */
            @JvmField public val VISUALS_ON: EngagementMode = EngagementMode(1)

            /**
             * Indicates the engagement mode includes an audio presentation. This can be active with
             * or without [VISUALS_ON]. When active without visuals, it signifies an audio-only
             * experience.
             */
            @JvmField public val AUDIO_ON: EngagementMode = EngagementMode(2)
        }
    }

    /**
     * Checks if a specific engagement mode is currently active.
     *
     * @param mode The [EngagementMode] to check for.
     * @return true if the mode is present in the [engagementModes] set, false otherwise.
     */
    public fun hasEngagementMode(mode: EngagementMode): Boolean {
        return engagementModes.contains(mode)
    }

    /**
     * Checks if all specified engagement modes are currently active.
     *
     * @param modes The [EngagementMode]-s to check for.
     * @return true if all specified modes are in the [engagementModes] set, false otherwise.
     */
    public fun hasEngagementModes(vararg modes: EngagementMode): Boolean {
        return engagementModes.containsAll(modes.asList())
    }

    public override fun toString(): String {
        return "WindowEngagementInfo{ engagementModes=$engagementModes }"
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WindowEngagementInfo) return false
        return engagementModes == other.engagementModes
    }

    public override fun hashCode(): Int {
        return engagementModes.hashCode()
    }
}
