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

package androidx.core.app

import android.app.PictureInPictureUiState
import android.os.Build

/**
 * A compatibility shim for the framework class [PictureInPictureUiState]. This class is safe to
 * reference across all API levels.
 */
public class PictureInPictureUiStateCompat(
    /**
     * Returns whether Picture-in-Picture is stashed or not. A stashed PiP means it is only
     * partially visible to the user, with some parts of it being off-screen. This is usually a UI
     * state that is triggered by the user, such as flinging the PiP to the edge or letting go of
     * PiP while dragging partially off-screen.
     *
     * Developers can use this in conjunction with [OnPictureInPictureUiStateChangedProvider] to get
     * a signal when the PiP stash state has changed. For example, if the state changed from false
     * to true, developers can choose to temporarily pause video playback if PiP is of video
     * content. Vice versa, if changing from true to false and video content is paused, developers
     * can resume video playback.
     *
     * Compatibility notes: this value is meaningful on API 31+
     */
    public val isStashed: Boolean,
    /**
     * Returns true if the app is going to enter Picture-in-Picture (PiP) mode.
     *
     * This state is associated with the entering PiP animation. When that animation starts, whether
     * via auto enter PiP or calling [ComponentActivity.enterPictureInPictureMode] explicitly, app
     * can expect [ComponentActivity.onPictureInPictureUiStateChanged] callback with
     * [isTransitioningToPip] to be true first, followed by
     * [ComponentActivity.onPictureInPictureModeChanged] when it fully settles in PiP mode.
     *
     * When app receives the [ComponentActivity.onPictureInPictureUiStateChanged] callback with
     * [isTransitioningToPip] being true, it's recommended to hide certain UI elements, such as
     * video controls, to archive a clean entering PiP animation.
     *
     * In case an application wants to restore the previously hidden UI elements when exiting PiP,
     * it is recommended to do that in [ComponentActivity.onPictureInPictureUiStateChanged] callback
     * when [isTransitioningToPip] is false, rather than the beginning of exit PiP animation.
     *
     * Compatibility notes: this value is meaningful on API 35+
     */
    public val isTransitioningToPip: Boolean,
) {

    /**
     * Factory method to safely create a PictureInPictureUiStateCompat instance from the framework's
     * [PictureInPictureUiState] object.
     */
    public companion object {
        @JvmStatic
        public fun fromPictureInPictureUiState(
            uiState: PictureInPictureUiState
        ): PictureInPictureUiStateCompat {
            // Construct PictureInPictureUiStateCompat class based on API levels.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return PictureInPictureUiStateCompat(
                    isStashed = uiState.isStashed,
                    isTransitioningToPip = uiState.isTransitioningToPip,
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return PictureInPictureUiStateCompat(
                    isStashed = uiState.isStashed,
                    isTransitioningToPip = false,
                )
            }

            // Fallback for API < 31, where these properties don't exist.
            return PictureInPictureUiStateCompat(isStashed = false, isTransitioningToPip = false)
        }
    }
}
