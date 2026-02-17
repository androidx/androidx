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

package androidx.compose.ui.uikit

import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.isClearFocusOnMouseDownEnabled

/**
 * Base configuration of the Compose container.
 */
sealed class ComposeContainerConfiguration {
    /**
     * Control Compose behaviour on focus changed inside Compose.
     */
    var onFocusBehavior: OnFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard

    /**
     * Determines whether the Compose view should have an opaque background.
     * Warning: disabling opaque layer may affect performance.
     */
    @ExperimentalComposeUiApi
    var opaque: Boolean = true

    /**
     * A boolean flag to enable or disable the strict sanity check for the `Info.plist` file.
     * If the flag is set to true, and keys are missing, the app will crash with an
     * explanation on how to fix the issue.
     */
    var enforceStrictPlistSanityCheck: Boolean = true

    /**
     * If set to true, the Compose will encode the rendering commands on a dedicated render thread,
     * when possible. This can improve the performance.
     *
     * This API is intended as a backup solution in case there are issues with parallel rendering.
     *
     * Changing this setting outside of `configure` argument scope has no effect.
     */
    @ExperimentalComposeUiApi
    var parallelRendering: Boolean = true

    /**
     * Determines how the end edge pan gestures will be handled.
     * In LTR layouts, the end edge is the right edge of the screen.
     * In RTL layouts, the end edge is the left edge of the screen.
     *
     * Note: this setting only affects the behavior of the end edge pan gestures.
     * The start edge pan gestures will always be handled as back navigation events.
     *
     * Default value is [EndEdgePanGestureBehavior.Disabled].
     */
    @ExperimentalComposeUiApi
    var endEdgePanGestureBehavior: EndEdgePanGestureBehavior = EndEdgePanGestureBehavior.Disabled

    /**
     * Controls whether a mouse/trackpad clicks on an unfocusable element clear focus.
     */
    @ExperimentalComposeUiApi
    var isClearFocusOnMouseDownEnabled: Boolean = ComposeUiFlags.isClearFocusOnMouseDownEnabled
}

/**
 * Specifies behaviour on focus changed inside Compose.
 */
sealed interface OnFocusBehavior {
    /**
     * The Compose view will stay on the current position.
     */
    @Suppress("unused")
    data object DoNothing : OnFocusBehavior

    /**
     * The Compose view will be panned in "y" coordinates.
     * A focusable element should be displayed above the keyboard.
     */
    data object FocusableAboveKeyboard : OnFocusBehavior
}

/**
 * Determines how the end edge pan gestures will be handled.
 * In LTR layouts, the end edge is the right edge of the screen.
 * In RTL layouts, the end edge is the left edge of the screen.
 */
@ExperimentalComposeUiApi
sealed interface EndEdgePanGestureBehavior {
    /**
     * No navigation events will be sent on the end edge.
     */
    data object Disabled : EndEdgePanGestureBehavior

    /**
     * Back navigation events will be sent on the end edge.
     */
    data object Back : EndEdgePanGestureBehavior

    /**
     * Forward navigation events will be sent on the end edge.
     */
    data object Forward : EndEdgePanGestureBehavior
}
