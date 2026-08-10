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
 * 1. [onInitContentPip], app can return the PiP eligibility
 * 2. [onPrepareContentPip], app prepares for the handoff
 * 3. [onAttachContentPip], app attaches the content to the new Activity (that will enter PiP)
 * 4. [onFinishContentPip], app is notified that content PiP is finished
 */
public interface ContentPipCallback {
    /**
     * Called before attempting to start the Content PiP solution.
     *
     * @return `true` if the application is eligible to enter PiP. If `false` is returned, the PiP
     *   flow is canceled.
     */
    public fun onInitContentPip(): Boolean

    /**
     * Called on the main Activity to prepare for the handoff (e.g., detaching a Player from its
     * View).
     *
     * @return `true` if the handoff is successful on the main Activity. If `false` is returned, the
     *   PiP flow is canceled.
     */
    public fun onPrepareContentPip(): Boolean

    /**
     * Called once the proxy PiP Activity is ready. The app should attach its content (e.g., a
     * Player) to this new [pipActivity].
     *
     * @param pipActivity The new Activity serving as the PiP container.
     */
    public fun onAttachContentPip(pipActivity: ComponentActivity)

    /**
     * Called when the PiP task is finishing or the enter PiP attempt is failed.
     *
     * For instance, a video app can use [isDismissed] to determine if it needs to stop playback.
     * This is `true` when the user dismisses or closes the PiP, and `false` when the user expands
     * the PiP to full-screen mode.
     *
     * @param isDismissed whether the PiP task is explicitly dismissed by the user.
     */
    public fun onFinishContentPip(isDismissed: Boolean)
}
