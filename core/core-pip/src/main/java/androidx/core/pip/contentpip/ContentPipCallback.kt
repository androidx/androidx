/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.pip.contentpip

import androidx.activity.ComponentActivity

/**
 * A callback interface for managing the handoff of a content between the main activity and the
 * Content PiP.
 *
 * After registering [ContentPipCallback] via [enablePipOnAppSwitch], application can expect the
 * callbacks in the following order
 * 1. [onInit], app can return the PiP eligibility
 * 2. [onPrepare], app prepares for the handoff
 * 3. [onAttach], app attaches the content to the new Activity (that will enter PiP)
 * 4. [onFinish], app is notified that content PiP is finished, or [onError] if the Content PiP
 *    fails
 */
public interface ContentPipCallback {
    /**
     * Called before attempting to start the Content PiP solution.
     *
     * @return `true` if the application is eligible to enter PiP. If `false` is returned, the PiP
     *   flow is canceled.
     */
    public fun onInit(): Boolean

    /**
     * Called on the main Activity to prepare for the handoff (e.g., detaching a Player from its
     * View).
     *
     * @return `true` if the handoff is successful on the main Activity. If `false` is returned, the
     *   PiP flow is canceled.
     */
    public fun onPrepare(): Boolean

    /**
     * Called once the proxy PiP Activity is ready. The app should attach its content (e.g., a
     * Player) to this new [pipActivity].
     *
     * @param pipActivity The new Activity serving as the PiP container.
     */
    public fun onAttach(pipActivity: ComponentActivity)

    /**
     * Called when the PiP task is finishing. If the Content PiP fails, for instance, the attempt to
     * enter PiP fails, [onError] is called instead.
     *
     * For instance, a video app can use [reason] to determine if it needs to stop playback. It can
     * stop the playback on [FinishReason.DISMISSED], and continue the playback in the main Activity
     * on [FinishReason.RESTORED].
     *
     * @param reason the reason why the Content PiP is finished, see [FinishReason].
     */
    public fun onFinish(reason: FinishReason)

    /**
     * Represents the reason why the Content PiP is finished, see [onFinish].
     *
     * New reasons may be added in the future, so apps should handle unknown reasons, for instance,
     * with an `else` branch in `when`.
     */
    public class FinishReason private constructor(private val value: Int) {
        override fun toString(): String {
            return when (value) {
                VALUE_DISMISSED -> "PiP is dismissed"
                VALUE_RESTORED -> "PiP is restored"
                else -> "Unknown reason"
            }
        }

        public companion object {
            private const val VALUE_UNKNOWN = 0
            private const val VALUE_DISMISSED = 1
            private const val VALUE_RESTORED = 2

            /** The reason is unknown or not covered by the other reasons. */
            @JvmField public val UNKNOWN: FinishReason = FinishReason(VALUE_UNKNOWN)
            /** The user dismissed or closed the PiP. */
            @JvmField public val DISMISSED: FinishReason = FinishReason(VALUE_DISMISSED)
            /** The user expanded the PiP to full-screen mode. */
            @JvmField public val RESTORED: FinishReason = FinishReason(VALUE_RESTORED)
        }
    }

    /**
     * Called when the Content PiP fails, for instance, the attempt to enter PiP fails. This is
     * called instead of [onFinish].
     *
     * Note that [onAttach] has been called before this callback, so the app should reclaim the
     * content (e.g., a Player) from the PiP Activity, which is about to finish.
     *
     * @param throwable the exception that describes the failure, for instance, an
     *   [IllegalStateException] if the system rejected the request to enter PiP.
     */
    public fun onError(throwable: Throwable)
}
